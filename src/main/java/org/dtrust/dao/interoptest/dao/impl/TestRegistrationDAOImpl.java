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
import org.dtrust.dao.interoptest.dao.TestRegistrationDAO;
import org.dtrust.dao.interoptest.entity.TestRegistration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class TestRegistrationDAOImpl implements TestRegistrationDAO
{
	@PersistenceContext
    @Autowired
    protected EntityManager entityManager;
	
	public TestRegistrationDAOImpl()
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
	public Collection<TestRegistration> getRegistrations() throws TestDAOException
	{
		validateState();
		
    	try
    	{    		
    		final String query = "select tr from TestRegistration tr";
					
			final Query select = entityManager.createQuery(query);	
							
			@SuppressWarnings("unchecked")
			final List<TestRegistration> rs = select.getResultList();
	        if (rs == null || (rs.size() == 0)) 
	        {
	            return Collections.emptyList();
	        }  
	        
	        return rs;
    	}
		catch (Exception e)
		{
			throw new TestDAOException("Failed to get test registrations", e);
		}	
	}
	
	@Override
    @Transactional(readOnly = true)
	public TestRegistration getRegistrationsById(long id) throws TestDAOException
	{
		validateState();
		
		try
		{
    		final String query = "select tr from TestRegistration tr where tr.registrationId =?1";
			final Query select = entityManager.createQuery(query);	
			select.setParameter(1, id);
			
			return (TestRegistration)select.getSingleResult();
		}
		catch (Exception e)
		{
			throw new TestEntityNotFoundException("Failed to get registration by id", e);
		}
	}
	
	@Override
    @Transactional(readOnly = true)
	public Collection<TestRegistration> getRegistrationsByUsername(String username) throws TestDAOException
	{
		validateState();
		
    	try
    	{    		
    		final String query = "select tr from TestRegistration tr where tr.userName =?1";
					
			final Query select = entityManager.createQuery(query);	
			select.setParameter(1, username.toUpperCase());	
			
			@SuppressWarnings("unchecked")
			final List<TestRegistration> rs = select.getResultList();
	        if (rs == null || (rs.size() == 0)) 
	        {
	            return Collections.emptyList();
	        }  
	        
	        return rs;
    	}
		catch (Exception e)
		{
			throw new TestDAOException("Failed to get test registrations by username", e);
		}
	}

	@Override
    @Transactional(readOnly = true)
	public Collection<TestRegistration> getRegistrationsByAndRegAddress(String username, String regAddress)
			throws TestDAOException
	{
		validateState();
		
    	try
    	{    		
    		final String query = "select tr from TestRegistration tr where tr.userName =?1 and tr.reportAddressAllCaps =?2";
					
			final Query select = entityManager.createQuery(query);	
			select.setParameter(1, username.toUpperCase());	
			select.setParameter(2, regAddress.toUpperCase());	
			
			@SuppressWarnings("unchecked")
			final List<TestRegistration> rs = select.getResultList();
	        if (rs == null || (rs.size() == 0)) 
	        {
	            return Collections.emptyList();
	        }  
	        
	        return rs;
    	}
		catch (Exception e)
		{
			throw new TestDAOException("Failed to get test registrations by userName and reg address", e);
		}
	}

	@Override
    @Transactional(readOnly = true)
	public Collection<TestRegistration> getRegistrationsSourceAddress(String sourceAddress) throws TestDAOException
	{
		validateState();
		
    	try
    	{    		
    		final String query = "select tr from TestRegistration tr where tr.sourceDirectAddressAllCaps =?1";
					
			final Query select = entityManager.createQuery(query);	
			select.setParameter(1, sourceAddress.toUpperCase());	
			
			@SuppressWarnings("unchecked")
			final List<TestRegistration> rs = select.getResultList();
	        if (rs == null || (rs.size() == 0)) 
	        {
	            return Collections.emptyList();
	        }  
	        
	        return rs;
    	}
		catch (Exception e)
		{
			throw new TestDAOException("Failed to get test registrations by userName and source address", e);
		}
	}
	
	@Override
    @Transactional(readOnly = true)
	public Collection<TestRegistration> getRegistrationsByAndSourceAddress(String username, String sourceAddress)
			throws TestDAOException
	{
		validateState();
		
    	try
    	{    		
    		final String query = "select tr from TestRegistration tr where tr.userName =?1 and tr.sourceDirectAddressAllCaps =?2";
					
			final Query select = entityManager.createQuery(query);	
			select.setParameter(1, username.toUpperCase());	
			select.setParameter(2, sourceAddress.toUpperCase());	
			
			@SuppressWarnings("unchecked")
			final List<TestRegistration> rs = select.getResultList();
	        if (rs == null || (rs.size() == 0)) 
	        {
	            return Collections.emptyList();
	        }  
	        
	        return rs;
    	}
		catch (Exception e)
		{
			throw new TestDAOException("Failed to get test registrations by userName and source address", e);
		}
	}

	@Override
    @Transactional(readOnly = false)
	public TestRegistration addRegistration(String username, String sourceAddress, String regAddress)
			throws TestDAOException
	{
		validateState();
		
		final TestRegistration addReg = new TestRegistration();
		
		final Calendar cal = Calendar.getInstance(Locale.getDefault());
		
		addReg.setUserName(username.toUpperCase());
		addReg.setSourceDirectAddress(sourceAddress);
		addReg.setSourceDirectAddressAllCaps(sourceAddress.toUpperCase());
		addReg.setReportAddress(regAddress);
		addReg.setReportAddressAllCaps(regAddress.toUpperCase());
		addReg.setStartDtTm(cal);
		addReg.setUpdateDtTm(cal);
		addReg.setUpdateCnt(0);
		
		try
		{
			entityManager.persist(addReg);
		}
		catch (Exception e)
		{
			throw new TestDAOException("Failed to add test registration.", e);
		}
		
		return addReg;
	}

	@Override
    @Transactional(readOnly = false)
	public void deleteRegistration(long[] ids) throws TestDAOException
	{
		validateState();
		
		for (long id : ids)
		{
			final TestRegistration delReg = getRegistrationsById(id);
			
	    	try
	    	{    		
	    		entityManager.remove(delReg);
	    	}
			catch (Exception e)
			{
				throw new TestDAOException("Failed to delete test registrations", e);
			}		
		}
	}
	
	@Override
    @Transactional(readOnly = false)
	public void cleanRegistrations() throws TestDAOException
	{
		validateState();
		
		// slow delete implementation
		
		final Collection<TestRegistration> regs = this.getRegistrations();
		for (TestRegistration reg : regs)
			entityManager.remove(reg);
	}
}
