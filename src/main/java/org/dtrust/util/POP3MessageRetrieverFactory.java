package org.dtrust.util;

public class POP3MessageRetrieverFactory implements MessageRetrieverFactory
{
	protected final String port;
	protected final String popServer;
	protected final String username;
	protected final String password;
	
	public POP3MessageRetrieverFactory(String port, String popServer, String user, String pass)
	{
		this.port = port;
		this.popServer = popServer;
		this.username = user;
		this.password = pass;
	}

	@Override
	public MessageRetriever createRetriver()
	{
		return new POP3MessageRetriever(port, popServer, username, password);
	}
	
	
}
