package org.dtrust.postapproval;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.jce.X509Principal;
import org.dtrust.resources.utils.MessageBuilderUtils;
import org.dtrust.util.SMTPMessageSender;
import org.nhindirect.policy.x509.AuthorityInfoAccessExtentionField;
import org.nhindirect.policy.x509.AuthorityInfoAccessMethodIdentifier;
import org.nhindirect.stagent.CryptoExtensions;
import org.nhindirect.stagent.cert.RevocationManager;
import org.nhindirect.stagent.cert.Thumbprint;
import org.nhindirect.stagent.cert.impl.CRLRevocationManager;

public class GTABCertPathValidation
{
	private static final String CA_ISSUER_CHECK_STRING = AuthorityInfoAccessMethodIdentifier.CA_ISSUERS.getName() + ":";
	
	protected static final int DEFAULT_URL_CONNECTION_TIMEOUT = 10000; // 10 seconds	
	
	protected static final int DEFAULT_URL_READ_TIMEOUT = 10000; // 10 hour seconds	
	
	protected static final String FED_BRIDGE_AIA_URL = "http://http.fpki.gov/bridge/caCertsIssuedTofbca2013.p7c";
	
	protected static final String COMMON_POLICY_AIA_URL = "http://http.fpki.gov/fcpca/caCertsIssuedTofcpca.p7c";
	
	protected static final String GTAB_ANCHOR_PATH;
	
	protected static final InternetAddress LOCAL_SENDER;
	
	protected static final Collection<InternetAddress> REPORT_RECIPS;
	
	protected static final SMTPMessageSender sender;
	
	static Map<String, X509Certificate> fbcaBridgeCerts;
	
	static Map<String, X509Certificate> commonPolicyCert;
	
	static Map<String, Collection<X509Certificate>> aiaCache;
	

	
	static
	{
		CryptoExtensions.registerJCEProviders();
		
		aiaCache = new HashMap<String, Collection<X509Certificate>>();
		
		try
		{
			fbcaBridgeCerts = new HashMap<String, X509Certificate>();
			final Collection<X509Certificate> certs = downloadCertsFromAIA(FED_BRIDGE_AIA_URL);
			for (X509Certificate cert : certs)
				fbcaBridgeCerts.put(Thumbprint.toThumbprint(cert).toString(), cert);
		}
		catch (Exception e)
		{
			System.err.println("Could not bootstrap FBCA Bridge Certs: " + e.getMessage());
			System.exit(-1);
		}
		
		try
		{
			commonPolicyCert = new HashMap<String, X509Certificate>();
			final Collection<X509Certificate> certs = downloadCertsFromAIA(COMMON_POLICY_AIA_URL);
			for (X509Certificate cert : certs)
				commonPolicyCert.put(Thumbprint.toThumbprint(cert).toString(), cert);
		}
		catch (Exception e)
		{
			System.err.println("Could not bootstrap Federal Common Policy Certs: " + e.getMessage());
			System.exit(-1);
		}		
		
		InputStream str = GTABCertPathValidation.class.getClassLoader().getResourceAsStream("properties/gtabValidate.properties");
		
		try
		{
			final Properties props = new Properties();
			props.load(str);
			
			GTAB_ANCHOR_PATH = props.getProperty("gtabValidate.bundleURL");
			LOCAL_SENDER = new InternetAddress(props.getProperty("gtabValidate.localSender"));
			
			REPORT_RECIPS = new ArrayList<InternetAddress>();
			final String repRecips = props.getProperty("gtabValidate.reportRecips");
			for (String recip : repRecips.split(","))
				REPORT_RECIPS.add(new InternetAddress(recip));
		
			
			sender = new SMTPMessageSender(props.getProperty("gtabValidate.msgSender.port"), props.getProperty("gtabValidate.msgSender.server"), 
					props.getProperty("gtabValidate.msgSender.username"), props.getProperty("gtabValidate.msgSender.password"));
		}
		catch (Exception e)
		{
			throw new IllegalStateException("Could not read properties from file properties/gtabValidate.properties.", e);
		}
		finally
		{
			IOUtils.closeQuietly(str);
		}
	}
	
