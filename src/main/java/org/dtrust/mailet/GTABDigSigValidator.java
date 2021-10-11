package org.dtrust.mailet;

import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.MimeMultipart;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.mail.smime.CMSProcessableBodyPart;
import org.nhindirect.stagent.CryptoExtensions;
import org.nhindirect.stagent.cryptography.DigestAlgorithm;

public class GTABDigSigValidator 
{
	private static final Log LOGGER = LogFactory.getFactory().getInstance(GTABDigSigValidator.class);	
	
	static Map<String, String> validDigestAlgs;
	
	static
	{
		validDigestAlgs = new HashMap<String, String>();
		
		validDigestAlgs.put(DigestAlgorithm.SHA256.getOID(), DigestAlgorithm.SHA256.getAlgName());
		validDigestAlgs.put(DigestAlgorithm.SHA384.getOID(), DigestAlgorithm.SHA384.getAlgName());
		validDigestAlgs.put(DigestAlgorithm.SHA512.getOID(), DigestAlgorithm.SHA512.getAlgName());
	}
	
	@SuppressWarnings("unchecked")
	public static final GTABValidationReportAttr validateDigSig(MimeMultipart msg, GTABValidationReportAttr report)
	{
		final GTABValidationReportAttr.DigSigReport digSigReport = new GTABValidationReportAttr.DigSigReport();
		try
		{
			// get the signature as a CMS String data object
			final CMSSignedData signed = new CMSSignedData(new CMSProcessableBodyPart(msg.getBodyPart(0)), msg.getBodyPart(1).getInputStream());
			
			final CertStore certs = signed.getCertificatesAndCRLs("Collection", CryptoExtensions.getJCEProviderName());
			
			// go through all the signers and make sure all requirements are satisfied
    		for (SignerInformation sigInfo : (Collection<SignerInformation>)signed.getSignerInfos().getSigners())	 
    		{
    			digSigReport.encouteredOID = sigInfo.getDigestAlgOID();
    			if (!isValidDigSigAlg(digSigReport.encouteredOID))
    			{
					// dig sig algorithm... bail
    				digSigReport.digSigValid = false;
    				digSigReport.comment = "Invalid digest algorithm.";
					report.digSigReport = digSigReport;
					return report;
    			}
    			
    			final Collection<? extends Certificate> certCollection = certs.getCertificates(sigInfo.getSID());
    			if (certCollection != null)
    			{
    				for (Certificate cert : certCollection)
    				{
    					final X509Certificate validateCert = (X509Certificate)cert;
    					
    			        // validate that the sender only uses certificates that assert
    			        // the digital signature key usage
    			        boolean[] usages = validateCert.getKeyUsage();
    			        // digital signature is at location [0]
    			        if (usages == null || !usages[0])
    			        {
    						// invalid key usage
    			        	digSigReport.digSigValid = false;
    			        	digSigReport.comment = "A certificate used to encrypt the message did not assert the digital signature key usage flag";
    						report.digSigReport = digSigReport;
    						return report;
    			        }
    				}
    			}		
    		}
			
	        digSigReport.digSigValid = true;
	        digSigReport.comment = "Digital signature validation successful.";
			report.digSigReport = digSigReport;
		}
		catch (Exception e)
		{
			LOGGER.error("Error validating digital signature.", e);
			// unexpected error
        	digSigReport.digSigValid = false;
        	digSigReport.comment = "Unexpected Error: " + e.getMessage();
			report.digSigReport = digSigReport;
			return report;
		}
		return report;
	}
	
	protected static boolean isValidDigSigAlg(String OID)
	{
		return (validDigestAlgs.get(OID) != null);
	}
}
