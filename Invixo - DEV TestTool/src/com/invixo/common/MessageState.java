package com.invixo.common;

import java.nio.file.Path;

import com.invixo.common.util.Util;
import com.invixo.compare.Comparer;
import com.invixo.main.special.ComparisonCase;

public class MessageState {
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
	
	public MessageState(ComparisonCase entry, String sourceId, String targetId) {
		this.dateTime = Util.getTime();
		
		this.sourceFileOutputPath = entry.getSourcePathOut();
		this.targetFileOutputPath = entry.getTargetPathOut();
		
		this.sourceFileName = sourceId;
		this.targetFileName = targetId;
		
		this.sourceInjectId = sourceId;
		if (entry.getCompareType().equals(ComparisonCase.TYPE.ICO_2_ICO)) {
			this.targetInjectId = targetId;
		}
		
		this.sourceIcoName = entry.getSourceIco();
		this.targetIcoName = entry.getTargetIco();

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
}
