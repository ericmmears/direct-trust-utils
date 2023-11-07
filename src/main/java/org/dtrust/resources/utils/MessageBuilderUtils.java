package org.dtrust.resources.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Enumeration;
import java.util.UUID;

import javax.mail.Address;
import javax.mail.Header;
import javax.mail.Session;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.nhindirect.stagent.AgentError;
import org.nhindirect.stagent.NHINDException;
import org.nhindirect.stagent.NHINDStandard;
import org.nhindirect.stagent.cert.CertificateResolver;
import org.nhindirect.stagent.cryptography.Cryptographer;
import org.nhindirect.stagent.cryptography.SignedEntity;
import org.nhindirect.stagent.mail.Message;
import org.nhindirect.stagent.mail.MimeEntity;
import org.nhindirect.stagent.mail.WrappedMessage;

public class MessageBuilderUtils
{

	public static MimeMessage createDirectMessage(Collection<CertificateResolver> publicCertResolvers, CertificateResolver privateCertResolver,
			Cryptographer crypto, MimeMessage orgigMessage, Collection<String> certsToSignWith) throws Exception
	{
		return createDirectMessage(publicCertResolvers, privateCertResolver, crypto, orgigMessage, certsToSignWith, true);
	}
	
	@SuppressWarnings("unchecked")
	public static MimeMessage createDirectMessage(Collection<CertificateResolver> publicCertResolvers, CertificateResolver privateCertResolver,
			Cryptographer crypto, MimeMessage orgigMessage, Collection<String> certsToSignWith, boolean validDigest) throws Exception
	{
		// first wrap the message
		final Message msg = new Message(orgigMessage);
		final Message wrappedMsg =  WrappedMessage.create(msg, NHINDStandard.MailHeadersUsed);
		System.out.println("Wrapped Message Id: " + wrappedMsg.getMessageID());
		
		
		final Address[] addrs = orgigMessage.getRecipients(RecipientType.TO);
		final Collection<X509Certificate> encCerts = new ArrayList<X509Certificate>();
		
		// get the encryption certs
		for (Address addr : addrs)
			encCerts.addAll(resolvePublicCerts((InternetAddress)addr, publicCertResolvers));
		
		// get the signing certs
		final Collection<X509Certificate> signCerts = new ArrayList<X509Certificate>();
		for (String str : certsToSignWith)
			signCerts.addAll(privateCertResolver.getCertificates(new InternetAddress(str)));
		
		// now sign the message
		final SignedEntity signedEntity = crypto.sign(wrappedMsg, signCerts);
		
		if (!validDigest)
		{
			// create an invalid digest
			final MimeMessage tempMsg = new MimeMessage((Session)null);
			tempMsg.addRecipients(RecipientType.TO, orgigMessage.getAllRecipients());
			tempMsg.setFrom(orgigMessage.getFrom()[0]);
			tempMsg.setText(UUID.randomUUID().toString());
			tempMsg.saveChanges();
			final SignedEntity tempSignedEntity = crypto.sign(new Message(tempMsg), signCerts);
			
			// replace the message signature of the message we are going to send with this 
			// new digest that won't match
			signedEntity.setSignature(tempSignedEntity.getSignature());
			signedEntity.getMimeMultipart().removeBodyPart((signedEntity.getMimeMultipart().getCount() - 1));
			signedEntity.getMimeMultipart().addBodyPart(tempSignedEntity.getSignature());
		}
		
		// now encrypt the message
        final MimeEntity encryptedEntity = crypto.encrypt(signedEntity.getMimeMultipart(), encCerts);
        //
        // Alter message content to contain encrypted data
        //
        
        final File fl = new File("/tmp/" + UUID.randomUUID().toString() + ".eml");
        
        
        OutputStream str = null;
        try
        {	
        	str = new FileOutputStream(fl);
        	signedEntity.getMimeMultipart().writeTo(str);
        }
        catch (Exception e)
        {
        	e.printStackTrace();
        }
        finally
        {
        	IOUtils.closeQuietly(str);
        }
        
        InternetHeaders headers = new InternetHeaders();
        Enumeration<Header> eHeaders = wrappedMsg.getAllHeaders();
        while (eHeaders.hasMoreElements())
        {
        	Header hdr = eHeaders.nextElement();
        	headers.setHeader(hdr.getName(), hdr.getValue());
        }    
          
        eHeaders = encryptedEntity.getAllHeaders();
        while (eHeaders.hasMoreElements())
        {
        	Header hdr = (Header)eHeaders.nextElement();
        	headers.setHeader(hdr.getName(), hdr.getValue());
        }    	            

        return new Message(headers, encryptedEntity.getContentAsBytes());	
	}
	
