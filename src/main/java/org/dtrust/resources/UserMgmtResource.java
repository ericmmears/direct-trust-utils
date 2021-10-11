package org.dtrust.resources;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dtrust.dao.interoptest.dao.TestConflictException;
import org.dtrust.dao.interoptest.dao.TestEntityNotFoundException;
import org.dtrust.dao.interoptest.dao.UserDAO;
import org.dtrust.dao.interoptest.entity.User;
import org.dtrust.dao.interoptest.entity.UserAccountStatus;
import org.dtrust.dao.interoptest.entity.UserRole;
import org.dtrust.resources.utils.MessageBuilderUtils;
import org.dtrust.util.MessageSender;
import org.nhindirect.common.crypto.exceptions.CryptoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sun.jersey.spi.resource.Singleton;

@Component
@Path("userMgmt/")
@Singleton
public class UserMgmtResource extends RoleProtectedResource
{
	protected static int RESET_PASS_TIMEOUT = 24;
	
	@Autowired
	@Qualifier("localSender")
	protected InternetAddress localSender;
	
	@Autowired
	@Qualifier("msgSender")
	protected MessageSender msgSender;
	
	@Autowired
	@Qualifier("userRegNotifRecip")
	protected InternetAddress userRegNotifAddress;	

	@Autowired
	@Qualifier("baseReturnURI")
	protected String baseReturnURI;		
	
