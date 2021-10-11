package org.dtrust.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.SearchTerm;

import com.sun.mail.util.MailSSLSocketFactory;

public class POP3MessageRetriever implements MessageRetriever
{
	protected final String port;
	protected final String popServer;
	protected final String username;
	protected final String password;
	protected MailSSLSocketFactory socketFactory;
	
	protected Session session;
	protected Folder inbox;
	protected Store store;
	
	public POP3MessageRetriever(String port, String popServer, String user, String pass)
	{
		this.port = port;
		this.popServer = popServer;
		this.username = user;
		this.password = pass;
		this.session = null;
		try
		{	
			socketFactory = new MailSSLSocketFactory();
	    	socketFactory.setTrustAllHosts(true);
		}
		catch (Exception e)
		{
			
		}
		

	}

	@Override
	public synchronized void connect() throws Exception
	{
		final Properties props = new Properties();
		props.put("mail.pop3.auth", "true");
		props.put("mail.pop3.starttls.enable", "true");
		props.put("mail.pop3.host", popServer);
		props.put("mail.pop3.port", port);
		props.put("mail.pop3.ssl.socketFactory", socketFactory);
		props.put("mail.pop3.ssl.trust", "*");
		props.put("mail.pop3.ssl.checkserveridentity", "false");
		props.put("mail.pop3.ssl.socketFactory", socketFactory);
		props.put("mail.pop3.ssl.trust", "*");
		props.put("mail.pop3.ssl.checkserveridentity", "false");
		
		session = Session.getInstance(props, new javax.mail.Authenticator() 
		{
			protected PasswordAuthentication getPasswordAuthentication() 
			{
				return new PasswordAuthentication(username, password);
			}
		});
		
		store = session.getStore("pop3");
		store.connect();
		
	    inbox = store.getFolder("inbox");
	    inbox.open(Folder.READ_WRITE);
	}
	
	@Override
	public synchronized void disconnect() throws Exception
	{
		inbox.close(true);
		inbox = null;
		store.close();
		store = null;
		
		session = null;
	}
	
	@Override
	public Collection<Message> retrieveMessages(SearchTerm searchTerm) throws Exception
	{
		final Message messages[] = (searchTerm != null) ? inbox.search(searchTerm) : inbox.getMessages();
		
		if (messages == null || messages.length == 0)
			return Collections.emptyList();
		
		return Arrays.asList(messages);
	}
	
	
}
