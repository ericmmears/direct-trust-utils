package org.dtrust.resources;

import java.io.File;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.x509.CRLNumber;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.x509.X509V2CRLGenerator;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;
import org.dtrust.dao.interoptest.dao.TestEntityNotFoundException;
import org.dtrust.dao.interoptest.dao.TestSuiteDAO;
import org.dtrust.dao.interoptest.entity.Test;
import org.dtrust.dao.interoptest.entity.TestStatus;
import org.dtrust.dao.interoptest.entity.TestSuite;
import org.dtrust.dao.interoptest.entity.TestType;
import org.dtrust.resources.utils.InteropTestMonitorFactory;
import org.dtrust.resources.utils.MessageBuilderUtils;
import org.dtrust.resources.utils.PrivateCertResolver;
import org.dtrust.util.MessageSender;
import org.nhindirect.policy.PolicyExpression;
import org.nhindirect.policy.PolicyFilter;
import org.nhindirect.policy.PolicyFilterFactory;
import org.nhindirect.policy.PolicyLexicon;
import org.nhindirect.policy.PolicyLexiconParser;
import org.nhindirect.policy.PolicyLexiconParserFactory;
import org.nhindirect.stagent.CryptoExtensions;
import org.nhindirect.stagent.cert.CertificateResolver;
import org.nhindirect.stagent.cert.Thumbprint;
import org.nhindirect.stagent.cert.impl.DNSCertificateStore;
import org.nhindirect.stagent.cert.impl.LDAPCertificateStore;
import org.nhindirect.stagent.cert.impl.LdapPublicCertUtilImpl;
import org.nhindirect.stagent.cryptography.Cryptographer;
import org.nhindirect.stagent.cryptography.SMIMECryptographerImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sun.jersey.spi.resource.Singleton;



@Component
@Path("interopTest/")
@Singleton
public class InteropTestResource
{
    static final String UTF8 = ";charset=UTF-8";
	
	/**
	 * Cache definition for no caching of responses.
	 */
	protected static final CacheControl noCache;
	static
	{
		noCache = new CacheControl();
		noCache.setNoCache(true);
	}
	
	private static final Log LOGGER = LogFactory.getFactory().getInstance(InteropTestResource.class);

	@Autowired
	protected TestSuiteDAO dao;
	
	@Autowired
	protected CertificateResolver privateCertResolver;
	
	
	protected String configURL;
	protected String configUser;
	protected String configPass;
	
	@Autowired
	@Qualifier("crlFile")
	protected String crlFile;
	
	@Autowired
	@Qualifier("localDomain")
	protected String localDomain;
	
	@Autowired
	@Qualifier("localSender")
	protected InternetAddress localSender;
	
	@Autowired
	@Qualifier("msgSender")
	protected MessageSender msgSender;
	
	@Autowired()
	protected InteropTestMonitorFactory testMonitorFactory;
	
	final protected byte[] ccdaContent;
	final protected byte[] imageContent;
	final protected byte[] pdfContent;
	final PolicyExpression certPolicy;
	final ExecutorService testThreadService;
	final ExecutorService testMonitorService;
	final ScheduledExecutorService clrGenScheduler;
	
	protected final X509Certificate crlSigningCert;
	protected final PrivateKey crlSigningKey;
	protected final X509Certificate certToRevoke;
	
	final protected Collection<CertificateResolver> publicCertResolvers;
	final protected Cryptographer crypto;
	
