package org.dtrust.resources;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.CacheControl;

import org.nhindirect.common.rest.auth.NHINDPrincipal;

public abstract class RoleProtectedResource
{
	protected static final String SESSION_PRINCIPAL_ATTRIBUTE = "NHINDAuthPrincipalAttr";
    
	/**
	 * Cache definition for no caching of responses.
	 */
	protected static final CacheControl noCache;
	static
	{
		noCache = new CacheControl();
		noCache.setNoCache(true);
	}
	
	public RoleProtectedResource()
	{
		
	}
	
	protected String getRoleByContext(HttpServletRequest request)
	{
		// TODO implement getting the username out of the URI Info.
		if (request != null)
		{
            final HttpSession session = request.getSession(true);
            final Principal sessionPrin = (Principal) session.getAttribute(SESSION_PRINCIPAL_ATTRIBUTE);
            if (sessionPrin != null)
            {
            	NHINDPrincipal prin = (NHINDPrincipal)sessionPrin;
            	return prin.getRole();
            }
		}
		return "";
	}
	
	protected String getUsernameByContext(HttpServletRequest request)
	{
		// TODO implement getting the username out of the URI Info.
		if (request != null)
		{
            final HttpSession session = request.getSession(true);
            final Principal sessionPrin = (Principal) session.getAttribute(SESSION_PRINCIPAL_ATTRIBUTE);
            if (sessionPrin != null)
            {
            	return sessionPrin.getName();
            }
		}
		return "SYSTEM";
	}
}
