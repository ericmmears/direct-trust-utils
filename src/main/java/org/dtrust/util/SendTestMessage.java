package org.dtrust.util;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang.StringUtils;

public class SendTestMessage 
{
	protected static String recipient;
	protected static String sender;
	protected static String server;
	protected static String port;
	protected static String username;
	protected static String password;
	protected static String dispatch = "false";
	
	public static void main(String[] args)
	{
		if (args.length == 0)
		{
            printUsage();
            System.exit(-1);			
		}
		
		// Check parameters
        for (int i = 0; i < args.length; i++)
        {
            String arg = args[i];

            // Options
            if (!arg.startsWith("-"))
            {
                System.err.println("Error: Unexpected argument [" + arg + "]\n");
                printUsage();
                System.exit(-1);
            }
            else if (arg.equalsIgnoreCase("-to"))
            {
                if (i == args.length - 1 || args[i + 1].startsWith("-"))
                {
                    System.err.println("Error: Missing message recipient");
                    System.exit(-1);
                }
                
                recipient = args[++i];
                
            }
            else if (arg.equalsIgnoreCase("-from"))
            {
                if (i == args.length - 1 || args[i + 1].startsWith("-"))
                {
                    System.err.println("Error: Missing message sender");
                    System.exit(-1);
                }
                
                sender = args[++i];
                
            }
            else if (arg.equalsIgnoreCase("-server"))
            {
                if (i == args.length - 1 || args[i + 1].startsWith("-"))
                {
                    System.err.println("Error: Missing SMTP server name");
                    System.exit(-1);
                }
                
                server = args[++i];
                
            }
            else if (arg.equalsIgnoreCase("-port"))
            {
                if (i == args.length - 1 || args[i + 1].startsWith("-"))
                {
                    System.err.println("Error: Missing SMTP port");
                    System.exit(-1);
                }
                
                port = args[++i];
                
            }            
            else if (arg.equalsIgnoreCase("-username"))
            {
                if (i == args.length - 1 || args[i + 1].startsWith("-"))
                {
                    System.err.println("Error: Missing SMTP username");
                    System.exit(-1);
                }
                
                username = args[++i];
                
            } 
            else if (arg.equalsIgnoreCase("-password"))
            {
                if (i == args.length - 1 || args[i + 1].startsWith("-"))
                {
                    System.err.println("Error: Missing SMTP password");
                    System.exit(-1);
                }
                
                password = args[++i];
                
            } 
            else if (arg.equalsIgnoreCase("-dispatch"))
            {
                if (i == args.length - 1 || args[i + 1].startsWith("-"))
                {
                    System.err.println("Error: Missing dispatch flag");
                    System.exit(-1);
                }
                
                dispatch = args[++i];
                
            }             
            else if (arg.equals("-help"))
            {
                printUsage();
                System.exit(-1);
            }            
            else
            {
                System.err.println("Error: Unknown argument " + arg + "\n");
                printUsage();
                System.exit(-1);
            }            
        }
        
        if (StringUtils.isEmpty(recipient) || StringUtils.isEmpty(sender) || StringUtils.isEmpty(port) ||
        		StringUtils.isEmpty(server) || StringUtils.isEmpty(username) || StringUtils.isEmpty(password))
        {
            System.err.println("Error: Missing a required field.\n");
            printUsage();
            System.exit(-1);
        }
        
        sendTestMessage();
	}
	
	protected static void sendTestMessage()
	{
		try
		{
			final SMTPMessageSender msgSender = new SMTPMessageSender(port, server, username, password);
			
			
			final MimeMessage msg = new MimeMessage((Session)null);
			msg.setFrom(new InternetAddress(sender));
			msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
			msg.setSubject("A test message");
			msg.setText("This is a plain text test message");
			
			if (dispatch.compareToIgnoreCase("true") == 0)
			{
				msg.setHeader("Disposition-Notification-Options", "X-DIRECT-FINAL-DESTINATION-DELIVERY=optional,true");
			}
			
			msg.saveChanges();
			
			msgSender.sendMessage(msg);
			
			System.out.println("Message successfully transfered to server " + server);
		}
		catch (Exception e)
		{
			System.err.println("Failed to send message: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
    private static void printUsage()
    {
        StringBuffer use = new StringBuffer();
        use.append("Usage:\n");
        use.append("java SendTestMessage (options)...\n\n");
        use.append("options:\n");
        use.append("-to		The message recipeint.\n");
        use.append("\n");
        use.append("-from	The message sender.\n");
        use.append("\n");        
        use.append("-server     The SMTP server used to send the message\n");       
        use.append("\n");          
        use.append("-username    Username to log into the SMTP server.\n");
        use.append("\n");          
        use.append("-password    Password to log into the SMTP server.\n");  
        use.append("-dispatch    Optional boolean string indicating if a dispatch notification is being requested.\n");
        use.append("			 Default: false\n\n");         

        System.err.println(use);        
    }	
}
