package com.ephesoft.customplugin.onbase_export;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.Assert;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ephesoft.dcma.batch.service.BatchSchemaService;
import com.ephesoft.dcma.batch.service.PluginPropertiesService;
import com.ephesoft.dcma.core.DCMAException;
import com.ephesoft.dcma.core.annotation.PostProcess;
import com.ephesoft.dcma.core.annotation.PreProcess;
import com.ephesoft.dcma.core.component.ICommonConstants;
import com.ephesoft.dcma.da.id.BatchInstanceID;
import com.ephesoft.dcma.da.service.BatchClassPluginConfigService;
import com.ephesoft.dcma.util.BackUpFileService;

public class OnBasePluginImpl implements OnBasePlugin, ICommonConstants {
	
	private String sharedFolderPath = "";
	private String finalDropFolderPath = "";
	private String systemBatchFolderPath = "";
	private String dipFolder = "";
	private String batchNameFinal = "_batch.xml";
	private List<NameValuePair> docTypeMetadataValues = new ArrayList<NameValuePair>();
	
	private BatchClassPluginConfigService batchClassPluginConfigService;
	
	@Autowired
	private BatchSchemaService batchSchemaService;
	
	@Autowired
	@Qualifier("batchInstancePluginPropertiesService")
	private PluginPropertiesService props;
	
	@PreProcess
	public void preProcess(final BatchInstanceID batchInstanceID, String pluginWorkflow){
		Assert.notNull(batchInstanceID);
		BackUpFileService.backUpBatch(batchInstanceID.getID());
	}
	
	@PostProcess
	public void postProcess(final BatchInstanceID batchInstanceID, String pluginWorkflow){
		Assert.notNull(batchInstanceID);
	}
	
	public void execute(BatchInstanceID batchInstanceID, String pluginWorkflow) throws DCMAException {

		try{
			System.out.println("Start OnBase Plugin ******");
			setParameters(batchInstanceID);
			//extracts the batch xml file from the zip file Ephesoft creates
			extractBatchXML(systemBatchFolderPath, batchInstanceID.toString());
			List<String> documentTypes = getDocumentTypes(batchInstanceID.toString(), systemBatchFolderPath);
			List<String> documentNames = getDocumentNames(batchInstanceID.toString(), systemBatchFolderPath);
			List<String> imagePaths = getDocumentImagePaths(batchInstanceID.toString(), sharedFolderPath, documentNames);
			List<NameValuePair> ephesoftMetadata = readEphesoftMetadata(batchInstanceID.toString(), systemBatchFolderPath);
				
			//file created and formatted for use with OnBase
			createMetadataFile(ephesoftMetadata, batchInstanceID.toString(), documentTypes, imagePaths);
			if(copyFilesToShare(batchInstanceID.toString())){
				System.out.println("Completed OnBase Plugin");
			}
		}
		catch(Exception e){
			System.out.println("OnBase Plugin: " + e.toString());
		}		
	}
	
	public BatchClassPluginConfigService getBatchClassPluginService(){
		return batchClassPluginConfigService;
	}
	
	public void setBatchClassPluginConfigService(BatchClassPluginConfigService batchClassPluginConfigService){
		this.batchClassPluginConfigService = batchClassPluginConfigService;
	}
	
	private void setParameters(BatchInstanceID batchInstanceID){
		sharedFolderPath = batchClassPluginConfigService.getPluginPropertiesForBatch(batchInstanceID.getID(), "ONBASE_EXPORT_PLUGIN").get("SHARED_FOLDER_PATH");
		finalDropFolderPath = batchClassPluginConfigService.getPluginPropertiesForBatch(batchInstanceID.getID(), "ONBASE_EXPORT_PLUGIN").get("FINAL_DROP_FOLDER_PATH");
		systemBatchFolderPath = batchClassPluginConfigService.getPluginPropertiesForBatch(batchInstanceID.getID(), "ONBASE_EXPORT_PLUGIN").get("SYSTEM_BATCH_FOLDER_PATH");
		dipFolder = batchClassPluginConfigService.getPluginPropertiesForBatch(batchInstanceID.getID(), "ONBASE_EXPORT_PLUGIN").get("DIP_FOLDER");
		//batchNameFinal = batchClassPluginConfigService.getPluginPropertiesForBatch(batchInstanceID.getID(), "ONBASE_EXPORT_PLUGIN").get("BATCH_NAME_FINAL");
	}
	
