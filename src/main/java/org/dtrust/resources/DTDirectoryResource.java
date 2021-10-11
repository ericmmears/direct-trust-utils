package org.dtrust.resources;

import java.io.InputStream;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.stereotype.Component;

/**
 * Fake data upload and download
 * @author gm2552
 *
 */
@Component
@Path("dd/api/")
public class DTDirectoryResource
{
	protected static final CacheControl noCache;
	
	static
	{
		noCache = new CacheControl();
		noCache.setNoCache(true);
	}
	
	public DTDirectoryResource()
	{
		System.out.println("Starting up DT Directory Service");
	}
	
	@GET
    @Path("download/{year}/{month}/{day}/{file}")  
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getDTDirectory(@Context HttpServletRequest request, @Context UriInfo uriInfo)
    {
        try
        {
        	final InputStream str = this.getClass().getClassLoader().getResourceAsStream("importFiles/singleOrgProvImport.csv.zip");
        	
        	return Response.ok(str)
        			.cacheControl(noCache).type("application/zip").build();
        }
    	catch (Throwable t)
    	{
    		System.err.println("Failed to download directory");
    		t.printStackTrace();
    		return Response.serverError().cacheControl(noCache).build();   
    	}  
    }	
	
	@POST 
    @Path("contribute")  
	public Response uploadDTDirectory(InputStream csv)
	{
		return Response.ok(UUID.randomUUID().toString())
    			.cacheControl(noCache).build();
	}	
}
