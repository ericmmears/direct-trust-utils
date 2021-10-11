package org.dtrust.resources.authstore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dtrust.dao.interoptest.dao.TestEntityNotFoundException;
import org.dtrust.dao.interoptest.dao.UserDAO;
import org.dtrust.dao.interoptest.entity.User;
import org.dtrust.dao.interoptest.entity.UserAccountStatus;
import org.nhindirect.common.rest.auth.BasicAuthCredential;
import org.nhindirect.common.rest.auth.BasicAuthCredentialStore;
import org.nhindirect.common.rest.auth.impl.DefaultBasicAuthCredential;
import org.springframework.beans.factory.annotation.Autowired;

public class RDBMSAuthStore implements BasicAuthCredentialStore
{
	private static final Log LOGGER = LogFactory.getFactory().getInstance(RDBMSAuthStore.class);
	
	@Autowired
	protected UserDAO dao;
	
	public RDBMSAuthStore()
	{
		
	}

	@Override
	public BasicAuthCredential getCredential(String name)
	{
		DefaultBasicAuthCredential cred = null;
		
		try
		{
			final User user = dao.getUserByUsername(name);
			
			// make sure the user is in an approved status before they can log in
			if (user.getAccountStatus() != UserAccountStatus.APPROVED)
				return null;
			
			cred = new DefaultBasicAuthCredential(name, user.getHashedPass(), user.getRole().toString());
		}
		catch (TestEntityNotFoundException e)
		{
			return null;
		}
		catch (Exception e)
		{
			LOGGER.error("Error retrieving basic auth credential for user " + name, e);
		}
		
		return cred;
	}
	
	
}