	private boolean copyFilesToShare(String batchInstanceId) throws Exception{
		try{
			Path SourcePath = Paths.get(systemBatchFolderPath, batchInstanceId);
			Path XMLPath = Paths.get(systemBatchFolderPath, batchInstanceId, batchInstanceId + batchNameFinal);
			Path XMLTargetPath = Paths.get(sharedFolderPath, batchInstanceId, batchInstanceId + batchNameFinal);

			File targetBatchDirectory = new File(Paths.get(sharedFolderPath, batchInstanceId).toString());
			
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
			return true;
		}
		catch(IOException e){
			System.out.println(e.toString());
			return false;
		}
	}
	
	private void copyAllFiles(File sourceLocation , File targetLocation)
		    throws IOException {

		        if (sourceLocation.isDirectory()) {
		            if (!targetLocation.exists()) {
		                targetLocation.mkdir();
		            }
		            File[] files = sourceLocation.listFiles();
		            for(File file:files){
		            	
		            	if(file.getName().endsWith(".tif") || file.getName().endsWith(".xml") || file.getName().endsWith(".txt") || file.getName().endsWith(".pdf") || file.getName().endsWith(".tiff"))
		            	{
		                InputStream in = new java.io.FileInputStream(file);
		                OutputStream out = new FileOutputStream(targetLocation+"/"+file.getName());

		                // Copy the bits from input stream to output stream
		                byte[] buf = new byte[100000];
		                int len;
		                while ((len = in.read(buf)) > 0) {
		                    out.write(buf, 0, len);
		                }
		                in.close();
		                out.close();
		            	}
		            }            
		        }
		    }
	
	private void extractBatchXML(String filePath, String batchInstanceId) throws IOException {
		String fullFileName = filePath + "\\" + batchInstanceId + "\\" + batchInstanceId + "_batch.xml.zip";
		String xmlFile = filePath + "\\" + batchInstanceId + "\\" + batchInstanceId + "_batch.xml";
		File batchXMLFile = new File(xmlFile);
		
		if(batchXMLFile.exists()){
			return;
		}
		
		String zipFileName = batchInstanceId + batchNameFinal;
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
	    File batchXml = new File(filePath + "\\" + batchInstanceId + "\\" + batchInstanceId + batchNameFinal);
	    FileOutputStream output = new FileOutputStream(filePath + "\\" + batchInstanceId + "\\" + batchInstanceId + batchNameFinal); 
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
	
	private java.util.List<NameValuePair> readEphesoftMetadata(String ephesoftBatchId, String finalDropFolderPath) throws SAXException, IOException{
		Path path = Paths.get(finalDropFolderPath, ephesoftBatchId);
		File file = new File(path.toString() + "\\" + ephesoftBatchId + batchNameFinal);
		javax.xml.xpath.XPath xpath = javax.xml.xpath.XPathFactory.newInstance().newXPath();
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
					//System.out.println(element.getElementsByTagName("Name").item(0).getTextContent() + ": " + element.getElementsByTagName("Value").item(0).getTextContent());
					data = new BasicNameValuePair(element.getElementsByTagName("Name").item(0).getTextContent(), element.getElementsByTagName("Value").item(0).getTextContent());
				}
				else{
					//System.out.println(element.getElementsByTagName("Name").item(0).getTextContent());
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
	
	private List<String> getDocumentTypes(String ephesoftBatchId, String finalDropFolderPath) throws SAXException, IOException{
		Path path = Paths.get(finalDropFolderPath, ephesoftBatchId);
		System.out.println("path: " + path);
		File file = new File(path.toString() + "\\" + ephesoftBatchId + batchNameFinal);
		System.out.println("file: " + file.getName());
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
		//return documentTypesList;
		return ChangeDocumetTypesForDemo(documentTypesList);
	}
	
	//this is because I'm too lazy to change Ephesoft doc types
	private List<String> ChangeDocumetTypesForDemo(List<String> myDocumentTypes)
	{
		List<String> fixedDocTypeNames = new ArrayList<String>();
		
		for(int i = 0; i < myDocumentTypes.size(); i++){
			if(myDocumentTypes.get(i).toString().equals("WORKORDER")){
				fixedDocTypeNames.add("SEH- Work Order");
			}
			if(myDocumentTypes.get(i).toString().equals("COCVerification")){
				fixedDocTypeNames.add("SEH- COC Verification");
			}
			if(myDocumentTypes.get(i).toString().equals("EPI")){
				fixedDocTypeNames.add("SEH- EPI Work Order");
			}
			if(myDocumentTypes.get(i).toString().equals("FG-KIT")){
				fixedDocTypeNames.add("SEH- FGKit");
			}
		}
		
		if(fixedDocTypeNames.size() == 0){
			System.out.println("document type not fixed");
		}
		return fixedDocTypeNames;
	}
	
	private List<String> getDocumentNames(String ephesoftBatchId, String finalDropFolderPath) throws SAXException, IOException{
		Path path = Paths.get(finalDropFolderPath, ephesoftBatchId);
		File file = new File(path.toString() + "\\" + ephesoftBatchId + batchNameFinal);
		javax.xml.xpath.XPath xpath = javax.xml.xpath.XPathFactory.newInstance().newXPath();
		String documentType = "";
		List<String> documentTypesList = new ArrayList<String>();

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
					documentTypesList.add(documentType);
				}
			}
			
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return documentTypesList;
	}
	
