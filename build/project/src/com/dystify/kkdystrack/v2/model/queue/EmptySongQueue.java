package com.dystify.kkdystrack.v2.model.queue;

import com.dystify.kkdystrack.v2.model.QueueEntry;

import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Represents a dummy queue that is perpetually empty. Useful as 
 * a default value
 * @author Duemmer
 *
 */
public class EmptySongQueue implements SongQueue 
{
	private static final String QUEUE_NAME = "EMPTY_QUEUE";
	
	private ObservableList<QueueEntry> emptyList = FXCollections.observableArrayList();
	private ReadOnlyIntegerWrapper queueSizeProp = new ReadOnlyIntegerWrapper(0);
	private ReadOnlyDoubleWrapper queueLengthProp = new ReadOnlyDoubleWrapper(0);
	private ReadOnlyDoubleWrapper rngInQueueProp = new ReadOnlyDoubleWrapper(-1);
	

	@Override
	public QueueEntry popNextSong() {
		return null;
	}

	@Override
	public QueueEntry peekNextSong() {
		return null;
	}

	@Override
	public String getQueueId() {
		return QUEUE_NAME;
	}

	@Override
	public ObservableList<QueueEntry> getQueue() {
		return emptyList;
	}

	
	@Override
	public ReadOnlyIntegerProperty queueSizeProperty() {
		return queueSizeProp.getReadOnlyProperty();
	}

	
	@Override
	public ReadOnlyDoubleProperty queueLengthProperty() {
		return queueLengthProp.getReadOnlyProperty();
	}

	
	@Override
	public ReadOnlyDoubleProperty rngInQueuePercentProperty() {
		return rngInQueueProp.getReadOnlyProperty();
	}
	
	
	@Override public void updateIndicatorProps() {
		queueSizeProp.set(0);
		queueLengthProp.set(0);
		rngInQueueProp.set(-1);
	}

	@Override
	public String getQueueDispName() {
		return QUEUE_NAME;
	}
	
	

}
















