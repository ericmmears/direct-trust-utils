package org.dtrust.utils;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.dtrust.providers.DTJSONProvider;
import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class TestUtils
{
	private static final Integer CONNECTION_TIMEOUT = 10000; // 10 seconds
	private static final Integer READ_TIMEOUT = 10000000; // 10 seconds
	
	@Test
	public void dummy()
	{
		
	}
	
    public static final String uriEscape(String val) 
    {
        try 
        {
            final String escapedVal = URLEncoder.encode(val, "UTF-8");
            // Spaces are treated differently in actual URLs. There don't appear to be any other
            // differences...
            return escapedVal.replace("+", "%20");
        } 
        catch (UnsupportedEncodingException e) 
        {
            throw new RuntimeException("Failed to encode value: " + val, e);
        }
    }
	
	public static WebResource getResource(String serviceURL)
	{
		final ClientConfig config = new DefaultClientConfig();

		config.getSingletons().add(new DTJSONProvider());

		// need to set timeouts so we don't block forever in the event of a bad URL or hung web server
		config.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
		config.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, READ_TIMEOUT);
		
		
		final Client client = Client.create(config);
		WebResource resource = client.resource(serviceURL);
		
		return resource;

	}
}
