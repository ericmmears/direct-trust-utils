package org.dtrust.utils;

import java.util.Collection;
import java.util.Map;

import javax.mail.Message;
import javax.mail.internet.MimeMessage;

import org.dtrust.util.POP3MessageRetriever;
import org.junit.Test;
import org.nhindirect.common.tx.TxDetailParser;
import org.nhindirect.common.tx.TxUtil;
import org.nhindirect.common.tx.impl.DefaultTxDetailParser;
import org.nhindirect.common.tx.model.TxDetail;
import org.nhindirect.common.tx.model.TxMessageType;

public class POP3MessageRetrieverTest
{
	@Test
	public void getPop3Messages() throws Exception
	{
		final POP3MessageRetriever retriever = new POP3MessageRetriever("110", "localhost", "atabInterop", "1kingpuff");
		
		final Collection<Message> msgs = retriever.retrieveMessages(null);
		
		for (Message msg : msgs)
		{
			TxDetailParser parser = new DefaultTxDetailParser();
			TxMessageType msgType = TxUtil.getMessageType((MimeMessage)msg);

			final Map<String, TxDetail> details = parser.getMessageDetails((MimeMessage)msg);
				

		}
	}
}
