package com.dystify.kkdystrack.v2.manager;

import com.dystify.kkdystrack.v2.model.OutputFile;
import com.dystify.kkdystrack.v2.model.OutputFileWithFollower;

public class FileManager extends AbstractManager 
{
	private OutputFileWithFollower nowPlayingRating;
	private OutputFile songComment;
	private OutputFile requestStatus;
	private OutputFile requestedBy;
	
	// formatting parameters
	private String requestStatusOpenFmt;
	private String requestStatusClosedFmt;
	private String songCommentFmt;
	private int maxSongLength;
	
	
	
	
	
	public OutputFile getNowPlayingRating() {
		return nowPlayingRating;
	}
	public OutputFile getSongComment() {
		return songComment;
	}
	public OutputFile getRequestStatus() {
		return requestStatus;
	}
	public void setNowPlayingRating(OutputFileWithFollower nowPlayingRating) {
		this.nowPlayingRating = nowPlayingRating;
	}
	public void setSongComment(OutputFile songComment) {
		this.songComment = songComment;
	}
	public void setRequestStatus(OutputFile requestStatus) {
		this.requestStatus = requestStatus;
	}
	public OutputFile getRequestedBy() {
		return requestedBy;
	}
	public void setRequestedBy(OutputFile requestedBy) {
		this.requestedBy = requestedBy;
	}
	
	
	
	public String getRequestStatusOpenFmt() {
		return requestStatusOpenFmt;
	}
	public String getRequestStatusClosedFmt() {
		return requestStatusClosedFmt;
	}
	public void setRequestStatusOpenFmt(String requestStatusOpenFmt) {
		this.requestStatusOpenFmt = requestStatusOpenFmt;
	}
	public void setRequestStatusClosedFmt(String requestStatusClosedFmt) {
		this.requestStatusClosedFmt = requestStatusClosedFmt;
	}
	public int getMaxSongLength() {
		return maxSongLength;
	}
	public void setMaxSongLength(int maxSongLength) {
		this.maxSongLength = maxSongLength;
	}
	public String getSongCommentFmt() {
		return songCommentFmt;
	}
	public void setSongCommentFmt(String songCommentFmt) {
		this.songCommentFmt = songCommentFmt;
	}
}
