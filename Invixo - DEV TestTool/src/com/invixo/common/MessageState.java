package com.invixo.common;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.events.XMLEvent;

import com.invixo.common.util.Logger;
import com.invixo.common.util.Util;
import com.invixo.compare.CompareException;
import com.invixo.compare.Comparer;
import com.invixo.consistency.FileStructure;
import com.invixo.main.special.ComparisonCase;

public class MessageState {
	private static Logger logger 			= Logger.getInstance();
	private static final String LOCATION 	= IcoOverviewDeserializer.class.getName();	
	
	private long dateTime = 0;
	private String sourceFileOutputPath = null;
	private String sourceFileName = null;
	private String sourceInjectId = null;
	private String sourceIcoName = null;
	private String targetFileOutputPath = null;
	private String targetFileName = null;
	private String targetInjectId = null;
	private String targetIcoName = null;
	private Comparer comp = null;
	private Exception ex = null;	
	private ArrayList<String> xpathExceptions = new ArrayList<String>();
	
	public MessageState(ComparisonCase entry, String sourceId, String targetId) {
		this.dateTime = Util.getTime();
		
		this.sourceFileOutputPath = entry.getSourcePathOut();
		this.sourceFileName = sourceId + ".xml";
		this.sourceInjectId = sourceId;
		
		this.targetFileOutputPath = entry.getTargetPathOut();		
		if (entry.getCompareType().equals(ComparisonCase.TYPE.ICO_2_ICO)) {
			this.targetInjectId = targetId;
			this.targetFileName = targetId + ".xml";
		} else {
			this.targetFileName = targetId;
		}
		
		this.sourceIcoName = entry.getSourceIco();
		this.targetIcoName = entry.getTargetIco();
		
		
		// Build exception map to be used to exclude data elements in later compare
		try {
			setXpathExceptions(extractIcoCompareExceptionsFromFile(FileStructure.FILE_CONFIG_COMPARE_EXEPTIONS, entry.getSourceIco()));
		} catch (GeneralException e) {
			this.ex = e;
		}

	}
	
	
	
	/**
	 * Extract configured XPath compare exceptions (SIMILAR) from file matching ICO.
	 * @param exceptionXPathConfigFilePath		Location of configuration file
	 * @param icoName							Name of relevant ICO
	 * @return									List of matching compare exceptions
	 * @throws CompareException
	 */
	private ArrayList<String> extractIcoCompareExceptionsFromFile(String exceptionXPathConfigFilePath, String icoName) throws GeneralException {
		final String SIGNATURE = "extractIcoCompareExceptionsFromFile(String, String)";
		logger.writeDebug(LOCATION, SIGNATURE, "Building MAP of exceptions using data from: " + exceptionXPathConfigFilePath);
		
		ArrayList<String> icoExceptions = new ArrayList<String>();
		
		try {
			InputStream fileStream = new FileInputStream(exceptionXPathConfigFilePath);		
			XMLInputFactory factory = XMLInputFactory.newInstance();
			XMLEventReader eventReader = factory.createXMLEventReader(fileStream);
			boolean correctIcoFound = false;
			while (eventReader.hasNext()) {
			    XMLEvent event = eventReader.nextEvent();
			    
			    switch(event.getEventType()) {
			    case XMLStreamConstants.START_ELEMENT:
			    	String currentStartElementName = event.asStartElement().getName().getLocalPart();
			    	if ("Name".equals(currentStartElementName)) {
						if (icoName.equals(eventReader.peek().asCharacters().getData())) {
							// We are at the correct ICO element
							correctIcoFound = true;
						}
					}
			    	
			    	if ("XPath".equals(currentStartElementName) && correctIcoFound && eventReader.peek().isCharacters()) {
			    		String configuredExceptionXPath = eventReader.peek().asCharacters().getData();
			    		
			    		if (configuredExceptionXPath.length() > 0) {
				    		// Add exception data if we are at the right ICO and correct element
				    		icoExceptions.add(configuredExceptionXPath);
						}
			    	}
			    	break;
			    	
			    case XMLStreamConstants.END_ELEMENT:
			    	String currentEndElementName = event.asEndElement().getName().getLocalPart();
			    	if ("IntegratedConfiguration".equals(currentEndElementName)) {
			    		// We don't want to read any more ICO data
			    		correctIcoFound = false;
					}
			    	break;
			    }
			}
			
			// Return exceptions found
			return icoExceptions; 
		} catch (Exception e) {
			String msg = "Error extracting exceptions.\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new GeneralException(msg);
		}
	}
	
	
	public Long getDateTime() {
		return dateTime;
	}
	public void setDateTime(long dateTime) {
		this.dateTime = dateTime;
	}
	public String getSourceFileOutputPath() {
		return sourceFileOutputPath;
	}
	public void setSourceFileOutputPath(String sourceFileOutputPath) {
		
		this.sourceFileOutputPath = sourceFileOutputPath;
	}
	public String getSourceFileName() {
		return sourceFileName;
	}
	public void setSourceFileName(String sourceFileName) {
		this.sourceFileName = sourceFileName;
	}
	public String getSourceInjectId() {
		return sourceInjectId;
	}
	public void setSourceInjectId(String sourceInjectId) {
		this.sourceInjectId = sourceInjectId;
	}
	public String getSourceIcoName() {
		return sourceIcoName;
	}
	public void setSourceIcoName(String sourceIcoName) {
		this.sourceIcoName = sourceIcoName;
	}
	public String getTargetFileOutputPath() {
		return targetFileOutputPath;
	}
	public void setTargetFileOutputPath(String targetFileOutputPath) {
		this.targetFileOutputPath = targetFileOutputPath;
	}
	public String getTargetFileName() {
		return targetFileName;
	}
	public void setTargetFileName(String targetFileName) {
		this.targetFileName = targetFileName;
	}
	public String getTargetInjectId() {
		return targetInjectId;
	}
	public void setTargetInjectId(String targetInjectId) {
		this.targetInjectId = targetInjectId;
	}
	public String getTargetIcoName() {
		return targetIcoName;
	}
	public void setTargetIcoName(String targetIcoName) {
		this.targetIcoName = targetIcoName;
	}

	public Comparer getComp() {
		return comp;
	}

	public void setComp(Comparer comp) {
		this.comp = comp;
	}

	public Exception getEx() {
		return ex;
	}

	public void setEx(Exception ex) {
		this.ex = ex;
	}



	public ArrayList<String> getXpathExceptions() {
		return xpathExceptions;
	}



	public void setXpathExceptions(ArrayList<String> xpathExceptions) {
		this.xpathExceptions = xpathExceptions;
	}
}
