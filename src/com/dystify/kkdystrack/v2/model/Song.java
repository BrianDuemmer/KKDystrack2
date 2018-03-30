package com.dystify.kkdystrack.v2.model;

import java.util.Date;

/**
 * Represents a single entry in the playlist, a song. Each song contains the following fields:
 * <ul>
 * <li>song_name</li>
 * <li>ost_name</li>
 * <li>song_franchise</li>
 * <li>song_length (in seconds)</li>
 * <li>song_id (path to song file on filesystem)</li>
 * <li>song_points (represent how much song has played recently)</li>
 * </ul>
 * 
 * Note that songs are distinct from queue entries in that songs aren't bound to a queue, requester,
 * timestamp, etc. Each Song serves as a link to a physical song on the filesystem
 * @author Duemmer
 *
 */
public class Song 
{
	/** When rating / play info can't be obtained, use this as a placeholder */
	public static final int DEFAULT_VAL = -1;
	
	private String songName;
	private String ostName;
	private String songFranchise;
	private String songId;
	private double songLength;
	private double songPoints;
	private double songCost;
	private double ratingPct;
	private int ratingNum;
	private Date lastPlay;
	private int timesPlayed;
	
	private OverrideRule costRule;
	
	
	public Song() {
		this.ratingNum = DEFAULT_VAL;
		this.ratingPct = DEFAULT_VAL;
		this.songPoints = DEFAULT_VAL;
		this.songCost =  DEFAULT_VAL;
		this.timesPlayed = DEFAULT_VAL;
		this.lastPlay = null;
		this.costRule = new OverrideRule();
		
		this.songId = "";
		this.songName = "";
		this.ostName = "";
		this.songFranchise = "";
	}
	
	
	/**@param includeSongID if true, appends the song ID to the output. Useful for logging.
	 * @return a typical display text for a song, in ost - songName format
	 */
	public String getDispText(boolean includeSongID) {
		String s = ostName +" - "+ songName;
		if(includeSongID)
			s += " (" +songId+ ")";
		return s;
	}
	
	
	
	/**
	 * gets a rating string in the following format: 
	 * <code>#.# / 5 [# vote(s)]<code>, or <code>No votes</code> if the rating isn't set
	 * @return
	 */
	public String getRatingTxt() {
		String fmtStr = "%1.1f / 5 [%d vote";

		// apply singular / plural changes to text
		if(ratingNum > 0) {
			if(ratingNum > 1)
				fmtStr += "s]";
			else if(ratingNum == 1)
				fmtStr += "]";
			return String.format(fmtStr, ratingPct*5, ratingNum);
		}
		return "No votes";
	}
	
	
	
	public String getSongName() { return songName; }
	public void setSongName(String songname) { this.songName = songname; }
	
	public String getOstName() { return ostName; }
	public void setOstName(String ostName) { this.ostName = ostName; }
	
	public String getSongFranchise() { return songFranchise; }
	public void setSongFranchise(String songFranchise) { this.songFranchise = songFranchise; }
	
	public String getSongId() { return songId; }
	public void setSongId(String songId) { this.songId = songId; }
	
	public double getSongLength() { return songLength; }
	public void setSongLength(double songLength) { this.songLength = songLength; }
	
	public double getSongPoints() { return songPoints; }
	public void setSongPoints(double songPoints) { this.songPoints = songPoints; }
	
	public double getRatingPct() { return ratingPct; }
	public void setRatingPct(double ratingPct) { this.ratingPct = ratingPct; }
	
	public int getRatingNum() { return ratingNum; }
	public void setRatingNum(int ratingNum) { this.ratingNum = ratingNum; }
	
	
	/**
	 * Returns true if obj is a Song and its songID matches this one's songID
	 */
	@Override public boolean equals(Object obj) {
		return obj instanceof Song && ((Song)obj).songId.equalsIgnoreCase(songId);
	}



	public Date getLastPlay() {
		return lastPlay;
	}



	public int getTimesPlayed() {
		return timesPlayed;
	}



	public void setLastPlay(Date lastPlay) {
		this.lastPlay = lastPlay;
	}



	public void setTimesPlayed(int timesPlayed) {
		this.timesPlayed = timesPlayed;
	}


	public double getSongCost() {
		return songCost;
	}


	public OverrideRule getCostRule() {
		return costRule;
	}


	public void setCostRule(OverrideRule costRule) {
		this.costRule = costRule;
	}


	public void setSongCost(double songCost) {
		this.songCost = songCost;
	}
	
}