	public InteropTestResource()
	{
		ccdaContent = readMsgContent("healthSummary.xml");
		imageContent = readMsgContent("healthImage.jpg");
		pdfContent = readMsgContent("healthSummary.pdf");
		
		publicCertResolvers = new ArrayList<CertificateResolver>();
		publicCertResolvers.add(new DNSCertificateStore(Arrays.asList("8.8.8.8")));
		
		final LdapPublicCertUtilImpl utilImpl = new LdapPublicCertUtilImpl();
		publicCertResolvers.add(new LDAPCertificateStore(utilImpl, null, null));
		
		crypto = new SMIMECryptographerImpl();
		
		InputStream ioStream = null;
		InputStream certStream = null;
		InputStream keyStream = null;
		try
		{			
			ioStream = this.getClass().getClassLoader().getResourceAsStream("policies/interopTestCertPolicy.pol");
			final PolicyLexiconParser parser = PolicyLexiconParserFactory.getInstance(PolicyLexicon.SIMPLE_TEXT_V1);
			certPolicy = parser.parse(ioStream);
			
			certStream = this.getClass().getClassLoader().getResourceAsStream("certs/crlSignCert.der");
			keyStream = this.getClass().getClassLoader().getResourceAsStream("certs/crlSignKey.der");
			
			crlSigningCert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(certStream);
			final PKCS8EncodedKeySpec keysp = new PKCS8EncodedKeySpec ( IOUtils.toByteArray(keyStream) );
			crlSigningKey = KeyFactory.getInstance("RSA").generatePrivate(keysp);
			
			certToRevoke = PrivateCertResolver.loadCertFromResource("certs/revoked.p12");

		}
		catch (Exception e)
		{
			throw new IllegalStateException("Could not load certificate policy checker or could not get crl signing keys.");
		}
		finally
		{
			IOUtils.closeQuietly(ioStream);
			IOUtils.closeQuietly(certStream);
			IOUtils.closeQuietly(keyStream);	
		}
		
		// don't allow more that 10 requests to be submitted at the same time
		testThreadService = Executors.newFixedThreadPool(10);
		testMonitorService = Executors.newFixedThreadPool(10);
		clrGenScheduler = Executors.newScheduledThreadPool(1);
		
	}
	
	public void setDao(TestSuiteDAO dao)
	{
		this.dao = dao;
	}

	
	public void setTestMonitorFactory(InteropTestMonitorFactory testMonitorFactory)
	{
		this.testMonitorFactory = testMonitorFactory;
	}

	protected byte[] readMsgContent(String resourceName)
	{
		InputStream ioStream = null;
		try
		{
			ioStream = this.getClass().getClassLoader().getResourceAsStream("msgContent/" + resourceName);
			return IOUtils.toByteArray(ioStream);
			
		}
		catch (Exception e)
		{
			throw new IllegalStateException("Failed to load message content data: " + e.getMessage(), e);
		}
		finally
		{
			IOUtils.closeQuietly(ioStream);
		}
	}
	
	@PostConstruct
	public void startCRLScheduler()
	{
		final CRLCreationTask crlTask = new CRLCreationTask(crlSigningCert, crlSigningKey, 
				new File(crlFile), Arrays.asList(certToRevoke));
		clrGenScheduler.scheduleAtFixedRate(crlTask, 0, 1, TimeUnit.DAYS);		
	}
	
	public void setConfigURL(String configURL)
	{
		this.configURL = configURL;
	}

	public void setConfigUser(String configUser)
	{
		this.configUser = configUser;
	}

	public void setConfigPass(String configPass)
	{
		this.configPass = configPass;
	}
	
	public void setLocalDomain(String localDomain)
	{
		this.localDomain = localDomain;
	}
	
	
	public void setLocalSender(String localSender)
	{
		try
		{
			this.localSender = new InternetAddress(localSender);
		}
		catch (Exception e)
		{
			// handle queitly
		}
	}

	public void setPrivateCertResolver(CertificateResolver privateCertResolver)
	{
		this.privateCertResolver = privateCertResolver;
	}

	

	public void setMsgSender(MessageSender msgSender)
	{
		this.msgSender = msgSender;
	}


	
	protected Collection<MimeMessage> getHappyPathMessages(InternetAddress to) throws Exception
	{
		final Collection<MimeMessage> retVal = new ArrayList<MimeMessage>();
		
		// hard code for now
		retVal.add(MessageBuilderUtils.createMimeMessage(Arrays.asList(to), localSender, "Happy path, text only", "This is a test message.", null , null, null, false));
		retVal.add(MessageBuilderUtils.createMimeMessage(Arrays.asList(to), localSender, "Happy path, c-cda only", "", ccdaContent , null, null, false));
		retVal.add(MessageBuilderUtils.createMimeMessage(Arrays.asList(to), localSender, "Happy path, image only", "", null , imageContent, null, false));
		retVal.add(MessageBuilderUtils.createMimeMessage(Arrays.asList(to), localSender, "Happy path, pdf only", "", null , null, pdfContent, false));
		retVal.add(MessageBuilderUtils.createMimeMessage(Arrays.asList(to), localSender, "Happy path, mixed content", "This is a mixed message", ccdaContent , null, pdfContent, false));
		retVal.add(MessageBuilderUtils.createMimeMessage(Arrays.asList(to), localSender, "Happy path, mixed content", "This is another mixed message", null , this.imageContent, null, false));

		
		return retVal;
	}
	
