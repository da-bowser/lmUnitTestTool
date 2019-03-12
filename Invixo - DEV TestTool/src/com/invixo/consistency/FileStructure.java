package com.invixo.consistency;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

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
		
		// Ensure that a exception file exists
		File f = new File(FILE_CONFIG_COMPARE_EXEPTIONS);
		if (!f.exists()) {
			generateInitialIcoExeptionContent(icoList);
		}		
		
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
	
	
	private static void generateInitialIcoExeptionContent(ArrayList<IcoOverviewInstance> icoList) {
		final String	XML_PREFIX = "inv";
		final String	XML_NS = "urn:invixo.com.consistency";
		try {
			XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
			XMLStreamWriter xmlWriter = xMLOutputFactory.createXMLStreamWriter(new FileOutputStream(FILE_CONFIG_COMPARE_EXEPTIONS), GlobalParameters.ENCODING);

			// Add xml version and encoding to output
			xmlWriter.writeStartDocument(GlobalParameters.ENCODING, "1.0");

			// Create element: Configuration
			xmlWriter.writeStartElement(XML_PREFIX, "Configuration", XML_NS);
			xmlWriter.writeNamespace(XML_PREFIX, XML_NS);
			
			// Loop ICO's found
			for (IcoOverviewInstance ico : icoList) {
				// Get name of current ICO
				String icoName = ico.getName();
				
				// Create element: Configuration | IntegratedConfiguration
				xmlWriter.writeStartElement(XML_PREFIX, "IntegratedConfiguration", XML_NS);
				
				// Create element: Configuration | IntegratedConfiguration | Name
				xmlWriter.writeStartElement(XML_PREFIX, "Name", XML_NS);
				xmlWriter.writeCharacters(icoName);
				// Close element: Configuration | IntegratedConfiguration | Name
				xmlWriter.writeEndElement();
				
				// Create element: Configuration | IntegratedConfiguration | Exceptions
				xmlWriter.writeStartElement(XML_PREFIX, "Exceptions", XML_NS);
				
				// Create element: Configuration | IntegratedConfiguration | Exceptions | XPath
				xmlWriter.writeStartElement(XML_PREFIX, "XPath", XML_NS);
				// Close element: Configuration | IntegratedConfiguration | Exceptions | XPath
				xmlWriter.writeEndElement();
				
				// Close element: Configuration | IntegratedConfiguration | Exceptions
				xmlWriter.writeEndElement();
				
				// Close element: Configuration | IntegratedConfiguration
				xmlWriter.writeEndElement();
			}
			
			// Close element: IntegratedConfigurations
			xmlWriter.writeEndElement();
			
			// Finalize writing
			xmlWriter.flush();
			xmlWriter.close();
		} catch (XMLStreamException | FileNotFoundException e) {
			throw new RuntimeException("Error generating compareExceptions.xml file! " + e);
		}
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
