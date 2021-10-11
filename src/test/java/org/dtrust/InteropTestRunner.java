package org.dtrust;

import org.apache.mina.util.AvailablePortFinder;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.webapp.WebAppContext;
import org.springframework.context.ApplicationContext;


public class InteropTestRunner
{
	private static Server server;
	private static int HTTPPort;
	private static String serviceURL;
	
	static
	{
		try
		{
			System.setProperty("derby.system.home", "target/data");	
		}
		catch (Exception e)
		{
			
		}
	}	
	
	
	public synchronized static void startInteropTestService() throws Exception
	{
		
		if (server == null)
		{
			/*
			 * Setup the configuration service server
			 */
			server = new Server();
			SocketConnector connector = new SocketConnector();
			
			HTTPPort = AvailablePortFinder.getNextAvailable( 8090 );
			connector.setPort(HTTPPort);

			// certificate service
			WebAppContext context = new WebAppContext("src/test/resources/webapp", "/");
			    	
			server.setSendServerVersion(false);
			server.addConnector(connector);
			server.addHandler(context);
		
			server.start();
			
			serviceURL = "http://localhost:" + HTTPPort + "/";
		
		}
	}
	
	public synchronized static ApplicationContext getSpringApplicationContext()
	{
		return TestApplicationContext.getApplicationContext();
	}

	public synchronized static boolean isServiceRunning()
	{
		return (server != null && server.isRunning());
	}
	
	public synchronized static void shutDownService() throws Exception
	{
		if (isServiceRunning())
		{
			server.stop();
			server = null;
		}
	}
	
	public synchronized static String getInteropTestServiceURL()
	{
		return serviceURL;
	}
}
