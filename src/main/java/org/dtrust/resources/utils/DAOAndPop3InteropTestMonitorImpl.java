package org.dtrust.resources.utils;

import java.util.Collection;
import java.util.Map;

import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dtrust.dao.interoptest.dao.TestSuiteDAO;
import org.dtrust.dao.interoptest.entity.Test;
import org.dtrust.dao.interoptest.entity.TestStatus;
import org.dtrust.dao.interoptest.entity.TestSuite;
import org.dtrust.util.MessageRetriever;
import org.dtrust.util.MessageSender;
import org.nhindirect.common.mail.MDNStandard;
import org.nhindirect.common.tx.TxDetailParser;
import org.nhindirect.common.tx.TxUtil;
import org.nhindirect.common.tx.impl.DefaultTxDetailParser;
import org.nhindirect.common.tx.model.TxDetail;
import org.nhindirect.common.tx.model.TxDetailType;
import org.nhindirect.common.tx.model.TxMessageType;


public class DAOAndPop3InteropTestMonitorImpl implements Runnable
{
	private static final Log LOGGER = LogFactory.getFactory().getInstance(DAOAndPop3InteropTestMonitorImpl.class);
	
	protected final TestSuite suite;
	protected final TestSuiteDAO dao;
	protected final MessageRetriever retriever;
	protected final Collection<InternetAddress> reportAddresses;
	protected final MessageSender msgSender;
	protected final InternetAddress reportSender;
	
	public DAOAndPop3InteropTestMonitorImpl(TestSuite suite, TestSuiteDAO dao, 
			Collection<InternetAddress> reportAddresses, MessageRetriever retriever, MessageSender msgSender, InternetAddress reportSender)
	{
		this.suite = suite;
		this.dao = dao;
		this.retriever = retriever;
		this.reportAddresses = reportAddresses;
		this.msgSender = msgSender;
		this.reportSender = reportSender;
	}
	
