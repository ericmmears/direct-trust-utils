package org.dtrust.resources.utils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.Test;

public class CreateMimeMessageTest 
{
	@Test
	public void testCreateMimeMessage() throws Exception
	{

		final Collection<InternetAddress> reportAddresses = new ArrayList<InternetAddress>();
		
		reportAddresses.add(new InternetAddress("me@you.com"));
		reportAddresses.add(new InternetAddress("myself@you.com"));
		
		final InternetAddress localSender = new InternetAddress("local@sender.com");
		
		final MimeMessage reportMsg = MessageBuilderUtils.createMimeMessage(reportAddresses, localSender, 
			"This is a test message", "This is the mail text.", null, null, null, false);
		
		final ByteArrayOutputStream oStream = new ByteArrayOutputStream();
		
		reportMsg.writeTo(oStream);
		
		String rawMsg = new String(oStream.toByteArray());
		
		System.out.println(rawMsg);
	}
}
