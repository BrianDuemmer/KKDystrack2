package com.dystify.kkdystrack.v2.model.queue;

import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;

import com.dystify.kkdystrack.v2.core.util.Util;
import com.dystify.kkdystrack.v2.dao.QueueDAO;
import com.dystify.kkdystrack.v2.model.QueueEntry;
import com.dystify.kkdystrack.v2.service.DBTask;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;


/**
 * Represents a Queue that will keep some amount of random selections
 * Pre-queued, which will autofill with randomly selected songs from the playlist
 * @author Duemmer
 *
 */
public class SimpleRandomSongQueue implements SongQueue 
{
	private static final String QUEUE_NAME = "SimpleRandom";
	private ObservableList<QueueEntry> queue;
	private QueueDAO queueDao;
	private int poolSize;
	
	private ExecutorService dbTaskQueue;
	
	private ReadOnlyIntegerWrapper queueSizeProp = new ReadOnlyIntegerWrapper(0);
	private ReadOnlyDoubleWrapper queueLengthProp = new ReadOnlyDoubleWrapper(0);
	private ReadOnlyDoubleWrapper rngInQueueProp = new ReadOnlyDoubleWrapper(1);
	
	
	public SimpleRandomSongQueue(QueueDAO queueDao, int poolSize) {
		this.queueDao = queueDao;
		this.queue = FXCollections.observableArrayList();
		this.poolSize = poolSize;
		
		// we just care about size changes (namely removes), so only go off of the list size
		queue.addListener((ListChangeListener.Change<? extends QueueEntry> c) -> {
			c.next();
			if(c.wasRemoved() && c.getList().size() < poolSize) // restock if we're low
				addSongs(poolSize - c.getList().size());
			queueSizeProp.set(c.getList().size());
			queueLengthProp.set(getQueueLength());
		});
	}
	
	
	
	
	
	/**
	 * Adds {@code numSongs} randomly chosen songs to the queue. Does so in a new thread.
	 * @param numSongs
	 */
	private void addSongs(int numSongs) {
		dbTaskQueue.submit(new DBTask(true, "Add Songs to Queue \"" +QUEUE_NAME+ "\"", () -> { 
			List<QueueEntry> added = queueDao.getSimpleRandomQueueEntries(numSongs); 
			Platform.runLater(() -> { 
				queue.addAll(added); 
			});
		}));
	}
	
	
	
	@PostConstruct
	private void initQueue() {
		// Add some songs to start off
		addSongs(poolSize);
	}
	
	
	
	
	
	
	@Override public QueueEntry popNextSong() {
		if(queue.size() > 0)
			return queue.remove(0);
		return null;
	}

	@Override public QueueEntry peekNextSong() {
		if(queue.size() > 0)
			return queue.get(0);
		return null;
	}

	@Override public String getQueueId() {
		return QUEUE_NAME;
	}

	@Override public ObservableList<QueueEntry> getQueue() {
		return queue;
	}


	private double getQueueLength() {
		double len = 0;
		for(QueueEntry q : queue)
			len += q.getSong().getSongLength();
		return len;
	}
	
	
	
	
	@Override public ReadOnlyIntegerProperty queueSizeProperty() {
		return queueSizeProp.getReadOnlyProperty();
	}

	
	@Override public ReadOnlyDoubleProperty queueLengthProperty() {
		return queueLengthProp.getReadOnlyProperty();
	}

	
	@Override public ReadOnlyDoubleProperty rngInQueuePercentProperty() {
		return rngInQueueProp.getReadOnlyProperty();
	}
	
	
	
	@Override public void updateIndicatorProps() {
		queueLengthProp.set(getQueueLength());
		queueSizeProp.set(queue.size());
		rngInQueueProp.set(1);
	}





	public int getPoolSize() {
		return poolSize;
	}





	@Override public String getQueueDispName() {
		return QUEUE_NAME;
	}





	public void setDbTaskQueue(ExecutorService dbTaskQueue) {
		this.dbTaskQueue = dbTaskQueue;
	}
}
















