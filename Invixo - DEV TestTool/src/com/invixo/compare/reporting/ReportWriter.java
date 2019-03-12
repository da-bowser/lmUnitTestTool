package com.invixo.compare.reporting;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.xmlunit.diff.Difference;

import com.invixo.consistency.FileStructure;
import com.invixo.main.special.ComparisonCase;
import com.invixo.main.special.GlobalParameters;
import com.invixo.common.MessageState;
import com.invixo.compare.Comparer;

public class ReportWriter {
	private static final String	XML_PREFIX = "inv";
	private static final String	XML_NS = "urn:invixo.com.compare.report";

	private final String REPORT_FILE = FileStructure.DIR_REPORTS + "CompareReport_" + System.currentTimeMillis() + ".xml";
	private ArrayList<ComparisonCase> testCases = null;

	// ICO general
	private int	countTestCasesTotal = 0;
	private int	countComparedSuccessTotal = 0;
	private int	countComparedErrorTotal = 0;
	private double totalExecutionTime = 0;
	
	public ReportWriter(ArrayList<ComparisonCase> testCases) {
		this.testCases = testCases;

		// Interpret general data
		this.evaluateGeneralResults();
	}


	private void evaluateGeneralResults() {
		// Set total number of ICO's processed
		this.countTestCasesTotal = testCases.size();
		
		for (ComparisonCase testCase : testCases) {
			Comparer comp = null;
			for (MessageState mst : testCase.getCaseList()) {
				comp = mst.getComp();
				this.countComparedSuccessTotal += comp.getCompareSuccess();
				this.countComparedErrorTotal += comp.getCompareSkipped();
				this.totalExecutionTime += comp.getExecutionTimeSeconds();				
			}		
		}
		
		this.countTestCasesTotal = this.countComparedSuccessTotal + this.countComparedErrorTotal;
	}
	
	public String create() {
		try {
			XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
			XMLStreamWriter xmlWriter = xMLOutputFactory.createXMLStreamWriter(new FileOutputStream(REPORT_FILE), GlobalParameters.ENCODING);

			// Add xml version and encoding to output
			xmlWriter.writeStartDocument(GlobalParameters.ENCODING, "1.0");

			// Create element: CompareReport
			xmlWriter.writeStartElement(XML_PREFIX, "CompareReport", XML_NS);
			xmlWriter.writeNamespace(XML_PREFIX, XML_NS);

			// Add structure: CompareReport | IcoOverview
			addCompareGlobalOverview(xmlWriter);
			
			// Create element: CompareReport | Details
			xmlWriter.writeStartElement(XML_PREFIX, "Details", XML_NS);

			// Add list: CompareReport | Details | IntegratedConfiguration
			addCompareDetails(this.testCases, xmlWriter);

			// Close element: CompareReport | Details
			xmlWriter.writeEndElement();

			// Close element: CompareReport
			xmlWriter.writeEndElement();

			// Finalize writing
			xmlWriter.flush();
			xmlWriter.close();

			// Return report name
			return REPORT_FILE;
		} catch (XMLStreamException | FileNotFoundException e) {
			throw new RuntimeException("Error generating report! " + e);
		}
		
	}
	
	
	private void addCompareDetails(ArrayList<ComparisonCase> testCases,
			XMLStreamWriter xmlWriter) throws XMLStreamException {
		// Add detail info per Test Case
		for (ComparisonCase testCase : this.testCases) {
			// Create element: CompareReport | TestCase
			xmlWriter.writeStartElement(XML_PREFIX, "TestCase", XML_NS);
			
			xmlWriter.writeStartElement(XML_PREFIX, "SourceIco", XML_NS);
			xmlWriter.writeCharacters(testCase.getSourceIco());
			xmlWriter.writeEndElement();
			xmlWriter.writeStartElement(XML_PREFIX, "TargetIco", XML_NS);
			xmlWriter.writeCharacters(testCase.getTargetIco());
			xmlWriter.writeEndElement();
			
			xmlWriter.writeStartElement(XML_PREFIX, "Type", XML_NS);
			xmlWriter.writeCharacters(testCase.getCompareType().name().toString());
			xmlWriter.writeEndElement();
			
			addTestCaseData(xmlWriter, testCase);
						
			// Close element: CompareReport | TestCase
			xmlWriter.writeEndElement();
		}
	}
	
