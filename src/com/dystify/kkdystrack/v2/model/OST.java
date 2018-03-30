package com.dystify.kkdystrack.v2.model;

import java.util.List;

public class OST {
	private String ostName;
	private List<Song> songs;
	
	public OST(String ostName) {
		this.ostName = ostName;
	}

	public String getOstName() {
		return ostName;
	}

	public List<Song> getSongs() {
		return songs;
	}

	public void setOstName(String ostName) {
		this.ostName = ostName;
	}

	public void setSongs(List<Song> songs) {
		this.songs = songs;
	}

	@Override
	public String toString() {
		return ostName;
	}
	
	
	

}