	private List<String> getDocumentImagePaths(String ephesoftBatchId, String exportPath, List<String> documentNames) throws SAXException, IOException{
		List<String> imagePaths = new ArrayList<String>();
		for (int i = 0; i < documentNames.size(); i++) {
			Path path = Paths.get(exportPath, ephesoftBatchId, ephesoftBatchId + "_document" + documentNames.get(i));
			imagePaths.add(path.toString() + ".pdf");
		}
		
		return imagePaths;
	}
	
	private void createMetadataFile(List<NameValuePair> ephesoftMetadata, String batchInstanceId, List<String> documentTypes, List<String> imagePaths) throws IOException
	{
		String currentDocType = "";
		boolean resetNewDocType = false;
		int j = 0;
		String dipFileName= dipFolder + "\\" + batchInstanceId +  ".txt";
		FileWriter fw = new FileWriter(dipFileName,true); //the true will append the new data
		for(int i = 0; i < ephesoftMetadata.size(); i++){
				
			NameValuePair docPair = ephesoftMetadata.get(i);
			String docKey = docPair.getName();
			String docValue = org.apache.commons.lang3.StringEscapeUtils.escapeXml(docPair.getValue());
			if(docTypeMetadataValues.get(i).getName() != currentDocType){
				resetNewDocType = true;
				if(i > 0){
					fw.write("\r\n");
					j++;
				}
			}
			else{
				resetNewDocType = false;
				fw.write(",");
			}
				
			try {
				File targetDIPFileDirectory = new File(Paths.get(sharedFolderPath, dipFolder).toString());
				if (!targetDIPFileDirectory.exists()) {
					targetDIPFileDirectory.mkdir();
				}
				
				File targetBatchDirectory = new File(Paths.get(sharedFolderPath, batchInstanceId).toString());
				
				if (!targetBatchDirectory.exists()) {
					targetBatchDirectory.mkdir();
				}
				
				if(resetNewDocType)
				{
					fw.write(documentTypes.get(j) + "," + imagePaths.get(j)+ ",");
				}
			    
				fw.write(docValue);
					
				currentDocType = docTypeMetadataValues.get(i).getName();		    
			}
			catch(IOException ioe)
			{
				System.err.println("IOException: " + ioe.getMessage());
			}
		}
		fw.close();		
	}
}
