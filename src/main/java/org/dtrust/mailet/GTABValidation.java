package org.dtrust.mailet;


import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dtrust.client.TestRegService;
import org.dtrust.client.impl.TestRegServiceImpl;
import org.dtrust.dao.interoptest.entity.TestRegistration;
import org.nhindirect.common.rest.BootstrapBasicAuthServiceSecurityManager;
import org.nhindirect.common.rest.HttpClientFactory;
import org.nhindirect.common.rest.ServiceSecurityManager;
import org.nhindirect.stagent.NHINDAddressCollection;

import org.nhindirect.stagent.mail.MailStandard;
import org.nhindirect.stagent.mail.Message;
import org.nhindirect.stagent.mail.MimeEntity;

public class GTABValidation extends AbstractValidation
{
	private static final Log LOGGER = LogFactory.getFactory().getInstance(GTABValidation.class);	

	protected TestRegService service;
	
	@Override
	public void init() throws MessagingException
	{
		super.init();
		

		
		final String testRegServiceBaseURL = getMailetConfig().getInitParameter(TEST_REG_BASE_URL);
		if (StringUtils.isEmpty(testRegServiceBaseURL))
		{
			LOGGER.error("Test reg service URL cannot be null or empty.");
			throw new MessagingException("Test reg service URL cannot be null or empty.");
		}
		
		final String testRegUsername = getMailetConfig().getInitParameter(TEST_REG_USERNAME);
		if (StringUtils.isEmpty(testRegUsername))
		{
			LOGGER.error("Test reg username cannot be null or empty.");
			throw new MessagingException("Test reg username cannot be null or empty.");
		}
		
		final String testRegPassword = getMailetConfig().getInitParameter(TEST_REG_PASSWORD);
		if (StringUtils.isEmpty(testRegPassword))
		{
			LOGGER.error("Test reg password cannot be null or empty.");
			throw new MessagingException("Test reg password cannot be null or empty.");
		}
		
		final ServiceSecurityManager mgr = new BootstrapBasicAuthServiceSecurityManager(testRegUsername, testRegPassword);
		
		service = new TestRegServiceImpl(testRegServiceBaseURL, HttpClientFactory.createHttpClient(), mgr);
	}
	
	@Override
	protected ValidationReportAttr validateMessageAttributes(Message msg, NHINDAddressCollection recips)
	{
		GTABValidationReportAttr reportAttr = new GTABValidationReportAttr();

		// validate the encryption requirements
		reportAttr = EncrValidator.validateEncryption(msg, reportAttr, recips, proxy, true);
		if (!reportAttr.encrReport.encrValid)
			return reportAttr; 			// bail 

		// decrypt the message so we can inspect the rest of the message
		MimeEntity decryptedEntity = null;
		
		try
		{
			decryptedEntity = decryptMessage(msg, recips);
		}
		catch (Exception e)
		{
			LOGGER.error("Error decrypting message.", e);
			reportAttr.encrReport.encrValid = false;
			reportAttr.encrReport.comment = "Could not decrypt message.";
			return reportAttr;
		}
		
		// validate the signature requirements
		// turn this into a Multipart Mime object
		MimeMultipart verifyMM = null;
		try
		{
			final ByteArrayDataSource dataSource = new ByteArrayDataSource(decryptedEntity.getRawInputStream(), decryptedEntity.getContentType());
			
			verifyMM = new MimeMultipart(dataSource);	
		}
		catch (Exception e)
		{
			LOGGER.error("Error transforming message to multi part MIME.", e);
			reportAttr.encrReport.encrValid = false;
			reportAttr.encrReport.comment = "Message is not a multiple part MIME message.";
			return reportAttr;
		}
		
		reportAttr = GTABDigSigValidator.validateDigSig(verifyMM, reportAttr);
		if (!reportAttr.digSigReport.digSigValid)
			return reportAttr; 			// bail 
		
		
		// validate that the message is wrapped
		final GTABValidationReportAttr.WrappedReport wrappedReport = new GTABValidationReportAttr.WrappedReport();
		wrappedReport.isWrapped = isWrapped(verifyMM);
		reportAttr.wrappedReport = wrappedReport;
		
		return reportAttr;
	}

	@Override
	protected String getReportSubject()
	{
		return "GTAB Validation report for";
	}
	
	protected boolean isWrapped(MimeMultipart verifyMM)
	{
    	try
    	{    
    		// the wrapped part is the first part
    		final BodyPart part = verifyMM.getBodyPart(0);
            ContentType contentType = new ContentType(part.getContentType());
    		return contentType.match(MailStandard.MediaType.WrappedMessage);
    	}
    	catch (MessagingException e) {/* no-op */}
    	
    	return false;
	}
	
	protected Collection<InternetAddress> getAdditionalReportAddrs(String sourceAddr)
	{
		final Map<String, InternetAddress> retVal = new HashMap<String, InternetAddress>();
		
		try
		{	
			final Collection<TestRegistration> addrs = service.getReportAddrBySourceAddr(sourceAddr);
			if (addrs != null)
			{
				for (TestRegistration addr : addrs)
				{
					if (retVal.get(addr.getReportAddressAllCaps()) == null)
						retVal.put(addr.getReportAddressAllCaps(), new InternetAddress(addr.getReportAddress()));
				}
			}
		}
		catch (Throwable e)
		{
			LOGGER.warn("Failed to get additional report addresses by source address", e);
			return Collections.emptyList();
		}
		return retVal.values();
	}
}
