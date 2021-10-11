package org.dtrust.mailet;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.any;

import java.io.File;
import java.util.Arrays;
import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.FileUtils;
import org.apache.mailet.Mail;
import org.apache.mailet.MailetConfig;
import org.apache.mailet.MailetContext;
import org.dtrust.BaseTestPlan;
import org.dtrust.InteropTestRunner;
import org.dtrust.client.TestRegService;
import org.dtrust.client.impl.TestRegServiceImpl;
import org.dtrust.dao.interoptest.entity.TestRegistration;
import org.dtrust.utils.TestUtils;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.nhindirect.common.rest.HttpClientFactory;
import org.nhindirect.stagent.mail.Message;

import com.sun.jersey.api.client.WebResource;

public class ATABValidator_serviceReportSendTest 
{
	static WebResource resource;
	
	static TestRegService service;
	
	abstract class TestPlan extends BaseTestPlan 
	{
		protected ATABValidation mailet;
		
		protected MimeMessage reportMsg = null;
		
		@Override
		protected void setupMocks() throws Exception
		{
			try
			{
				InteropTestRunner.startInteropTestService();
				super.setupMocks();
				cleanDAOs();
													
				resource = 	TestUtils.getResource(InteropTestRunner.getInteropTestServiceURL());	
				
				service = new TestRegServiceImpl(InteropTestRunner.getInteropTestServiceURL(), HttpClientFactory.createHttpClient(), getTestServiceSecurityManager());
				
				final MailetConfig config = mock(MailetConfig.class);
				when(config.getInitParameter(eq("ConfigURL"))).thenReturn("http://test.com/");
				when(config.getInitParameter(eq("ValidationReportSender"))).thenReturn("atabTest@direct.securehealthemail.com");
				when(config.getInitParameter(eq("ValidationReportSender"))).thenReturn("atabTest@direct.securehealthemail.com");
				when(config.getInitParameter(eq("TestRegServiceURL"))).thenReturn(InteropTestRunner.getInteropTestServiceURL());
				when(config.getInitParameter(eq("TestRegUserName"))).thenReturn("gm2552");
				when(config.getInitParameter(eq("TestRegPassword"))).thenReturn("1kingpuff");
				
				final MailetContext context = mock(MailetContext.class);
				doAnswer(new Answer<Void>() {
				    public Void answer(InvocationOnMock invocation) 
				    {
				        Object[] args = invocation.getArguments();
				        System.out.println("called with arguments: " + Arrays.toString(args));
				        reportMsg = (MimeMessage)args[0];
				        return null;
				      }
				  }).when(context).sendMail((MimeMessage)any());
				
				when(config.getMailetContext()).thenReturn(context);
				mailet = new ATABValidation();
				mailet.init(config);
				
			}
			catch (Throwable t)
			{
				throw new RuntimeException(t);
			}
			

		}
		
		@Override
		protected void tearDownMocks() throws Exception
		{
			try
			{
				cleanDAOs();
			}
			catch (Exception e)
			{
				
			}
			
			super.tearDownMocks();
		}
		
		protected void cleanDAOs() throws Exception
		{
			testRegDao.cleanRegistrations();
		}
		
		protected abstract String getReportAddress();
		
		protected abstract String getSourceAddress();
		
		@Override
		protected void performInner() throws Exception
		{			
			
			final String repAddress = TestUtils.uriEscape(getReportAddress());
			
			final String sourceAddress = TestUtils.uriEscape(getSourceAddress());
			
			// add the reg 
			resource.path("/interopReg/" +  repAddress + "/" + sourceAddress).put(TestRegistration.class);

			final Message msg = new Message(FileUtils.openInputStream(new File("./src/test/resources/messages/ValidKeyUsage.txt")));
			
			Mail mail = mock(Mail.class);
			when(mail.getMessage()).thenReturn(msg);
			
			mailet.service(mail);
			
			doAssertions(reportMsg);
		}
		
		protected void doAssertions(MimeMessage reportMsg) throws Exception
		{
			
		}
	}
	
	@Test
	public void testATABValidator_serviceMail() throws Exception
	{
		new TestPlan()
		{
			@Override
			protected String getSourceAddress()
			{
				return "gm2552@testri.direct.com";
			}
			
			@Override
			protected String getReportAddress()
			{
				return "gm2552@cerner.com";
			}
						
			@Override
			protected void doAssertions(MimeMessage reportMsg) throws Exception
			{
				boolean foundReportRecip = false;
				assertNotNull(reportMsg);
				for (Address addr : reportMsg.getAllRecipients())
				{
					InternetAddress iAddr = (InternetAddress)addr;
					if (iAddr.getAddress().equals(getReportAddress()))
					{
						foundReportRecip = true;
						break;
					}	
				}
				
				assertTrue(foundReportRecip);
			}
		}.perform();
	}		
}
