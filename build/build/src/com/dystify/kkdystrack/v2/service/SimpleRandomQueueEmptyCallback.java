package com.dystify.kkdystrack.v2.service;

import com.dystify.kkdystrack.v2.model.QueueEntry;
import com.dystify.kkdystrack.v2.model.queue.SimpleRandomSongQueue;



/**
 * {@link QueueEmptyCallback} for the {@link MusicPlayer} that will attempt to 
 * queue a randomly chosen song when the queue
 * @author Duemmer
 *
 */
public class SimpleRandomQueueEmptyCallback implements QueueEmptyCallback 
{
	private SimpleRandomSongQueue randQueue;
	

	public SimpleRandomQueueEmptyCallback(SimpleRandomSongQueue randQueue) {
		this.randQueue = randQueue;
	}


	@Override public QueueEntry onQueueEmpty() {
		return randQueue.popNextSong();
	}

}
