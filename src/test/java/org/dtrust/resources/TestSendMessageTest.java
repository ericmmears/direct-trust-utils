package org.dtrust.resources;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.mail.internet.InternetAddress;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.dtrust.dao.interoptest.dao.TestSuiteDAO;
import org.dtrust.dao.interoptest.entity.TestSuite;
import org.dtrust.dao.interoptest.entity.TestType;
import org.dtrust.util.MessageSender;
import org.junit.Before;
import org.junit.Test;
import org.nhindirect.stagent.cert.CertificateResolver;
import org.nhindirect.gateway.smtp.config.cert.impl.CertStoreUtils;

public class TestSendMessageTest
{
	protected InteropTestResource rec;
	protected CertificateResolver privResolver;
	protected MessageSender msgSender;
	
	@Before
	public void setUp() throws Exception
	{
		rec = new InteropTestResource();
		rec.setLocalDomain("direct.securehealthemail.com");
		rec.setLocalSender("atabInterop@direct.securehealthemail.com");
		
		final InputStream ioStream = FileUtils.openInputStream(new File("./src/test/resources/certs/direct.securehealthemail.com-digSig.p12"));
		final X509Certificate cert = CertStoreUtils.certFromData(null, IOUtils.toByteArray(ioStream));
		IOUtils.closeQuietly(ioStream);
		
		privResolver = mock(CertificateResolver.class);
		when(privResolver.getCertificates((InternetAddress)any())).thenReturn(Arrays.asList(cert));
		
		rec.setPrivateCertResolver(privResolver);
		
		msgSender = mock(MessageSender.class);

		//msgSender = new SMTPMessageSender("25", "securehealthemail.com", "gm2552", "1kingpuff");
		
		rec.setMsgSender(msgSender);
		
		TestSuiteDAO dao = mock(TestSuiteDAO.class);
		when(dao.initiateTestSuite((String)any(), (String)any(), anyInt())).thenReturn(new TestSuite());
		when(dao.initiateTest((String)any(), (TestType)any(), anyLong())).thenReturn(new org.dtrust.dao.interoptest.entity.Test());

		rec.setDao(dao);
		
	}
	
	@Test
	public void testSendMessage() throws Exception
	{
		//rec.testSendMessage("gm2552@direct.securehealthemail.com");
		rec.testSendMessage(null, "gm2552@demo.sandboxcernerdirect.com", 2);
	}
}
