package com.netlocity.customplugin.cspro;

import java.io.File;
import java.io.FileOutputStream;
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

import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.springframework.util.Assert;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ephesoft.dcma.core.DCMAException;
import com.ephesoft.dcma.core.annotation.PostProcess;
import com.ephesoft.dcma.core.annotation.PreProcess;
import com.ephesoft.dcma.core.component.ICommonConstants;
import com.ephesoft.dcma.da.id.BatchInstanceID;
import com.ephesoft.dcma.da.service.BatchClassPluginConfigService;
import com.ephesoft.dcma.util.BackUpFileService;

public class CSProPluginImpl implements csproplugin, ICommonConstants {
	
	private BatchClassPluginConfigService batchClassPluginConfigService;
	private static String batchInstanceId = null;
	private String finalDropFolderPath = "";
	private String systemBatchFolderPath = "";
	private String sharedFolderPath = "";
	private static String ephesoftRestServiceUrl = "";
	private static String releaseHoldUrl = "";
	
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
		//TODO Auto-generated method stub
		System.out.println("*************  Start execution of CSPro Plugin.");
		
		setParameters(batchInstanceID);
		this.batchInstanceId = batchInstanceID.toString();
		String csBatchId = null;
		try {
			csBatchId = getCSBatchId(batchInstanceID.toString());
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			if(copyXMLToShare(csBatchId)){
			    System.out.println("starting releaseHoldById()");
			    releaseHoldById(csBatchId);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("*************  End execution of the ScriptExport scripts.");
	} 
	
	private void setParameters(BatchInstanceID batchInstanceID){
		sharedFolderPath = batchClassPluginConfigService.getPluginPropertiesForBatch(batchInstanceID.getID(), "CSPRO_PLUGIN").get("SHARED_FOLDER_PATH");
		finalDropFolderPath = batchClassPluginConfigService.getPluginPropertiesForBatch(batchInstanceID.getID(), "CSPRO_PLUGIN").get("FINAL_DROP_FOLDER_PATH");
		systemBatchFolderPath = batchClassPluginConfigService.getPluginPropertiesForBatch(batchInstanceID.getID(), "CSPRO_PLUGIN").get("SYSTEM_BATCH_FOLDER_PATH");
		ephesoftRestServiceUrl = batchClassPluginConfigService.getPluginPropertiesForBatch(batchInstanceID.getID(), "CSPRO_PLUGIN").get("EPHESOFT_REST_SERVICE_URI");
		releaseHoldUrl = batchClassPluginConfigService.getPluginPropertiesForBatch(batchInstanceID.getID(), "CSPRO_PLUGIN").get("RELEASE_HOLD_URI");
	}
		
	public BatchClassPluginConfigService getBatchClassPluginService(){
		return batchClassPluginConfigService;
	}
	
	public void setBatchClassPluginConfigService(BatchClassPluginConfigService batchClassPluginConfigService){
		this.batchClassPluginConfigService = batchClassPluginConfigService;
	}
	
	private String getCSBatchId(String ephesoftBatchId) throws SAXException, IOException{
		String batchName = "";
		OutputStream outputStream = null;
		File batchXml;
		
		batchXml = getCSBatchFile(systemBatchFolderPath, ephesoftBatchId);

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
		org.w3c.dom.Document document = documentBuilder.parse(batchXml);
		XPathExpression expr = null;
		
		try {
			XPath xPath =  XPathFactory.newInstance().newXPath();
			expr = xpath.compile("/Batch");
			NodeList nl = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
						
			Node nNode = nl.item(0);
			org.w3c.dom.Element element = (org.w3c.dom.Element) nNode;
				
			System.out.println(element.getElementsByTagName("BatchName").item(0).getTextContent());
			batchName = element.getElementsByTagName("BatchName").item(0).getTextContent();
				
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return batchName;
	}
	
	private boolean copyXMLToShare(String batchId) throws Exception{
		try{
			Path SourcePath = Paths.get(finalDropFolderPath, batchInstanceId);
			Path XMLPath = Paths.get(finalDropFolderPath, batchInstanceId, batchInstanceId + "_batch.xml");
			Path XMLTargetPath = Paths.get(sharedFolderPath, batchId, batchId + "_batch.xml");

			File targetBatchDirectory = new File(Paths.get(sharedFolderPath, batchId).toString());
			
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
	
	private void copyAllFiles(File sourceLocation , File targetLocation)throws IOException {

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
	
	private static void releaseHoldById(String batchId) throws Exception{
		String url = releaseHoldUrl;
		String soapEnvelope = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:a=\"http://www.w3.org/2005/08/addressing\"><s:Header><a:Action s:mustUnderstand=\"1\">http://tempuri.org/ICoreService/ReleaseHoldById</a:Action><a:MessageID>urn:uuid:38de6a31-8bd2-42f6-b174-406009437c1b</a:MessageID><a:ReplyTo><a:Address>http://www.w3.org/2005/08/addressing/anonymous</a:Address></a:ReplyTo><a:To s:mustUnderstand=\"1\">" + ephesoftRestServiceUrl + "</a:To></s:Header><s:Body><ReleaseHoldById xmlns=\"http://tempuri.org/\"><batchId>" + batchId + "</batchId><error>false</error><errorMessage></errorMessage></ReleaseHoldById></s:Body></s:Envelope>";
		
		doPost(soapEnvelope, url);
	}
	
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
	
	private File getCSBatchFile(String filePath, String fileName) throws IOException {
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
