package com.invixo.compare;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

import com.invixo.common.GeneralException;
import com.invixo.common.util.Logger;
import com.invixo.common.util.Util;

public class Comparer {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = Comparer.class.getName();
	
	private Path sourceFile;
	private long sourceFileSize = 0;
	private Path compareFile;
	private long compareFileSize = 0;

	private int compareSuccessCount = 0; // Max 1, but it is easier to summarize in IntegratedConfiguration afterwards 
	private int compareSkippedCount = 0; // Max 1, but it is easier to summarize in IntegratedConfiguration afterwards
	private double executionTimeSeconds = 0;
	
	private Map<String, String> diffsIgnoredByConfiguration = new HashMap<String, String>();
	private ArrayList<Difference> compareDifferences = new ArrayList<Difference>();
	private ArrayList<String> icoXPathExceptions = new ArrayList<String>();

	private CompareException ce;
	
	public Comparer(Path sourceFile, Path compareFile, ArrayList<String> icoXPathExceptions) {
		this.sourceFile = sourceFile;
		this.compareFile = compareFile;
		this.icoXPathExceptions = icoXPathExceptions;
	}

	
	/**
	 * Start Compare
	 * @throws GeneralException 
	 */
	public void start() {
		final String SIGNATURE = "start()";
		
		try {
			// Set file sizes for later reporting purposes
			setFileSizes(this.sourceFile, this.compareFile);
	
			// Prepare files for compare
			String sourceFileString = new String(Util.readFile(this.sourceFile.toString()));
			String compareFileString = new String(Util.readFile(this.compareFile.toString()));
	
			// Do compare
			Diff diff = compare(sourceFileString, compareFileString);
	
			// Add differences found for later reporting
			for (Difference d : diff.getDifferences()) {
				this.compareDifferences.add(d);
			}
	
			// Increment compare success for reporting purposes
			this.compareSuccessCount++;
		
		} catch (CompareException e) {
			this.compareSkippedCount++;
			this.ce = e;
		}
	}

	
	private void setFileSizes(Path sourceFile, Path compareFile) {
		final String SIGNATURE = "setFileSizes(Path, Path)";
			
			// Get size of source file
			byte[] sourcePayloadBytes = Util.readFile(sourceFile.toString());
			this.sourceFileSize = sourcePayloadBytes.length;
			logger.writeDebug(LOCATION, SIGNATURE, "Source file size (bytes): " + this.sourceFileSize);
			
			// Get size of target file
			byte[] comparePayloadBytes = Util.readFile(compareFile.toString());
			this.compareFileSize = comparePayloadBytes.length;
			logger.writeDebug(LOCATION, SIGNATURE, "Compare file size (bytes): " + this.compareFileSize);

	}


	private Diff compare(String sourceFileString, String compareFileString) throws CompareException {
		final String SIGNATURE = "compare(String, String)";

		try {

			logger.writeDebug(LOCATION, SIGNATURE, "---- Compare: start");

			// Set start timer
			long startTime = Util.getTime();

			// Compare string representations of source and compare payloads
			Diff diff = DiffBuilder
					.compare(sourceFileString)
					.withTest(compareFileString)
					.withDifferenceEvaluator(new CustomDifferenceEvaluator(this.icoXPathExceptions, this))
					.ignoreWhitespace()
					.normalizeWhitespace()
					.build();

			// Set end timer
			long endTime = Util.getTime();

			// Calculate execution time
			this.executionTimeSeconds = Util.measureTimeTaken(startTime, endTime);

			logger.writeDebug(LOCATION, SIGNATURE, "---- Compare: done");

			return diff;

		} catch (Exception e) {
			String msg = "Error during compare, please check compare files.\nError: " + e.getMessage(); 
			throw new CompareException(msg);
		}
	}


	
	/*====================================================================================
	 *------------- Getters and Setters
	 *====================================================================================*/
	public CompareException getCompareException() {
		return this.ce;
	}
	
	
	public ArrayList<String> getXPathExceptions() {
		return icoXPathExceptions;
	}
	
	
	public ArrayList<Difference> getCompareDifferences() {
		return this.compareDifferences;
	}
	
	
	void addDiffIgnored(String diffFound, String ignoreXPath) {
		this.diffsIgnoredByConfiguration.put(diffFound, ignoreXPath);
	}
	
	
	public Map<String, String> getDiffsIgnoredByConfiguration() {
		return this.diffsIgnoredByConfiguration;
	}
	
	
	public int getCompareSuccess() {
		return this.compareSuccessCount;
	}
	
	
	public int getCompareSkipped() {
		return this.compareSkippedCount;
	}
	
	
	public Path getSourceFile() {
		return this.sourceFile;
	}
	
	
	public Path getCompareFile() {
		return this.compareFile;
	}
	
	
	public double getExecutionTimeSeconds() {
		return this.executionTimeSeconds;
	}

	
	public long getSourceFileSize() {
		return sourceFileSize;
	}

	
	public long getCompareFileSize() {
		return compareFileSize;
	}
}
