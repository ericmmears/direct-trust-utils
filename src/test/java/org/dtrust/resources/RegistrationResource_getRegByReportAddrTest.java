package org.dtrust.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.dtrust.BaseTestPlan;
import org.dtrust.InteropTestRunner;
import org.dtrust.dao.interoptest.entity.TestRegistration;
import org.dtrust.utils.TestUtils;
import org.junit.Test;

import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

public class RegistrationResource_getRegByReportAddrTest
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
		
		protected abstract Collection<String> getSourceAddresses();
		
		@Override
		protected void performInner() throws Exception
		{			
			
			final String repAddress = TestUtils.uriEscape(getReportAddress());
			
			final Collection<String> sourceAddresses = getSourceAddresses();
			
			// add the reges
			for (String sourceAddr : sourceAddresses)
			{
				resource.path("/interopReg/" +  repAddress + "/" + 
						TestUtils.uriEscape(sourceAddr)).put(TestRegistration.class);
			}
			
			Collection<TestRegistration> retrievedReges = null;
			try
			{
				// get the reges
				GenericType<ArrayList<TestRegistration>> genType = new GenericType<ArrayList<TestRegistration>>(){};
				retrievedReges = resource.path("/interopReg/reportAdd/" +  repAddress).get(genType);
				doAssertions(retrievedReges);
			}
			catch (UniformInterfaceException e)
			{
				if (e.getResponse().getStatus() == 204)
					doAssertions(new ArrayList<TestRegistration>());
				else
					throw e;
			}
		}
		
		
		protected void doAssertions(Collection<TestRegistration> retrievedReges) throws Exception
		{
			
		}
	}
	
	@Test
	public void testGetRegesByReportAdd_assertRetrieved() throws Exception
	{
		new TestPlan()
		{
			@Override
			protected Collection<String> getSourceAddresses()
			{
				return Arrays.asList("gm2552@demo.sandboxcernerdirect.com");
			}
			
			@Override
			protected String getReportAddress()
			{
				return "gm2552@cerner.com";
			}
			
			@Override
			protected void doAssertions(Collection<TestRegistration> retrievedReges) throws Exception
			{
				assertNotNull(retrievedReges);
				
				assertEquals(1, retrievedReges.size());
				
				final TestRegistration reg = retrievedReges.iterator().next();
				
				assertEquals(getReportAddress(), reg.getReportAddress());
				assertEquals(getSourceAddresses().iterator().next() , reg.getSourceDirectAddress());
			}
		}.perform();
	}
}