	private void addTestCaseData(XMLStreamWriter xmlWriter, ComparisonCase testCase) throws XMLStreamException {
		for(MessageState mst : testCase.getCaseList()) {
			xmlWriter.writeStartElement(XML_PREFIX, "Compare", XML_NS);
			
			Comparer comp = mst.getComp();				
			// Create element: CompareReport | TestCase | Error
			xmlWriter.writeStartElement(XML_PREFIX, "Error", XML_NS);
			xmlWriter.writeCharacters(mst.getEx() == null ? "" : mst.getEx().getMessage());
			// Close element: CompareReport | TestCase | Error
			xmlWriter.writeEndElement();

			// Create element: CompareReport | TestCase | ExecutionTimeSeconds
			xmlWriter.writeStartElement(XML_PREFIX, "ExecutionTime", XML_NS);
			xmlWriter.writeAttribute("unit", "seconds");
			xmlWriter.writeCharacters("" + comp.getExecutionTimeSeconds());
			xmlWriter.writeEndElement();

			// Add compare header data
			addCompareOverview(xmlWriter, comp);	

			if (comp.getCompareDifferences().size() > 0) {
				// Create element: CompareReport | IntegratedConfiguration | CompareOverview | CompareDetails
				xmlWriter.writeStartElement(XML_PREFIX, "CompareDetails", XML_NS);
				// Add compare details
				addCompareDetails(xmlWriter, comp);
				// Close element: CompareReport | IntegratedConfiguration | CompareOverview | CompareDetails
				xmlWriter.writeEndElement();

			}
			
			xmlWriter.writeEndElement();
		}
	}


	private void addCompareOverview(XMLStreamWriter xmlWriter, Comparer comp) throws XMLStreamException {
		// Create element: CompareReport | IntegratedConfiguration | CompareOverview | Differences
		xmlWriter.writeStartElement(XML_PREFIX, "Differences", XML_NS);
		
		// Create element: CompareReport | IntegratedConfiguration | CompareOverview | Differences | Found
		xmlWriter.writeStartElement(XML_PREFIX, "Found", XML_NS);
		xmlWriter.writeCharacters("" + comp.getCompareDifferences().size());
		// Close element: CompareReport | IntegratedConfiguration | CompareOverview | Differences | Found
		xmlWriter.writeEndElement();
		
		// Create element: CompareReport | IntegratedConfiguration | CompareOverview | Differences | Handled
		xmlWriter.writeStartElement(XML_PREFIX, "Handled", XML_NS);
		xmlWriter.writeCharacters("" + comp.getDiffsIgnoredByConfiguration().size());
		// Close element: CompareReport | IntegratedConfiguration | CompareOverview | Differences | Handled
		xmlWriter.writeEndElement();
		
		// Create element: CompareReport | IntegratedConfiguration | CompareOverview | Differences | Unhandled
		xmlWriter.writeStartElement(XML_PREFIX, "Unhandled", XML_NS);
		xmlWriter.writeCharacters("" + (comp.getCompareDifferences().size() - comp.getDiffsIgnoredByConfiguration().size()));
		// Close element: CompareReport | IntegratedConfiguration | CompareOverview | Differences | Unhandled
		xmlWriter.writeEndElement();
				
		// Close element:  CompareReport | IntegratedConfiguration | CompareOverview | Differences
		xmlWriter.writeEndElement();
		
		// Create element: CompareReport | IntegratedConfiguration | CompareOverview | CompareDetails | Compare | ConfiguredExceptions
		xmlWriter.writeStartElement(XML_PREFIX, "ConfiguredExceptions", XML_NS);
		// Add exception data
		addCompareExceptionInfo(xmlWriter, comp);
		
		// Close element: CompareReport | IntegratedConfiguration | CompareOverview | CompareDetails | Compare | ConfiguredExceptions
		xmlWriter.writeEndElement();
	}
	
	private void addCompareDetails(XMLStreamWriter xmlWriter, Comparer comp) throws XMLStreamException {
		
			// Create element: | CompareDetails | Compare | 
			xmlWriter.writeStartElement(XML_PREFIX, "Compare", XML_NS);
			
			// Create element: | CompareDetails | Compare | Files
			xmlWriter.writeStartElement(XML_PREFIX, "Files", XML_NS);
			
			// Create element:  | CompareDetails | Compare | Files | Source
			xmlWriter.writeStartElement(XML_PREFIX, "Source", XML_NS);
			xmlWriter.writeAttribute("file", extractInfoFromPath(comp.getSourceFile(), "FILE"));
			xmlWriter.writeAttribute("bytes", "" + comp.getSourceFileSize());
			xmlWriter.writeCharacters(comp.getSourceFile().toAbsolutePath().toString().replace(comp.getSourceFile().getFileName().toString(), ""));
			// Close element: | CompareDetails | Compare | Source
			xmlWriter.writeEndElement();
			
			// Create element:  | CompareDetails | Compare | Files | Target
			xmlWriter.writeStartElement(XML_PREFIX, "Target", XML_NS);
			xmlWriter.writeAttribute("file", extractInfoFromPath(comp.getCompareFile(), "FILE"));
			xmlWriter.writeAttribute("bytes", "" + comp.getCompareFileSize());
			xmlWriter.writeCharacters(extractInfoFromPath(comp.getCompareFile(), "DIRECTORY"));
			// Close element: | CompareDetails | Compare | Target
			xmlWriter.writeEndElement();
			
			// Close element:  | CompareDetails | Compare | Files
			xmlWriter.writeEndElement();
			
			// Create element: | CompareDetails | Compare | Result
			xmlWriter.writeStartElement(XML_PREFIX, "Result", XML_NS);
			addCompareResult(xmlWriter, comp);			
			// Close element: | CompareDetails | Compare | Result
			xmlWriter.writeEndElement();
			
			// Close element: | CompareDetails | Compare |
			xmlWriter.writeEndElement();
	}
	
