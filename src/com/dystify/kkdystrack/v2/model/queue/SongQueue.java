package com.dystify.kkdystrack.v2.model.queue;

import com.dystify.kkdystrack.v2.model.QueueEntry;

import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.collections.ObservableList;

/**
 * Represents A queue of songs that can be displayed on the GUI, and backed either by a physical
 * medium of storage, Random selector, etc. 
 * @author Duemmer
 *
 */
public interface SongQueue 
{
	/** Returns the next song in the queue and removes it, or returns null if the song isn't found*/
	public QueueEntry popNextSong();
	
	/** Returns the next song in the queue but does not remove it, or returns null if the song isn't found*/
	public QueueEntry peekNextSong();
	
	/** returns the unique identifier for this queue */
	public String getQueueId();
	
	/** Returns the display name for this queue. Unlike queueId, it allows arbitrary text / characters to be present */
	public String getQueueDispName();
	
	/** Returns a reference to the list containing the queue contents. Should not return null */
	public ObservableList<QueueEntry> getQueue();
	
	/** Returns the number of songs in the queue */
	public ReadOnlyIntegerProperty queueSizeProperty();
	
	/** Returns the sum of all the song lengths in the queue */
	public ReadOnlyDoubleProperty queueLengthProperty();
	
	/** Returns the percentage of this queue that is composed of RNG */
	public ReadOnlyDoubleProperty rngInQueuePercentProperty();

	/** Forces an update on the 3 Read only indicator properties (queue size, length, and %rng), and should also recalculate those values */
	public void updateIndicatorProps();
}
