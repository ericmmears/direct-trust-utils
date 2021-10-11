package org.dtrust.dao.interoptest.dao;

import java.util.Collection;

import org.dtrust.dao.interoptest.entity.Test;
import org.dtrust.dao.interoptest.entity.TestSuite;
import org.dtrust.dao.interoptest.entity.TestType;

public interface TestSuiteDAO
{
	public Collection<TestSuite> getTestSuites() throws TestDAOException;
	
	public TestSuite getTestSuite(long testSuiteId) throws TestDAOException;
	
	public TestSuite initiateTestSuite(String name, String targetAddress, int testTimeout) throws TestDAOException;
	
	public TestSuite updateTestSuite(TestSuite suite) throws TestDAOException;
	
	public void cleanTestSuites() throws TestDAOException;
	
	public Test getTest(long testId) throws TestDAOException;
	
	public Test initiateTest(String name, TestType testType, long testSuiteId) throws TestDAOException;
	
	public Test updateTest(Test test) throws TestDAOException;	
}