	protected Collection<Collection<String>> getCertificateSendScenarios()
	{
		final Collection<Collection<String>> retVal = new ArrayList<Collection<String>>();
		
		// hard code for now
		retVal.add(Arrays.asList(PrivateCertResolver.GOOD));
		retVal.add(Arrays.asList(PrivateCertResolver.EXPIRED));
		retVal.add(Arrays.asList(PrivateCertResolver.REVOKED));
		retVal.add(Arrays.asList(PrivateCertResolver.NON_TRUSTED));
		retVal.add(Arrays.asList(PrivateCertResolver.NON_TRUSTED, PrivateCertResolver.GOOD));
		
		return retVal;
	}
	
	@GET
    @Path("sendTests/{suiteId}")
    @Produces(MediaType.APPLICATION_JSON + UTF8)   
	public Response getTestSuite(@PathParam("suiteId") long suiteId) throws Exception
	{
		TestSuite suite = null;
		try
		{
			suite = dao.getTestSuite(suiteId);
		}
		catch (TestEntityNotFoundException notFoundEx)
		{
    		LOGGER.error("Test sute with id " + suiteId + " does not exist.", notFoundEx);
    		return Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).cacheControl(noCache).build();   
		}
    	catch (Throwable t)
    	{
    		LOGGER.error("Error retrieving test suite with id " + suiteId, t);
    		return Response.serverError().cacheControl(noCache).build();   
    	}
		
		// sort the list by test it
		// create a new list because returned lists may be 
		// immutable
		final List<Test> tests = new ArrayList<Test>(suite.getTests());
		final Comparator<Test> comparator = new Comparator<Test>() 
		{
		    public int compare(Test c1, Test c2) 
		    {
		        return (c1.getTestId() < c2.getTestId()) ? -1 : 1;
		    }
		};
		Collections.sort(tests, comparator);
		
		suite.setTests(new LinkedHashSet<Test>(tests));
		
