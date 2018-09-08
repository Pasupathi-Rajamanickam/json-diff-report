package com.nexttimespace.utilities.diffbox;

import java.io.Serializable;

public class DiffResponse implements Serializable{
	private static final long serialVersionUID = 3451985135067844197L;
	private String sourceTag;
	private String compareWithTag;
	private String sourceValue;
	private String compareWithValue;
	private boolean status;
	public String getSourceTag() {
		return sourceTag;
	}
	public void setSourceTag(String sourceTag) {
		this.sourceTag = sourceTag;
	}
	public String getCompareWithTag() {
		return compareWithTag;
	}
	public void setCompareWithTag(String compareWithTag) {
		this.compareWithTag = compareWithTag;
	}
	public String getSourceValue() {
		return sourceValue;
	}
	public void setSourceValue(String sourceValue) {
		this.sourceValue = sourceValue;
	}
	public String getCompareWithValue() {
		return compareWithValue;
	}
	public void setCompareWithValue(String compareWithValue) {
		this.compareWithValue = compareWithValue;
	}
	public boolean isStatus() {
		return status;
	}
	public void setStatus(boolean status) {
		this.status = status;
	}
}
