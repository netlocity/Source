//package org.nuxeo.client.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import org.nuxeo.client.api.NuxeoClient;
import org.nuxeo.client.api.objects.upload.BatchUpload;
import org.nuxeo.client.api.objects.user.CurrentUser;
import org.nuxeo.ecm.automation.client.*;
import org.nuxeo.ecm.automation.client.adapters.DocumentService;
import org.nuxeo.ecm.automation.client.adapters.VersionIncrement;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.model.Document;
import org.nuxeo.ecm.automation.client.model.FileBlob;
import org.nuxeo.ecm.automation.client.model.PropertyMap;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.platform.localconfiguration.simple.SimpleConfiguration;
import org.nuxeo.ecm.platform.localconfiguration.simple.SimpleConfigurationAdapter;



public class NuxeoClientTest {

	private static final String SYSTEM_BATCH_FOLDER_PATH = "c:\\Ephesoft\\SharedFolders\\ephesoft-system-folder";
	private static final String BATCH_NAME_FINAL = "_batch.xml";
	private static final String NUXEO_URL = "http://11.11.11.63:8080/nuxeo/site/automation";
	private static String batchInstanceId = null;
	private static List<NameValuePair> docTypeMetadataValues = new ArrayList<NameValuePair>();
	
	public static void main (String[] args) throws Exception {
		//getBatchInstanceId(document);
		test();
		
		batchInstanceId = "BI37C";
		
		//extracts the batch xml file from the zip file Ephesoft creates
		extractBatchXML(SYSTEM_BATCH_FOLDER_PATH, batchInstanceId);
		
		List<String> documentTypes = getDocumentTypes(batchInstanceId, SYSTEM_BATCH_FOLDER_PATH);
		List<String> documentNames = getDocumentNames(batchInstanceId, SYSTEM_BATCH_FOLDER_PATH);
		PropertyMap props = new PropertyMap();
		
		List<NameValuePair> ephesoftMetadata = readEphesoftMetadata(batchInstanceId, SYSTEM_BATCH_FOLDER_PATH);
		
		File file = new File("c:\\export\\cocVerification.tif");
		FileBlob fb = new FileBlob(file);
		
		HttpAutomationClient client = new HttpAutomationClient(NUXEO_URL);
		Session session = client.getSession("Administrator", "Welcome1");
		Document doc = null;
		// get the root
		try{
			doc = (Document) session.newRequest(DocumentService.FetchDocument).setHeader(Constants.HEADER_NX_SCHEMAS, "*").set("value", "Mark" + "/" + "test").execute();
		}
		catch(Exception e){
			session.newRequest(DocumentService.CreateDocument).setInput(doc).set("type", "File").set("name", "cocVerification.tif").set("properties", props).execute();
			session.newRequest(DocumentService.SetBlob).setHeader(Constants.HEADER_NX_VOIDOP, "true").setInput(fb).set("document", "Mark" + "/" + "Test").execute();
		}
	}
	
