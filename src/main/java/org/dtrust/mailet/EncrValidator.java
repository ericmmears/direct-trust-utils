package org.dtrust.mailet;

import java.lang.reflect.Constructor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.InternetAddress;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.cms.RecipientId;
import org.bouncycastle.cms.RecipientInformationStore;
import org.bouncycastle.mail.smime.SMIMEEnveloped;
import org.nhind.config.ConfigurationServiceProxy;
import org.nhindirect.gateway.smtp.config.cert.impl.ConfigServiceCertificateStore;
import org.nhindirect.stagent.NHINDAddressCollection;
import org.nhindirect.stagent.cert.CertificateStore;
import org.nhindirect.stagent.cryptography.EncryptionAlgorithm;
import org.nhindirect.stagent.cryptography.SMIMEStandard;
import org.nhindirect.stagent.cryptography.activekeyops.DefaultDirectRecipientInformationFactory;
import org.nhindirect.stagent.cryptography.activekeyops.DirectRecipientInformation;
import org.nhindirect.stagent.mail.Message;
import org.nhindirect.stagent.mail.MimeEntity;
import org.nhindirect.stagent.mail.MimeException;
import org.nhindirect.stagent.mail.MimeStandard;

public class EncrValidator 
{
	private static final Log LOGGER = LogFactory.getFactory().getInstance(EncrValidator.class);	
	
	private static CertificateStore store;
	
	static Map<String, String> validEncAlgs;
	
	static
	{
		validEncAlgs = new HashMap<String, String>();
		
		validEncAlgs.put(EncryptionAlgorithm.AES128.getOID(), EncryptionAlgorithm.AES128.getAlgName());
		validEncAlgs.put(EncryptionAlgorithm.AES128_CBC.getOID(), EncryptionAlgorithm.AES128_CBC.getAlgName());
		validEncAlgs.put(EncryptionAlgorithm.AES192.getOID(), EncryptionAlgorithm.AES192.getAlgName());
		validEncAlgs.put(EncryptionAlgorithm.AES192_CBC.getOID(), EncryptionAlgorithm.AES192_CBC.getAlgName());
		validEncAlgs.put(EncryptionAlgorithm.AES256.getOID(), EncryptionAlgorithm.AES256.getAlgName());
		validEncAlgs.put(EncryptionAlgorithm.AES256_CBC.getOID(), EncryptionAlgorithm.AES256_CBC.getAlgName());
	}
	
	
	/**
	 * Mainly used for unit testing purposes
	 */
	public static synchronized void setCertificateStore(CertificateStore newStore)
	{
		store = newStore;
	}
	
	public static final GTABValidationReportAttr validateEncryption(Message msg, GTABValidationReportAttr report, 
			NHINDAddressCollection recips, ConfigurationServiceProxy proxy, boolean validateCertKeyUsage)
	{
		// make sure it is encrypted
		if (isEncrypted(msg))
		{
			// get the encrypted attributes
			final GTABValidationReportAttr.EncryReport encrReport = new GTABValidationReportAttr.EncryReport();
			try 
			{
				SMIMEEnveloped env = new SMIMEEnveloped(msg);

				encrReport.encouteredOID = env.getEncryptionAlgOID();
				if (!isValidEncryAlg(encrReport.encouteredOID))
				{
					// invalid encryption algorithm... bail
					encrReport.encrValid = false;
					encrReport.comment = "Invalid encryption algorithm.";
					report.encrReport = encrReport;
					return report;
				}

				
				if (validateCertKeyUsage)
				{
					// get any certificates we may have for this message
					final Collection<X509Certificate> possibleEncCerts = getRecipCerts(recips,proxy);
					
					// makes sure that the sending system only encrypt with certificates that have the key encipherment bit
		            for (X509Certificate decryptCert : possibleEncCerts)
		            {   
			            final RecipientId recId = generateRecipientSelector(decryptCert);
				
			            // check if this certificate is in the message
				        final RecipientInformationStore recipients = env.getRecipientInfos();
				        final DirectRecipientInformation recipient = (new DefaultDirectRecipientInformationFactory("BC")).createInstance(recipients.get(recId), env);	
				        if (recipient == null)
				        	continue; // certificate is not in the message... move on
		
				        // this certificate is in the message
				        // validate that the sender only uses certificates that assert
				        // the key encipherment key usage
				        boolean[] usages = decryptCert.getKeyUsage();
				        // key encipherment is at location [2]
				        if (usages == null || !usages[2])
				        {
							// invalid key usage
							encrReport.encrValid = false;
							encrReport.comment = "A certificate used to encrypt the message did not assert the key encipherment key usage flag";
							report.encrReport = encrReport;
							return report;
				        }
				       
		            }
				}
				
				encrReport.encrValid = true;
				encrReport.comment = "Encryption validation successful.";
				report.encrReport = encrReport;
	            
			}
			catch (Exception e) 
			{
				LOGGER.error("Error validating encryption.", e);
				// unexpected error
				encrReport.encrValid = false;
				encrReport.comment = "Unexpected Error: " + e.getMessage();
				report.encrReport = encrReport;
				return report;
			} 
		}
		else
		{
			final GTABValidationReportAttr.EncryReport encrReport = new GTABValidationReportAttr.EncryReport();
			encrReport.encrValid = false;
			encrReport.comment = "Message does not have proper encryption headers.";
			
			report.encrReport = encrReport;
		}
		
		return report;
	}
	
