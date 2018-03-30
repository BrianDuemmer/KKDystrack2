package com.dystify.kkdystrack.v2.service;

import com.dystify.kkdystrack.v2.model.QueueEntry;

/**
 * specifies what the {@link MusicPlayer} should do in the event that its queue is empty when 
 * a new song needs to be added. In effect, it is simply an alternative backup source of songs.
 * If {@link #onQueueEmpty()} returns null, no song will be played and the player will pause.
 * @author Duemmer
 *
 */
@FunctionalInterface public interface QueueEmptyCallback {
	public QueueEntry onQueueEmpty();
}
