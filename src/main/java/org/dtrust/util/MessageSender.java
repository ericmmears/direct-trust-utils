package org.dtrust.util;

import javax.mail.internet.MimeMessage;

public interface MessageSender
{
	public void sendMessage(MimeMessage msg) throws Exception;
}
