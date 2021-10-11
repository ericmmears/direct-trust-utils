package org.dtrust.dao.interoptest.dao.impl;

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.dtrust.dao.interoptest.dao.TestConflictException;
import org.dtrust.dao.interoptest.dao.TestDAOException;
import org.dtrust.dao.interoptest.dao.TestEntityNotFoundException;
import org.dtrust.dao.interoptest.dao.UserDAO;
import org.dtrust.dao.interoptest.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class UserDAOImpl implements UserDAO
{
	@PersistenceContext
    @Autowired
    protected EntityManager entityManager;
	
	public UserDAOImpl()
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
	public Collection<User> getUsers() throws TestDAOException
	{
		validateState();
		
    	try
    	{    		
    		final String query = "select u from User u";
					
			final Query select = entityManager.createQuery(query);	
							
			@SuppressWarnings("unchecked")
			final List<User> rs = select.getResultList();
	        if (rs == null || (rs.size() == 0)) 
	        {
	            return Collections.emptyList();
	        }  
	        
	        return rs;
    	}
		catch (Exception e)
		{
			throw new TestDAOException("Failed to get users", e);
		}	
	}

	@Override
    @Transactional(readOnly = true)
	public User getUser(long id) throws TestDAOException
	{
		validateState();
		
		try
		{
    		final String query = "select u from User u where u.userId =?1";
			final Query select = entityManager.createQuery(query);	
			select.setParameter(1, id);
			
			return (User)select.getSingleResult();
		}
		catch (Exception e)
		{
			throw new TestEntityNotFoundException("Failed to get user by id", e);
		}
	}
	
	@Override
    @Transactional(readOnly = true)
	public User getUserByUsername(String username) throws TestDAOException
	{
		validateState();
		
		try
		{
    		final String query = "select u from User u where u.usernameAllCaps =?1";
			final Query select = entityManager.createQuery(query);	
			select.setParameter(1, username.toUpperCase());
			
			return (User)select.getSingleResult();
		}
		catch (Exception e)
		{
			throw new TestEntityNotFoundException("Failed to get user by username", e);
		}
	}

	@Override
    @Transactional(readOnly = true)
	public User getUserByContactEmailAddress(String contactAddress) throws TestDAOException
	{
		validateState();
		
		try
		{
    		final String query = "select u from User u where u.contactEmailAllCaps =?1";
			final Query select = entityManager.createQuery(query);	
			select.setParameter(1, contactAddress.toUpperCase());
			
			return (User)select.getSingleResult();
		}
		catch (Exception e)
		{
			throw new TestEntityNotFoundException("Failed to get user by contact email addresss", e);
		}		
	}
	
	@Override
    @Transactional(readOnly = false)
	public User addUser(User user) throws TestDAOException
	{
		validateState();
		
		try
		{
			if (getUserByUsername(user.getUsername()) != null)
				throw new TestConflictException("Username " + user.getUsername() + " already exists.");
		}
		catch (TestEntityNotFoundException e)
		{
			// this is what we want
		}
		
		final User newUser = new User();
		
		final Calendar cal = Calendar.getInstance(Locale.getDefault());
		
		newUser.setAccountStatus(user.getAccountStatus());
		newUser.setAddressCity(user.getAddressCity());
		newUser.setAddressState(user.getAddressState());
		newUser.setContactEmail(user.getContactEmail());
		newUser.setContactEmailAllCaps(user.getContactEmail().toUpperCase());
		newUser.setContactPhone(user.getContactPhone());
		newUser.setFirstName(user.getFirstName());
		newUser.setHashedPass(user.getHashedPass());
		newUser.setLastName(user.getLastName());
		newUser.setRole(user.getRole());
		newUser.setUsername(user.getUsername());
		newUser.setUsernameAllCaps(user.getUsername().toUpperCase());
		newUser.setOrganization(user.getOrganization());
		
		newUser.setStartDtTm(cal);
		newUser.setUpdateDtTm(cal);
		newUser.setUpdateCnt(0);
		
		try
		{
			entityManager.persist(newUser);
		}
		catch (Exception e)
		{
			throw new TestDAOException("Failed to add users.", e);
		}
		
		return newUser;
	}
	
	@Override
    @Transactional(readOnly = false)
	public void deleteUser(long id) throws TestDAOException
	{
		validateState();
		

		final User delUser = getUser(id);
		
    	try
    	{    		
    		entityManager.remove(delUser);
    	}
		catch (Exception e)
		{
			throw new TestDAOException("Failed to delete user", e);
		}		
	}

	@Override
    @Transactional(readOnly = false)
	public User updateUser(User user) throws TestDAOException
	{
		validateState();
		
		final User updateUser = getUser(user.getUserId());
		
		final Calendar cal = Calendar.getInstance(Locale.getDefault());
		
		updateUser.setAccountStatus(user.getAccountStatus());
		updateUser.setAddressCity(user.getAddressCity());
		updateUser.setAddressState(user.getAddressState());
		updateUser.setContactEmail(user.getContactEmail());
		updateUser.setContactEmailAllCaps(user.getContactEmail().toUpperCase());
		updateUser.setContactPhone(user.getContactPhone());
		updateUser.setFirstName(user.getFirstName());
		updateUser.setHashedPass(user.getHashedPass());
		updateUser.setLastName(user.getLastName());
		updateUser.setRole(user.getRole());
		updateUser.setUsername(user.getUsername());
		updateUser.setUsernameAllCaps(user.getUsernameAllCaps());
		updateUser.setUpdateChallenge(user.getUpdateChallenge());
		updateUser.setUpdateChallengeIssuedDtTm(user.getUpdateChallengeIssuedDtTm());
		updateUser.setOrganization(user.getOrganization());
		
		updateUser.setUpdateDtTm(cal);
		updateUser.setUpdateCnt(updateUser.getUpdateCnt() + 1);
		
		try
		{
			entityManager.merge(updateUser);
		}
		catch (Exception e)
		{
			throw new TestDAOException("Failed to update users.", e);
		}
		
		return updateUser;		
	}	
	
	@Override
    @Transactional(readOnly = false)
	public void cleanUsers() throws TestDAOException
	{
		validateState();
		
		// slow delete implementation
		
		final Collection<User> users = this.getUsers();
		for (User user : users)
			entityManager.remove(user);
	}
}