	protected final char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', 
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};	
	
    static final String UTF8 = ";charset=UTF-8";
	
	protected static final String SESSION_PRINCIPAL_ATTRIBUTE = "NHINDAuthPrincipalAttr";
	
	protected final String resetPasswordTemplate;
	protected final String accountApprovalTemplate;	
	protected final X509Certificate signingCert;
	protected final PrivateKey signingKey;
	
	/**
	 * Cache definition for no caching of responses.
	 */
	protected static final CacheControl noCache;
	static
	{
		noCache = new CacheControl();
		noCache.setNoCache(true);
	}
	
	private static final Log LOGGER = LogFactory.getFactory().getInstance(UserMgmtResource.class);

	@Autowired
	protected UserDAO dao;
	
	public UserMgmtResource()
	{
		final InputStream accountApprovalStr = this.getClass().getClassLoader().getResourceAsStream("templates/accountApproval.txt");
		final InputStream str = this.getClass().getClassLoader().getResourceAsStream("templates/resetPasswordTemplate.txt");
		final InputStream certStream = this.getClass().getClassLoader().getResourceAsStream("certs/msgsign.der");
		final InputStream keyStream = this.getClass().getClassLoader().getResourceAsStream("certs/msgsignkey.der");
		
		try
		{
			
			resetPasswordTemplate = IOUtils.toString(str);
			accountApprovalTemplate = IOUtils.toString(accountApprovalStr);
			
			signingCert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(certStream);
			final PKCS8EncodedKeySpec keysp = new PKCS8EncodedKeySpec ( IOUtils.toByteArray(keyStream) );
			signingKey = KeyFactory.getInstance("RSA").generatePrivate(keysp);
		}
		catch (Exception e)
		{
			throw new IllegalStateException("Could not load signing cert information", e);
		}
		finally
		{
			IOUtils.closeQuietly(str);
			IOUtils.closeQuietly(accountApprovalStr);
			IOUtils.closeQuietly(certStream);
			IOUtils.closeQuietly(keyStream);
		}
	}
	
	protected Response ensureAdminRoleAccess(@Context HttpServletRequest request)
	{
		final String username = this.getUsernameByContext(request);
		final String role = this.getRoleByContext(request);
		
		if (!role.equals(UserRole.ADMIN.toString()))
		{
    		LOGGER.error("User " + username + " does not have permission to perform this action");
    		return Response.status(javax.ws.rs.core.Response.Status.FORBIDDEN).cacheControl(noCache).build();  			
		}
		
		return null;
	}
	
	protected Response ensureAdminRoleOrUserAccess(@Context HttpServletRequest request, String actionalbeUserName)
	{
		final String username = this.getUsernameByContext(request);
		final String role = this.getRoleByContext(request);
		
		
		if (!((role.equals(UserRole.TESTER.toString()) && username.equalsIgnoreCase(actionalbeUserName)) || role.equals(UserRole.ADMIN.toString())))
		{
    		return Response.status(javax.ws.rs.core.Response.Status.FORBIDDEN).cacheControl(noCache).build();  	
		}
		
		return null;
	}
	
	@GET
    @Produces(MediaType.APPLICATION_JSON + UTF8)  
	public Response getUsers(@Context HttpServletRequest request)
	{		
		final Response forbiddenResp = ensureAdminRoleAccess(request);
		if (forbiddenResp != null)
			return forbiddenResp;
		
		GenericEntity<Collection<? extends User>> entity = null;
		
		try
		{
			Collection<User> users = dao.getUsers();
			
    		if (users.isEmpty())
    		{
    			return Response.noContent().cacheControl(noCache).build();
    		}
    		
    		// do not return admin roles
    		final Collection<User> retUsers = new ArrayList<User>();
    		
    		for (User user : users)
    		{
    			if (user.getRole() != UserRole.ADMIN)
    			{
    				// blow away the password
    				user.setHashedPass(null);
    				user.setUpdateChallenge(null);
    				user.setUpdateChallengeIssuedDtTm(null);
    				retUsers.add(user);
    			}
    		}
    		
    		if (retUsers.isEmpty())
    		{
    			return Response.noContent().cacheControl(noCache).build();
    		}
    		
    		entity = new GenericEntity<Collection<? extends User>>(retUsers) {};
		}
    	catch (Throwable t)
    	{
    		LOGGER.error("Error retrieving users.", t);
    		return Response.serverError().cacheControl(noCache).build();   
    	}
		
		return Response.ok(entity).cacheControl(noCache).build();
	}
	
	@GET
    @Produces(MediaType.APPLICATION_JSON + UTF8)  
    @Path("{username}")
	public Response getUserByUsername(@Context HttpServletRequest request, @PathParam("username") String getUser)
	{
		final Response forbiddenResp = ensureAdminRoleOrUserAccess(request, getUser);
		if (forbiddenResp != null)
			return forbiddenResp;
		
		User user = null;
		
		try
		{
			user = dao.getUserByUsername(getUser);
			user.setHashedPass(null);
			user.setUpdateChallenge(null);
			user.setUpdateChallengeIssuedDtTm(null);			
		}
		catch (TestEntityNotFoundException notFoundEx)
		{
    		LOGGER.error("User with username " + getUser + " does not exist.", notFoundEx);
    		return Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).cacheControl(noCache).build();   
		}
    	catch (Throwable t)
    	{
    		LOGGER.error("Error retrieving users.", t);
    		return Response.serverError().cacheControl(noCache).build();   
    	}
		
		return Response.ok(user).cacheControl(noCache).build();
	}
	
	@POST
    @Produces(MediaType.APPLICATION_JSON + UTF8)  
    @Consumes(MediaType.APPLICATION_JSON + UTF8)   
	public Response updateUser(@Context HttpServletRequest request, User user)
	{
		final Response forbiddenResp = ensureAdminRoleAccess(request);
		if (forbiddenResp != null)
			return forbiddenResp;
		
		User updatedUser = null;
		
		try
		{
			updatedUser = dao.updateUser(user);
	
		}
    	catch (Throwable t)
    	{
    		LOGGER.error("Failed to update user " + user.getUsername());
    		return Response.serverError().cacheControl(noCache).build();   
    	}
		
		
		return Response.ok(updatedUser).cacheControl(noCache).build();
	}	
	
	@POST
    @Produces(MediaType.APPLICATION_JSON + UTF8)  
    @Consumes(MediaType.APPLICATION_JSON + UTF8)  
    @Path("{updateUserAccountStatus}")
	public Response updateUserAcccountStatus(@Context HttpServletRequest request, Collection<UpdateUserStatus> stats)
	{
		final Response forbiddenResp = ensureAdminRoleAccess(request);
		if (forbiddenResp != null)
			return forbiddenResp;
		
		try
		{
			for (UpdateUserStatus stat : stats)
			{
				final User user = dao.getUserByUsername(stat.getUsername());
				
				if (stat.accountStatus == UserAccountStatus.TERMINATED)
					dao.deleteUser(user.getUserId());
				else
				{
					final UserAccountStatus oldStatus = user.getAccountStatus();
					
					user.setAccountStatus(stat.getAccountStatus());
				
					dao.updateUser(user);
					
					// don't send a message if their status is already been set to approved
					if (oldStatus != stat.getAccountStatus() && stat.getAccountStatus() == UserAccountStatus.APPROVED)
					{
						final Collection<InternetAddress> notificationAddress = Arrays.asList(new InternetAddress(user.getContactEmail()));

						
						String approvalText = accountApprovalTemplate.replace("{Name}", user.getFirstName());

						
						String path = baseReturnURI;		
						
						
						approvalText = approvalText.replace("{baseURI}", path);
						
						approvalText = approvalText.replace("{username}", user.getUsername());
						
						final MimeMessage approvalMsg = MessageBuilderUtils.createMimeMessage(notificationAddress, localSender, 
								"DirectTrust Interop Registration Approval", "", null, null, null, false);
						
						approvalMsg.setContent(approvalText, "text/html");
						
						approvalMsg.saveChanges();
						
						try
						{
							msgSender.sendMessage(approvalMsg);
						}
						catch (Exception e)
						{
							LOGGER.error("Failed to send approval email to user " + user.getUsername());
						}
					}
				}
			}
		}    	
		catch (Throwable t)
    	{
    		LOGGER.error("Failed to update user account statuses ");
    		return Response.serverError().cacheControl(noCache).build();   
    	}
		return Response.ok().cacheControl(noCache).build();
	}
		
	@PUT
    @Produces(MediaType.APPLICATION_JSON + UTF8)  
    @Consumes(MediaType.APPLICATION_JSON + UTF8)   
	public Response selfRegister(@Context UriInfo uriInfo, User user)
	{
		
		User addedUser = null;
		
		try
		{
			// the password needs to be hashed
			user.setHashedPass(convertPassToHash(user.getHashedPass()));
			user.setRole(UserRole.TESTER);
			user.setAccountStatus(UserAccountStatus.REQUESTED);
			
			addedUser = dao.addUser(user);
			
			final Collection<InternetAddress> notificationAddress = Arrays.asList(userRegNotifAddress);

			
			final MimeMessage reportMsg = MessageBuilderUtils.createMimeMessage(notificationAddress, localSender, 
					"DirectTrust Interop Registration Approval Request", "User " +  user.getUsername() +
							 " has request access to the Bundle Interop Application.", null, null, null, false);
			
			try
			{
				msgSender.sendMessage(reportMsg);
			}
			catch (Exception e)
			{
				LOGGER.error("Failed to send approval request notification email for user " + user.getUsername());
			}
	
		}
		catch (TestConflictException e)
		{
    		LOGGER.error("User with username " + user.getUsername() + " already exists.");
    		return Response.status(javax.ws.rs.core.Response.Status.CONFLICT).cacheControl(noCache).build();  
		}
    	catch (Throwable t)
    	{
    		LOGGER.error("Error adding user " + user.getUsername(), t);
    		return Response.serverError().cacheControl(noCache).build();   
    	}
		
		UriBuilder newLocBuilder = uriInfo.getBaseUriBuilder();
		URI newLoc = newLocBuilder.path(user.getUsername()).build();
		
		return Response.created(newLoc).entity(addedUser).cacheControl(noCache).build();
	}	
	
	@POST
    @Produces(MediaType.APPLICATION_JSON + UTF8)  
    @Consumes(MediaType.APPLICATION_JSON + UTF8)   
    @Path("changePassword/{username}/{challenge}/{newPassword}")
	public Response changePassword(@Context UriInfo uriInfo, @PathParam("username") String username,
			@PathParam("challenge") String challenge, 
			@PathParam("newPassword") String newPassword, String signature)
	{
		String path = baseReturnURI;
		
		try
		{
			final User user = dao.getUserByUsername(username);
			
			if (!challenge.equals(user.getUpdateChallenge()))
			{
	    		LOGGER.error("Change password challenge does not match");
	    		return Response.status(javax.ws.rs.core.Response.Status.UNAUTHORIZED).cacheControl(noCache).build();   				
			}
			
			// verify the signature
			final String signedData = user.getUsername() +  challenge;
			final Signature rsaSig = Signature.getInstance("SHA256WITHRSA");
			rsaSig.initVerify(signingCert);
			rsaSig.update(signedData.getBytes());
			if (!rsaSig.verify(Base64.decodeBase64(signature)))
			{
	    		LOGGER.error("Change password challenge signature does not match");
	    		return Response.status(javax.ws.rs.core.Response.Status.UNAUTHORIZED).cacheControl(noCache).build(); 				
			}
			
			// make sure the invite hasn't expired
			Calendar now = Calendar.getInstance();
			now.add(Calendar.HOUR_OF_DAY, -RESET_PASS_TIMEOUT);
			
			if (now.getTimeInMillis() > user.getUpdateChallengeIssuedDtTm().getTimeInMillis())
			{
				// expired
	    		LOGGER.error("Change password has expired");
	    		return Response.status(javax.ws.rs.core.Response.Status.GONE).cacheControl(noCache).build(); 
			}
			
			// update the password
			user.setHashedPass(convertPassToHash(newPassword));
			user.setUpdateChallenge(null);
			user.setUpdateChallengeIssuedDtTm(null);
			dao.updateUser(user);
		}
		catch (TestEntityNotFoundException notFoundEx)
		{
    		LOGGER.error("User with username " + username + " does not exist.", notFoundEx);
    		return Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).cacheControl(noCache).build();   
		}
    	catch (Throwable t)
    	{
    		LOGGER.error("Error retrieving user.", t);
    		return Response.serverError().cacheControl(noCache).build();   
    	}
		
		return Response.ok(path).cacheControl(noCache).build();	
	}
	
	@POST
    @Produces(MediaType.APPLICATION_JSON + UTF8)  
    @Consumes(MediaType.APPLICATION_JSON + UTF8)   
    @Path("forgotPassword/{username}")
	public Response forgotPassword(@Context UriInfo uriInfo, @PathParam("username") String username)
	{
		
		
		try
		{
			final User user = dao.getUserByUsername(username);
			
			// generate a challenge
			final String challengeString = createChallengeString();
			
			final String signedData = user.getUsername() +  challengeString;
			


			final Signature rsaSig = Signature.getInstance("SHA256WITHRSA");
			rsaSig.initSign(signingKey, new SecureRandom());
			
			
			rsaSig.update(signedData.getBytes());
			final byte[] theSig = rsaSig.sign();
			final String encodedData = uriEscape( Base64.encodeBase64String(theSig));
			
			// update the dao
			user.setUpdateChallenge(challengeString);
			user.setUpdateChallengeIssuedDtTm(Calendar.getInstance());
			
			dao.updateUser(user);
			
			// Create the message
			String resetText = resetPasswordTemplate.replace("{Name}", user.getFirstName());

			
			String path = baseReturnURI + "/changePassword.html";			
			
			
			resetText = resetText.replace("{baseURI}", path);
			
			resetText = resetText.replace("{usernameToChange}", user.getUsername());
			resetText = resetText.replace("{userChallenge}", challengeString);
			resetText = resetText.replace("{signeddata}", encodedData);

			
			// send the message
			final Collection<InternetAddress> notificationAddress = Arrays.asList(new InternetAddress(user.getContactEmail()));

			
			final MimeMessage reportMsg = MessageBuilderUtils.createMimeMessage(notificationAddress, localSender, 
					"DirectTrust Interop Registration Password Change Request", "", null, null, null, false);
			
			reportMsg.setContent(resetText, "text/html");
			
			reportMsg.saveChanges();
			
			try
			{
				msgSender.sendMessage(reportMsg);
			}
			catch (Exception e)
			{
				LOGGER.error("Failed to send forgot password request for user " + user.getUsername());
			}
		}
		catch (TestEntityNotFoundException notFoundEx)
		{
    		LOGGER.error("User with username " + username + " does not exist.", notFoundEx);
    		return Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).cacheControl(noCache).build();   
		}
    	catch (Throwable t)
    	{
    		LOGGER.error("Error retrieving user.", t);
    		return Response.serverError().cacheControl(noCache).build();   
    	}
		
		return Response.ok().cacheControl(noCache).build();		
	}
	
	protected String convertPassToHash(String password) throws CryptoException
	{		
		try
		{
			final MessageDigest md = MessageDigest.getInstance("SHA-256");
	
			md.update(password.getBytes());
	        final byte[] digest = md.digest();
	        
	        return createStringRep(digest);
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new CryptoException("Algorithm not supported.", e);
		}
	}
	
	/**
	 * Creates a string representation of a hash digest.
	 * @param digest The digest to convert to a string representation.
	 * @return A string representation of a hash digest.
	 */
	private String createStringRep(byte[] digest)
	{
        final StringBuffer buf = new StringBuffer(digest.length * 2);

        for (byte bt : digest) 
        {
            buf.append(hexDigits[(bt & 0xf0) >> 4]);
            buf.append(hexDigits[bt & 0x0f]);
        }

        return buf.toString();
	}
	
	private String createChallengeString()
	{
		final StringBuilder builder = new StringBuilder();
		
	    final SecureRandom r = new SecureRandom();
	    byte[] seed = r.generateSeed(16);
	    r.setSeed(seed);
	    
	    final String alphabet = "1234567890ABCDEFGHIJKLMNOPQSTUVWXYZ";
	    for (int i = 0; i < 30; i++) 
	        builder.append(alphabet.charAt(r.nextInt(alphabet.length())));
	    
	    return builder.toString();
	}
	
    protected final String uriEscape(String val) 
    {
        try 
        {
            final String escapedVal = URLEncoder.encode(val, "UTF-8");
            // Spaces are treated differently in actual URLs. There don't appear to be any other
            // differences...
            return escapedVal.replace("+", "%20");
        } 
        catch (UnsupportedEncodingException e) 
        {
            throw new RuntimeException("Failed to encode value: " + val, e);
        }
    }
	
	protected static class UpdateUserStatus
	{
		private String username;
		private UserAccountStatus accountStatus;
		
		public UpdateUserStatus()
		{
		}

		public String getUsername()
		{
			return username;
		}

		public void setUsername(String username)
		{
			this.username = username;
		}

		public UserAccountStatus getAccountStatus()
		{
			return accountStatus;
		}

		public void setAccountStatus(UserAccountStatus accountStatus)
		{
			this.accountStatus = accountStatus;
		}
		
	}
	
	
}