	protected static boolean isValidEncryAlg(String OID)
	{
		return (validEncAlgs.get(OID) != null);
	}
	
	protected static boolean isEncrypted(Message msg)
	{
		final MimeEntity encryptedEntity = msg.extractMimeEntity();
		
		try
		{
			encryptedEntity.verifyContentType(SMIMEStandard.EncryptedContentTypeHeaderValue);
			encryptedEntity.verifyTransferEncoding(MimeStandard.TransferEncodingBase64);
		}
		catch (MimeException e)
		{
			return false;
		}
		
		return true;
	}
	
	public static Collection<X509Certificate> getRecipCerts(NHINDAddressCollection recips, ConfigurationServiceProxy proxy)
	{
		final Collection<X509Certificate> retVal = new ArrayList<X509Certificate>();
		
		final CertificateStore certStore = (store == null) ? new ConfigServiceCertificateStore(proxy) : store;
		
		for (InternetAddress recip : recips.toInternetAddressCollection())
		{
			final Collection<X509Certificate> lookupCerts = certStore.getCertificates(recip);
			if (lookupCerts != null)
				retVal.addAll(lookupCerts);
		}
		
		return retVal;
	}
	
    /*
     * Construct an RecipientId.  Added private function to support multiple versions of BC libraries.
     */
    @SuppressWarnings("unchecked")
	protected static RecipientId generateRecipientSelector(X509Certificate decryptCert)
    {
    	RecipientId retVal = null;
    	Class<RecipientId> loadClass = null;
    	
    	try
    	{
    		// support for bcmail-jdk15-146
    		loadClass = (Class<RecipientId>)EncrValidator.class.getClassLoader().loadClass("org.bouncycastle.cms.jcajce.JceKeyTransRecipientId");
    		
    		final Constructor<RecipientId> constructor = loadClass.getConstructor(X509Certificate.class);
    		retVal = constructor.newInstance(decryptCert);
    	}
    	catch (Throwable e)
    	{
    		if (LOGGER.isDebugEnabled())
    		{
    			final StringBuilder builder = new StringBuilder("bcmail-jdk15-146 org.bouncycastle.cms.jcajce.JceKeyTransRecipientId class not found.");
    			builder.append("\r\n\tAttempt to fall back to bcmail-jdk15-140 org.bouncycastle.cms.RecipientId");
    			LOGGER.debug(builder.toString());
    		}
    	}
    	
    	if (retVal == null)
    	{
    		try
    		{
	    		// fall back to bcmail-jdk15-140 interfaces
	    		loadClass = (Class<RecipientId>)EncrValidator.class.getClassLoader().loadClass("org.bouncycastle.cms.RecipientId");

	    		retVal = loadClass.newInstance();
	    		
	    		retVal.setSerialNumber(decryptCert.getSerialNumber());
	    		retVal.setIssuer(decryptCert.getIssuerX500Principal().getEncoded());
    		}
        	catch (Throwable e)
        	{
         		LOGGER.error("Attempt to fall back to bcmail-jdk15-140 org.bouncycastle.cms.RecipientId failed.", e);
        	}    		
    	}
    	
    	return retVal;

    }
}
