package org.dtrust.resources.utils;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.search.SearchTerm;

import org.dtrust.dao.interoptest.entity.TestStatus;
import org.dtrust.util.MessageRetriever;
import org.junit.Test;

public class CheckProcessedDispatchedMDNTest
{
	@Test
	public void testAthenaProcessedDispatched() throws Exception
	{
		final InputStream dispatchedStr = getClass().getClassLoader().getResourceAsStream("messages/AthenaDispatch.txt");
		final InputStream processedStr = getClass().getClassLoader().getResourceAsStream("messages/AthenaProcessed.txt");
		final Collection<Message> msgs = new ArrayList<Message>();
		msgs.add(new MimeMessage((Session)null, dispatchedStr));
		msgs.add(new MimeMessage(null, processedStr));
		
		final Retriever retriever = new Retriever(msgs);
		
		
		final org.dtrust.dao.interoptest.entity.Test currentTest = new org.dtrust.dao.interoptest.entity.Test();
		currentTest.setTestId(1);
		currentTest.setCorrelationId("<1087135417.35.1502195576436.JavaMail.root@ip-172-31-10-81>");
		currentTest.setStartDtTm(Calendar.getInstance());
		currentTest.setTestStatus(TestStatus.RUNNING);
		
		final DAOAndPop3InteropTestMonitorImpl checker = new DAOAndPop3InteropTestMonitorImpl(null, null, null, retriever, null, null);
		final TestStatus status = checker.checkGoodReliableStatus(currentTest, 120);
		
		assertEquals(TestStatus.COMPLETED_SUCCESS, status);
	}
	
	protected static class Retriever implements MessageRetriever
	{
		final protected Collection<Message> msgs;
		
		public Retriever(Collection<Message> msgs)
		{
			this.msgs = msgs;
		}
		
		public void connect() throws Exception
		{
		}
		
		public Collection<Message> retrieveMessages(SearchTerm searchTerm) throws Exception
		{
			return msgs;
		}
		
		public void disconnect() throws Exception
		{
			
		}
	}
}
