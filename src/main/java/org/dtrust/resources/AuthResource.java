package org.dtrust.resources;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.dtrust.dao.interoptest.dao.UserDAO;
import org.dtrust.dao.interoptest.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sun.jersey.spi.resource.Singleton;

@Component
@Path("auth/")
@Singleton
public class AuthResource extends RoleProtectedResource
{
	protected static final String SESSION_PRINCIPAL_ATTRIBUTE = "NHINDAuthPrincipalAttr";
	
    static final String UTF8 = ";charset=UTF-8";
	
	@Autowired
	protected UserDAO dao;
	
	public AuthResource()
	{
		
	}
	
	@POST
    @Path("login")
	public Response login(@Context HttpServletRequest request) throws Exception
	{
		final String username = getUsernameByContext(request);
		
		final User user = dao.getUserByUsername(username);
		user.setHashedPass("");
		
		return Response.ok(user).cacheControl(noCache).build();
	}
	
	@POST
    @Path("logout")
	public Response logout(@Context HttpServletRequest request) throws Exception
	{
		if (request != null)
		{
            final HttpSession session = request.getSession(true);
            final Principal sessionPrin = (Principal) session.getAttribute(SESSION_PRINCIPAL_ATTRIBUTE);
            if (sessionPrin != null)
            {
            	session.removeAttribute(SESSION_PRINCIPAL_ATTRIBUTE);
            }
		}
		
		return Response.ok("Logout.").cacheControl(noCache).build();
	}	
}
