package com.invixo.main.special;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;

import com.invixo.common.IcoOverviewDeserializer;
import com.invixo.common.IcoOverviewInstance;
import com.invixo.common.util.Logger;
import com.invixo.consistency.FileStructure;

public class OrchrestateComparison {

	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = OrchrestateComparison.class.getName();
	

	
	
	public static void start() {
		final String SIGNATURE = "start()";
		logger.writeDebug(LOCATION, SIGNATURE, "Start comparison orchestration...");

		// Get ICO Overview list
		ArrayList<IcoOverviewInstance> icoInstancesList = loadIcoOverview(FileStructure.DIR_CONFIG, FileStructure.FILE_ICO_OVERVIEW);
		
		// Get Comparison Cases
		ArrayList<ComparisonCase> comparisonList = loadComparisonCases(FileStructure.DIR_CONFIG, FileStructure.FILE_COMPARISON_OVERVIEW);
		
		// Process each comparison case
		for (ComparisonCase currentEntry : comparisonList) {
			// Get current ICO ref
			String currentIcoNameRef = currentEntry.getSourceIco();
			
			// Get matching ICO instance
			IcoOverviewInstance icoInstance = getIcoFromOverview(icoInstancesList, currentIcoNameRef);
			
			// Get list of source files to be injected
			
			// For each source file:	
				// Inject file to SAP PO, EXTRACT ENTRY
				// If comparisonType == FILE, THEN build STATE FILE, ELSE INJECT TARGET FILE
			
			
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
