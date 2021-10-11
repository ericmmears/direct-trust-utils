package org.dtrust.client.impl;

import java.util.Collection;

import org.apache.http.client.HttpClient;
import org.dtrust.client.TestRegService;
import org.dtrust.dao.interoptest.entity.TestRegistration;
import org.nhindirect.common.rest.AbstractSecuredService;
import org.nhindirect.common.rest.ServiceSecurityManager;
import org.nhindirect.common.rest.exceptions.ServiceException;

public class TestRegServiceImpl extends AbstractSecuredService implements TestRegService
{
    public TestRegServiceImpl(String serviceUrl, HttpClient httpClient, 
    		ServiceSecurityManager securityManager) 
    {	
        super(serviceUrl, httpClient, securityManager);
    }

	@Override
	public Collection<TestRegistration> getReportAddrBySourceAddr(String sourceAddr) throws ServiceException
	{
		return callWithRetry(new GetTestRegBySourceAddrRequest(httpClient, serviceURL, jsonMapper, securityManager, sourceAddr));	
	}   
}