	public static MimeMessage createMimeMessage(Collection<InternetAddress> to, InternetAddress localSender, String subject, 
			String text, byte[] ccda, byte[] image, byte[] pdf, boolean timelyAndReliable) throws Exception
	{
		final Calendar now = Calendar.getInstance();
		
		final MimeMessage retVal = new MimeMessage((Session)null);
		retVal.setFrom(localSender);
		retVal.addRecipients(RecipientType.TO, to.toArray(new InternetAddress[to.size()]));
		
		retVal.setSentDate(now.getTime());
		retVal.setSubject(subject);
		retVal.setHeader("Mime-Version", "1.0");
		
		if (timelyAndReliable)
		{
			retVal.addHeaderLine("Disposition-Notification-Options: X-DIRECT-FINAL-DESTINATION-DELIVERY=optional,true");
			retVal.addHeaderLine("Disposition-Notification-To: " + localSender.toString());
		}
		if (!StringUtils.isEmpty(text) && ccda == null && image == null && pdf == null)
		{
			// text only message
			retVal.setText(text);
		}
		else if (StringUtils.isEmpty(text) && ccda != null && image == null && pdf == null)
		{
			// CCDA only
			retVal.setHeader("Content-Type", "application/xml; name=health-summary.xml");
			retVal.setHeader("Content-Transfer-Encoding", "BASE64");
			retVal.setHeader("Content-Disposition", "attachment; filename=\"health-summary.xml\"");
			retVal.setContent(ccda, "application/xml");
			
		}
		else if (StringUtils.isEmpty(text) && ccda == null && image != null && pdf == null)
		{
			// CCDA only
			retVal.setHeader("Content-Type", "image/jpeg; name=health.jpg");
			retVal.setHeader("Content-Transfer-Encoding", "BASE64");
			retVal.setHeader("Content-Disposition", "attachment; filename=\"health.jpg\"");
			retVal.setContent(image, "image/jpeg");
			
		}
		else if (StringUtils.isEmpty(text) && ccda == null && image == null && pdf != null)
		{
			// CCDA only
			retVal.setHeader("Content-Type", "application/pdf; name=health-summary.pdf");
			retVal.setHeader("Content-Transfer-Encoding", "BASE64");
			retVal.setHeader("Content-Disposition", "attachment; filename=\"health-summary.pdf\"");
			retVal.setContent(pdf, "application/pdf");
		}
		else
		{
			// multipart mixed
			final MimeMultipart mm = new MimeMultipart();
			
			// check for text
			if (!StringUtils.isEmpty(text))
			{
				final MimeBodyPart part = new MimeBodyPart(new InternetHeaders(), text.getBytes());
				part.setText(text);
				mm.addBodyPart(part);
			}
			if (ccda != null)
			{
				final String base64Content = Base64.encodeBase64String(ccda);
				final MimeBodyPart part = new MimeBodyPart(new InternetHeaders(), base64Content.getBytes("ASCII"));
				part.addHeader("Content-Type", "application/xml; name=health-summary.xml");
				part.addHeader("Content-Transfer-Encoding", "BASE64");
				part.addHeader("Content-Disposition", "attachment; filename=\"health-summary.xml\"");
				
				mm.addBodyPart(part);
			}
			if (image != null)
			{
				final String base64Content = Base64.encodeBase64String(image);
				final MimeBodyPart part = new MimeBodyPart(new InternetHeaders(), base64Content.getBytes("ASCII"));
				part.addHeader("Content-Type", "image/jpeg; name=health.jpg");
				part.addHeader("Content-Transfer-Encoding", "BASE64");
				part.addHeader("Content-Disposition", "attachment; filename=\"health.jpg\"");
				
				mm.addBodyPart(part);
			}
			if (pdf != null)
			{
				final String base64Content = Base64.encodeBase64String(pdf);
				final MimeBodyPart part = new MimeBodyPart(new InternetHeaders(), base64Content.getBytes("ASCII"));
				part.addHeader("Content-Type", "application/pdf; name=health-summary.pdf");
				part.addHeader("Content-Transfer-Encoding", "BASE64");
				part.addHeader("Content-Disposition", "attachment; filename=\"health-summary.pdf\"");
				
				mm.addBodyPart(part);
			}
			
			retVal.setContent(mm);
		}
		
		
		retVal.saveChanges();
		
		return retVal;
	}
	
    public static Collection<X509Certificate> resolvePublicCerts(InternetAddress address, Collection<CertificateResolver> publicCertResolvers) throws NHINDException
    {
    	Collection<X509Certificate> certs = null;
        try
        {
            // try each resolver until it's found
        	for (CertificateResolver publicResolver : publicCertResolvers)
        	{
        		try
        		{
        			certs = publicResolver.getCertificates(address);
        		}
        		catch (NHINDException e)
        		{
        			// if we found some certs, but they are invalid, we are not
        			// suppose to move on
        			if (e.getError().equals(AgentError.AllCertsInResolverInvalid))
        				break;	
        			else 
        				throw e;
        		}
        		if (certs != null)
        			break;
        	}
        }
        catch (Exception ex)
        {
        	throw new RuntimeException("Exception when looking up certificates: " + ex.getMessage(), ex);
        }

        return certs;
    }
}
