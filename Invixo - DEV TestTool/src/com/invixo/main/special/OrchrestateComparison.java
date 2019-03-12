package com.invixo.main.special;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.mail.MessagingException;

import org.apache.http.client.methods.HttpPost;

import com.invixo.common.GeneralException;
import com.invixo.common.IcoOverviewDeserializer;
import com.invixo.common.IcoOverviewInstance;
import com.invixo.common.MessageState;
import com.invixo.common.XiMessage;
import com.invixo.common.XiMessageException;
import com.invixo.common.util.HttpException;
import com.invixo.common.util.HttpHandler;
import com.invixo.common.util.Logger;
import com.invixo.common.util.PropertyAccessor;
import com.invixo.common.util.Util;
import com.invixo.compare.Comparer;
import com.invixo.compare.reporting.ReportWriter;
import com.invixo.consistency.FileStructure;
import com.invixo.extraction.ExtractorException;
import com.invixo.extraction.WebServiceUtil;
import com.invixo.injection.InjectionPayloadException;
import com.invixo.injection.RequestGeneratorUtil;

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
		ArrayList<MessageState> resultList = new ArrayList<MessageState>();
		for (ComparisonCase currentEntry : comparisonList) {
			// Get current ICO ref
			String currentIcoNameRef = currentEntry.getSourceIco();
			
			// Get matching ICO instance
			IcoOverviewInstance icoInstance = getIcoFromOverview(icoInstancesList, currentIcoNameRef);
			
			ArrayList<MessageState> stateMap = processInjection(currentEntry, icoInstance);
			
			// Extract LAST messages based on inject map (only extract source LAST for ICO_2_FILE compare)
			extractLastMessages(stateMap);
			
			// Compare
			resultList.addAll(compareLastMessages(stateMap));
		}
		
		// Create compare report
		ReportWriter wr = new ReportWriter(resultList);
		wr.create();

	}


	private static ArrayList<MessageState> compareLastMessages(ArrayList<MessageState> stateMap) {
		ArrayList<MessageState> messageStateListWithCompareResult = new ArrayList<MessageState>();
		
		for(MessageState mst : stateMap) {
			Path sourcePath = Paths.get(FileStructure.DIR_TEST_CASES + mst.getSourceFileOutputPath() + mst.getSourceFileName());
			Path targetPath = Paths.get(FileStructure.DIR_TEST_CASES + mst.getTargetFileOutputPath() + mst.getTargetFileName());
			
			// Create new comparer
			Comparer comp = new Comparer(sourcePath, targetPath, mst.getXpathExceptions());
			
			// Do compare
			comp.start();
			
			// Set compare result om message state object
			mst.setComp(comp);
			
			// Add processed object to result list
			messageStateListWithCompareResult.add(mst);
		}
		
		// Return statemap with added comparers
		return messageStateListWithCompareResult;
	}


	private static void extractLastMessages(ArrayList<MessageState> stateMap) {

		try {
			XiMessage sourceXiMsg = new XiMessage();
			XiMessage targetXiMsg = new XiMessage();
			for(MessageState mst : stateMap) {
				// Get message key for source inject id
				String sourceMessageKey = WebServiceUtil.lookupMessageKey(mst.getSourceInjectId(), mst.getSourceIcoName());
				sourceXiMsg.setSapMessageKey(sourceMessageKey);
				// Get LAST message for source injection id
				String multipartBase64Bytes = WebServiceUtil.lookupSapXiMessage(sourceMessageKey, -1);
				
				// Set multipart of source message
				sourceXiMsg.setMultipartBase64Bytes(multipartBase64Bytes);
				
				// Persist message on file system
				Util.writeFileToFileSystem(FileStructure.DIR_TEST_CASES + mst.getSourceFileOutputPath() + mst.getSourceFileName(), sourceXiMsg.getXiPayload().getInputStream().readAllBytes());
								
				
				if (mst.getTargetInjectId() == null) {
					// ICO 2 FILE scenario, nothing to extract output files on target side is already ready for compare
				} else {
					String targetMessageKey = WebServiceUtil.lookupMessageKey(mst.getSourceInjectId(), mst.getSourceIcoName());
					targetXiMsg.setSapMessageKey(targetMessageKey);
					// Get LAST message for target inject id
					multipartBase64Bytes = WebServiceUtil.lookupSapXiMessage(targetMessageKey, -1); 
					
					// Set multipart of target message
					targetXiMsg.setMultipartBase64Bytes(multipartBase64Bytes);
					
					// Persist message on file system
					Util.writeFileToFileSystem(FileStructure.DIR_TEST_CASES + mst.getTargetFileOutputPath() + mst.getTargetFileName(), sourceXiMsg.getXiPayload().getInputStream().readAllBytes());
				}
			}
		}
		catch (ExtractorException | HttpException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XiMessageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private static ArrayList<MessageState> processInjection(ComparisonCase currentEntry, IcoOverviewInstance icoInstance) throws GeneralException {
		ArrayList<MessageState> stateMap = new ArrayList<MessageState>();
				
		// Special handling for EOIO
		String queueId = null;
		if (icoInstance.getQualityOfService().equals("EOIO")) {
			queueId = generateQueueId();
		}
					
		// Get list of source files to be injected
		String sourceDir = FileStructure.DIR_TEST_CASES + currentEntry.getSourcePathIn();
		List<Path> sourceInjectPaths = Util.generateListOfPaths(sourceDir, "FILE");
		String targetDir = null;
		List<Path> targetInjectPaths = null;
		
		// Get list of target files according to compare type
		boolean isIcoCompare = currentEntry.getCompareType().equals(ComparisonCase.TYPE.ICO_2_ICO);
		if (isIcoCompare) {
			targetDir = FileStructure.DIR_TEST_CASES + currentEntry.getTargetPathIn();
			targetInjectPaths = Util.generateListOfPaths(targetDir, "FILE");
			
		} else {
			// Load target output files and correlate with source messageId for later compare
			targetDir = FileStructure.DIR_TEST_CASES + currentEntry.getTargetPathOut();
			targetInjectPaths = Util.generateListOfPaths(targetDir, "FILE");
			stateMap = handleInject(currentEntry, icoInstance, sourceInjectPaths, targetInjectPaths, queueId);
		}
		
		stateMap = handleInject(currentEntry, icoInstance, sourceInjectPaths, targetInjectPaths, queueId);
		
		// Return map
		return stateMap;
	}


	private static ArrayList<MessageState> handleInject(ComparisonCase currentEntry, IcoOverviewInstance icoInstance, List<Path> sourceInjectPaths, List<Path> targetInjectPaths, String queueId) throws GeneralException {
		ArrayList<MessageState> stateMap = new ArrayList<MessageState>();
		// Validate that source and target count matches
		if (sourceInjectPaths.size() == targetInjectPaths.size()) {
			// Inject source files
			for (int i = 0; i < sourceInjectPaths.size(); i++) {
				// Get source message information and inject
				Path sourcePath = sourceInjectPaths.get(i);
				String sourceId = UUID.randomUUID().toString();
				injectPayload(icoInstance, queueId, sourceId, sourcePath);
				
				// Get target message information
				Path targetPath = targetInjectPaths.get(i);
				String targetId = UUID.randomUUID().toString();
				
				// Check if we should inject target "input" files or load "output" files
				if (currentEntry.getCompareType().equals(ComparisonCase.TYPE.ICO_2_ICO)) {
					// Inject target file
					injectPayload(icoInstance, queueId, targetId, targetPath);
				} else {
					// Correlate source inject id with target file name instead of messageId
					targetId = targetPath.getFileName().toString();
				}
				
				// Create new message state object
				MessageState mst = new MessageState(currentEntry, sourceId, targetId);
				
				// Add message id's to map for correlation
				stateMap.add(mst);
			}
			
		} else {
			String msg = "File mismatch for ICO 2 ICO scenario. Sources: " + sourceInjectPaths.size() + " targets: " + targetInjectPaths.size();
			throw new GeneralException(msg);
		}
		
		// Return correlation map
		return stateMap;
		
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
