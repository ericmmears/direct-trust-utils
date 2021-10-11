package org.dtrust.client.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.codehaus.jackson.map.ObjectMapper;
import org.dtrust.dao.interoptest.entity.TestRegistration;
import org.nhindirect.common.rest.AbstractGetRequest;
import org.nhindirect.common.rest.ServiceSecurityManager;
import org.nhindirect.common.rest.exceptions.ServiceException;
import org.nhindirect.common.rest.exceptions.ServiceMethodException;
public class GetTestRegBySourceAddrRequest extends AbstractGetRequest<TestRegistration>
{
	private final String sourceAddr;
	
	private static final Log LOGGER = LogFactory.getFactory().getInstance(GetTestRegBySourceAddrRequest.class);	
	
    public GetTestRegBySourceAddrRequest(HttpClient httpClient, String certServerUrl,
            ObjectMapper jsonMapper, ServiceSecurityManager securityManager, String sourceAddr) 
    {
        super(httpClient, certServerUrl, jsonMapper, securityManager, true);
        
        if (sourceAddr == null || sourceAddr.isEmpty())
        	throw new IllegalArgumentException("Source address cannot be null or empty");
        
        this.sourceAddr = sourceAddr;
    }
    
    @Override
    protected String getRequestUri() throws ServiceException
    {
    	final StringBuilder builder = new StringBuilder(serviceUrl);
    	if (!serviceUrl.endsWith("/"))
    		builder.append("/");
    	
    	builder.append("interopReg/sourceAdd/").append(uriEscape(sourceAddr));
    	
    	LOGGER.info("Getting test regs from " + builder.toString());
    	
    	return builder.toString();//serviceUrl + "/interopReg/sourceAdd/" + uriEscape(sourceAddr);
    }
    
    @Override
    protected Collection<TestRegistration> interpretResponse(int statusCode, HttpResponse response)
            throws IOException, ServiceException 
    {
        switch (statusCode) 
        {
        	case 200:
        		return super.interpretResponse(statusCode, response);        		
        	case 204:	
        		return Collections.emptyList();
        	case 404:
	            throw new ServiceMethodException(404, "Failed to locate target service. Is '"
	                    + serviceUrl + "' the correct URL?");
        	///CLOVER:OFF
        	default:
        		return super.interpretResponse(statusCode, response);
        	///CLOVER:ON
        }
    }         
}
