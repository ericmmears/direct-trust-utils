package org.dtrust.resources.utils;

import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;

import javax.mail.internet.InternetAddress;

import org.apache.commons.io.IOUtils;
import org.nhindirect.gateway.smtp.config.cert.impl.CertStoreUtils;
import org.nhindirect.stagent.cert.CertificateResolver;

public class PrivateCertResolver implements CertificateResolver
{
	public static final String GOOD = "good";
	public static final String EXPIRED = "expired";
	public static final String REVOKED = "revoked";
	public static final String NON_TRUSTED = "nontrusted";
	
	protected boolean loadedCerts = false;
	
	protected String goodCertificateFile;
	protected String expiredCertificateFile;
	protected String revokedCertificateFile;
	protected String nonTrustedCertificateFile;
	
	protected X509Certificate goodCertificate;
	protected X509Certificate expiredCertificate;
	protected X509Certificate revokedCertificate;
	protected X509Certificate nonTrustedCertificate;
	
	public PrivateCertResolver()
	{
		
	}

	
	
	public void setGoodCertificateFile(String goodCertificateFile)
	{
		this.goodCertificateFile = goodCertificateFile;
	}



	public void setExpiredCertificateFile(String expiredCertificateFile)
	{
		this.expiredCertificateFile = expiredCertificateFile;
	}



	public void setRevokedCertificateFile(String revokedCertificateFile)
	{
		this.revokedCertificateFile = revokedCertificateFile;
	}



	public void setNonTrustedCertificateFile(String nonTrustedCertificateFile)
	{
		this.nonTrustedCertificateFile = nonTrustedCertificateFile;
	}



	@Override
	public Collection<X509Certificate> getCertificates(InternetAddress address)
	{
		loadCertificates();
		
		if (address.toString() == GOOD)
			return Arrays.asList(goodCertificate);
		else if (address.toString() == EXPIRED)
			return Arrays.asList(expiredCertificate);
		else if (address.toString() == REVOKED)
			return Arrays.asList(revokedCertificate);
		else if (address.toString() == NON_TRUSTED)
 			return Arrays.asList(nonTrustedCertificate);
		
		return null;
	}
	
	protected synchronized void loadCertificates()
	{
		if (!loadedCerts)
		{
			try
			{
				
				// load certificate from a resource
				goodCertificate = loadCertFromResource(goodCertificateFile);
				expiredCertificate = loadCertFromResource(expiredCertificateFile);
				revokedCertificate = loadCertFromResource(revokedCertificateFile);
				nonTrustedCertificate = loadCertFromResource(nonTrustedCertificateFile);
				
				loadedCerts = true;
				
			}
			catch (Exception e)
			{
				throw new IllegalStateException("Could not load certificates: " + e.getMessage(), e);
			}
		}
	}
	
	public static X509Certificate loadCertFromResource(String resourceName) throws Exception
	{
		InputStream ioStream = null;
		try
		{
			ioStream = PrivateCertResolver.class.getClassLoader().getResourceAsStream(resourceName);		
			return CertStoreUtils.certFromData(null, IOUtils.toByteArray(ioStream));
		}
		finally
		{
			IOUtils.closeQuietly(ioStream);
		}
	}
}