	private static void test() throws IOException{
		FileInputStream in = null;
        try {
            HttpAutomationClient client = new HttpAutomationClient(NuxeoCfg.URL_HTTP_AUTOMATION_CLIENT);
            Session session = client.getSession(NuxeoCfg.NUXEO_USERNAME, NuxeoCfg.NUXEO_PASSWORD);

            String fileName = "epi.pdf";
            String fileDescription = "This is a Nuxeo Automation Client test.";

            String filePath = "c:\\export\\" + fileName;
            FileInputStream fis = new FileInputStream(filePath);
            byte[] bytes = new byte[fis.available()];
            fis.read(bytes);
            fis.close();

            // Get the nuxeo-SIB workspace
            Document root = (Document) session.newRequest("Document.Fetch").set("value", "/").execute();
            String customFolderName = "Test";
            String workingPath = "/" + customFolderName;
            Document customFolder;
            try {
                customFolder = (Document) session.newRequest("Document.Fetch").set("value", workingPath).execute();
                // customerFolder exists
            } 
            catch (Exception e) {
                // customerFolder doesn't exist -> Create a Folder on nuxeo
                session.newRequest("Document.Create").setInput(root).set("type", "Folder").set("name", customFolderName).set("properties", "dc:title=" + customFolderName).execute();
                customFolder = (Document) session.newRequest("Document.Fetch").set("value", workingPath).execute();
            }

            // Create temp file
            File file = new File(fileName);
            // Write file content
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bytes);
            fos.close();

            FileBlob fb = new FileBlob(file);
            fb.setMimeType(FileBlob.getMimeTypeFromExtension(fileName));

            Document doc;
            // Check if doc exists, create it if it doesn't (-> catch Exception)
            try {

                doc = (Document) session.newRequest("Document.Fetch").setHeader(Constants.HEADER_NX_SCHEMAS, "*").set("value", workingPath + "/" + fileName).execute();

                //System.out.println("WARNING: file already exists, file ID: " + doc.getProperties().getMap("file:content").getString("data") + " -> Update it...");
                //DocumentModel i = (DocumentModel) doc;
                //SimpleConfigurationAdapter sCfg = new SimpleConfigurationAdapter(i);
                //sCfg.put("mydocType", "test");
                
                PropertyMap props = new PropertyMap();
                props.set("dc:title", fileName);
                props.set("dc:description", fileDescription);
                //props.set("dc:myprop", "Coop");
                session.newRequest("Blob.Attach").setHeader(Constants.HEADER_NX_VOIDOP, "true").setInput(fb).set("document", doc.getId()).execute();
                session.newRequest("Document.CreateVersion").setInput(doc).set("increment", VersionIncrement.MAJOR).execute();
                //session.newRequest("Document.Create").setInput(customFolder).set("type", "File").set("name", fileName).set("properties", props).execute();
            } 
            catch (Exception e) {
            	System.out.println(e.getMessage());

                // doc doesn't exist -> create it

                PropertyMap props = new PropertyMap();
                props.set("dc:title", fileName);
                props.set("dc:description", fileDescription);
                props.set("dc:myprop", "Coop");

                // Create a File on nuxeo
                doc = (Document) session.newRequest("Document.Create").setInput(customFolder).set("type", "File").set("name", fileName).set("properties", props).execute();
                // Uploading a file will return null since we used HEADER_NX_VOIDOP
                session.newRequest("Blob.Attach").setHeader(Constants.HEADER_NX_VOIDOP, "true").setInput(fb).set("document", workingPath + "/" + fileName).execute();
                // Create new version
                session.newRequest("Document.CreateVersion").setInput(doc).set("increment", VersionIncrement.MAJOR).execute();
            }


            doc = (Document) session.newRequest("Document.Fetch").setHeader(Constants.HEADER_NX_SCHEMAS, "*").set("value", workingPath + "/" + fileName).execute();                

            // get the file content property
            PropertyMap map = doc.getProperties().getMap("file:content");
            // get the data URL
            String fileID = map.getString("data");
            System.out.println("Upload complete, file ID: " + fileID);

            client.shutdown();

            System.out.println("TestWriteFile END");
        } 
        catch (Exception e) {
        	System.out.println("TestWriteFile ERROR: " + e.toString());
        } 
        finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {}
            }
        }
    }


	private static void extractBatchXML(String filePath, String batchInstanceId) throws IOException {
		String fullFileName = filePath + "\\" + batchInstanceId + "\\" + batchInstanceId + "_batch.xml.zip";
		String xmlFile = filePath + "\\" + batchInstanceId + "\\" + batchInstanceId + "_batch.xml";
		File batchXMLFile = new File(xmlFile);
		
		if(batchXMLFile.exists()){
			return;
		}
		
		String zipFileName = batchInstanceId + BATCH_NAME_FINAL;
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
	    File batchXml = new File(filePath + "\\" + batchInstanceId + "\\" + batchInstanceId + BATCH_NAME_FINAL);
	    FileOutputStream output = new FileOutputStream(filePath + "\\" + batchInstanceId + "\\" + batchInstanceId + BATCH_NAME_FINAL); 
        byte[] buf = bytes; 
        int bytesRead; 

        while ((bytesRead = input.read(buf)) > 0) {  
            output.write(buf, 0, bytesRead); 
        } 

        zipFile.close();
	    input.close();
	    stream.close();
	    output.close();
	}
	
	private static List<String> getDocumentTypes(String ephesoftBatchId, String finalDropFolderPath) throws SAXException, IOException{
		Path path = Paths.get(finalDropFolderPath, ephesoftBatchId);
		File file = new File(path.toString() + "\\" + ephesoftBatchId + BATCH_NAME_FINAL);
		javax.xml.xpath.XPath xpath = javax.xml.xpath.XPathFactory.newInstance().newXPath();
		String documentType = "";
		String metadataName = "";
		int index = 0;
		
		List<String> documentTypesList = new ArrayList<String>();

		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = null;
		try {
			documentBuilder = documentBuilderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		org.w3c.dom.Document document = documentBuilder.parse(file);
		XPathExpression expr = null;
		
		try {
			XPath xPath =  XPathFactory.newInstance().newXPath();
			expr = xpath.compile("/Batch/Documents/Document");
			NodeList nl = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
			int numNodes = nl.getLength();
			
			for (int i = 0; i < numNodes; i++) {
				Node nNode = nl.item(i);
				org.w3c.dom.Element element = (org.w3c.dom.Element) nNode;
				if (element.getElementsByTagName("Type").item(0) != null){
					documentType = element.getElementsByTagName("Type").item(0).getTextContent();
					documentTypesList.add(documentType);
				}
				
				
				Node docMetadata = null;
				int count = element.getElementsByTagName("DocumentLevelField").getLength();
				for (int z = 0; z < count; z++)
				{
					docMetadata = element.getElementsByTagName("DocumentLevelField").item(z);
				
					org.w3c.dom.Element docElement = (org.w3c.dom.Element) docMetadata;
					metadataName = element.getElementsByTagName("Name").item(z).getTextContent();
					
					docTypeMetadataValues.add(index, new BasicNameValuePair(documentType, metadataName));
					index++;
				}
			}
			
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//totally change this back to
		return documentTypesList;
	}
	
	private static List<String> getDocumentNames(String ephesoftBatchId, String finalDropFolderPath) throws SAXException, IOException{
		Path path = Paths.get(finalDropFolderPath, ephesoftBatchId);
		File file = new File(path.toString() + "\\" + ephesoftBatchId + BATCH_NAME_FINAL);
		javax.xml.xpath.XPath xpath = javax.xml.xpath.XPathFactory.newInstance().newXPath();
		String documentType = "";
		List<String> documentNamesList = new ArrayList<String>();

		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
		        .newInstance();
		DocumentBuilder documentBuilder = null;
		try {
			documentBuilder = documentBuilderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		org.w3c.dom.Document document = documentBuilder.parse(file);
		XPathExpression expr = null;
		
		try {
			XPath xPath =  XPathFactory.newInstance().newXPath();
			expr = xpath.compile("/Batch/Documents/Document");
			NodeList nl = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
			int numNodes = nl.getLength();
			
			for (int i = 0; i < numNodes; i++) {
				Node nNode = nl.item(i);
				org.w3c.dom.Element element = (org.w3c.dom.Element) nNode;
				if (element.getElementsByTagName("Identifier").item(0) != null){
					documentType = element.getElementsByTagName("Identifier").item(0).getTextContent();
					documentNamesList.add(documentType);
				}
			}
			
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return documentNamesList;
	}
	
	private static java.util.List<NameValuePair> readEphesoftMetadata(String ephesoftBatchId, String finalDropFolderPath) throws SAXException, IOException{
		Path path = Paths.get(finalDropFolderPath, ephesoftBatchId);
		File file = new File(path.toString() + "\\" + ephesoftBatchId + BATCH_NAME_FINAL);
		javax.xml.xpath.XPath xpath = javax.xml.xpath.XPathFactory.newInstance().newXPath();
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
		org.w3c.dom.Document document = documentBuilder.parse(file);
		XPathExpression expr = null;
		
		try {
			XPath xPath =  XPathFactory.newInstance().newXPath();
			expr = xpath.compile("/Batch/Documents/Document/DocumentLevelFields/DocumentLevelField");
			NodeList nl = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
			int numNodes = nl.getLength();
			
			for (int i = 0; i < numNodes; i++) {
				Node nNode = nl.item(i);
				org.w3c.dom.Element element = (org.w3c.dom.Element) nNode;
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
}