	@Override
	public void run()
	{
		boolean aTestIsRunning = true;
		
		// get the start time... should not take anymore that 2 minutes to run these tests
		long startTime = System.currentTimeMillis();
		
		// this is the controller loop that will keep the monitor going
		// the individual test checkers will determine if a test is complete or not
		// add a sanity checker of 7 minutes that doesn't let this loop run forever
		while(aTestIsRunning && ((System.currentTimeMillis() - startTime) < 420000))
		{
			try
			{
				retriever.connect();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			
			aTestIsRunning = false;
			for (Test test : suite.getTests())
			{
				if (test.getTestStatus() == TestStatus.RUNNING || test.getTestStatus() == TestStatus.INITIATED)
				{
					// hold the current status to see if it changes
					// we will need to update the DAO if the status changes
					final TestStatus holdStatus = test.getTestStatus();
					final String holdComments = test.getComments();
					
					aTestIsRunning = true;
					switch (test.getType())
					{
						case SEND_NORMAL_GOOD_MESSAGE:
						{
							test.setTestStatus(checkGoodMessageStatus(test, suite.getTestTimout()));
							break;
						}
						case SEND_NORMAL_BAD_MESSAGE:
						{
							test.setTestStatus(checkBadMessageStatus(test, suite.getTestTimout()));
							break;
						}
						case SEND_RELIABLE_GOOD_MESSAGE:
						{
							test.setTestStatus(checkGoodReliableStatus(test, suite.getTestTimout()));
							break;
						}
						case SEND_RELIABLE_BAD_MESSAGE:
						{
							test.setTestStatus(checkBadReliableStatus(test, startTime));
							break;
						}
						default:
						{
							// do nothing
						}
					}
					
					if (test.getTestStatus() != holdStatus || !(test.getComments().equals(holdComments)))
					{
						if (test.getTestStatus() == TestStatus.COMPLETED_SUCCESS)
							test.setComments("Test Completed Successfully");
						
						try
						{
							dao.updateTest(test);
						}
						catch (Exception e) { /* no-op */}
					}
				}
			}
			
			try
			{
				retriever.disconnect();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			
			// sleep for 5 seconds
			try
			{
				Thread.sleep(5000);
			}
			catch (Exception e) {/* no - op */}
		}
		

		
		// check if any of the tests failel
		TestStatus suiteStatus = TestStatus.COMPLETED_SUCCESS;
		for (Test test : suite.getTests())
			if (test.getTestStatus() != TestStatus.COMPLETED_SUCCESS)
			{
				suiteStatus = TestStatus.COMPLETED_FAIL;
				break;
			}
		
		suite.setTestStatus(suiteStatus);
		try
		{
			dao.updateTestSuite(suite);
		}
		catch (Exception e) {/* no-op */}
		
		// generate a report if a report recipient was specified
		if (!this.reportAddresses.isEmpty())
		{
			try
			{
				LOGGER.info("Sending report message for test suite id " + suite.getTestSuiteid());
				final MimeMessage reportMsg = MessageBuilderUtils.createMimeMessage(this.reportAddresses, reportSender, 
						suite.getTestSuiteName() + " for " + suite.getTargetAddress(), suite.toString(), null, null, null, false);
				
				msgSender.sendMessage(reportMsg);
			}
			catch (Exception e)
			{
				LOGGER.error("Failed to send report message: " + e.getMessage(), e);
			}
		}
	}
	
	protected TestStatus checkGoodMessageStatus(Test test, int testTimeout)
	{
		final long timeoutThreshold = testTimeout * 60 * 1000;
		
		if (!(test.getTestStatus() == TestStatus.RUNNING || test.getTestStatus() == TestStatus.INITIATED))
			return test.getTestStatus();
		
		TestStatus retVal = test.getTestStatus();
		
		// check how much longer we should wait
		long holdTimeRemaining = (timeoutThreshold - (System.currentTimeMillis() - test.getStartDtTm().getTimeInMillis())) / 1000;
		test.setComments("Checking for MDN.  Time remaing to receive MDN: " + holdTimeRemaining + " seconds");
		
		Message processedMDN = null;
		Message dispatchedMDN = null;
		
		// get the messages
		try
		{
			TxDetailParser parser = new DefaultTxDetailParser();
			final Collection<Message> msgs = retriever.retrieveMessages(null);
			for (Message msg : msgs)
			{
			
				TxMessageType msgType = TxUtil.getMessageType((MimeMessage)msg);
				if (msgType == TxMessageType.MDN)
				{
					final Map<String, TxDetail> details = parser.getMessageDetails((MimeMessage)msg);
					final TxDetail detail = details.get(TxDetailType.PARENT_MSG_ID.getType());
					final TxDetail mdnType = details.get(TxDetailType.DISPOSITION.getType());
					
					if (detail != null && detail.getDetailValue().equals(test.getCorrelationId()))
					{
						// check the type of MDN
						final String detailValue = mdnType.getDetailValue().toLowerCase();
						if (detailValue.startsWith("automatic-action/mdn-sent-automatically"))
						{
							if (detailValue.endsWith("processed"))
							{
								test.setComments("Holding to ensure no dispatched MDN is received.  Hold time remaining: " + holdTimeRemaining + " seconds");
								processedMDN = msg;
							}
							else if (detailValue.endsWith("dispatched"))
							{
								// this appears to be dispatched MDN, but we need to check if it has the
								// context header of X-DIRECT-FINAL-DESTINATION-DELIVERY
								if (isDeliveryGuideDispatchedMDN((MimeMessage)msg))
									dispatchedMDN = msg;						
							}
						}
					}
				}
			}
		}
		catch (Throwable t)
		{
			retVal = TestStatus.ABORTED;
			return retVal;
		}
	
		
		// dispatched MDNs are not allowed when we don't ask for them
		if (dispatchedMDN != null)
		{
			retVal =  TestStatus.COMPLETED_FAIL;
			test.setComments("An dispatched MDN was received when one should not have been.");
			
			try
			{
				// clean up
				dispatchedMDN.setFlag(Flags.Flag.DELETED, true);
				if (processedMDN != null)
					processedMDN.setFlag(Flags.Flag.DELETED, true);
			}
			catch (Throwable t)
			{
				retVal = TestStatus.ABORTED;
				return retVal;
			}
			
			return retVal;
		}
		
		// time is up...  make sure we got our processed MDN
		if ((test.getTestStatus() == TestStatus.RUNNING || test.getTestStatus() == TestStatus.INITIATED) && 
				System.currentTimeMillis() - test.getStartDtTm().getTimeInMillis() > timeoutThreshold)
		{
			if (processedMDN != null)
			{
				try
				{
					// clean up
					processedMDN.setFlag(Flags.Flag.DELETED, true);
				}
				catch (Throwable t)
				{
					retVal = TestStatus.ABORTED;
				}
			
				retVal =  TestStatus.COMPLETED_SUCCESS;
			}
			else
			{
				retVal =  TestStatus.COMPLETED_FAIL;
				test.setComments("No Processed MDN received within the alloted time.");
			}
		}
		
		return retVal;
	}
	
	protected TestStatus checkBadMessageStatus(Test test, int testTimeout)
	{
		final long timeoutThreshold = testTimeout * 60 * 1000;
		
		if (!(test.getTestStatus() == TestStatus.RUNNING || test.getTestStatus() == TestStatus.INITIATED))
			return test.getTestStatus();
		
		TestStatus retVal = test.getTestStatus();
		
		// check how much longer we should wait
		long holdTimeRemaining = (timeoutThreshold - (System.currentTimeMillis() - test.getStartDtTm().getTimeInMillis())) / 1000;
		test.setComments("Holding to ensure no MDN is received.  Hold time remaining: " + holdTimeRemaining + " seconds");
		
		// get the messages
		try
		{
			TxDetailParser parser = new DefaultTxDetailParser();
			final Collection<Message> msgs = retriever.retrieveMessages(null);
			for (Message msg : msgs)
			{
			
				
				TxMessageType msgType = TxUtil.getMessageType((MimeMessage)msg);
				if (msgType == TxMessageType.MDN)
				{
					final Map<String, TxDetail> details = parser.getMessageDetails((MimeMessage)msg);
					final TxDetail detail = details.get(TxDetailType.PARENT_MSG_ID.getType());
					// if we get an MDN, then it's a failure
					if (detail != null && detail.getDetailValue().equals(test.getCorrelationId()))
					{
						// delete the message from the server
						msg.setFlag(Flags.Flag.DELETED, true);
						
						retVal = TestStatus.COMPLETED_FAIL;
						test.setComments("An MDN was received when one should not have been.");
						break;
					}
				}
			}
		}
		catch (Throwable t)
		{
			retVal = TestStatus.ABORTED;
		}
		
		// if the test is still running, determine if we need to call it good
		// due to running too long to find an MDN
		if ((test.getTestStatus() == TestStatus.RUNNING || test.getTestStatus() == TestStatus.INITIATED) && 
				System.currentTimeMillis() - test.getStartDtTm().getTimeInMillis() > timeoutThreshold)
		{
				retVal =  TestStatus.COMPLETED_SUCCESS;
		}
		
		return retVal;
	}
	
	protected TestStatus checkGoodReliableStatus(Test test, int testTimeout)
	{
		final long timeoutThreshold = testTimeout * 60 * 1000;
		
		if (!(test.getTestStatus() == TestStatus.RUNNING || test.getTestStatus() == TestStatus.INITIATED))
			return test.getTestStatus();
		
		TestStatus retVal = test.getTestStatus();
		
		// check how much longer we should wait
		long holdTimeRemaining = (timeoutThreshold - (System.currentTimeMillis() - test.getStartDtTm().getTimeInMillis())) / 1000;
		test.setComments("Checking for processed and dispatached MDNs.  Time remaing to receive MDNs: " + holdTimeRemaining + " seconds");
		
		Message processedMDN = null;
		Message dispatchedMDN = null;
		
		// get the messages
		try
		{
			TxDetailParser parser = new DefaultTxDetailParser();
			final Collection<Message> msgs = retriever.retrieveMessages(null);
			for (Message msg : msgs)
			{
			
				TxMessageType msgType = TxUtil.getMessageType((MimeMessage)msg);
				if (msgType == TxMessageType.MDN)
				{
					final Map<String, TxDetail> details = parser.getMessageDetails((MimeMessage)msg);
					final TxDetail detail = details.get(TxDetailType.PARENT_MSG_ID.getType());
					final TxDetail mdnType = details.get(TxDetailType.DISPOSITION.getType());
					
					if (detail != null && detail.getDetailValue().equals(test.getCorrelationId()))
					{
						final String detailValue = mdnType.getDetailValue().toLowerCase();
						if (detailValue.startsWith("automatic-action/mdn-sent-automatically"))
						{
							if (detailValue.endsWith("processed"))
								processedMDN = msg;
							else if (detailValue.endsWith("dispatched") && isDeliveryGuideDispatchedMDN((MimeMessage)msg))
								dispatchedMDN = msg;
						}

					}
				}
			}
			
			if (processedMDN != null && dispatchedMDN == null)
			{
				test.setComments("Processed MDN received; waiting for dispatached MDNs.  Time remaing to receive MDNs: " + holdTimeRemaining + " seconds");
			}
			else if (processedMDN == null && dispatchedMDN != null)
			{
				test.setComments("Dispatched MDN received; waiting for processed MDNs.  Time remaing to receive MDNs: " + holdTimeRemaining + " seconds");
			}			
			else if (processedMDN != null && dispatchedMDN != null)
			{						
				// make sure the message ids are unique
				if (((MimeMessage)processedMDN).getMessageID().equals(((MimeMessage)dispatchedMDN).getMessageID()))
				{
					retVal =  TestStatus.COMPLETED_FAIL;
					test.setComments("Dispatched and Processed MDN message ids are the same.");
				}
				else
				{
					retVal = TestStatus.COMPLETED_SUCCESS;
				}
				// delete the message from the server
				processedMDN.setFlag(Flags.Flag.DELETED, true);
				dispatchedMDN.setFlag(Flags.Flag.DELETED, true);

			}
		}
		catch (Throwable t)
		{
			retVal = TestStatus.ABORTED;
		}
	
		
		// if the test is still running, determine if we need to call it failed
		// due to running too long to find an MDN
		if ((test.getTestStatus() == TestStatus.RUNNING || test.getTestStatus() == TestStatus.INITIATED) && 
				System.currentTimeMillis() - test.getStartDtTm().getTimeInMillis() > timeoutThreshold)
		{
				retVal =  TestStatus.COMPLETED_FAIL;
				test.setComments("No MDN received within alloted time.");
		}
		
		return retVal;
	}
	
	protected TestStatus checkBadReliableStatus(Test test, long monitorStartTime)
	{
		if (!(test.getTestStatus() == TestStatus.RUNNING || test.getTestStatus() == TestStatus.INITIATED))
			return test.getTestStatus();
		
		TestStatus retVal = test.getTestStatus();
		
		// check to see if this test has ran for too long
		if (monitorStartTime - test.getStartDtTm().getTimeInMillis() > 120000)
			retVal =  TestStatus.COMPLETED_FAIL;
		
		return retVal;
	}	
	
	protected boolean isDeliveryGuideDispatchedMDN(MimeMessage msg)
	{
		final InternetHeaders headers = MDNStandard.getNotificationFieldsAsHeaders(msg);
		
		return (headers.getHeader(MDNStandard.DispositionOption_TimelyAndReliable) != null);
	}
}
