package com.invixo.main.special;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import com.invixo.common.util.Logger;
import com.invixo.common.util.PropertyAccessor;
import com.invixo.common.util.Util;
import com.invixo.main.special.GlobalParameters;


public class MainNew {
	private static Logger logger = null;
	private static final String LOCATION = MainNew.class.getName();
	private static final boolean SKIP_ICO_OVERVIEW_GENERATION = Boolean.parseBoolean(PropertyAccessor.getProperty("SKIP_ICO_OVERVIEW_GENERATION"));

	// Parameter: base directory for all reading and writing to/from file system
	private static final String PARAM_KEY_BASE_DIR 				= "baseDirectory";
	
	// Parameter: location of a credentials file (expected to contain 2 lines. First line contains user name only, second line contains password
	private static final String PARAM_KEY_CREDENTIALS_FILE		= "credentialsFile";
	private static String PARAM_VAL_CREDENTIALS_FILE 			= null;
	
	// Parameter: SAP PO host name. Example: ipod.invixo.com
	private static final String PARAM_KEY_HTTP_HOST				= "httpHost";

	// Parameter: SAP PO host name. Example: 50000
	private static String PARAM_KEY_HTTP_PORT					= "httpPort";
	
	// Parameter: SAP XI sender adapter name
	private static final String PARAM_KEY_XI_SENDER_ADAPTER		= "xiSenderAdapter";

	// Parameter: SAP XI sender component containing the XI adapter
	private static final String PARAM_KEY_SENDER_COMPONENT		= "senderComponent";

	
	
	
	public static void main(String[] args) {
		final String SIGNATURE = "main(String[])";
		long startTime = 0;
		try {
			// Get start time
			startTime = Util.getTime();
			
			// Set internal parameters based on program input arguments		
			setInternalParameters(args);
					
			// Validation of common parameters (relevant for all types of operations)
			validateGeneralParameters();
			
			// Init logger (done at this location since it requires both base location and operation when used in FILE mode)
			logger = Logger.getInstance();
			
			// Post parameter handling: get user/pass from credential file
			readAndSetCredentials(PARAM_VAL_CREDENTIALS_FILE);
				
			// Post parameter handling: build complete PO host and port
			GlobalParameters.SAP_PO_HTTP_HOST_AND_PORT = buildHttpHostPort();
				
			// Create ICO overview
			if (SKIP_ICO_OVERVIEW_GENERATION) {
				logger.writeDebug(LOCATION, SIGNATURE, "ICO Overview generation skipped");
			} else {
				createIcoOverview();				
			}

			// Start comparing (inject + extract + compare)
			compare();
		} catch (ValidationException e) {
			// TODO: Not valid input, inform end user in the nicest way possible
			e.printStackTrace(System.err);
		} finally {
			long endTime = Util.getTime();
			logger.writeInfo(LOCATION, SIGNATURE, "Program execution took (seconds): " + Util.measureTimeTaken(startTime, endTime));
		}
	}
	
	
	private static void createIcoOverview() {
		final String SIGNATURE = "createIcoOverview()";
		
		String fileName = com.invixo.directory.api.Orchestrator.start();
		logger.writeInfo(LOCATION, SIGNATURE, "Ico overview generated: " + fileName);
	}


	private static void compare() {
		com.invixo.main.special.OrchrestateComparison.start();		
	}


	private static String buildHttpHostPort() {
		return "http://" + GlobalParameters.PARAM_VAL_HTTP_HOST + ":" + GlobalParameters.PARAM_VAL_HTTP_PORT + "/";
	}
	
	
	private static void setInternalParameters(String[] args) {
		for (String param : args) {	
			if(param.contains(PARAM_KEY_BASE_DIR)) {
				GlobalParameters.PARAM_VAL_BASE_DIR = param.replace(PARAM_KEY_BASE_DIR + "=", "");
			} else if(param.contains(PARAM_KEY_CREDENTIALS_FILE)) {
				PARAM_VAL_CREDENTIALS_FILE = param.replace(PARAM_KEY_CREDENTIALS_FILE + "=", "");
			} else if(param.contains(PARAM_KEY_HTTP_HOST)) {
				GlobalParameters.PARAM_VAL_HTTP_HOST = param.replace(PARAM_KEY_HTTP_HOST + "=", "");
			} else if(param.contains(PARAM_KEY_HTTP_PORT)) {
				GlobalParameters.PARAM_VAL_HTTP_PORT = param.replace(PARAM_KEY_HTTP_PORT + "=", "");
			} else if(param.contains(PARAM_KEY_XI_SENDER_ADAPTER)) {
				GlobalParameters.PARAM_VAL_XI_SENDER_ADAPTER = param.replace(PARAM_KEY_XI_SENDER_ADAPTER + "=", "");
			} else if(param.contains(PARAM_KEY_SENDER_COMPONENT)) {
				GlobalParameters.PARAM_VAL_SENDER_COMPONENT = param.replace(PARAM_KEY_SENDER_COMPONENT + "=", "");
			}
		}
	}
	
	
	private static void readAndSetCredentials(String sourceDirectory) throws ValidationException {
		try {
			// Get credential file
			List<Path> credentialsFile = Util.generateListOfPaths(sourceDirectory, "FILE");
			
			// We expect only credentials file
			List<String> credentialLines = Files.lines(credentialsFile.get(0)).collect(Collectors.toList());
			
			// Line 1: user name
			GlobalParameters.CREDENTIAL_USER = credentialLines.get(0);
			
			// line 2: password
			GlobalParameters.CREDENTIAL_PASS = credentialLines.get(1);
		} catch (IOException e) {
			String msg = "Error | Problem reading credentials file from :" + sourceDirectory + " " + e.getMessage();
			throw new ValidationException(msg);
		}
	}


	private static void validateGeneralParameters() throws ValidationException {
		StringWriter sw = new StringWriter();

		if (GlobalParameters.PARAM_VAL_BASE_DIR == null) {
			sw.write("Obligatory program parameter " + PARAM_KEY_BASE_DIR + " not set.\n");
		}
		
		if (!sw.toString().equals("")) {
			throw new ValidationException(sw.toString());
		}
	}
	
}
