package org.dtrust.dao.interoptest.dao.impl;

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.dtrust.dao.interoptest.dao.TestDAOException;
import org.dtrust.dao.interoptest.dao.TestEntityNotFoundException;
import org.dtrust.dao.interoptest.dao.TestSuiteDAO;
import org.dtrust.dao.interoptest.entity.Test;
import org.dtrust.dao.interoptest.entity.TestStatus;
import org.dtrust.dao.interoptest.entity.TestSuite;
import org.dtrust.dao.interoptest.entity.TestType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class TestSuiteDAOImpl implements TestSuiteDAO
{
	@PersistenceContext
    @Autowired
    protected EntityManager entityManager;
	
	public TestSuiteDAOImpl()
	{
		
	}

	public void setEntityManager(EntityManager entityManager)
	{
		this.entityManager = entityManager;
	}
	
	protected void validateState()
	{	
    	if (entityManager == null)
    		throw new IllegalStateException("entityManger has not been initialized");
	}

	@Override
    @Transactional(readOnly = true)
	public Collection<TestSuite> getTestSuites() throws TestDAOException
	{
		validateState();
		
    	try
    	{    		
    		final String query = "select ts from TestSuite ts";
					
			final Query select = entityManager.createQuery(query);	
							
			@SuppressWarnings("unchecked")
			final List<TestSuite> rs = select.getResultList();
	        if (rs == null || (rs.size() == 0)) 
	        {
	            return Collections.emptyList();
	        }  
	        
	        return rs;
    	}
		catch (Exception e)
		{
			throw new TestDAOException("Failed to get test suites", e);
		}	
	}
	
	@Override
    @Transactional(readOnly = true)
	public TestSuite getTestSuite(long testSuiteId) throws TestDAOException
	{
		validateState();
		
		try
		{
			final Query select = entityManager.createQuery("select ts from TestSuite ts where ts.testSuiteid =?1");
			select.setParameter(1, testSuiteId);
			
			return (TestSuite)select.getSingleResult();
		}
		catch (Exception e)
		{
			throw new TestEntityNotFoundException("Failed to find existing test suite.", e);
		}
	}

	@Override
    @Transactional(readOnly = false)
	public TestSuite initiateTestSuite(String name, String targetAddress, int testTimeout) throws TestDAOException
	{
		validateState();
		
		final TestSuite addSuite = new TestSuite();
		
		final Calendar cal = Calendar.getInstance(Locale.getDefault());
		
		addSuite.setStartDtTm(cal);
		addSuite.setUpdateDtTm(cal);
		addSuite.setTestStatus(TestStatus.INITIATED);
		addSuite.setTestSuiteName(name);
		addSuite.setTestTimout(testTimeout);
		addSuite.setTargetAddress(targetAddress);
		
		try
		{
			entityManager.persist(addSuite);
		}
		catch (Exception e)
		{
			throw new TestDAOException("Failed to add test suite.", e);
		}
		
		return addSuite;
	}

	@Override
    @Transactional(readOnly = false)
	public TestSuite updateTestSuite(TestSuite suite) throws TestDAOException
	{
		validateState();
		
		final TestSuite updateSuite = this.getTestSuite(suite.getTestSuiteid());
		if (updateSuite == null)
			throw new TestEntityNotFoundException("Test suite with id " + suite.getTestSuiteid() + " does not exist.");
		
		updateSuite.setTestStatus(suite.getTestStatus());
		updateSuite.setUpdateDtTm((Calendar.getInstance(Locale.getDefault())));
		updateSuite.setUpdateCnt(updateSuite.getUpdateCnt() + 1);
		
		try
		{
			entityManager.merge(updateSuite);
		}
		catch (Exception e)
		{
			throw new TestDAOException("Failed to update test suite.", e);
		}
		
		return updateSuite;
	}

	@Override
    @Transactional(readOnly = false)
	public void cleanTestSuites() throws TestDAOException
	{
		validateState();
		
		// slow non-cascading delete implementation
		
		final Collection<TestSuite> suites = this.getTestSuites();
		for (TestSuite suite : suites)
		{
			for (Test test : suite.getTests())
				entityManager.remove(test);
			
			entityManager.remove(suite);
		}
	}
	
	@Override
    @Transactional(readOnly = true)
	public Test getTest(long testId) throws TestDAOException
	{
		validateState();
		
		try
		{
			final Query select = entityManager.createQuery("select t from Test t where t.testId =?1");
			select.setParameter(1, testId);
			
			return (Test)select.getSingleResult();
		}
		catch (Exception e)
		{
			throw new TestEntityNotFoundException("Failed to find existing test.", e);
		}
	}

	@Override
    @Transactional(readOnly = false)
	public Test initiateTest(String name, TestType testType, long testSuiteId) throws TestDAOException
	{
		validateState();
		
		// make sure the test exists
		final TestSuite suite = this.getTestSuite(testSuiteId);
		
		final Test addTest = new Test();
		
		final Calendar cal = Calendar.getInstance(Locale.getDefault());
		
		addTest.setStartDtTm(cal);
		addTest.setUpdateDtTm(cal);
		addTest.setTestStatus(TestStatus.INITIATED);
		addTest.setUpdateCnt(0);
		addTest.setTestName(name);
		addTest.setType(testType);
		
		try
		{
			entityManager.persist(addTest);
			
			suite.getTests().add(addTest);
			
			entityManager.merge(suite);
		}
		catch (Exception e)
		{
			throw new TestDAOException("Failed to add test.", e);
		}
		
		return addTest;
	}

	@Override
    @Transactional(readOnly = false)
	public Test updateTest(Test test) throws TestDAOException
	{
		validateState();
		
		final Test updateTest = this.getTest(test.getTestId());
		if (updateTest == null)
			throw new TestEntityNotFoundException("Test with id " + test.getTestId() + " does not exist.");
		
		updateTest.setTestStatus(test.getTestStatus());
		updateTest.setUpdateDtTm((Calendar.getInstance(Locale.getDefault())));
		updateTest.setUpdateCnt(updateTest.getUpdateCnt() + 1);
		updateTest.setComments(test.getComments());
		updateTest.setCorrelationId(test.getCorrelationId());
		
		try
		{
			entityManager.merge(updateTest);
		}
		catch (Exception e)
		{
			throw new TestDAOException("Failed to update test.", e);
		}
		
		return updateTest;
	}
	
	
}