	public static void main(String[] args)
	{
		try
		{

			
			// get anchors from bundle
			final Collection<X509Certificate> bundleAnchors = getAnchorsInBundle(GTAB_ANCHOR_PATH);
			
			for (X509Certificate bundleAnchor : bundleAnchors)
			{
			
				System.out.println("Validating cert path for anchor " + getCertPrintName(bundleAnchor));
				
				final PathTree pathTree = new PathTree(bundleAnchor);
				
				// build the path
				pathTree.buildPathTree();
	
				if (pathTree.rootNode.commonPolicyCertChain)
				{
					System.out.println("Certificate chains to FBCA Common CA");
				}
				
				pathTree.printFBCACommonPath();
				
				if (pathTree.isValidFBCAPathChain())
				{
					System.out.println("Valid chain to FBCA Common");
				}
				else
				{
					System.out.println("Invalid chain to FBCA Common");
					sendFailureMessage(bundleAnchor);
				}
				
				System.out.println("\r\n\r\n");
			}
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	protected static class PathTree
	{
		final RevocationManager revocationManager;
		
		protected final Node rootNode;
		
		protected final Map<String, TraverseLock> encounteredCerts;
		
		protected class TraverseLock
		{
			protected String travId;
			protected X509Certificate cert;
			
			public TraverseLock(String travId, X509Certificate cert)
			{
				this.travId = travId;
				this.cert = cert;
			}
		}
		
		public class Node
		{
			protected final X509Certificate cert;
			
			protected final ArrayList<Node> parents;	
			
			protected final Node child;
			
			protected boolean commonPolicyCertChain;
			
			public Node(X509Certificate cert, Node child)
			{
				this.cert = cert;
				this.child = child;
				this.parents = new ArrayList<Node>();
			}
			
			public X509Certificate getCert()
			{
				return this.cert;
			}
		}
		
		public PathTree(X509Certificate rootCert)
		{
			this.revocationManager = CRLRevocationManager.getInstance();
			this.encounteredCerts = new HashMap<String, TraverseLock>();
			this.rootNode = new Node(rootCert, null);
		}
		
		public void buildPathTree() throws Exception
		{
			buildTree(rootNode, null);
		}
		
		protected boolean buildTree(Node node, Node child)
		{
			boolean retVal = false;
			
			final String travId = UUID.randomUUID().toString();
			
			try
			{
				Collection<X509Certificate> aiaCerts = getIntermediateCertsByAIA(node.cert);
				
				if (aiaCerts != null && !aiaCerts.isEmpty())
				{
					// breadth search first
					for (X509Certificate possibleParent : aiaCerts)
					{
						if (isParentCert(Arrays.asList(node.cert), Arrays.asList(possibleParent)))
						{
							final Node parentNode = new Node(possibleParent, node);
							node.parents.add(parentNode);
							
							final String tp = Thumbprint.toThumbprint(parentNode.cert).toString();
							
							// try to get a lock on this cert
							if (encounteredCerts.get(tp) == null)
							{
								// I'm the first to encounter this cert
								final TraverseLock lock = new TraverseLock(travId, possibleParent);
								encounteredCerts.put(tp, lock);
							}
						}
					}
					// now go to each parent node if necessary
					for (Node parentNode : node.parents)
					{

						final String tp = Thumbprint.toThumbprint(parentNode.cert).toString();
						
						// if this is a common policy cert, return true as the function value
						// we will stop at the common policy cert
						if (commonPolicyCert.containsKey(tp))
						{
							parentNode.commonPolicyCertChain = true;
							node.commonPolicyCertChain = true;
							retVal = true;
						}
						else if (encounteredCerts.get(tp).travId.equals(travId))
						{
							// I was the first to encounter this cert, so I get to traverse it
							// set the common policy cert path chain flag equal
							// to the value of the truee build
							if (buildTree(parentNode, node))
							{
								node.commonPolicyCertChain = true;
								retVal = true;
							}
						}
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			
			return retVal;
		}
		
		public void printPath()
		{
			printPath(this.rootNode, 0, false);
		}
		
		protected void printFBCACommonPath()
		{
			printPath(this.rootNode, 0, true);
		}
		
		protected void printPath(Node node, final int level, boolean fbcaPathOnly)
		{
			if (!node.commonPolicyCertChain && fbcaPathOnly)
			{
				if (level == 0)
					System.out.println("Cert does not chain to FBCA common CA");
				return;
			}
			
			for (int idx = 1; idx <= level; ++idx)
			{
				if (idx == level)
				{
					if (node.parents == null || node.parents.isEmpty())
						System.out.print("+- ");
					else 
						System.out.print("\\- ");
						
				}
				else
					System.out.print("|   ");
			}

			System.out.println(getCertPrintName(node.cert) + "  (SHA1:" + Thumbprint.toThumbprint(node.cert).toString() + ")");
			
			if (node.parents != null && !node.parents.isEmpty())
			{
				for (Node parentNode: node.parents)
					printPath(parentNode, (level + 1), fbcaPathOnly);
			}
		}
		
		protected boolean isValidFBCAPathChain()
		{
			// visit all nodes that are in the FBCA Common chain and make sure they are not expired or revoked
			// and that they can build a valid chain
			
			final List<X509Certificate> pathCerts = new ArrayList<X509Certificate>();
			
			// start at the bottom and go all the way to the top validating that the certificates are not revoke
			buildFBCACertifiedPath(this.rootNode, pathCerts);
			
			// all of the certs should be in the path certs list to create the cert path
			// chain
			return isParentCert(pathCerts, commonPolicyCert.values());
		}
		
		protected void buildFBCACertifiedPath(Node node, List<X509Certificate> pathCerts)
		{
			if (node.commonPolicyCertChain)
			{
				// make sure it's not revoke
				if (!revocationManager.isRevoked(node.getCert()))
				{
					pathCerts.add(node.getCert());
				}
				
				for (Node parent : node.parents)
				{
					if (parent.commonPolicyCertChain && 
							!commonPolicyCert.containsKey(Thumbprint.toThumbprint(parent.cert).toString()))
					{
						buildFBCACertifiedPath(parent, pathCerts);
					}
				}
			}
		}
	}
	
	protected static boolean isParentCert(List<X509Certificate> certCol, Collection<X509Certificate> possibleParentCerts)
	{
		try
		{			
			final Set<TrustAnchor> trustAnchorSet = new HashSet<TrustAnchor>();
			for (X509Certificate parent : possibleParentCerts)
				trustAnchorSet.add(new TrustAnchor(parent, null));
			
			final PKIXParameters params = new PKIXParameters(trustAnchorSet); 
			params.setRevocationEnabled(false);
			
			final CertificateFactory factory = CertificateFactory.getInstance("X509");
			final CertPath certPath = factory.generateCertPath(certCol);
	    	final CertPathValidator pathValidator = CertPathValidator.getInstance("PKIX", CryptoExtensions.getJCEProviderNameForTypeAndAlgorithm("CertPathValidator", "PKIX"));    		
			
	    	
	    	
	    	pathValidator.validate(certPath, params);
			return true;
		}
		catch (Exception e)
		{
			return false;
		}

	}
	
    protected static Collection<X509Certificate> getIntermediateCertsByAIA(X509Certificate certificate) throws Exception 
    {
    	final Collection<X509Certificate> retVal = new ArrayList<X509Certificate>();
    
    	// check to see if there are extensions
    	final AuthorityInfoAccessExtentionField aiaField = new AuthorityInfoAccessExtentionField(false);
    	
		// we can get all names from the AuthorityInfoAccessExtentionField objects
		aiaField.injectReferenceValue(certificate);
		
		final Collection<String> urlPairs = aiaField.getPolicyValue().getPolicyValue();
		
		// look through all of the values (if they exist) for caIssuers
		for (String urlPair : urlPairs)
		{
			if (urlPair.startsWith(CA_ISSUER_CHECK_STRING))
			{
				// the url pair is in the format of caIssuer:URL... need to break it 
				// apart to get the url
				final String url = urlPair.substring(CA_ISSUER_CHECK_STRING.length());
				
				if (url.toLowerCase().startsWith("http"))
				{
					// now pull the certificate from the URL
	
					final Collection<X509Certificate> intermCerts = downloadCertsFromAIA(url);
					retVal.addAll(intermCerts);
				}
			}
		}
    	
    	return retVal;
    }	
    
	@SuppressWarnings("unchecked")
	protected static Collection<X509Certificate> downloadCertsFromAIA(String url) throws Exception
	{
		final Collection<X509Certificate> cachedAia = aiaCache.get(url.toLowerCase());
		if (cachedAia != null)
			return cachedAia;
		
		InputStream inputStream = null;

		Collection<? extends Certificate> retVal = null;
		
		try
		{
			// in this case the cert is a binary representation
			// of the CERT URL... transform to a string
			final URL certURL = new URL(url);
			
			final URLConnection connection = certURL.openConnection();
			
			// the connection is not actually made until the input stream
			// is open, so set the timeouts before getting the stream
			connection.setConnectTimeout(DEFAULT_URL_CONNECTION_TIMEOUT);
			connection.setReadTimeout(DEFAULT_URL_READ_TIMEOUT);
			
			// open the URL as in input stream
			inputStream = connection.getInputStream();
			
			// download the trust bundle
			retVal = CertificateFactory.getInstance("X.509", "BC").generateCertificates(inputStream);
			
			aiaCache.put(url.toLowerCase(), (Collection<X509Certificate>)retVal);
		}
		catch (Exception e)
		{
			System.err.println("Error getting certs from AIA at URL " + url);
			return Collections.emptyList();
		}
		finally
		{
			IOUtils.closeQuietly(inputStream);
		}
		
		return (Collection<X509Certificate>)retVal;
	}  	
	
	protected static Collection<X509Certificate> getAnchorsInBundle(String bundleURL)
	{
		final byte[] bundleBytes = downloadBundleToByteArray(bundleURL);
		if (bundleBytes == null)
		{
	    	System.out.println("Could not get bundle at URL " + bundleURL);
	    	System.exit(-1);
		}
			
		return convertRawBundleToAnchorCollection(bundleBytes);
	}
	
	protected static byte[] downloadBundleToByteArray(String url)
	{
		InputStream inputStream = null;

		byte[] retVal = null;
		final ByteArrayOutputStream ouStream = new ByteArrayOutputStream();
		
		try
		{
			// in this case the cert is a binary representation
			// of the CERT URL... transform to a string
			final URL certURL = new URL(url);
			
			final URLConnection connection = certURL.openConnection();
			
			// the connection is not actually made until the input stream
			// is open, so set the timeouts before getting the stream
			connection.setConnectTimeout(DEFAULT_URL_CONNECTION_TIMEOUT);
			connection.setReadTimeout(DEFAULT_URL_READ_TIMEOUT);
			
			// open the URL as in input stream
			inputStream = connection.getInputStream();
			
			int BUF_SIZE = 2048;		
			int count = 0;

			final byte buf[] = new byte[BUF_SIZE];
			
			while ((count = inputStream.read(buf)) > -1)
			{
				ouStream.write(buf, 0, count);
			}
			
			retVal = ouStream.toByteArray();
		}
		///CLOVER:OFF
		catch (Exception e)
		{
			e.printStackTrace();
		}
		///CLOVER:ON
		finally
		{
			IOUtils.closeQuietly(inputStream);
			IOUtils.closeQuietly(ouStream);
		}
		
		return retVal;
	}
	
	@SuppressWarnings("unchecked")
	protected static Collection<X509Certificate> convertRawBundleToAnchorCollection(byte[] rawBundle)
	{
		Collection<? extends Certificate> bundleCerts = null;
		InputStream inStream = null;
		// check to see if its an unsigned PKCS7 container
		try
		{
			inStream = new ByteArrayInputStream(rawBundle);
			bundleCerts = CertificateFactory.getInstance("X.509").generateCertificates(inStream);
			
			// in Java 7, an invalid bundle may be returned as a null instead of throw an exception
			// if its null and has no anchors, then try again as a signed bundle
			if (bundleCerts != null && bundleCerts.size() == 0)
				bundleCerts = null;
			
		}
		catch (Exception e)
		{
			/* no-op for now.... this may not be a p7b, so try it as a signed message*/
			e.printStackTrace();
		}
		finally
		{
			IOUtils.closeQuietly(inStream);
		}

		return (Collection<X509Certificate>)bundleCerts;
	}	
	
	protected static String getCertPrintName(X509Certificate cert)
	{
		X509Principal principal = null;
		try
		{
			principal = PrincipalUtil.getSubjectX509Principal(cert);
			final Vector<?> values = principal.getValues(X509Name.CN);
			final String cn = (String) values.get(0);
			return cn;
		}
		catch (Exception e)
		{
			if (principal == null)
				return "NA";
			else
				return principal.getName();
		}
	}
	
	protected static void sendFailureMessage(X509Certificate bundleAnchor) throws Exception
	{
		final StringBuilder builder = new StringBuilder("Certificat path validation to FBCA Common CA failed for the the following anchor:");
		builder.append("\r\n\tAnchor Common Name:").append(getCertPrintName(bundleAnchor));
		builder.append("\r\n\tSerial Number: ").append(bundleAnchor.getSerialNumber().toString(16));
		builder.append("\r\n\tThumbprint: ").append(Thumbprint.toThumbprint(bundleAnchor).toString());
		
		final MimeMessage reportMsg = MessageBuilderUtils.createMimeMessage(REPORT_RECIPS, LOCAL_SENDER, 
				"GTAB Anchor Validation Error", builder.toString(), null, null, null, false);
		
		sender.sendMessage(reportMsg);
	}
}
