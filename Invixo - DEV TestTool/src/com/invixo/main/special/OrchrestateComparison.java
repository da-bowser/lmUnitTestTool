package com.invixo.main.special;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.http.client.methods.HttpPost;

import com.invixo.common.GeneralException;
import com.invixo.common.IcoOverviewDeserializer;
import com.invixo.common.IcoOverviewInstance;
import com.invixo.common.IntegratedConfigurationMain;
import com.invixo.common.StateException;
import com.invixo.common.util.HttpException;
import com.invixo.common.util.HttpHandler;
import com.invixo.common.util.Logger;
import com.invixo.common.util.PropertyAccessor;
import com.invixo.common.util.Util;
import com.invixo.consistency.FileStructure;
import com.invixo.injection.InjectionPayloadException;
import com.invixo.injection.IntegratedConfiguration;
import com.invixo.injection.RequestGeneratorUtil;
import com.invixo.main.special.ComparisonCase.TYPE;

public class OrchrestateComparison {

	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = OrchrestateComparison.class.getName();
	private static final String SERVICE_HOST_PORT 		= GlobalParameters.SAP_PO_HTTP_HOST_AND_PORT;
	private static final String SERVICE_PATH_INJECT 	= PropertyAccessor.getProperty("SERVICE_PATH_INJECT") + GlobalParameters.PARAM_VAL_SENDER_COMPONENT + ":" + GlobalParameters.PARAM_VAL_XI_SENDER_ADAPTER;
	private static final String INJECT_ENDPOINT 				= SERVICE_HOST_PORT + SERVICE_PATH_INJECT;
//	private static final String EXTRACT_ENDPOINT 				= SERVICE_HOST_PORT + SERVICE_PATH_INJECT;

	
	
	public static void start() throws GeneralException {
		final String SIGNATURE = "start()";
		
		logger.writeDebug(LOCATION, SIGNATURE, "Start comparison orchestration...");

		// Get ICO Overview list
		ArrayList<IcoOverviewInstance> icoInstancesList = loadIcoOverview(FileStructure.DIR_CONFIG, FileStructure.FILE_ICO_OVERVIEW);
		
		// Check project folder setup
		FileStructure.startCheck(icoInstancesList);
		
		// Get Comparison Cases
		ArrayList<ComparisonCase> comparisonList = loadComparisonCases(FileStructure.DIR_CONFIG, FileStructure.FILE_COMPARISON_OVERVIEW);
		
		// Process each comparison case
		for (ComparisonCase currentEntry : comparisonList) {
			// Get current ICO ref
			String currentIcoNameRef = currentEntry.getSourceIco();
			
			// Get matching ICO instance
			IcoOverviewInstance icoInstance = getIcoFromOverview(icoInstancesList, currentIcoNameRef);
			
			HashMap<String, String> injectMap = processInjection(currentEntry, icoInstance);
			
			// Extract LAST messages based on inject map (only extract source LAST for ICO_2_FILE compare)
			
			
		}
		
		// Inject file A: source (any b2b specifics we need to worry about?)
		
		// Inject file B: target (any b2b specifics we need to worry about?)
		
		// Extract LAST from system, based on inject ID for file A
		
		// Extract LAST from system, based on inject ID for file B
		
		// Do we need to handle split, multimap?!?!
		
		// Create line in STATE file
		// NB: handle situation where in some cases we do not want to make 2. injection since 
		// sometimes we need to compare with a target file on file system
		
		// Hand over to Compare
	}


	private static HashMap<String, String> processInjection(ComparisonCase currentEntry, IcoOverviewInstance icoInstance) throws GeneralException {
		HashMap<String, String> injectMap = new HashMap<String, String>();
				
		// Get list of source files to be injected
		String sourceDir = FileStructure.DIR_TEST_CASES + currentEntry.getSourcePathIn();
		String targetDir = FileStructure.DIR_TEST_CASES + currentEntry.getTargetPathIn();
		List<Path> sourceInjectPaths = Util.generateListOfPaths(sourceDir, "FILE");
		List<Path> targetInjectPaths = Util.generateListOfPaths(targetDir, "FILE");
					
		// Special handling for EOIO
		String queueId = null;
		if (icoInstance.getQualityOfService().equals("EOIO")) {
			queueId = generateQueueId();
		}
					
		// Inject according to compare type
		boolean isIcoCompare = currentEntry.getCompareType().equals(ComparisonCase.TYPE.ICO_2_ICO);
		if (isIcoCompare) {
			injectMap = handleInject(isIcoCompare, icoInstance, sourceInjectPaths, targetInjectPaths, queueId);
		} else {
			// Load target output files and correlate with source messageId for later compare
			targetDir = FileStructure.DIR_TEST_CASES + currentEntry.getTargetPathOut();
			targetInjectPaths = Util.generateListOfPaths(targetDir, "FILE");
			injectMap = handleInject(isIcoCompare, icoInstance, sourceInjectPaths, targetInjectPaths, queueId);
		}
		
		// Return map
		return injectMap;
	}

	private static HashMap<String, String> handleFileTypeInjection() {
		// TODO Auto-generated method stub
		HashMap<String, String> injectMap = new HashMap<String, String>();
		
		return injectMap;
		
	}


