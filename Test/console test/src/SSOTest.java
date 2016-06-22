import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.security.Principal;
import java.security.Provider.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.CXFBusFactory;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.http.HttpEntity; 
import org.apache.http.HttpResponse; 
import org.apache.http.auth.AuthScope; 
import org.apache.http.auth.Credentials; 
import org.apache.http.client.methods.HttpGet; 
import org.apache.http.client.methods.HttpUriRequest; 
import org.apache.http.client.params.AuthPolicy; 
import org.apache.http.impl.auth.SPNegoSchemeFactory; 
import org.apache.http.impl.client.DefaultHttpClient; 
import org.apache.http.util.EntityUtils;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;
import org.w3c.dom.Entity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.WinHttpClients;

public class SSOTest {

	private static final String SERVICE_NAME = null;

	public static void main(String[] args) throws IOException, GSSException {
		testSo("https://knowledgelake.sharepoint.com/", "mark.carter", "Bayfield11");
		System.setProperty("java.security.auth.login.config", "login.conf"); 
        System.setProperty("java.security.krb5.conf", "krb5.conf"); 
        System.setProperty("sun.security.krb5.debug", "true"); 
        System.setProperty("javax.security.auth.useSubjectCredsOnly","false");
        
        //test();
        DefaultHttpClient httpclient = new DefaultHttpClient(); 
        try { 
        	httpclient.getAuthSchemes().register(AuthPolicy.SPNEGO, new SPNegoSchemeFactory()); 
  
            Credentials use_jaas_creds = new Credentials() { 
  
            	public String getPassword() { 
                     return null; 
            	} 
  
            	@Override
            	public Principal getUserPrincipal() {

            		return new Principal() {

            			@Override
            			public String getName() {
            				return getName();
            			}
            		};
            	}
            };

            com.sun.security.auth.module.NTSystem NTSystem = new com.sun.security.auth.module.NTSystem();
            System.out.println(NTSystem.getName());
            
            
            httpclient.getCredentialsProvider().setCredentials(new AuthScope(null, -1, null), use_jaas_creds); 
            HttpUriRequest request = new HttpGet("https://knowledgelake.sharepoint.com/"); 
            HttpResponse response = httpclient.execute(request); 
            HttpEntity entity = response.getEntity(); 
  
            System.out.println("----------------------------------------"); 
            System.out.println(response.getStatusLine()); 
            System.out.println("----------------------------------------"); 
            if (entity != null) { 
            	System.out.println(EntityUtils.toString(entity)); 
            } 
            System.out.println("----------------------------------------"); 
  
            // This ensures the connection gets released back to the manager 
            EntityUtils.consume(entity); 
  
        	} 
        	finally { 
        		// When HttpClient instance is no longer needed, 
        		// shut down the connection manager to ensure 
        		// immediate deallocation of all system resources 
        		httpclient.getConnectionManager().shutdown(); 
        	} 
    	} 
	
		private static void testSo(String endpointUrl, String username, String password) {
			String busFactory = System.getProperty(BusFactory.BUS_FACTORY_PROPERTY_NAME);
			try {
				// Setup the system properties to use the CXFBusFactory not the SpringBusFactory
				System.setProperty(BusFactory.BUS_FACTORY_PROPERTY_NAME, "org.apache.cxf.bus.CXFBusFactory");

				CXFBusFactory bf = new CXFBusFactory();         
				Bus bus = bf.createBus();
				bus.getFeatures().add(new org.apache.cxf.feature.LoggingFeature());

				STSClient stsClient = new STSClient(bus);
				stsClient.setWsdlLocation("https://example.com/adfs/services/trust/mex");
				stsClient.setServiceName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}SecurityTokenService");
				stsClient.setEndpointName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}CustomBinding_IWSTrustFeb2005Async");

				bus.setProperty("ws-security.callback-handler", "com.example.ClientCallbackHandler");
				bus.setProperty("ws-security.username", username);
				bus.setProperty("ws-security.password", password);
				bus.setProperty("ws-security.sts.client", stsClient);

				BusFactory.setDefaultBus(bus);
				BusFactory.setThreadDefaultBus(bus);

				URL wsdlUrl = new URL(endpointUrl + "?singleWsdl");
				//Service ss = OrganizationService.create(wsdlUrl, SERVICE_NAME);         
				//IOrganizationService port = ss.getPort(IOrganizationService.class);

				//ColumnSet cs = new ColumnSet();
				//cs.setAllColumns(true);
				//Entity e = port.retrieve("account", "323223", cs);          
			} 
			catch (Exception ex) {
				ex.printStackTrace();
			} 
			finally {
				// clean up the system properties
				if (busFactory != null) {
					System.setProperty(BusFactory.BUS_FACTORY_PROPERTY_NAME, busFactory);
				} else {
					System.clearProperty(BusFactory.BUS_FACTORY_PROPERTY_NAME);
			}
		}
	}
		
	private static void test() throws GSSException, IOException{
		DataInputStream inStream = null;
		DataOutputStream outStream = null;
		Oid krb5Oid = new Oid("1.2.840.113554.1.2.2");
		GSSManager manager = GSSManager.getInstance();
		GSSName serverName = manager.createName("11.11.24.43", null);

		GSSContext context = 
				manager.createContext(serverName,
	        	                      krb5Oid,
	        	                      null,
	        	                      GSSContext.DEFAULT_LIFETIME);
			
		context.requestMutualAuth(true);  // Mutual authentication
		context.requestConf(true);  // Will use encryption later
		context.requestInteg(true); // Will use integrity later
			
			
		byte[] token = new byte[0];

		while (!context.isEstablished()) {

			// token is ignored on the first call
			token = context.initSecContext(token, 0, token.length);

			// Send a token to the server if one was generated by
			// initSecContext
			if (token != null) {
			    System.out.println("Will send token of size " + token.length + " from initSecContext.");
			    outStream.writeInt(token.length);
			    outStream.write(token);
			    outStream.flush();
			}

			// If the client is done with context establishment
			// then there will be no more tokens to read in this loop
			if (!context.isEstablished()) {
			    token = new byte[inStream.readInt()];
			    System.out.println("Will read input token of size " + token.length + " for processing by initSecContext");
			    inStream.readFully(token);
			}
		}

		System.out.println("Context Established! ");
		System.out.println("Client is " + context.getSrcName());
		System.out.println("Server is " + context.getTargName());
		if (context.getMutualAuthState())
			System.out.println("Mutual authentication took place!");
		}
}

