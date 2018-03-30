package com.dystify.kkdystrack.v2.model;


/**
 * Acts as a wrapper for a value in General Settings, which can be stored as either
 * a double or a String, that can take on either one.
 * @author Duemmer
 *
 */
public class SettingVal {
	private double numericVal = 0;
	private String stringVal = null;
	private boolean isNumeric;
	public static final SettingVal defaultVal = new SettingVal(0, "");
	
	
	public SettingVal(double val) {
		isNumeric = true;
		numericVal = val;
	}
	
	
	public SettingVal(String val) {
		isNumeric = false;
		stringVal = val;
	}
	
	
	
	public SettingVal(boolean val) {
		isNumeric = true;
		numericVal = val ? 1 : 0;
	}
	
	
	


	/**
	 * @param numericVal
	 * @param stringVal
	 */
	public SettingVal(double numericVal, String stringVal) {
		this.numericVal = numericVal;
		this.stringVal = stringVal;
		isNumeric = true; // technically the numeric value is set, so it's true
	}


	public double getNumericVal() {
		return numericVal;
	}


	public String getStringVal() {
		return stringVal;
	}
	
	
	public boolean getBooleanVal() {
		return numericVal != 0;
	}
	


	public boolean isNumeric() {
		return isNumeric;
	}
}
