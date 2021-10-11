package org.dtrust.util;

import java.util.Collection;

import javax.mail.Message;
import javax.mail.search.SearchTerm;

public interface MessageRetriever
{
	public void connect() throws Exception;
	
	public Collection<Message> retrieveMessages(SearchTerm searchTerm) throws Exception;
	
	public void disconnect() throws Exception;
}
