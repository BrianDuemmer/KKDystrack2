package com.dystify.kkdystrack.v2.model.queue;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dystify.kkdystrack.v2.core.exception.QueueNotFoundException;
import com.dystify.kkdystrack.v2.core.util.Util;
import com.dystify.kkdystrack.v2.dao.QueueDAO;
import com.dystify.kkdystrack.v2.manager.ViewerManager;
import com.dystify.kkdystrack.v2.model.QueueEntry;
import com.dystify.kkdystrack.v2.service.DBTask;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 * Represents a loosley ordered queue of songs to play. Each queue is 
 * backed by a dynamically generated table on the database, containing:
 * <ul>
 * 	<li>user_id(who queued it)</li>
 * 	<li>time_requested(when it was requested)</li>
 * 	<li>song_id(the song itself)</li>
 * 	<li>list_order(represents the order the songs should play)</li>
 * </ul>. For Display, only song name, ost name, song length, username, and time requested will be shown
 * @author Duemmer
 *
 */
public class StdSongQueue implements SongQueue
{
	private ObservableList<QueueEntry> queue;
	private String queueId;
	private String queueDispName;
	private boolean deleteOnEmpty;
	private QueueDAO queueDao;
	private ExecutorService dbTaskQueue;
	
	private ReadOnlyIntegerWrapper queueSizeProp = new ReadOnlyIntegerWrapper(0);
	private ReadOnlyDoubleWrapper queueLengthProp = new ReadOnlyDoubleWrapper(0);
	private ReadOnlyDoubleWrapper rngInQueueProp = new ReadOnlyDoubleWrapper(1);

	private Logger log = LogManager.getLogger(getClass());

	private Object updateLock = new Object();



	@SuppressWarnings("unchecked")
	public StdSongQueue(String queueName, String queueDispName,  ObservableList<QueueEntry> queue, QueueDAO queueDao) {
		this.queue = queue;
		this.queueDao = queueDao;
		this.queueId = queueName;
		this.queueDispName = queueDispName;

		// attach listener to call any appropriate database updates
		queue.addListener((ListChangeListener.Change<? extends QueueEntry> c) -> {
			while(c.next()) {
				// it's very dirty but since we expect only QueueEntries we should be fine
				final List<QueueEntry> added = (
						(List<QueueEntry>)(List<?>) c.getAddedSubList()
					).stream().collect(Collectors.toList());
				
				final int from = c.getFrom();
				final int to = c.getTo();
				final boolean wasPermutated = c.wasPermutated();
				final boolean wasUpdated = c.wasUpdated();
				final boolean wasAdded = c.wasAdded();
				final boolean wasRemoved = c.wasRemoved();
				
				int[] perm = new int[to - from];
				if(wasPermutated)
					for(int i=from; i<to; i++) 
						perm[i] = c.getPermutation(i);
				
				// loop through each registered change. Since updates incur
				// database penalties, they have to run in a different thread. These
				// threads must also be guaranteed to run in a FIFO, sequential style,
				// so put them in shared synchronized blocks
				dbTaskQueue.submit(new DBTask("queue_" +queueName+" update", () -> {
					synchronized (updateLock) {
						try {
							if(wasPermutated) 
								queueDao.handlePermutation(queueName, from, perm);
							else if(wasUpdated)
								queueDao.addToQueue(queueName, added, from, true);
							else if(wasRemoved)
								queueDao.removeFromQueue(queueName, from, to);
							else if(wasAdded)
								queueDao.addToQueue(queueName, added, from, false);
						} catch (QueueNotFoundException e) { log.error("No queue backing table found for queue_" +queueName, e); }
						Platform.runLater(() -> {
							queueLengthProp.set(getQueueLength());
							rngInQueueProp.set(getRngInQueue());
							queueSizeProp.set(queue.size());
						});
					}
				}));
			}
		});
	}
	
	
	@Override public void updateIndicatorProps() {
		queueSizeProp.set(queue.size());
		queueLengthProp.set(getQueueLength());
		rngInQueueProp.set(getRngInQueue());
	}


	
	@Override
	public ObservableList<QueueEntry> getQueue() {
		return queue;
	}


	@Override
	public String getQueueId() {
		return queueId;
	}

	


	private double getQueueLength() {
		double len = 0;
		for(QueueEntry q : queue)
			len += q.getSong().getSongLength();
		return len;
	}
	
	
	/**
	 * Calculates the percentage of the current queue that is RNG, by song lengths
	 * (as opposed to by number of songs). Goes by the user that requested the song 
	 * being a predefined RNG user
	 * @return
	 */
	private double getRngInQueue() {
		double rngLen = 0;
		for(QueueEntry q : queue)
			if(q.getViewer().getUserId().equalsIgnoreCase(ViewerManager.getDystrackUserId()))
				rngLen += q.getSong().getSongLength();
		return rngLen / getQueueLength();
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
	
	



	public boolean getDeleteOnEmpty() {
		return deleteOnEmpty;
	}



	public void setDeleteOnEmpty(boolean deleteOnEmpty) {
		this.deleteOnEmpty = deleteOnEmpty;
	}



	public void setQueueDao(QueueDAO queueDao) {
		this.queueDao = queueDao;
	}



	@Override
	public QueueEntry popNextSong() {
		if(queue.size() > 0)
			return queue.remove(0);
		else
			return null;
	}



	@Override
	public QueueEntry peekNextSong() {
		if(queue.size() > 0)
			return queue.get(0);
		else
			return null;
	}


	public String getQueueDispName() {
		return queueDispName;
	}


	public void setDbTaskQueue(ExecutorService dbTaskQueue) {
		this.dbTaskQueue = dbTaskQueue;
	}
}
