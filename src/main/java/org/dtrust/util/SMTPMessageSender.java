package org.dtrust.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Properties;

import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import com.sun.mail.util.MailSSLSocketFactory;

public class SMTPMessageSender implements MessageSender
{
	protected final String port;
	protected final String smtpServer;
	protected final String username;
	protected final String password;
	protected MailSSLSocketFactory socketFactory;
	
	public SMTPMessageSender(String port, String smtpServer, String username, String password)
	{
		this.port = port;
		this.smtpServer = smtpServer;
		this.username = username;
		this.password = password;
		
		try
		{	
			socketFactory = new MailSSLSocketFactory();
	    	socketFactory.setTrustAllHosts(true);
		}
		catch (Exception e)
		{
			
		}
	}
	
	public void sendMessage(final MimeMessage msg) throws Exception
	{
		final Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", smtpServer);
		props.put("mail.smtp.port", port);
		props.put("mail.smtps.ssl.socketFactory", socketFactory);
		props.put("mail.smtps.ssl.trust", "*");
		props.put("mail.smtps.ssl.checkserveridentity", "false");
		props.put("mail.smtp.ssl.socketFactory", socketFactory);
		props.put("mail.smtp.ssl.trust", "*");
		props.put("mail.smtp.ssl.checkserveridentity", "false");
		
		final Session session = Session.getInstance(props, new javax.mail.Authenticator() 
		{
			protected PasswordAuthentication getPasswordAuthentication() 
			{
				return new PasswordAuthentication(username, password);
			}
		});
		
		final ByteArrayOutputStream outStr = new ByteArrayOutputStream();
		msg.writeTo(outStr);
		final ByteArrayInputStream inputStr = new ByteArrayInputStream(outStr.toByteArray());
		final MimeMessage sessionMessage = new MimeMessage(session, inputStr)
		{
			@Override
			protected void updateMessageID() 
			{
				try
				{
					setHeader("Message-ID", msg.getMessageID());
				}
				catch (Exception e)
				{
					
				}
			}
		};
		
		System.out.println("Transmission message id: " + sessionMessage.getMessageID());
		
		Transport.send(sessionMessage);
	}
}
