
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringBufferInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
//import java.util.zip.ZipEntry;
//import java.util.zip.ZipFile;
import java.util.UUID;

//inports for xml
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
//imports for http request
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
//import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;


public class BatchTest  {

	private static final String FINAL_DROP_FOLDER_PATH = "c:\\Ephesoft\\SharedFolders\\final-drop-folder";
	private static final String SHARED_FOLDER_PATH = "\\\\klfss1\\users\\mark.carter\\cache";
	private static final String SYSTEM_BATCH_FOLDER_PATH = "c:\\Ephesoft\\SharedFolders\\ephesoft-system-folder";
	private static final String SERVER_URL = "http://10.1.2.252:7212/coreservice/Coreservice/";
	private static final String RELEASEHOLD_URL = SERVER_URL + "ReleaseHoldById/";
	private static final String GET_BATCH_URL = SERVER_URL + "GetBatch/";
	private static final String REMOVE_DOCUMENT_URL = SERVER_URL + "RemoveDocument/";
	private static final String ADD_DOCUMENT_URL = SERVER_URL + "AddDocument/";
	private static final String ADD_DOCUMENT_FILE_URL = SERVER_URL + "ProcessDocument/";
	private static final String BATCH_NAME_FINAL = "_batch.xml";
	private static final String USER_TOKEN = "mark.carter@dev.local";
	private static String batchInstanceId = null;

	public static void main (String[] args) throws Exception {
		batchInstanceId = "BI27E5";
		String csBatchId = getCSBatchId(batchInstanceId);//getCSBatchId("BI278B");
		//String user = getBatchUser(csBatchId);
		//csBatchId="8c1225b3-8af8-4477-8e36-5f420582eed1";
		System.out.println("starting updateBatchMetadata");
		//updateDocumentMetadata(csBatchId, USER_TOKEN);
	    
		if(copyXMLToShare(csBatchId)){
			System.out.println("starting releaseHoldById()");
			releaseHoldById(csBatchId);
		}
	}
	
	private static void releaseHoldById(String batchId) throws Exception{
		String url = RELEASEHOLD_URL;
		String soapEnvelope = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:a=\"http://www.w3.org/2005/08/addressing\"><s:Header><a:Action s:mustUnderstand=\"1\">http://tempuri.org/ICoreService/ReleaseHoldById</a:Action><a:MessageID>urn:uuid:38de6a31-8bd2-42f6-b174-406009437c1b</a:MessageID><a:ReplyTo><a:Address>http://www.w3.org/2005/08/addressing/anonymous</a:Address></a:ReplyTo><a:To s:mustUnderstand=\"1\">" + SERVER_URL + "</a:To></s:Header><s:Body><ReleaseHoldById xmlns=\"http://tempuri.org/\"><batchId>" + batchId + "</batchId><error>false</error><errorMessage></errorMessage></ReleaseHoldById></s:Body></s:Envelope>";
		
		doPost(soapEnvelope, url);
	}
	
	private static String addDocument(String batchId, String user) throws Exception{
		String url = ADD_DOCUMENT_URL;
		String newDocumentId = UUID.randomUUID().toString();
		
		String soapEnvelope = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:a=\"http://www.w3.org/2005/08/addressing\"><s:Header><a:Action s:mustUnderstand=\"1\">http://tempuri.org/ICoreService/AddDocument</a:Action><a:MessageID>urn:uuid:38de6a31-8bd2-42f6-b174-406009437c1b</a:MessageID><a:ReplyTo><a:Address>http://www.w3.org/2005/08/addressing/anonymous</a:Address></a:ReplyTo><a:To s:mustUnderstand=\"1\">" + SERVER_URL + "</a:To></s:Header><s:Body><AddDocument xmlns=\"http://tempuri.org/\"><batchId>" + batchId + "</batchId><newDocumentId>" + newDocumentId + "</newDocumentId><userToken>" + user + "</userToken></AddDocument></s:Body></s:Envelope>";
				
		doPost(soapEnvelope, url);
		
		return newDocumentId;
	}
	
