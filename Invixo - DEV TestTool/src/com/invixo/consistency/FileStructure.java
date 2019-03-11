package com.invixo.consistency;

import java.util.ArrayList;
import com.invixo.common.IcoOverviewInstance;
import com.invixo.common.util.Logger;
import com.invixo.common.util.Util;
import com.invixo.main.special.GlobalParameters;

public class FileStructure {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = FileStructure.class.getName();
	
	// Base/root file location
	private static final String FILE_BASE_LOCATION					= GlobalParameters.PARAM_VAL_BASE_DIR;
	
	// Various
	private static final String DIR_LOGS							= FILE_BASE_LOCATION + "\\Logs\\";		// Manually set in Logger also.
	public static final String DIR_DEBUG							= FILE_BASE_LOCATION + "\\Debug\\";
	public static final String DIR_CONFIG							= FILE_BASE_LOCATION + "\\Config\\";
	public static final String DIR_REPORTS							= FILE_BASE_LOCATION + "\\Reports\\";
	public static final String DIR_STATE							= FILE_BASE_LOCATION + "\\State\\";
	public static final String DIR_TEST_CASES						= FILE_BASE_LOCATION + "\\TestCases\\";
	
	// Files
	public static final String FILE_CONFIG_COMPARE_EXEPTIONS		= DIR_CONFIG + "CompareExceptions.xml";
	public static final String FILE_ICO_OVERVIEW	 				= "IntegratedConfigurationsOverview.xml";
	public static final String FILE_COMPARISON_OVERVIEW				= "RttComparisonList.xml";	
	public static final String PAYLOAD_FILE_EXTENSION 				= ".multipart";	

	
	/**
	 * Start File Structure check.
	 */
	public static void startCheck(ArrayList<IcoOverviewInstance> icoList) {
		String SIGNATURE = "startCheck(ArrayList<IcoOverviewInstance>)";
		logger.writeDebug(LOCATION, SIGNATURE, "Start file structure check");

		// Ensure project folder structure is present
		checkFolderStructure(icoList);
				
		logger.writeDebug(LOCATION, SIGNATURE, "File structure check completed!");
	}


	/**
	 * Ensure project folder structure is healthy.
	 */
	private static void checkFolderStructure(ArrayList<IcoOverviewInstance> icoList) {
		Util.createDirIfNotExists(FILE_BASE_LOCATION);
		Util.createDirIfNotExists(DIR_STATE);
		Util.createDirIfNotExists(DIR_LOGS);
		Util.createDirIfNotExists(DIR_REPORTS);
		Util.createDirIfNotExists(DIR_CONFIG);
		Util.createDirIfNotExists(DIR_DEBUG);
		Util.createDirIfNotExists(DIR_TEST_CASES);
	}

	
	/**
	 * Generate file name for a file only used at debugging time (this is web service requests and responses).
	 * @param webServiceName
	 * @param isRequest
	 * @param identifier
	 * @param extension
	 * @return
	 */
	public static String getDebugFileName(String webServiceName, boolean isRequest, String identifier, String extension) {
		String fileName = FileStructure.DIR_DEBUG 
						+ webServiceName 
						+ "_" 
						+ (isRequest?"req":"resp") 
						+ "_" 
						+ identifier 
						+ "_" 
						+ System.currentTimeMillis() 
						+ "." 
						+ extension;
		return fileName;
	}
}
