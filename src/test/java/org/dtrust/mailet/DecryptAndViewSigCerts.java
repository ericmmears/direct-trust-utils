package org.dtrust.mailet;

import java.io.File;
import java.security.PrivateKey;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.mail.smime.CMSProcessableBodyPart;
import org.junit.Test;
import org.nhindirect.config.model.utils.CertUtils;
import org.nhindirect.config.model.utils.CertUtils.CertContainer;
import org.nhindirect.stagent.CryptoExtensions;
import org.nhindirect.stagent.cert.X509CertificateEx;
import org.nhindirect.stagent.cryptography.SMIMECryptographerImpl;
import org.nhindirect.stagent.mail.Message;
import org.nhindirect.stagent.mail.MimeEntity;

public class DecryptAndViewSigCerts 
{

	@SuppressWarnings("unchecked")
	@Test
	public void viewSigCerts() throws Exception
	{
		final Message msg = new Message(new MimeMessage( (Session)null, 
				FileUtils.openInputStream(new File("./src/test/resources/messages/BadDispRequest.txt"))));
		
		final CertContainer cont1 = CertUtils.toCertContainer(
				FileUtils.readFileToByteArray(new File("./src/test/resources/certs/direct.securehealthemail.com-keyEnc.p12")));
		
		final CertContainer cont2 = CertUtils.toCertContainer(
				FileUtils.readFileToByteArray(new File("./src/test/resources/certs/direct.securehealthemail.com-digSig.p12")));
		
		final X509CertificateEx decryptCert1 = X509CertificateEx.fromX509Certificate(cont1.getCert(), (PrivateKey)cont1.getKey());
		final X509CertificateEx decryptCert2 = X509CertificateEx.fromX509Certificate(cont2.getCert(), (PrivateKey)cont2.getKey());
		
		final SMIMECryptographerImpl crypto = new SMIMECryptographerImpl();
		
		final MimeEntity decryptEntity =  crypto.decrypt(msg.extractMimeEntity(), Arrays.asList(decryptCert1, decryptCert2));
		
		final ByteArrayDataSource dataSource = new ByteArrayDataSource(decryptEntity.getRawInputStream(), decryptEntity.getContentType());
		
		final MimeMultipart verifyMM = new MimeMultipart(dataSource);	
		
		final CMSSignedData signed = new CMSSignedData(new CMSProcessableBodyPart(verifyMM.getBodyPart(0)), verifyMM.getBodyPart(1).getInputStream());
		
		final CertStore certs = signed.getCertificatesAndCRLs("Collection", CryptoExtensions.getJCEProviderName());
		
		for (SignerInformation sigInfo : (Collection<SignerInformation>)signed.getSignerInfos().getSigners())	 
		{
			final Collection<? extends Certificate> certCollection = certs.getCertificates(sigInfo.getSID());
			if (certCollection != null)
			{
				for (Certificate cert : certCollection)
				{
					final X509Certificate validateCert = (X509Certificate)cert;
					FileUtils.writeByteArrayToFile(new File("sigCert3.der"), validateCert.getEncoded());
				}
			}
		}
		
	}
}
