package org.dtrust.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.dtrust.BaseTestPlan;
import org.dtrust.InteropTestRunner;
import org.dtrust.dao.interoptest.entity.TestRegistration;
import org.dtrust.utils.TestUtils;
import org.junit.Test;

import com.sun.jersey.api.client.WebResource;

public class RegistrationResource_addRegTest
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
			testRegDao.cleanRegistrations();
		}
		
		protected abstract String getReportAddress();
		
		protected abstract String getSourceAddress();
		
		@Override
		protected void performInner() throws Exception
		{			
			
			final String repAddress = TestUtils.uriEscape(getReportAddress());
			
			final String sourceAddress = TestUtils.uriEscape(getSourceAddress());
			
			// add the reg 
			final TestRegistration reg = resource.path("/interopReg/" +  repAddress + "/" + 
					sourceAddress).put(TestRegistration.class);

			
			doAssertions(reg);
		}
		
		protected void doAssertions(TestRegistration reg) throws Exception
		{
			
		}
	}
	
	@Test
	public void testAddReg_assertAdded() throws Exception
	{
		new TestPlan()
		{
			@Override
			protected String getSourceAddress()
			{
				return "gm2552@demo.sandboxcernerdirect.com";
			}
			
			@Override
			protected String getReportAddress()
			{
				return "gm2552@cerner.com";
			}
						
			@Override
			protected void doAssertions(TestRegistration reg) throws Exception
			{
				assertNotNull(reg);
				
				assertEquals(getReportAddress(), reg.getReportAddress());
				assertEquals(getSourceAddress(), reg.getSourceDirectAddress());
			}
		}.perform();
	}
}
