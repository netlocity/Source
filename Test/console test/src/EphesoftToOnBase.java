import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
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
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;



public class EphesoftToOnBase {

	private static final String SHARED_FOLDER_PATH = "\\\\192.168.2.131\\internal netlocity\\mark.carter\\Cache";
	private static final String FINAL_DROP_FOLDER_PATH = "c:\\Ephesoft\\SharedFolders\\final-drop-folder";
	private static final String SYSTEM_BATCH_FOLDER_PATH = "c:\\Ephesoft\\SharedFolders\\ephesoft-system-folder";
	private static final String DIP_FOLDER = "OnBaseDIP";
	private static final String BATCH_NAME_FINAL = "_batch.xml";
	
	private static String batchInstanceId = null;
	private static List<NameValuePair> docTypeMetadataValues = new ArrayList<NameValuePair>();
	
	public static void main (String[] args) throws Exception {
		//getBatchInstanceId(document);
		batchInstanceId = "BI3DF";
		
		//extracts the batch xml file from the zip file Ephesoft creates
		extractBatchXML(SYSTEM_BATCH_FOLDER_PATH, batchInstanceId);
		
		List<String> documentTypes = getDocumentTypes(batchInstanceId, SYSTEM_BATCH_FOLDER_PATH);
		List<String> documentNames = getDocumentNames(batchInstanceId, SYSTEM_BATCH_FOLDER_PATH);
		List<String> imagePaths = getDocumentImagePaths(batchInstanceId, SHARED_FOLDER_PATH, documentNames);
		
		List<NameValuePair> ephesoftMetadata = readEphesoftMetadata(batchInstanceId, SYSTEM_BATCH_FOLDER_PATH);
		
		//file created and formatted for use with OnBase
		createMetadataFile(ephesoftMetadata, SHARED_FOLDER_PATH, batchInstanceId, documentTypes, imagePaths);
		
		if(copyFilesToShare(batchInstanceId)){
			System.out.println("Completed ScriptExport");
		}
	}
	
	//private static String getBatchInstanceId(final Document document) {
	//	batchInstanceId = null;
	//	final List<?> batchInstanceIDList = document.getRootElement().getChildren(BATCH_INSTANCE_ID);
	//	if (null != batchInstanceIDList) {
	//		batchInstanceId = ((Element) batchInstanceIDList.get(0)).getText();
	//	}
	//	return batchInstanceId;
	//}
	
	private static boolean copyFilesToShare(String batchId) throws Exception{
		try{
			Path SourcePath = Paths.get(SYSTEM_BATCH_FOLDER_PATH, batchInstanceId);
			Path XMLPath = Paths.get(SYSTEM_BATCH_FOLDER_PATH, batchInstanceId, batchInstanceId + BATCH_NAME_FINAL);
			Path XMLTargetPath = Paths.get(SHARED_FOLDER_PATH, batchId, batchId + BATCH_NAME_FINAL);

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
		//return documentTypesList;
		return ChangeDocumetTypesForDemo(documentTypesList);
	}
	
	//this is because I'm too lazy to change Ephesoft doc types
	private static List<String> ChangeDocumetTypesForDemo(List<String> myDocumentTypes)
	{
		List<String> fixedDocTypeNames = new ArrayList<String>();
		
		for(int i = 0; i < myDocumentTypes.size(); i++){
			switch(myDocumentTypes.get(i)) {
		    	case "WORKORDER":
		    		fixedDocTypeNames.add("SEH- Work Order");
		    		break;
		    	case "COCVerification":
		    		fixedDocTypeNames.add("SEH- COC Verification");
		    		break;
		    	case "EPI":
		    		fixedDocTypeNames.add("SEH- EPI Work Order");
		    		break;
		    	case "FG-KIT":
		    		fixedDocTypeNames.add("SEH- FGKit");
		    		break;

			}
		}
		return fixedDocTypeNames;
	}
	
	private static List<String> getDocumentNames(String ephesoftBatchId, String finalDropFolderPath) throws SAXException, IOException{
		Path path = Paths.get(finalDropFolderPath, ephesoftBatchId);
		File file = new File(path.toString() + "\\" + ephesoftBatchId + BATCH_NAME_FINAL);
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
	
	private static List<String> getDocumentImagePaths(String ephesoftBatchId, String exportPath, List<String> documentNames) throws SAXException, IOException{
		List<String> imagePaths = new ArrayList<String>();
		for (int i = 0; i < documentNames.size(); i++) {
			Path path = Paths.get(exportPath, ephesoftBatchId, ephesoftBatchId + "_document" + documentNames.get(i));
			imagePaths.add(path.toString() + ".pdf");
		}
		
		return imagePaths;
	}
	
	private static void createMetadataFile(List<NameValuePair> ephesoftMetadata, String folderPath, String batchInstanceId, List<String> documentTypes, List<String> imagePaths) throws IOException
	{
		String currentDocType = "";
		boolean resetNewDocType = false;
		int j = 0;
		String dipFileName= folderPath + "\\" + DIP_FOLDER + "\\" + batchInstanceId +  ".txt";
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
				//String dipFileName= folderPath + "\\" + DIP_FOLDER + "\\" + batchInstanceId +  ".txt";
				//String filename= folderPath + "\\" + batchInstanceId + "\\" + batchInstanceId +  ".txt";
				File targetDIPFileDirectory = new File(Paths.get(SHARED_FOLDER_PATH, DIP_FOLDER).toString());
				if (!targetDIPFileDirectory.exists()) {
					targetDIPFileDirectory.mkdir();
				}
				
				File targetBatchDirectory = new File(Paths.get(SHARED_FOLDER_PATH, batchInstanceId).toString());
				
				if (!targetBatchDirectory.exists()) {
					targetBatchDirectory.mkdir();
				}
				
				//FileWriter fw = new FileWriter(dipFileName,true); //the true will append the new data
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
