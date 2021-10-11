package org.dtrust.mailet;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import javax.mail.MessagingException;

import org.apache.commons.io.FileUtils;
import org.apache.mailet.Mail;
import org.apache.mailet.base.GenericMailet;
import org.nhindirect.stagent.parser.EntitySerializer;

public class RawMsgDumper extends GenericMailet
{
	public RawMsgDumper()
	{
		super();
	}

	@Override
	public void service(Mail mail) throws MessagingException 
	{
    	File fl = new File("/tmp/" + generateUniqueFileName());

    	try
    	{
    		FileUtils.writeStringToFile(fl, EntitySerializer.Default.serialize(mail.getMessage()));
    	}
    	catch (IOException e)
    	{
    		/*
    		 * TODO: Add exception handling
    		 */
    	}
		
	}
	
	private String generateUniqueFileName()
	{
		return UUID.randomUUID().toString() + ".eml";
	}
}
