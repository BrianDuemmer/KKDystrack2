package com.dystify.kkdystrack.v2.model;

import java.util.Date;

public class Viewer 
{
	private String username;
	private String userId = "DEFAULT_USER_ID";
	private double rupees;
	private String favoriteSong;
	private boolean isBlacklisted;
	private boolean isAdmin;
	private double rupeeDiscount;
	private int freeRequests;
	private int loginBonusCount;
	private String staticRank;
	private String watchtimeRank;
	private Date birthday;
	private Date lastbirthdayWithdraw;
	private String songOnHold;
	private String note;
	
	
	
	
	
	
	// Getters / Setters
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public double getRupees() {
		return rupees;
	}
	public void setRupees(double rupees) {
		this.rupees = rupees;
	}
	public String getFavoriteSong() {
		return favoriteSong;
	}
	public void setFavoriteSong(String favoriteSong) {
		this.favoriteSong = favoriteSong;
	}
	public boolean isBlacklisted() {
		return isBlacklisted;
	}
	public void setBlacklisted(boolean isBlacklisted) {
		this.isBlacklisted = isBlacklisted;
	}
	public boolean isAdmin() {
		return isAdmin;
	}
	public void setAdmin(boolean isAdmin) {
		this.isAdmin = isAdmin;
	}
	public double getRupeeDiscount() {
		return rupeeDiscount;
	}
	public void setRupeeDiscount(double rupeeDiscount) {
		this.rupeeDiscount = rupeeDiscount;
	}
	public int getFreeRequests() {
		return freeRequests;
	}
	public void setFreeRequests(int freeRequests) {
		this.freeRequests = freeRequests;
	}
	public int getLoginBonusCount() {
		return loginBonusCount;
	}
	public void setLoginBonusCount(int loginBonusCount) {
		this.loginBonusCount = loginBonusCount;
	}
	public String getStaticRank() {
		return staticRank;
	}
	public void setStaticRank(String staticRank) {
		this.staticRank = staticRank;
	}
	public String getWatchtimeRank() {
		return watchtimeRank;
	}
	public void setWatchtimeRank(String watchtimeRank) {
		this.watchtimeRank = watchtimeRank;
	}
	public Date getBirthday() {
		return birthday;
	}
	public void setBirthday(Date birthday) {
		this.birthday = birthday;
	}
	public Date getLastbirthdayWithdraw() {
		return lastbirthdayWithdraw;
	}
	public void setLastbirthdayWithdraw(Date lastbirthdayWithdraw) {
		this.lastbirthdayWithdraw = lastbirthdayWithdraw;
	}
	public String getSongOnHold() {
		return songOnHold;
	}
	public void setSongOnHold(String songOnHold) {
		this.songOnHold = songOnHold;
	}
	public String getNote() {
		return note;
	}
	public void setNote(String note) {
		this.note = note;
	}
	
	
	
	
	/**
	 * Returns true if obj is a Viewer and its userID matches this one's userID
	 */
	@Override public boolean equals(Object obj) {
		return obj instanceof Viewer && ((Viewer)obj).userId.equalsIgnoreCase(userId);
	}
}
