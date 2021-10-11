package org.dtrust.resources;

import java.net.URI;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dtrust.dao.interoptest.dao.TestEntityNotFoundException;
import org.dtrust.dao.interoptest.dao.TestRegistrationDAO;
import org.dtrust.dao.interoptest.entity.TestRegistration;
import org.dtrust.dao.interoptest.entity.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sun.jersey.spi.resource.Singleton;

@Component
@Path("interopReg/")
@Singleton
public class RegistrationResource extends RoleProtectedResource
{
    static final String UTF8 = ";charset=UTF-8";
		
	private static final Log LOGGER = LogFactory.getFactory().getInstance(RegistrationResource.class);

	@Autowired
	protected TestRegistrationDAO dao;
	
	public RegistrationResource()
	{
		
	}
	
	@GET
    @Produces(MediaType.APPLICATION_JSON + UTF8)  
	public Response getRegByUsername(@Context HttpServletRequest request)
	{
		final String username = getUsernameByContext(request);
		
		GenericEntity<Collection<? extends TestRegistration>> entity = null;
		
		try
		{
			Collection<TestRegistration> regs = dao.getRegistrationsByUsername(username);
			
    		if (regs.isEmpty())
    		{
    			return Response.noContent().cacheControl(noCache).build();
    		}
    		
    		entity = new GenericEntity<Collection<? extends TestRegistration>>(regs) {};
		}
    	catch (Throwable t)
    	{
    		LOGGER.error("Error retrieving registrations by username " + username, t);
    		return Response.serverError().cacheControl(noCache).build();   
    	}
		
		return Response.ok(entity).cacheControl(noCache).build();
	}
	
	@GET
    @Path("sourceAdd/{sourceAdd}")
    @Produces(MediaType.APPLICATION_JSON + UTF8)  
	public Response getRegByUsernameAndSource(@Context HttpServletRequest request, @PathParam("sourceAdd") String sourceAddress)
	{
		final String username = getUsernameByContext(request);
		final String roll = getRoleByContext(request);
		
		GenericEntity<Collection<? extends TestRegistration>> entity = null;
		
		try
		{
			
			final Collection<TestRegistration> regs = (StringUtils.isEmpty(username) || roll.equals(UserRole.ADMIN.toString())) ? dao.getRegistrationsSourceAddress(sourceAddress) :
				dao.getRegistrationsByAndSourceAddress(username, sourceAddress);
			
    		if (regs.isEmpty())
    		{
    			return Response.noContent().cacheControl(noCache).build();
    		}
    		
    		entity = new GenericEntity<Collection<? extends TestRegistration>>(regs) {};
		}
    	catch (Throwable t)
    	{
    		LOGGER.error("Error retrieving registrations by username " + username + " and source address " + sourceAddress, t);
    		return Response.serverError().cacheControl(noCache).build();   
    	}
		
		return Response.ok(entity).cacheControl(noCache).build();
	}
	
	@GET
    @Path("reportAdd/{reportAdd}")
    @Produces(MediaType.APPLICATION_JSON + UTF8)  
	public Response getRegByUsernameAndReportAddr(@Context HttpServletRequest request, @PathParam("reportAdd") String reportAdd)
	{
		final String username = getUsernameByContext(request);
		
		GenericEntity<Collection<? extends TestRegistration>> entity = null;
		
		try
		{
			final Collection<TestRegistration> regs = dao.getRegistrationsByAndRegAddress(username, reportAdd);
			
    		if (regs.isEmpty())
    		{
    			return Response.noContent().cacheControl(noCache).build();
    		}
    		
    		entity = new GenericEntity<Collection<? extends TestRegistration>>(regs) {};
		}
    	catch (Throwable t)
    	{
    		LOGGER.error("Error retrieving registrations by username " + username + " and report address " + reportAdd, t);
    		return Response.serverError().cacheControl(noCache).build();   
    	}
		
		return Response.ok(entity).cacheControl(noCache).build();
	}
	
	@PUT
    @Path("{reportAdd}/{sourceAdd}")
    @Produces(MediaType.APPLICATION_JSON + UTF8)  
	public Response addReg(@Context HttpServletRequest request, @Context UriInfo uriInfo, @PathParam("reportAdd") String reportAdd, 
			@PathParam("sourceAdd") String sourceAddress)
	{
		// make sure this doesn't already exist
		final String username = getUsernameByContext(request);
		
		TestRegistration addReg  = null;
		
		try
		{
			final Collection<TestRegistration> regs = dao.getRegistrationsByAndRegAddress(username, reportAdd);
			for (TestRegistration reg : regs)
			{
				if (reg.getSourceDirectAddressAllCaps().equals(sourceAddress.toUpperCase()))
				{
		    		LOGGER.error("Registration with report address " + reportAdd + " and source address " + sourceAddress + " already exists.");
		    		return Response.status(javax.ws.rs.core.Response.Status.CONFLICT).cacheControl(noCache).build();  
				}
			}
			
			// now add
			addReg = dao.addRegistration(username, sourceAddress, reportAdd);
		}
		catch (Throwable t)
		{
    		LOGGER.error("Error adding registration with report address " + reportAdd + " and source address " + sourceAddress, t);
    		return Response.serverError().cacheControl(noCache).build();			
		}
		
		UriBuilder newLocBuilder = uriInfo.getBaseUriBuilder();
		URI newLoc = newLocBuilder.path("interopReg/" + sourceAddress).build();
		
		return Response.created(newLoc).entity(addReg).cacheControl(noCache).build();
	}
	
	@DELETE
    @Path("{regId}")
	public Response deleteReg(@Context HttpServletRequest request, @PathParam("regId") String regId)
	{
		final String[] strIds = regId.split(",");
		
		long[] ids = new long[strIds.length];
		for (int idx = 0; idx < ids.length; ++idx)
			ids[idx] = Long.parseLong(strIds[idx]);
		
		try
		{
			dao.deleteRegistration(ids);
		}
		catch (TestEntityNotFoundException t)
		{
    		LOGGER.error("Reg with id " + regId + " does not exist", t);
    		return Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).cacheControl(noCache).build();   
		}
		catch (Throwable t)
		{
    		LOGGER.error("Error deleting reg with id " + regId, t);
    		return Response.serverError().cacheControl(noCache).build();	
		}
		
		return Response.ok().cacheControl(noCache).build();
	}
}
