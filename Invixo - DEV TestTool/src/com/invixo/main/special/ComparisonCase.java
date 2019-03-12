package com.invixo.main.special;

import java.util.ArrayList;

import com.invixo.common.MessageState;

public class ComparisonCase {
	
	public static enum TYPE { ICO_2_ICO, ICO_2_FILE }
		
	private String baseFilePath 	= null;		// Base file path
	private int maxComparisons		= 0;		// Maximum number of source files to process
	
	private String sourcePathIn 	= null;		// Source file path to READ FIRST files from
	private String sourcePathOut 	= null;		// Source file path to WRITE extracted LAST messages to for later comparison
	private String sourceIco		= null;		// Source ICO name (ref to ICO Overview ICO name)
	
	private TYPE compareType 		= null;		// Defines what to compare source file with
	
	private String targetPathIn 	= null;		// Target file path to READ FIRST compare files to be injected from
	private String targetPathOut 	= null;		// Target file path containing LAST messsages
	private String targetIco		= null;		// Target ICO name (ref to ICO Overview ICO name). This is only for TYPE = ICO_2_ICO
	
	private ArrayList<MessageState> caseList = null;
	
	
	public String getSourcePathIn() {
		return sourcePathIn;
	}
	
	public void setSourcePathIn(String sourcePathIn) {
		this.sourcePathIn = sourcePathIn;
	}
	
	public String getSourcePathOut() {
		return sourcePathOut;
	}
	
	public void setSourcePathOut(String sourcePathOut) {
		this.sourcePathOut = sourcePathOut;
	}
	
	public String getSourceIco() {
		return sourceIco;
	}
	
	public void setSourceIco(String sourceIco) {
		this.sourceIco = sourceIco;
	}
	
	public TYPE getCompareType() {
		return compareType;
	}
	
	public void setCompareType(TYPE compareType) {
		this.compareType = compareType;
	}
	
	public String getTargetPathIn() {
		return targetPathIn;
	}
	
	public void setTargetPathIn(String targetPathIn) {
		this.targetPathIn = targetPathIn;
	}
	
	public String getTargetPathOut() {
		return targetPathOut;
	}
	
	public void setTargetPathOut(String targetPathOut) {
		this.targetPathOut = targetPathOut;
	}
	
	public String getTargetIco() {
		return targetIco;
	}
	
	public void setTargetIco(String targetIco) {
		this.targetIco = targetIco;
	}

	public String getBaseFilePath() {
		return baseFilePath;
	}

	public void setBaseFilePath(String baseFilePath) {
		this.baseFilePath = baseFilePath;
	}

	public int getMaxComparisons() {
		return maxComparisons;
	}

	public void setMaxComparisons(int maxComparisons) {
		this.maxComparisons = maxComparisons;
	}

	public ArrayList<MessageState> getCaseList() {
		return caseList;
	}

	public void setCaseList(ArrayList<MessageState> caseList) {
		this.caseList = caseList;
	}
	
}
