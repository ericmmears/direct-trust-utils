package org.dtrust.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.ws.rs.core.MediaType;

import org.dtrust.BaseTestPlan;
import org.dtrust.InteropTestRunner;
import org.dtrust.dao.interoptest.entity.User;
import org.dtrust.dao.interoptest.entity.UserRole;
import org.dtrust.utils.TestUtils;
import org.junit.Test;

import com.sun.jersey.api.client.WebResource;

public class UserMgmtResourse_addUserTest
{
	static WebResource resource;
	
	abstract class TestPlan extends BaseTestPlan 
	{
		@Override
		protected void setupMocks() throws Exception
		{
			try
			{
				InteropTestRunner.startInteropTestService();
				super.setupMocks();
				cleanDAOs();
													
				resource = 	TestUtils.getResource(InteropTestRunner.getInteropTestServiceURL());		

			}
			catch (Throwable t)
			{
				throw new RuntimeException(t);
			}
			

		}
		
		@Override
		protected void tearDownMocks() throws Exception
		{
			try
			{
				cleanDAOs();
			}
			catch (Exception e)
			{
				
			}
			
			super.tearDownMocks();
		}
		
		protected void cleanDAOs() throws Exception
		{
			userDao.cleanUsers();
		}
		
		protected abstract User getAddUser();
		
		
		@Override
		protected void performInner() throws Exception
		{			
			
			final User addUser = getAddUser();
			
			
			// add the reg 
			final User user = resource.path("/userMgmt").entity(addUser, MediaType.APPLICATION_JSON).put(User.class);

			
			doAssertions(user);
		}
		
		protected void doAssertions(User user) throws Exception
		{
			
		}
	}
	
	@Test
	public void testAddUser_assertAdded() throws Exception
	{
		new TestPlan()
		{
			
			@Override
			protected User getAddUser()
			{
				final User addUser = new User();
				addUser.setUsername("gm2552");
				addUser.setHashedPass("hashed");
				addUser.setRole(UserRole.ADMIN);
				
				return addUser;
			}
						
			@Override
			protected void doAssertions(User user) throws Exception
			{
				assertNotNull(user);
				
				assertEquals(getAddUser().getUsername(), user.getUsername());
				assertEquals(getAddUser().getRole(), user.getRole());
			}
		}.perform();
	}
}
