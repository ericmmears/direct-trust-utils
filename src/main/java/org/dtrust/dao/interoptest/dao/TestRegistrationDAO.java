package org.dtrust.dao.interoptest.dao;

import java.util.Collection;

import org.dtrust.dao.interoptest.entity.TestRegistration;

public interface TestRegistrationDAO
{
	public Collection<TestRegistration> getRegistrations() throws TestDAOException;
	
	public TestRegistration getRegistrationsById(long id) throws TestDAOException;
	
	public Collection<TestRegistration> getRegistrationsByUsername(String username) throws TestDAOException;
	
	public Collection<TestRegistration> getRegistrationsByAndRegAddress(String username, String regAddress) throws TestDAOException;
	
	public Collection<TestRegistration> getRegistrationsByAndSourceAddress(String username, String sourceAddress) throws TestDAOException;	

	public Collection<TestRegistration> getRegistrationsSourceAddress(String sourceAddress) throws TestDAOException;		
	
	public TestRegistration addRegistration(String username, String sourceAddress, String regAddress) throws TestDAOException;	
	
	public void deleteRegistration(long[] id) throws TestDAOException;
	
	public void cleanRegistrations() throws TestDAOException;
}