		return Response.ok(suite).cacheControl(noCache).build();
	}
	
	@POST
    @Path("sendTests/sendReport/{testId}/{reportAddress}")
    @Produces(MediaType.APPLICATION_JSON + UTF8)  
	public Response sendReport(@PathParam("reportAddress") String reportAddress,
			@PathParam("testId") long testSuiteId)
    {
    			
		// make sure the test exists
		try
		{
			final TestSuite testSuite = dao.getTestSuite(testSuiteId);
			
			final Collection<InternetAddress> reportAddresses = new ArrayList<InternetAddress>();
			if (!StringUtils.isEmpty(reportAddress))
			{
				// there can be multiple addresses, so split them out
				final String[] rpAddresses = reportAddress.split(",");
				try
				{
					for (String addr : rpAddresses)
					{
						reportAddresses.add(new InternetAddress(addr));
					}
				}
				catch (Exception e)
				{
					throw new IllegalArgumentException("Illegal report address: " + e.getMessage(), e);
				}
			}

			final MimeMessage reportMsg = MessageBuilderUtils.createMimeMessage(reportAddresses, localSender, 
				testSuite.getTestSuiteName() + " for " + testSuite.getTargetAddress(), testSuite.toString(), null, null, null, false);
		
			 msgSender.sendMessage(reportMsg);


		}
		catch (TestEntityNotFoundException notFoundEx)
		{
    		return Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).cacheControl(noCache).build();   
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to send report message: " + e.getMessage(), e);
			return Response.serverError().cacheControl(noCache).build();  
		}
		return Response.ok().cacheControl(noCache).build();   
    }
    		
	
	@POST
    @Path("sendTests/{toAddress}/{testTimeout}")
    @Produces(MediaType.APPLICATION_JSON + UTF8)  
	public Response testSendMessage(@Context UriInfo uriInfo, @PathParam("toAddress") String toAddress,
			@PathParam("testTimeout") int testTimeout) throws Exception	
	{
		return testSendMessage(uriInfo, toAddress, testTimeout, null);
	}
	
	@POST
    @Path("sendTests/{toAddress}/{testTimeout}/{reportAddress}")
    @Produces(MediaType.APPLICATION_JSON + UTF8)  
	public Response testSendMessage(@Context UriInfo uriInfo, @PathParam("toAddress") String toAddress,
			@DefaultValue("2") @PathParam("testTimeout") int testTimeout, @PathParam("reportAddress") String reportAddress) throws Exception
	{
		final TestSuite testSuite = dao.initiateTestSuite("ATAB Interop Message Receive Tests", toAddress, testTimeout);
		
		InternetAddress to;
		try
		{
			to = new InternetAddress(toAddress);;
		}
		catch (Exception e)
		{
			testSuite.setTestStatus(TestStatus.ABORTED);
			dao.updateTestSuite(testSuite);
			throw new IllegalArgumentException("Illegal to address: " + e.getMessage(), e);
		}
		
		// get the report addresses if there are any
		final Collection<InternetAddress> reportAddresses = new ArrayList<InternetAddress>();
		if (!StringUtils.isEmpty(reportAddress))
		{
			// there can be multiple addresses, so split them out
			final String[] rpAddresses = reportAddress.split(",");
			try
			{
				for (String addr : rpAddresses)
				{
					reportAddresses.add(new InternetAddress(addr));
				}
			}
			catch (Exception e)
			{
				testSuite.setTestStatus(TestStatus.ABORTED);
				dao.updateTestSuite(testSuite);
				throw new IllegalArgumentException("Illegal report address: " + e.getMessage(), e);
			}
		}
			
		// launch the test on a pooled thread queue
		final InteropTestRunner runner = new InteropTestRunner(to, reportAddresses, testSuite);
		
		testThreadService.execute(runner);
		
		final UriBuilder newLocBuilder = uriInfo.getBaseUriBuilder();
		final URI newLoc = newLocBuilder.path("interopTest/sendTests/" + testSuite.getTestSuiteid()).build();

		return Response.created(newLoc).entity(testSuite.getTestSuiteid()).cacheControl(noCache).build();

	}
	
	protected class InteropTestRunner implements Runnable
	{
		protected final InternetAddress to;
		protected final TestSuite testSuite;
		protected final Collection<InternetAddress> reportAddresses;
		
		public InteropTestRunner(InternetAddress to, Collection<InternetAddress> reportAddresses, TestSuite testSuite)
		{
			this.to = to;
			this.testSuite = testSuite;
			this.reportAddresses = reportAddresses;
		}
		
		@Override
		public void run()
		{
			try
			{
				testSuite.setTestStatus(TestStatus.RUNNING);
				dao.updateTestSuite(testSuite);

				final Test certPolTest = dao.initiateTest("Certificate Resolution/Policy", TestType.CERT_EVAL, testSuite.getTestSuiteid());
				try
				{
					certPolTest.setTestStatus(TestStatus.RUNNING);
					certPolTest.setComments("Looking up public certificate");
					dao.updateTest(certPolTest);
					// test certificates
					final Collection<X509Certificate> toAddressCerts = 
							MessageBuilderUtils.resolvePublicCerts(to, publicCertResolvers);
					
					final PolicyFilter filter = PolicyFilterFactory.getInstance();
	
					boolean allCompliant = true;
					for (X509Certificate toAddressCert : toAddressCerts)
					{
						if (!filter.isCompliant(toAddressCert, certPolicy))
						{
							allCompliant = false;
							certPolTest.setTestStatus(TestStatus.COMPLETED_FAIL);
							certPolTest.setComments("Certificate with thumbprint " + Thumbprint.toThumbprint(toAddressCert).toString() + 
									" is not compliant with bundle profile policies.");
							break;
						}
					}
						
					if (allCompliant)
					{
						certPolTest.setTestStatus(TestStatus.COMPLETED_SUCCESS);
						certPolTest.setComments("Test Completed Successfully");
					}
					dao.updateTest(certPolTest);
				}
				catch (Exception e)
				{
					certPolTest.setTestStatus(TestStatus.ABORTED);
					certPolTest.setComments("Certificate check aborted: " + e.getMessage());
					dao.updateTest(certPolTest);
				}
				
				try
				{
	
					// generate the happy path messages
					final Collection<MimeMessage> happyPathMessages = getHappyPathMessages(to);
	
					for (MimeMessage msgToProcess : happyPathMessages)
					{
						final Test happyPathTest = dao.initiateTest(msgToProcess.getSubject(), TestType.SEND_NORMAL_GOOD_MESSAGE, testSuite.getTestSuiteid());
						
						try
						{
							System.out.println("Test name: " + happyPathTest.getTestName());
							System.out.println("Initial message id: " + msgToProcess.getMessageID());
							
							happyPathTest.setTestStatus(TestStatus.RUNNING);
							happyPathTest.setComments("Creating direct message.");
							
							dao.updateTest(happyPathTest);
							final MimeMessage staEncryptedMsg = MessageBuilderUtils.createDirectMessage(publicCertResolvers, privateCertResolver, crypto, 
									msgToProcess, Arrays.asList(PrivateCertResolver.GOOD));
							
							
							System.out.println("Final encrypted message id: " + staEncryptedMsg.getMessageID() + "\r\n\r\n");
							
							
							happyPathTest.setCorrelationId(staEncryptedMsg.getMessageID());
							happyPathTest.setComments("Handing off to SMTP server.");
							dao.updateTest(happyPathTest);
							
							msgSender.sendMessage(staEncryptedMsg);
							
						}
						catch (Exception e)
						{
							happyPathTest.setTestStatus(TestStatus.ABORTED);
							happyPathTest.setComments("Messaging test aborted: " + e.getMessage());
							dao.updateTest(happyPathTest);
						}
					}
					
					final Test multiCertTest = dao.initiateTest("Happy path, multi certs (non trusted cert)", TestType.SEND_NORMAL_GOOD_MESSAGE, testSuite.getTestSuiteid());
					try
					{
						System.out.println("Test name: " + multiCertTest.getTestName());
						
						// send a message with a good and bad cert
						final MimeMessage goodBadCertMsg = MessageBuilderUtils.createMimeMessage(
								Arrays.asList(to), localSender, "Happy path, multi certs (non trusted cert)", "This is a test message.", null , null, null, false);
	
						System.out.println("Initial message id: " + goodBadCertMsg.getMessageID());
						
						multiCertTest.setTestStatus(TestStatus.RUNNING);
						multiCertTest.setComments("Creating direct message.");
						dao.updateTest(multiCertTest);
						
						final MimeMessage staEncryptedMsg = MessageBuilderUtils.createDirectMessage(publicCertResolvers, privateCertResolver, crypto, 
								goodBadCertMsg, Arrays.asList(PrivateCertResolver.NON_TRUSTED, PrivateCertResolver.GOOD));
						
						System.out.println("Final encrypted message id: " + staEncryptedMsg.getMessageID() + "\r\n\r\n");
						
						multiCertTest.setCorrelationId(staEncryptedMsg.getMessageID());
						multiCertTest.setComments("Handing off to SMTP server.");
						dao.updateTest(multiCertTest);
						msgSender.sendMessage(staEncryptedMsg);
					}
					catch (Exception e)
					{
						multiCertTest.setTestStatus(TestStatus.ABORTED);
						multiCertTest.setComments("Messaging test aborted: " + e.getMessage());
						dao.updateTest(multiCertTest);
					}
					
					final Test multiCertTestReverse = dao.initiateTest("Happy path, multi certs (non trusted cert, reverse signature order)", TestType.SEND_NORMAL_GOOD_MESSAGE, testSuite.getTestSuiteid());
					try
					{
						System.out.println("Test name: " + multiCertTestReverse.getTestName());
						
						// send a message with a good and bad cert
						final MimeMessage goodBadCertMsg = MessageBuilderUtils.createMimeMessage(
								Arrays.asList(to), localSender, "Happy path, multi certs (non trusted cert, reverse signature order)", "This is a test message.", null , null, null, false);
	
						System.out.println("Initial message id: " + goodBadCertMsg.getMessageID());
						
						multiCertTestReverse.setTestStatus(TestStatus.RUNNING);
						multiCertTestReverse.setComments("Creating direct message.");
						dao.updateTest(multiCertTestReverse);
						
						final MimeMessage staEncryptedMsg = MessageBuilderUtils.createDirectMessage(publicCertResolvers, privateCertResolver, crypto, 
								goodBadCertMsg, Arrays.asList(PrivateCertResolver.GOOD, PrivateCertResolver.NON_TRUSTED));
						
						System.out.println("Final encrypted message id: " + staEncryptedMsg.getMessageID() + "\r\n\r\n");
						multiCertTestReverse.setCorrelationId(staEncryptedMsg.getMessageID());
						multiCertTestReverse.setComments("Handing off to SMTP server.");
						dao.updateTest(multiCertTestReverse);
						msgSender.sendMessage(staEncryptedMsg);
					}
					catch (Exception e)
					{
						multiCertTestReverse.setTestStatus(TestStatus.ABORTED);
						multiCertTestReverse.setComments("Messaging test aborted: " + e.getMessage());
						dao.updateTest(multiCertTestReverse);
					}					

					final Test multiCertExpiredTest = dao.initiateTest("Happy path, multi certs (expired cert)", TestType.SEND_NORMAL_GOOD_MESSAGE, testSuite.getTestSuiteid());
					try
					{
						
						System.out.println("Test name: " + multiCertExpiredTest.getTestName());

						// send a message with a good and bad cert
						final MimeMessage goodBadCertMsg = MessageBuilderUtils.createMimeMessage(
								Arrays.asList(to), localSender, "Happy path, multi certs (expired cert)", "This is a test message.", null , null, null, false);
	
						System.out.println("Initial message id: " + goodBadCertMsg.getMessageID());
						
						multiCertExpiredTest.setTestStatus(TestStatus.RUNNING);
						multiCertExpiredTest.setComments("Creating direct message.");
						dao.updateTest(multiCertExpiredTest);
						
						final MimeMessage staEncryptedMsg = MessageBuilderUtils.createDirectMessage(publicCertResolvers, privateCertResolver, crypto, 
								goodBadCertMsg, Arrays.asList(PrivateCertResolver.EXPIRED, PrivateCertResolver.GOOD));
						
						
						System.out.println("Final encrypted message id: " + staEncryptedMsg.getMessageID() + "\r\n\r\n");
						
						multiCertExpiredTest.setCorrelationId(staEncryptedMsg.getMessageID());
						multiCertExpiredTest.setComments("Handing off to SMTP server.");
						dao.updateTest(multiCertExpiredTest);
						msgSender.sendMessage(staEncryptedMsg);
					}
					catch (Exception e)
					{
						multiCertExpiredTest.setTestStatus(TestStatus.ABORTED);
						multiCertExpiredTest.setComments("Messaging test aborted: " + e.getMessage());
						dao.updateTest(multiCertExpiredTest);
					}					
					
					// delivery notification
					final Test deliveryNoteTest = dao.initiateTest("Happy path, delivery notification IG", TestType.SEND_RELIABLE_GOOD_MESSAGE, testSuite.getTestSuiteid());
					try
					{
						System.out.println("Test name: " + deliveryNoteTest.getTestName());
						
						// send a message with a good and bad cert
						final MimeMessage reliableMsg = MessageBuilderUtils.createMimeMessage(
								Arrays.asList(to), localSender, deliveryNoteTest.getTestName(), "This is a reliable test message.", ccdaContent , null, null, true);
	
						
						System.out.println("Initial message id: " + reliableMsg.getMessageID());
						
						deliveryNoteTest.setTestStatus(TestStatus.RUNNING);
						deliveryNoteTest.setComments("Creating direct message.");
						dao.updateTest(deliveryNoteTest);
						
						
						
						final MimeMessage staEncryptedMsg = MessageBuilderUtils.createDirectMessage(publicCertResolvers, privateCertResolver, crypto, 
								reliableMsg, Arrays.asList(PrivateCertResolver.GOOD));
						
						System.out.println("Final encrypted message id: " + staEncryptedMsg.getMessageID() + "\r\n\r\n");
						
						deliveryNoteTest.setCorrelationId(staEncryptedMsg.getMessageID());
						deliveryNoteTest.setComments("Handing off to SMTP server.");
						dao.updateTest(deliveryNoteTest);
						
						msgSender.sendMessage(staEncryptedMsg);
					}
					catch (Exception e)
					{
						deliveryNoteTest.setTestStatus(TestStatus.ABORTED);
						deliveryNoteTest.setComments("Messaging test aborted: " + e.getMessage());
						dao.updateTest(deliveryNoteTest);
					}	
			
				}
				catch (Exception e)
				{
					
				}
				
				// now do the negative tests

	
				// generate the exception path messages


				// expired cert
				MimeMessage exceptionPathMessage = null;
				MimeMessage staEncryptedMsg = null;
				Test negativeTest = dao.initiateTest("Exception path, expired cert", TestType.SEND_NORMAL_BAD_MESSAGE, testSuite.getTestSuiteid());
				try
				{
					System.out.println("Test name: " + negativeTest.getTestName());
					
					negativeTest.setTestStatus(TestStatus.RUNNING);
					negativeTest.setComments("Creating direct message.");
					dao.updateTest(negativeTest);
					
					exceptionPathMessage = MessageBuilderUtils.createMimeMessage(Arrays.asList(to), localSender, negativeTest.getTestName(),
							"This is a test message.", null , null, null, false);
					
					System.out.println("Initial message id: " + exceptionPathMessage.getMessageID());
					
					staEncryptedMsg = MessageBuilderUtils.createDirectMessage(publicCertResolvers, privateCertResolver, crypto,
							exceptionPathMessage, Arrays.asList(PrivateCertResolver.EXPIRED));
					
					
					System.out.println("Final encrypted message id: " + staEncryptedMsg.getMessageID() + "\r\n\r\n");
					negativeTest.setCorrelationId(staEncryptedMsg.getMessageID());
					negativeTest.setComments("Handing off to SMTP server.");
					dao.updateTest(negativeTest);
					
					msgSender.sendMessage(staEncryptedMsg);
				}
				catch (Exception e)
				{
					negativeTest.setTestStatus(TestStatus.ABORTED);
					negativeTest.setComments("Exception message test aborted: " + e.getMessage());
					dao.updateTest(negativeTest);
				}
					
				negativeTest = dao.initiateTest("Exception path, non trusted cert", TestType.SEND_NORMAL_BAD_MESSAGE, testSuite.getTestSuiteid());
				try
				{
					System.out.println("Test name: " + negativeTest.getTestName());
					
					negativeTest.setTestStatus(TestStatus.RUNNING);
					negativeTest.setComments("Creating direct message.");
					dao.updateTest(negativeTest);
					// non trusted cert... chains to a different CA
					exceptionPathMessage = MessageBuilderUtils.createMimeMessage(Arrays.asList(to), localSender, negativeTest.getTestName(), ""
							+ "This is a test message.", null , null, null, false);
					
					System.out.println("Initial message id: " + exceptionPathMessage.getMessageID());
					
					
					
					staEncryptedMsg = MessageBuilderUtils.createDirectMessage(publicCertResolvers, privateCertResolver, crypto,
							exceptionPathMessage, Arrays.asList(PrivateCertResolver.NON_TRUSTED));
					
					
					System.out.println("Final encrypted message id: " + staEncryptedMsg.getMessageID() + "\r\n\r\n");
					negativeTest.setCorrelationId(staEncryptedMsg.getMessageID());
					negativeTest.setComments("Handing off to SMTP server.");
					dao.updateTest(negativeTest);
					
					msgSender.sendMessage(staEncryptedMsg);
				}
				catch (Exception e)
				{
					negativeTest.setTestStatus(TestStatus.ABORTED);
					negativeTest.setComments("Exception message test aborted: " + e.getMessage());
					dao.updateTest(negativeTest);				
				}
					
				// revoked cert
				negativeTest = dao.initiateTest("Exception path, revoked cert", TestType.SEND_NORMAL_BAD_MESSAGE, testSuite.getTestSuiteid());
				try
				{
					
					System.out.println("Test name: " + negativeTest.getTestName());
					
					negativeTest.setTestStatus(TestStatus.RUNNING);
					negativeTest.setComments("Creating direct message.");
					dao.updateTest(negativeTest);
					
					exceptionPathMessage = MessageBuilderUtils.createMimeMessage(Arrays.asList(to), localSender, negativeTest.getTestName(), ""
							+ "This is a test message.", null , null, null, false);
					
					System.out.println("Initial message id: " + exceptionPathMessage.getMessageID());
					
					staEncryptedMsg = MessageBuilderUtils.createDirectMessage(publicCertResolvers, privateCertResolver, crypto,
							exceptionPathMessage, Arrays.asList(PrivateCertResolver.REVOKED));
					
					System.out.println("Final encrypted message id: " + staEncryptedMsg.getMessageID() + "\r\n\r\n");
					
					negativeTest.setCorrelationId(staEncryptedMsg.getMessageID());
					negativeTest.setComments("Handing off to SMTP server.");
					dao.updateTest(negativeTest);
					
					msgSender.sendMessage(staEncryptedMsg);
				}
				catch (Exception e)
				{
					negativeTest.setTestStatus(TestStatus.ABORTED);
					negativeTest.setComments("Exception message test aborted: " + e.getMessage());
					dao.updateTest(negativeTest);				
				}
				
				
				// invalid digest
				negativeTest = dao.initiateTest("Exception path, invalid digest", TestType.SEND_NORMAL_BAD_MESSAGE, testSuite.getTestSuiteid());
				try
				{
					System.out.println("Test name: " + negativeTest.getTestName());
					
					
					negativeTest.setTestStatus(TestStatus.RUNNING);
					negativeTest.setComments("Creating direct message.");
					dao.updateTest(negativeTest);
					
					exceptionPathMessage = MessageBuilderUtils.createMimeMessage(Arrays.asList(to), localSender, negativeTest.getTestName(), ""
							+ "This is a test message.", null , null, null, false);
					
					System.out.println("Initial message id: " + exceptionPathMessage.getMessageID());
					
					
					staEncryptedMsg = MessageBuilderUtils.createDirectMessage(publicCertResolvers, privateCertResolver, crypto,
							exceptionPathMessage, Arrays.asList(PrivateCertResolver.REVOKED), false);
					
					
					System.out.println("Final encrypted message id: " + staEncryptedMsg.getMessageID() + "\r\n\r\n");
					
					negativeTest.setCorrelationId(staEncryptedMsg.getMessageID());
					negativeTest.setComments("Handing off to SMTP server.");
					dao.updateTest(negativeTest);
					
					msgSender.sendMessage(staEncryptedMsg);
				}
				catch (Exception e)
				{
					negativeTest.setTestStatus(TestStatus.ABORTED);
					negativeTest.setComments("Exception message test aborted: " + e.getMessage());
					dao.updateTest(negativeTest);				
				}

							
			}
			catch (Exception e)
			{
				try
				{
					testSuite.setTestStatus(TestStatus.ABORTED);
					dao.updateTestSuite(testSuite);
					
					// send a report message if need be
					if (!this.reportAddresses.isEmpty())
					{
						try
						{
							final MimeMessage reportMsg = MessageBuilderUtils.createMimeMessage(this.reportAddresses, localSender, 
									testSuite.getTestSuiteName() + " for " + testSuite.getTargetAddress(), testSuite.toString(), null, null, null, false);
						
							msgSender.sendMessage(reportMsg);
						}
						catch (Exception msgEx)
						{
							LOGGER.error("Failed to send report message: " + msgEx.getMessage(), msgEx);
						}
					}
				}
				catch (Exception e2){/*no op */}
			}
			
			try
			{
				// the the final version of the test suite
				final TestSuite finalTestSuite = dao.getTestSuite(this.testSuite.getTestSuiteid());
				
				// all of the tests have been kicked off... now send them to the monitor
				final Runnable monitor = testMonitorFactory.createTestMonitor(finalTestSuite, reportAddresses);
				testMonitorService.execute(monitor);
			}
			catch (Exception e) {/* no-op */}
		}
		
	}
	
	// CRL creation
	protected static class CRLCreationTask implements Runnable
	{
		protected final X509Certificate signingCert;
		protected final PrivateKey signingKey;
		protected File crlFile;
		protected final Collection<X509Certificate> revokedCerts;
		protected long crlNum = 1;
		
		public CRLCreationTask(X509Certificate signingCert, PrivateKey signingKey, 
				File crlFile, Collection<X509Certificate> revokedCerts)
		{
			this.signingCert = signingCert;
			this.signingKey = signingKey;
			this.crlFile = crlFile;
			this.revokedCerts = revokedCerts;
		}
		
		@Override
		public void run()
		{
			try
			{
				X509V2CRLGenerator   crlGen = new X509V2CRLGenerator();
				Date now = new Date();
				Calendar nextUpdate = Calendar.getInstance();
				nextUpdate.add(Calendar.DAY_OF_MONTH, 30);
				crlGen.setIssuerDN(signingCert.getSubjectX500Principal());
				
				crlGen.setThisUpdate(now);
				crlGen.setNextUpdate(nextUpdate.getTime());
				crlGen.setSignatureAlgorithm("SHA256WithRSAEncryption");
				
				for (X509Certificate revokedCert : revokedCerts)
					crlGen.addCRLEntry(revokedCert.getSerialNumber(), now, CRLReason.keyCompromise);
				
				crlGen.addExtension(X509Extensions.AuthorityKeyIdentifier,
		                false, new AuthorityKeyIdentifierStructure(signingCert));
				
				crlGen.addExtension(X509Extensions.CRLNumber,
		                false, new CRLNumber(BigInteger.valueOf(crlNum++)));
				
				X509CRL crl = crlGen.generate(signingKey, CryptoExtensions.getJCEProviderName());
				FileUtils.writeByteArrayToFile(crlFile, crl.getEncoded());		
			}
			catch (Exception e)
			{
				LOGGER.warn("Failed to create new CRL.", e);
			}
		}
	}
}
