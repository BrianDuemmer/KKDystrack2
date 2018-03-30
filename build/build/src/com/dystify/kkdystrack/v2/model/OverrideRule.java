package com.dystify.kkdystrack.v2.model;


/**
 * Override rules are the set of constraints that determine song cost and cooldown. Using 
 * a directory style hierarchy of songs, different sets of rules, "override" functionality
 * for cost calculation, with more specific rules taking precedence over more generic ones.
 * <p/> 
 * What these rules do is provide a framework to determine the amount of "points any given
 * song has. as the number of points goes higher, the more of that songs and songs like it
 * have played recently, and thus cost should go up, and a song with above a certain number 
 * of points should go on cooldown.
 * @author Duemmer
 *
 */
public class OverrideRule 
{
	private String overrideId;
	private double songPts; // points for a direct match
	private double ostPts; // points for an OST match
	private double franchisePts; // same franchise
	private double timeChecked; // seconds of time to analyze songs from
	private int id; // unique id
	
	
	
	
	public OverrideRule() {
	}
	
	
	
	public String getOverrideId() {
		return overrideId;
	}
	public double getSongPts() {
		return songPts;
	}
	public double getOstPts() {
		return ostPts;
	}
	public double getFranchisePts() {
		return franchisePts;
	}
	public double getTimeChecked() {
		return timeChecked;
	}
	public int getId() {
		return id;
	}
	public void setOverrideId(String overrideId) {
		this.overrideId = overrideId;
	}
	public void setSongPts(double songPts) {
		this.songPts = songPts;
	}
	public void setOstPts(double ostPts) {
		this.ostPts = ostPts;
	}
	public void setFranchisePts(double franchisePts) {
		this.franchisePts = franchisePts;
	}
	public void setTimeChecked(double timeChecked) {
		this.timeChecked = timeChecked;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	
	/**
	 * Checks if this rule corresponds to the root override rule; that is, overrideID is an 
	 * empty string. This is necessary because there must be added restrictions on manipulating
	 * the root override, as inadvertadly removing or damaging it could break the entire point
	 * system
	 * @return
	 */
	public boolean isRootOverride() {
		return overrideId.trim().isEmpty();
	}
	
	
	/**
	 * Returns two if obj is an OverrideRule and its overrideID matches this one's override id
	 */
	@Override public boolean equals(Object obj) {
		return obj instanceof OverrideRule && ((OverrideRule)obj).overrideId.equalsIgnoreCase(overrideId);
	}
}