	private static boolean copyXMLToShare(String batchId) throws Exception{
		try{
			Path SourcePath = Paths.get(FINAL_DROP_FOLDER_PATH, batchInstanceId);
			Path XMLPath = Paths.get(FINAL_DROP_FOLDER_PATH, batchInstanceId, batchInstanceId + "_batch.xml");
			Path XMLTargetPath = Paths.get(SHARED_FOLDER_PATH, batchId, batchId + "_batch.xml");

			File targetBatchDirectory = new File(Paths.get(SHARED_FOLDER_PATH, batchId).toString());
			
			File destFile = new File(XMLTargetPath.toString());
			File sourceFile = new File(XMLPath.toString());
			
			if (!targetBatchDirectory.exists()) {
				targetBatchDirectory.mkdir();
			}
			
			if(!destFile.exists()) {
		        destFile.createNewFile();
		    }

		    java.nio.channels.FileChannel source = null;
		    java.nio.channels.FileChannel destination = null;
		    try {
		        source = new java.io.FileInputStream(sourceFile).getChannel();
		        destination = new FileOutputStream(destFile).getChannel();

		        // previous code: destination.transferFrom(source, 0, source.size());
		        // to avoid infinite loops, should be:
		        long count = 0;
		        long size = source.size();              
		        while((count += destination.transferFrom(source, count, size-count))<size);
		        
		        File sourceDirectory = new File(SourcePath.toString());
		        
		        copyAllFiles(sourceDirectory, targetBatchDirectory);
		    }
		    finally {
		        if(source != null) {
		            source.close();
		        }
		        if(destination != null) {
		            destination.close();
		        }
		    }

			//Files.copy(XMLPath, XMLTargetPath, StandardCopyOption.REPLACE_EXISTING);
			return true;
		}
		catch(IOException e){
			System.out.println(e.toString());
			return false;
		}
	}
	
	private static void copyAllFiles(File sourceLocation , File targetLocation)
		    throws IOException {

		        if (sourceLocation.isDirectory()) {
		            if (!targetLocation.exists()) {
		                targetLocation.mkdir();
		            }
		            File[] files = sourceLocation.listFiles();
		            for(File file:files){
		                InputStream in = new java.io.FileInputStream(file);
		                OutputStream out = new FileOutputStream(targetLocation+"/"+file.getName());

		                // Copy the bits from input stream to output stream
		                byte[] buf = new byte[1024];
		                int len;
		                while ((len = in.read(buf)) > 0) {
		                    out.write(buf, 0, len);
		                }
		                in.close();
		                out.close();
		            }            
		        }
		    }
	
//	private static String getBatchUser(String batchId) throws Exception {
//		String url = GET_BATCH_URL;
//		String soapEnvelope = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:a=\"http://www.w3.org/2005/08/addressing\"><s:Header><a:Action s:mustUnderstand=\"1\">http://tempuri.org/ICoreService/GetBatch</a:Action><a:MessageID>urn:uuid:38de6a31-8bd2-42f6-b174-406009437c1b</a:MessageID><a:ReplyTo><a:Address>http://www.w3.org/2005/08/addressing/anonymous</a:Address></a:ReplyTo><a:To s:mustUnderstand=\"1\">" + SERVER_URL + "</a:To></s:Header><s:Body><GetBatch xmlns=\"http://tempuri.org/\"><batchId>" + batchId + "</batchId><userToken></userToken></GetBatch></s:Body></s:Envelope>";
//				
//		//read the response from the GetBatch call
//		String responseString = doPost(soapEnvelope, url);
//		
//		String tokenUser = readUserResponseData(responseString);
//		
//		return tokenUser;
//		
//	}
	
	private static String doPost(String envelope, String methodUrl) throws Exception, IOException{
		String url = methodUrl;
		String soapEnvelope = envelope;
		CloseableHttpClient client = HttpClientBuilder.create().build();
		HttpPost post = new HttpPost(url);
		String responseString = "";

		// add header
		post.setHeader("Content-type", "application/soap+xml; charset=utf-8");

		HttpEntity entity = null;
		entity = new ByteArrayEntity(soapEnvelope.getBytes("UTF-8"));
		

		post.setEntity(entity);

		HttpResponse response = null;
		response = client.execute(post);
		if(response != null){
			HttpEntity respEntity = response.getEntity();
			responseString = EntityUtils.toString(respEntity, "UTF-8");
		}
		
		System.out.println("Response Code : " + response.getStatusLine().getStatusCode());

		return responseString;
	}
	
