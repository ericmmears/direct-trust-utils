package org.dtrust.client;

import java.util.Collection;

import org.dtrust.dao.interoptest.entity.TestRegistration;
import org.nhindirect.common.rest.exceptions.ServiceException;

public interface TestRegService
{
	public Collection<TestRegistration> getReportAddrBySourceAddr(String sourceAddr) throws ServiceException;
}
