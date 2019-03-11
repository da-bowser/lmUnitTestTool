package com.invixo.main.special;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.Attribute;
import javax.xml.transform.stream.StreamSource;

import com.invixo.common.util.Logger;


public class ComparisonXmlDeserializer {
	private static Logger logger 			= Logger.getInstance();
	private static final String LOCATION 	= ComparisonXmlDeserializer.class.getName();	
	
	
	/**
	 * Deserialize ComparisonOverview XML file into Java object.
	 * Only active ComparisonCases are collected.
	 * @param comparisonXmlStream			Stream to Comparison Overview XML
	 * @return
	 */
	public static ArrayList<ComparisonCase> deserialize(InputStream comparisonXmlStream) {
		final String SIGNATURE = "deserialize(InputStream)";
		try {
			// Prepare
			XMLInputFactory factory = XMLInputFactory.newInstance();
			StreamSource ss = new StreamSource(comparisonXmlStream);
			XMLEventReader eventReader = factory.createXMLEventReader(ss);
			
			// Parse XML file and extract data
		    boolean isSourceFound = false;
		    boolean isTargetFound = false;
		    boolean isActive = false;
			ArrayList<ComparisonCase> comparisonList = new ArrayList<ComparisonCase>();
			ComparisonCase currentExtract = null;
			int maxComparisons = 0;
		    
			while (eventReader.hasNext()) {
			    XMLEvent event = eventReader.nextEvent();

			    switch(event.getEventType()) {
			    case XMLStreamConstants.START_ELEMENT:
			    	String currentStartElementName = event.asStartElement().getName().getLocalPart().toString();

			    	// Root
			    	if ("Comparisons".equals(currentStartElementName) ) {

			    		// Get attribute values
			    		Iterator<Attribute> iterator = event.asStartElement().getAttributes();
			            while (iterator.hasNext())
			            {
			                Attribute attribute = iterator.next();
			                String name = attribute.getName().toString();
		                	if ("MaxComparisons".equals(name)) {
		                		maxComparisons = Integer.parseInt(attribute.getValue());
			                }
			            }
			            
					// CompareEntry (single instance/entry root)			            
			    	} else if ("CompareEntry".equals(currentStartElementName)) {
						currentExtract = new ComparisonCase();
						currentExtract.setMaxComparisons(maxComparisons);
						isSourceFound = false;
						isTargetFound = false;
						isActive = false;

					// Active
			    	} else if ("Active".equals(currentStartElementName) && eventReader.peek().isCharacters()) {
			    		isActive = Boolean.parseBoolean(eventReader.peek().asCharacters().getData());

					// Max messages
					} else if ("MaxMessages".equals(currentStartElementName) && eventReader.peek().isCharacters()) {
						currentExtract.setMaxComparisons(Integer.parseInt(eventReader.peek().asCharacters().getData()));	
			    	
					// Source
					} else if ("Source".equals(currentStartElementName) && eventReader.peek().isCharacters()) {
						isTargetFound = false;	
						isSourceFound = true;

					// Source | FilePathInput
					} else if (isSourceFound && "FilePathInput".equals(currentStartElementName) && eventReader.peek().isCharacters()) {
						currentExtract.setSourcePathIn(eventReader.peek().asCharacters().getData());
			    	
					// Source | FilePathOutput
					} else if (isSourceFound && "FilePathOutput".equals(currentStartElementName) && eventReader.peek().isCharacters()) {
						currentExtract.setSourcePathOut(eventReader.peek().asCharacters().getData());
			    	
					// Source | Ico
					} else if (isSourceFound && "Ico".equals(currentStartElementName) && eventReader.peek().isCharacters()) {
						currentExtract.setSourceIco(eventReader.peek().asCharacters().getData());
						
					// CompareType
					} else if (isSourceFound && "Ico".equals(currentStartElementName) && eventReader.peek().isCharacters()) {
						currentExtract.setCompareType(ComparisonCase.TYPE.valueOf(eventReader.peek().asCharacters().getData()));
			    	
					// Target
					} else if ("Target".equals(currentStartElementName) && eventReader.peek().isCharacters()) {
						isTargetFound = true;	
						isSourceFound = false;
						
					// Target | FilePathInput
					} else if (isTargetFound && "FilePathInput".equals(currentStartElementName) && eventReader.peek().isCharacters()) {
						currentExtract.setTargetPathIn(eventReader.peek().asCharacters().getData());
			    	
					// Target | FilePathOutput
					} else if (isTargetFound && "FilePathOutput".equals(currentStartElementName) && eventReader.peek().isCharacters()) {
						currentExtract.setTargetPathOut(eventReader.peek().asCharacters().getData());
			    	
					// Target | Ico
					} else if (isTargetFound && "Ico".equals(currentStartElementName) && eventReader.peek().isCharacters()) {
						currentExtract.setTargetIco(eventReader.peek().asCharacters().getData());
						
					}
			    	break;
			    				    	
			    case XMLStreamConstants.END_ELEMENT:
			    	String currentEndElementName = event.asEndElement().getName().getLocalPart().toString();
			    
			    	if (isActive && "CompareEntry".equals(currentEndElementName)) {
			    		comparisonList.add(currentExtract);
			    	}
			    	break;
			    }
			}
			
			return comparisonList;
		} catch (XMLStreamException e) {
			String msg = "Error deserializing Comparison Overview XML file into list of java objects\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new RuntimeException(msg);
		} 
	}
	
}