	private static List<NameValuePair> readEphesoftMetadata(String ephesoftBatchId, String finalDropFolderPath) throws SAXException, IOException{
		Path path = Paths.get(finalDropFolderPath, ephesoftBatchId);
		File file = new File(path.toString() + "\\" + ephesoftBatchId + BATCH_NAME_FINAL);
		XPath xpath = XPathFactory.newInstance().newXPath();
		List<NameValuePair> metadata = new ArrayList<NameValuePair>();
		NameValuePair data = null;

		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
		        .newInstance();
		DocumentBuilder documentBuilder = null;
		try {
			documentBuilder = documentBuilderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Document document = documentBuilder.parse(file);
		XPathExpression expr = null;
		
		try {
			XPath xPath =  XPathFactory.newInstance().newXPath();
			expr = xpath.compile("/Batch/Documents/Document/DocumentLevelFields/DocumentLevelField");
			NodeList nl = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
			int numNodes = nl.getLength();
			
			for (int i = 0; i < numNodes; i++) {
				Node nNode = nl.item(i);
				Element element = (Element) nNode;
				if (element.getElementsByTagName("Value").item(0) != null){
					System.out.println(element.getElementsByTagName("Name").item(0).getTextContent() + ": " + element.getElementsByTagName("Value").item(0).getTextContent());
					data = new BasicNameValuePair(element.getElementsByTagName("Name").item(0).getTextContent(), element.getElementsByTagName("Value").item(0).getTextContent());
				}
				else{
					System.out.println(element.getElementsByTagName("Name").item(0).getTextContent());
					data = new BasicNameValuePair(element.getElementsByTagName("Name").item(0).getTextContent(), "");
				}
				
			    metadata.add(data);
			}
			
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return metadata;
	}
	
	private static List<NameValuePair> getEphesoftDocumentIds(String ephesoftBatchId, String finalDropFolderPath) throws SAXException, IOException{
		Path path = Paths.get(finalDropFolderPath, ephesoftBatchId);
		File file = new File(path.toString() + "\\" + ephesoftBatchId + BATCH_NAME_FINAL);
		XPath xpath = XPathFactory.newInstance().newXPath();
		List<NameValuePair> metadata = new ArrayList<NameValuePair>();
		NameValuePair data = null;

		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
		        .newInstance();
		DocumentBuilder documentBuilder = null;
		try {
			documentBuilder = documentBuilderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Document document = documentBuilder.parse(file);
		XPathExpression expr = null;
		
		try {
			XPath xPath =  XPathFactory.newInstance().newXPath();
			expr = xpath.compile("/Batch/Documents/Document");
			NodeList nl = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
			int numNodes = nl.getLength();
			
			for (int i = 0; i < numNodes; i++) {
				Node nNode = nl.item(i);
				Element element = (Element) nNode;
				if (element.getElementsByTagName("Identifier").item(0) != null){
					System.out.println(element.getElementsByTagName("Identifier").item(0).getTextContent() + ": " + element.getElementsByTagName("Identifier").item(0).getTextContent());
					data = new BasicNameValuePair(element.getElementsByTagName("Identifier").item(0).getTextContent(), element.getElementsByTagName("Identifier").item(0).getTextContent());
				}
			    metadata.add(data);
			}
			
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return metadata;
	}
	
	private static List<NameValuePair> readDocumentIdsResponseData(String response) throws SAXException, IOException, SOAPException{
		 MessageFactory factory = MessageFactory.newInstance();
		 List<NameValuePair> metadata = new ArrayList<NameValuePair>();
		 NameValuePair data = null;

		 SOAPMessage message = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL)
				    .createMessage(null, new StringBufferInputStream(response));
		 SOAPBody body = message.getSOAPBody();

		 NodeList returnList = body.getElementsByTagName("c:Document");

		 for (int k = 0; k < returnList.getLength(); k++) {
		     NodeList innerResultList = returnList.item(k).getChildNodes();
		     for (int l = 0; l < innerResultList.getLength(); l++) {
		         if (innerResultList.item(l).getNodeName().equalsIgnoreCase("c:_x003C_Id_x003E_k__BackingField")) {
		             String documentId = innerResultList.item(l).getTextContent().trim();
		             data = new BasicNameValuePair("Document " + l, innerResultList.item(l).getTextContent().trim());
		             System.out.println(documentId);
		             metadata.add(data);
		         }
		     }
		 }
		 
		 return metadata;
	}
	
//	private static String readUserResponseData(String response) throws SAXException, IOException, SOAPException{
//		 String lockedUserToken = "";
//
//		 SOAPMessage message = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL).createMessage(null, new StringBufferInputStream(response));
//		 SOAPBody body = message.getSOAPBody();
//
//		 NodeList returnList = body.getElementsByTagName("b:Data");
//
//		 for (int k = 0; k < returnList.getLength(); k++) {
//		     NodeList innerResultList = returnList.item(k).getChildNodes();
//		     for (int l = 0; l < innerResultList.getLength(); l++) {
//		         if (innerResultList.item(l).getNodeName().equalsIgnoreCase("c:_LockedUserToken")) {
//		             lockedUserToken = innerResultList.item(l).getTextContent().trim();
//		         }
//		     }
//		 }
//		 
//		 return lockedUserToken;
//	}
	
	private static List<NameValuePair> readDocumentMetadataResponseData(String response) throws SAXException, IOException, SOAPException{
		 MessageFactory factory = MessageFactory.newInstance();
		 List<NameValuePair> metadata = new ArrayList<NameValuePair>();
		 NameValuePair data = null;
		 NameValuePair data1 = null;

		 SOAPMessage message = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL)
				    .createMessage(null, new StringBufferInputStream(response));
		 SOAPBody body = message.getSOAPBody();

		 NodeList returnList = body.getElementsByTagName("d:KeyValueOfstringstring");

		 for (int k = 0; k < returnList.getLength(); k++) {
			 String key = "";
			 String value = "";
		     NodeList innerResultList = returnList.item(k).getChildNodes();
		     for (int l = 0; l < innerResultList.getLength(); l++) {
		         if (innerResultList.item(l).getNodeName().equalsIgnoreCase("d:Key")) {
		             key = innerResultList.item(l).getTextContent().trim();
		             //System.out.println(key);
		         }
		         if (innerResultList.item(l).getNodeName().equalsIgnoreCase("d:Value")) {
		             value = innerResultList.item(l).getTextContent().trim();
		             //System.out.println(value);
		         }
		         
		         if(key.isEmpty() == false && value.isEmpty() == false){
		        	 data = new BasicNameValuePair(key, value);
		        	 if(data1 == null || !data.toString().equals(data1.toString())){
		        		 System.out.println(key);
		        		 System.out.println(value);
		        		 metadata.add(data);
		        	 }
		        	 data1 = new BasicNameValuePair(key, value);
		         }
		     }
		 }
		 return metadata;
	}
	
	private static String getCSBatchId(String ephesoftBatchId) throws SAXException, IOException{
		String batchName = "";
		OutputStream outputStream = null;
		File batchXml;
		
		batchXml = getCSBatchFile(SYSTEM_BATCH_FOLDER_PATH, ephesoftBatchId);

		outputStream = null;

		int read = 0;
		
		XPath xpath = XPathFactory.newInstance().newXPath();
		List<NameValuePair> metadata = new ArrayList<NameValuePair>();
		NameValuePair data = null;

		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = null;
		try {
			documentBuilder = documentBuilderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Document document = documentBuilder.parse(batchXml);
		XPathExpression expr = null;
		
		try {
			XPath xPath =  XPathFactory.newInstance().newXPath();
			expr = xpath.compile("/Batch");
			NodeList nl = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
						
			Node nNode = nl.item(0);
			Element element = (Element) nNode;
				
			System.out.println(element.getElementsByTagName("BatchName").item(0).getTextContent());
			batchName = element.getElementsByTagName("BatchName").item(0).getTextContent();
				
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return batchName;
	}
	
	private static File getCSBatchFile(String filePath, String fileName) throws IOException {
		String fullFileName = filePath + "\\" + fileName + "\\" + fileName + "_batch.xml.zip";
		String zipFileName = fileName + "_batch.xml";
	    java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(fullFileName);
	    InputStream stream = null;

	    int read = 0;
		byte[] bytes = null;
	    Enumeration<? extends java.util.zip.ZipEntry> entries = zipFile.entries();

	    while(entries.hasMoreElements()){
	        java.util.zip.ZipEntry entry = entries.nextElement();
	        if(entry.getName().toLowerCase().equals(zipFileName.toLowerCase())){
	        	stream = zipFile.getInputStream(entry);
	        	bytes = new byte[stream.available()];
	        }
	    }
	    
	    InputStream input = stream;
	    FileOutputStream output = new FileOutputStream(filePath + "\\" + fileName + "\\" + fileName + "_batch.xml"); 
        byte[] buf = bytes; 
        int bytesRead; 

        while ((bytesRead = input.read(buf)) > 0) {  
            output.write(buf, 0, bytesRead); 
        } 

        zipFile.close();
	    input.close();
	    stream.close();
	    output.close();
	    
	    File batchXml = new File(filePath + "\\" + fileName + "\\" + fileName + "_batch.xml");
	    
	    return batchXml;
	}
}