	private static HashMap<String, String> handleInject(boolean isIcoCompare, IcoOverviewInstance icoInstance, List<Path> sourceInjectPaths, List<Path> targetInjectPaths, String queueId) throws GeneralException {
		HashMap<String, String> injectMap = new HashMap<String, String>();
		// Validate that source and target count matches
		if (sourceInjectPaths.size() == targetInjectPaths.size()) {
			// Inject source files
			for (int i = 0; i < sourceInjectPaths.size(); i++) {
				// Inject source file
				Path sourceFile = sourceInjectPaths.get(i);
				String sourceMessageId = UUID.randomUUID().toString();
				injectPayload(icoInstance, queueId, sourceMessageId, sourceFile);
				
				Path targetFile = targetInjectPaths.get(i);
				String targetMessageId = UUID.randomUUID().toString();
				
				// Check if we should inject target "input" files or load "output" files
				if (isIcoCompare) {
					// Inject target file
					injectPayload(icoInstance, queueId, targetMessageId, targetFile);
				} else {
					// Correlate source inject id with target file name instead of messageId
					targetMessageId = targetFile.getFileName().toString();
				}
				
				// Add message id's to map for correlation
				injectMap.put(sourceMessageId, targetMessageId);
			}
			
		} else {
			String msg = "File mismatch for ICO 2 ICO scenario. Sources: " + sourceInjectPaths.size() + " targets: " + targetInjectPaths.size();
			throw new GeneralException(msg);
		}
		
		// Return correlation map
		return injectMap;
		
	}


	private static void injectPayload(IcoOverviewInstance icoInstance, String queueId, String messageId, Path injectPath) throws GeneralException {
		final String SIGNATURE = "injectPayload(IcoOverviewInstance, String, String, Path)";
		
		try {
			String soapXiHeader = RequestGeneratorUtil.generateSoapXiHeaderPart(icoInstance, queueId, messageId);
			
			// Read bytes of file to be injected
			byte[] payload = Files.readAllBytes(injectPath);
			
			// Build Request to be sent via Web Service call
			HttpPost webServiceRequest = HttpHandler.buildMultipartHttpPostRequest(INJECT_ENDPOINT, soapXiHeader.getBytes(GlobalParameters.ENCODING), payload); 
			
			// Store request on file system (only relevant for debugging purposes)
			if (GlobalParameters.DEBUG) {
				String filePath = FileStructure.getDebugFileName("InjectionMultipart", true, icoInstance.getName() + "_" + messageId, "txt");
				webServiceRequest.getEntity().writeTo(new FileOutputStream(new File(filePath)));
				logger.writeDebug(LOCATION, SIGNATURE, "<debug enabled> Request message to be sent to SAP PO is stored here: " + filePath);
			}
			
			// Call SAP PO Web Service (using XI protocol)
			HttpHandler.post(webServiceRequest);
		} catch (InjectionPayloadException|HttpException e) {
			String msg = "Error injecting payload to SAP PO" + e.getMessage();
			logger.writeInfo(LOCATION, SIGNATURE, msg);
			throw new GeneralException(msg);
		} catch (IOException e) {
			String msg = "Error reading file to be injected: " + injectPath.toString();
			logger.writeInfo(LOCATION, SIGNATURE, msg);
			throw new GeneralException(msg);
		}
	}
	
	
	static String generateQueueId() {
		String result	= "_" 
						+ ("" + System.nanoTime()).substring(1);
		return result;
	}
	
	
	private void buildStateLine() {
		final String separator = "_#_";
		String line	= "datetime "
					+ separator
					+ "compare entry counter"
					+ separator
					+ "source file output path "
					+ separator
					+ "source file name "
					+ separator
					+ "source inject id "
					+ separator
					+ "soruce ICO name"
					+ separator
					+ "target file output path "
					+ separator
					+ "target file name "
					+ separator
					+ "target inject id"
					+ separator
					+ "target ICO";

	}
	
	
	
	private static IcoOverviewInstance getIcoFromOverview(ArrayList<IcoOverviewInstance> icoInstancesList, String name) {
		IcoOverviewInstance instance = null;
		for (IcoOverviewInstance currentInstance : icoInstancesList) {
			if (currentInstance.getName().equals(name)) {
				instance = currentInstance;
				break;
			}
		}
		return instance;
	}


	private static ArrayList<IcoOverviewInstance> loadIcoOverview(String basePath, String fileName) {
		final String SIGNATURE = "loadIcoOverview(String, String)";
		try {
			InputStream icoOverviewXmlStream = new FileInputStream(basePath + fileName);
			ArrayList<IcoOverviewInstance> icoInstancesList = IcoOverviewDeserializer.deserialize(icoOverviewXmlStream);
			return icoInstancesList;
		} catch (FileNotFoundException e) {
			String msg = "ICO Overview file not found: " + basePath + fileName + "\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new RuntimeException(msg);
		}
	}
	
	
	private static ArrayList<ComparisonCase> loadComparisonCases(String basePath, String fileName) {
		final String SIGNATURE = "loadComparisonCases(String, String)";
		try {
			InputStream comparisonXmlStream = new FileInputStream(basePath + fileName);
			ArrayList<ComparisonCase> comparisonList = ComparisonXmlDeserializer.deserialize(comparisonXmlStream);
			return comparisonList;
		} catch (FileNotFoundException e) {
			String msg = "Comparison Overview file not found: " + basePath + fileName + "\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new RuntimeException(msg);
		}
	}

}
