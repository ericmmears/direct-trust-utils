package org.dtrust.resources;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dtrust.dao.interoptest.dao.TestConflictException;
import org.dtrust.dao.interoptest.dao.TestDAOException;
import org.dtrust.dao.interoptest.dao.TestEntityNotFoundException;
import org.dtrust.dao.interoptest.entity.User;
import org.dtrust.dao.interoptest.entity.UserAccountStatus;
import org.dtrust.dao.interoptest.entity.UserRole;
import org.nhindirect.common.rest.auth.BasicAuthCredential;
import org.nhindirect.common.rest.auth.BasicAuthValidator;
import org.nhindirect.common.rest.auth.impl.BasicAuthFilter;
import org.nhindirect.common.rest.auth.impl.DefaultBasicAuthCredential;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class AuthFilter extends BasicAuthFilter
{	
	private static final Log LOGGER = LogFactory.getFactory().getInstance(AuthFilter.class);
	
	protected Map<String, BasicAuthCredential> credentialMap;
	
	public AuthFilter()
	{
		this(null);
	}
	
    public AuthFilter(BasicAuthValidator validator)
    {
    	super(validator);
    	
    	credentialMap = new HashMap<String, BasicAuthCredential>();
    }
	
	/**
	 * Sets the collections of credentials used to validate requests.
	 * @param credentials Collections of credentials used to validate requests.
	 */
	public void setCredentails(List<BasicAuthCredential> credentials)
	{
		for (BasicAuthCredential cred : credentials)
			credentialMap.put(cred.getUser().toUpperCase(Locale.getDefault()), cred);
	}

	/**
	 * Sets a list of credentials delimited by a ",".  The credentials are in the following order:
	 * <br><i>username,password,role</i>
	 * @param credentials List of credentials delimited by a ",".
	 */
	public void setCredentialsAsDelimetedString(List<String> credentials)
	{
		for (String str : credentials)
		{
			final String parsedStr[] = str.split(",");
			final BasicAuthCredential cred = new DefaultBasicAuthCredential(parsedStr[0], parsedStr[1], parsedStr[2]);
			credentialMap.put(cred.getUser().toUpperCase(Locale.getDefault()), cred);
		}
	}
	
	/**
	 * Sets a list of credentials as a set of properties delimited by a ",".  The credentials are in the following order:
	 * <br><i>username,password,role</i>
	 * @param credentials List of credentials a set of properties delimited by a ",".
	 */
	public void setCredentialsAsProperties(Properties credentials)
	{
		for (Entry<Object, Object> entry : credentials.entrySet())
		{
			final String parsedStr[] = entry.getValue().toString().split(",");
			final BasicAuthCredential cred = new DefaultBasicAuthCredential(parsedStr[0], parsedStr[1], parsedStr[2]);
			credentialMap.put(cred.getUser().toUpperCase(Locale.getDefault()), cred);
		}
	}
    
    @Override
    public void init(final FilterConfig filterConfig) throws ServletException 
    {	
    	
    	filterConfig.getServletContext();
    	final WebApplicationContext webCtx = WebApplicationContextUtils.getWebApplicationContext(filterConfig.getServletContext());
    	
    	final EntityManagerFactory factory = (EntityManagerFactory)webCtx.getBean("entityManagerFactory");
    	final EntityManager entityManager = factory.createEntityManager();
    	

    	
    	EntityTransaction transaction = entityManager.getTransaction();
    	transaction.begin();
    	
		try
		{
	    	// create admin users in the store if they don't already exist
	    	if (credentialMap != null)
	    	{
	    		for (BasicAuthCredential cred : credentialMap.values())
	    		{
	    			UserRole role = null;
	    			
	    			try
	    			{	
	    				role = UserRole.valueOf(cred.getRole());
	    			}
	    			catch (Exception e) {}
	    			
	    			if (role != null && role.equals(UserRole.ADMIN))
	    			{
		    			try
		    			{
		    				User user = null;
		    				try
		    				{
			    				user = getUserByUsername(cred.getUser(), entityManager);
		    				}
		    				catch (Exception e)
		    				{
		    					// no-op... this is what we want
		    				}
		    				
		    				if (user == null)
		    				{
		    					User addUser = new User();
		    					addUser.setUsername(cred.getUser());
		    					addUser.setHashedPass(cred.getPassword());
		    					addUser.setRole(role);
		    					addUser.setAccountStatus(UserAccountStatus.APPROVED);
		    					addUser.setFirstName(cred.getUser());
		    					addUser.setLastName(cred.getUser());
		    					addUser.setAddressCity("Washington DC");
		    					addUser.setAddressState("DC");
		    					
		    					addUser(addUser, entityManager);
		    				}
		    			}
		    			catch (Exception e)
		    			{
		    				LOGGER.warn("Failed to bootstrap admin user " + cred.getUser(), e);
		    			}
	    			}
	    		}
	    	}
		}
		finally
		{
			entityManager.flush();
			transaction.commit();
		}
    	super.init(filterConfig);
	    	
    }
    
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException
    {
    	
    	// based on the request URI, certain functions may be allowed to pass without 
    	// auth 
    	
    	
    	final HttpServletRequest httpReq = (HttpServletRequest)request;
    	final String reqURI = httpReq.getRequestURI();
    	
    	if (httpReq.getMethod().compareToIgnoreCase("put") == 0)
    	{
    		if (reqURI.endsWith("userMgmt"))
    		{
    			// this is a self registration
    			// allow it to just pass through
    			chain.doFilter(request, response);
    			
    			return;
    		}
    	}
    	else if (httpReq.getMethod().compareToIgnoreCase("post") == 0)
    	{	
    		if (reqURI.contains("forgotPassword"))
    		{
    			// this is a self registration
    			// allow it to just pass through
    			chain.doFilter(request, response);
    			
    			return;
    		}
    		else if (reqURI.contains("changePassword"))
    		{
    			// this is a self registration
    			// allow it to just pass through
    			chain.doFilter(request, response);
    			
    			return;
    		} 
    		else if (reqURI.contains("dd/api/contribute"))
    		{
    			// this is a self registration
    			// allow it to just pass through
    			chain.doFilter(request, response);
    			
    			return;
    		}     		
    	}
    	else if (httpReq.getMethod().compareToIgnoreCase("get") == 0)
    	{
    		if (reqURI.contains("dd/api/download"))
    		{
    			// this is a self registration
    			// allow it to just pass through
    			chain.doFilter(request, response);
    			
    			return;
    		}
    		
    	}
    	
    	super.doFilter(request, response, chain);
    	

    }
    
	public User getUserByUsername(String username, EntityManager entityManager) throws TestDAOException
	{
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
    
	public User addUser(User user, EntityManager entityManager) throws TestDAOException
	{
		try
		{
			if (getUserByUsername(user.getUsername(), entityManager) != null)
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
		newUser.setContactPhone(user.getContactPhone());
		newUser.setFirstName(user.getFirstName());
		newUser.setHashedPass(user.getHashedPass());
		newUser.setLastName(user.getLastName());
		newUser.setRole(user.getRole());
		newUser.setUsername(user.getUsername());
		newUser.setUsernameAllCaps(user.getUsername().toUpperCase());
		
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
}