	private void addCompareResult(XMLStreamWriter xmlWriter, Comparer comp) throws XMLStreamException {
		
		// Create element: | Status
		xmlWriter.writeStartElement(XML_PREFIX, "Status", XML_NS);
		xmlWriter.writeCharacters(comp.getCompareSkipped() == 1 ? "Skipped" : "Success");
		// Close element: | Found
		xmlWriter.writeEndElement();
		
		// Create element: | DifferenceList | Difference | ExecutionTime
		xmlWriter.writeStartElement(XML_PREFIX, "ExecutionTime", XML_NS);
		xmlWriter.writeAttribute("unit", "seconds");
		xmlWriter.writeCharacters("" + comp.getExecutionTimeSeconds());			
		// Close element: | DifferenceList | Difference | ExecutionTime
		xmlWriter.writeEndElement();
		
		// Create element: | Error
		xmlWriter.writeStartElement(XML_PREFIX, "Error", XML_NS);
		xmlWriter.writeCharacters(comp.getCompareException() == null ? "" : comp.getCompareException().getMessage());
		// Close element: | Found
		xmlWriter.writeEndElement();
				
		// Create element: | Found
		xmlWriter.writeStartElement(XML_PREFIX, "Found", XML_NS);
		xmlWriter.writeCharacters("" + comp.getCompareDifferences().size());
		// Close element: | Found
		xmlWriter.writeEndElement();
		
		// Create element: | Handled
		xmlWriter.writeStartElement(XML_PREFIX, "Handled", XML_NS);
		xmlWriter.writeCharacters("" + comp.getDiffsIgnoredByConfiguration().size());
		// Close element: | Handled
		xmlWriter.writeEndElement();	
		
		// Create element: | Unhandled
		xmlWriter.writeStartElement(XML_PREFIX, "Unhandled", XML_NS);
		xmlWriter.writeCharacters("" + (comp.getCompareDifferences().size() - comp.getDiffsIgnoredByConfiguration().size()));
		// Close element: | Unhandled
		xmlWriter.writeEndElement();
				
		// Create element: | DifferenceList
		xmlWriter.writeStartElement(XML_PREFIX, "DifferenceList", XML_NS);
		
		addDifferenceData(xmlWriter, comp);

		// Close element: | DifferenceList
		xmlWriter.writeEndElement();
	}


	private void addDifferenceData(XMLStreamWriter xmlWriter, Comparer comp) throws XMLStreamException {
		// Create for each difference found
		for (Difference d : comp.getCompareDifferences()) {
			// Create element: Difference
			xmlWriter.writeStartElement(XML_PREFIX, "Difference", XML_NS);

			// Create element: | DifferenceList | Difference | Status
			xmlWriter.writeStartElement(XML_PREFIX, "Status", XML_NS);
			xmlWriter.writeCharacters(d.getResult().name());			
			// Close element: | DifferenceList | Difference | Status
			xmlWriter.writeEndElement();
			
			// Create element: | DifferenceList | Difference | Type
			xmlWriter.writeStartElement(XML_PREFIX, "Type", XML_NS);
			xmlWriter.writeCharacters(d.getComparison().getType().toString());			
			// Close element: | DifferenceList | Difference | Type
			xmlWriter.writeEndElement();
			
			// Create element: | DifferenceList | Difference | Control
			xmlWriter.writeStartElement(XML_PREFIX, "Control", XML_NS);
			
			// Add Control value and xpath to Control element
			addValueAndXPath(xmlWriter, d, "control");
						
			// Close element | DifferenceList | Difference | Control
			xmlWriter.writeEndElement();
			
			
			// Create element: | DifferenceList | Difference | Test
			xmlWriter.writeStartElement(XML_PREFIX, "Test", XML_NS);
			
			// Add Test value and xpath to Test element
			addValueAndXPath(xmlWriter, d, "test");
			
			// Close element | DifferenceList | Difference | Test
			xmlWriter.writeEndElement();
			
			String ignoredXpath = comp.getDiffsIgnoredByConfiguration().get(extractXpathFromDifference(d, "control"));
			if (ignoredXpath != null) {
				// Create element: | DifferenceList | Difference | IgnoreMatchFound
				xmlWriter.writeStartElement(XML_PREFIX, "IgnoreMatchFound", XML_NS);
				xmlWriter.writeCharacters(ignoredXpath);			
				// Close element: | DifferenceList | Difference | IgnoredByConfiguration
				xmlWriter.writeEndElement();
			}
			
			// Close element: | DifferenceList | Difference
			xmlWriter.writeEndElement();
		}

	}


