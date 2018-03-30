package com.dystify.kkdystrack.v2.service;

import com.dystify.kkdystrack.v2.core.util.Util;
import com.dystify.kkdystrack.v2.model.QueueEntry;
import com.dystify.kkdystrack.v2.model.queue.SongQueue;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;


/** 
 * Represents a link to a music player controller
 * @author Duemmer
 *
 */
public interface MusicPlayer 
{
	/** Sets the music player to a fresh, initialized state, regardless of whether it is already active or not */
	public void reset();
	
	/** initializes the player into a clean starting state */
	public void startPlayback();
	
	/** Makes sure the player is in a paused state. If it is already paused, this has no effect. */
	public void pausePlayback();
	
	/** Makes sure the player is in a playing state. If it is already playing, this has no effect. */
	public void playPlayback();
	
	/** Ends the song and immediately progresses to the next one in queue, along with popping */
	public void skipSong();
	
	/** Sets the queue to use to pull songs from */
	public void setQueueProperty(ObjectProperty<SongQueue> queue);
	
	/** Returns the currently playing song */
	public QueueEntry getNowPlaying();
	
	/** gets the amount of seconds left for the song that is currently playing, or -1 to indicate that no song is playing */
	public ReadOnlyDoubleProperty currentSongTimeRemaining();
	
	/** returns the current status property of the music player */
	public ReadOnlyObjectProperty<MusicPlayerState> playStatusProperty();
	
	public void setLeadTime(double leadTime);
	
	
	/**
	 * Formats a string containing a descriptive now playing banner for the song, formatted like
	 * <p/> {@code Now Playing: ost_name - song_name  -  time_at/song_len}
	 * @return
	 */
	public default String fmtNowPlayingMsg() {
		StringBuilder sb = new StringBuilder();
		if(playStatusProperty().get() != MusicPlayerState.STOPPED) {
			sb.append("Now Playing: ");
			sb.append(getNowPlaying().getSong().getDispText(false));
			sb.append("  -  ");
			sb.append(Util.intSecondsToTimeString(getNowPlaying().getSong().getSongLength() - currentSongTimeRemaining().get()));
			sb.append('/');
			sb.append(Util.intSecondsToTimeString(getNowPlaying().getSong().getSongLength()));
		} else
			sb.append("Not playing anything!");
		return sb.toString();
	}

	/** Contains a nicely formated display message for the currently playing song */
	public ReadOnlyStringProperty nowPlayingTextProperty();

	/** references the QueueEntry that is actively playing in the MusicPlayer*/
	public ReadOnlyObjectProperty<QueueEntry> nowPlayingProperty();

	public void setQueueEmptyCallback(QueueEmptyCallback queueEmptyCallback);
}
