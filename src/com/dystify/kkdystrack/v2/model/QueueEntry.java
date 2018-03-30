package com.dystify.kkdystrack.v2.model;

import java.util.Date;

public class QueueEntry 
{
	private Song song;
	private Viewer viewer;
	private Date timeRequested;
	private int entry_id;
	private String owningQueue;



	// Getters / Setters
	public Song getSong() {
		return song;
	}
	public void setSong(Song song) {
		this.song = song;
	}
	public Viewer getViewer() {
		return viewer;
	}
	public void setViewer(Viewer viewer) {
		this.viewer = viewer;
	}
	public Date getTimeRequested() {
		return timeRequested;
	}
	public void setTimeRequested(Date timeRequested) {
		this.timeRequested = timeRequested;
	}



	@Override
	public boolean equals(Object obj) {
		if(obj != null) {
			try {
				QueueEntry other = (QueueEntry) obj;
				boolean eq = timeRequested.getTime() == other.timeRequested.getTime() &&
						song.getSongId().equalsIgnoreCase(other.song.getSongId()) &&
						owningQueue.equals(other.owningQueue);
				return eq;
			} catch(ClassCastException e) {}
		}
		return false;
	}



	public int getEntry_id() {
		return entry_id;
	}


	public void setEntry_id(int entry_id) {
		this.entry_id = entry_id;
	}


	public String getOwningQueue() {
		return owningQueue;
	}


	public void setOwningQueue(String owningQueue) {
		this.owningQueue = owningQueue;
	}



}
