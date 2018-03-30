package com.dystify.kkdystrack.v2.model;

public class SongAlias 
{
	private String aliasID;
	private Song songAlias;

	public SongAlias() {}

	public String getAliasID() {
		return aliasID;
	}

	public Song getSongAlias() {
		return songAlias;
	}

	public void setAliasID(String aliasID) {
		this.aliasID = aliasID;
	}

	public void setSongAlias(Song songAlias) {
		this.songAlias = songAlias;
	}

}
