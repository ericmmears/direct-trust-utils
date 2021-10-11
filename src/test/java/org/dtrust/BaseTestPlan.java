package org.dtrust;

import org.dtrust.dao.interoptest.dao.TestRegistrationDAO;
import org.dtrust.dao.interoptest.dao.TestSuiteDAO;
import org.dtrust.dao.interoptest.dao.UserDAO;
import org.nhindirect.common.rest.BootstrapBasicAuthServiceSecurityManager;
import org.nhindirect.common.rest.ServiceSecurityManager;
import org.springframework.beans.factory.annotation.Autowired;


public abstract class BaseTestPlan
{
	@Autowired
	protected TestSuiteDAO testSuiteDao;
	
	@Autowired
	protected TestRegistrationDAO testRegDao;
	
	@Autowired
	protected UserDAO userDao;
	
	public void perform() throws Exception 
	{
		try 
		{
			setupMocks();
			Exception exception = null;
			try 
			{
				performInner();
			} 
			catch (Exception e) 
			{
				exception = e;
			}
			assertException(exception);
		} 
		finally 
		{
			tearDownMocks();
		}
	}

	protected abstract void performInner() throws Exception;

	protected void setupMocks() throws Exception
	{
		
		testSuiteDao = (TestSuiteDAO)InteropTestRunner.getSpringApplicationContext().getBean("testSuiteDAOImpl");
		testRegDao = (TestRegistrationDAO)InteropTestRunner.getSpringApplicationContext().getBean("testRegistrationDAOImpl");
		userDao = (UserDAO)InteropTestRunner.getSpringApplicationContext().getBean("userDAOImpl");
	}

	protected void tearDownMocks() throws Exception
	{
		testSuiteDao.cleanTestSuites();
	}

	protected void assertException(Exception exception) throws Exception {
		// default case should not throw an exception
		if (exception != null) {
			throw exception;
		}
	}
	
	public static ServiceSecurityManager getTestServiceSecurityManager() 
	{
		//return new OpenServiceSecurityManager();
		return new BootstrapBasicAuthServiceSecurityManager("gm2552", "password");
	}
}