	private void addValueAndXPath(XMLStreamWriter xmlWriter, Difference d, String type) throws XMLStreamException {
		// Create element: | DifferenceList | Difference | Value
		xmlWriter.writeStartElement(XML_PREFIX, "Value", XML_NS);
		xmlWriter.writeCharacters(extractValueFromDifference(d, type));			
		// Close element: | DifferenceList | Difference | IgnoredByConfiguration
		xmlWriter.writeEndElement();
					
		// Create element: | DifferenceList | Difference | XPath
		xmlWriter.writeStartElement(XML_PREFIX, "XPath", XML_NS);
		xmlWriter.writeCharacters(extractXpathFromDifference(d, type));
		// Close element: | DifferenceList | Difference | ControlXPath
		xmlWriter.writeEndElement();
		
	}


	private void addCompareExceptionInfo(XMLStreamWriter xmlWriter, Comparer comp) throws XMLStreamException {
		// Add custom exceptions to report for each ICO
		List<String> xpathExceptions = comp.getXPathExceptions();
		for (String xPath : xpathExceptions) {
			// Create element: | Exceptions | Configured | XPath
			xmlWriter.writeStartElement(XML_PREFIX, "XPath", XML_NS);
			xmlWriter.writeCharacters(xPath);
			// Close element: | Exceptions | Configured | XPath
			xmlWriter.writeEndElement();
		}
	}


	private void addCompareGlobalOverview(XMLStreamWriter xmlWriter) throws XMLStreamException {
		// Create element: CompareReport | TestCases
		xmlWriter.writeStartElement(XML_PREFIX, "TestCases", XML_NS);

		// Create element: CompareReport | TestCases | Success
		xmlWriter.writeStartElement(XML_PREFIX, "Success", XML_NS);
		xmlWriter.writeCharacters("" + this.countComparedSuccessTotal);
		xmlWriter.writeEndElement();

		// Create element: CompareReport | TestCases | TechnicalError
		xmlWriter.writeStartElement(XML_PREFIX, "Error", XML_NS);
		xmlWriter.writeCharacters("" + this.countComparedErrorTotal);
		xmlWriter.writeEndElement();

		// Create element: CompareReport | TestCases | Total
		xmlWriter.writeStartElement(XML_PREFIX, "Total", XML_NS);
		xmlWriter.writeCharacters("" + this.countTestCasesTotal);
		xmlWriter.writeEndElement();
		
		// Create element: CompareReport | TestCases | ExecutionTime
		xmlWriter.writeStartElement(XML_PREFIX, "ExecutionTime", XML_NS);
		xmlWriter.writeAttribute("unit", "seconds");
		xmlWriter.writeCharacters("" + this.totalExecutionTime);
		xmlWriter.writeEndElement();

		// Close element: CompareReport | TestCases
		xmlWriter.writeEndElement();
	}
	
	
	private String extractXpathFromDifference(Difference d, String type) {
		String xPath = "";
		
		try {
			if (type.equals("control")) {
				xPath = d.getComparison().getControlDetails().getXPath().toString();
			} else {
				xPath = d.getComparison().getTestDetails().getXPath().toString();
			}
			
		} catch (NullPointerException e) {
			xPath = "";
		}
		
		return xPath;
	}
	
	
	private String extractValueFromDifference(Difference d, String type) {
		String value = "";
		
		try {
			if (type.equals("control")) {
				value = d.getComparison().getControlDetails().getValue().toString();
			} else {
				value = d.getComparison().getTestDetails().getValue().toString();
			}
			
		} catch (NullPointerException e) {
			value = "";
		}
		
		return value;
	}
	
	
	private String extractInfoFromPath(Path p, String type) {
		String value = "";
		
		try {
			if (type.equals("FILE")) {
				value = p.getFileName().toString();	
			} else {
				value = p.toString().replace(p.getFileName().toString(), "");
			}
			
		} catch (NullPointerException e) {
			value = "null";
		}
		
		return value;
		
	}
}
