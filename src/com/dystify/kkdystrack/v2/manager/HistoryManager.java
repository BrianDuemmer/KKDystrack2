package com.dystify.kkdystrack.v2.manager;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;

import com.dystify.kkdystrack.v2.core.util.Util;
import com.dystify.kkdystrack.v2.dao.QueueDAO;
import com.dystify.kkdystrack.v2.dao.SongDAO;
import com.dystify.kkdystrack.v2.model.QueueEntry;
import com.dystify.kkdystrack.v2.service.DBTask;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;


/**
 * Manages writing QueueEntries to the play history as they get played, 
 * and possibly more in the future
 * @author Duemmer
 *
 */
public class HistoryManager extends AbstractManager 
{
	private QueueDAO queueDao;
	private SongDAO songDao;
	private ObjectProperty<QueueEntry> nowPlayingProperty;
	private boolean ignoreHistory;
	private ExecutorService dbTaskQueue;
	
	public HistoryManager(QueueDAO queueDao, SongDAO songDao) {
		this.queueDao = queueDao;
		this.songDao = songDao;
		this.nowPlayingProperty = new SimpleObjectProperty<>();
		
		nowPlayingProperty.addListener((obs, oldVal, newVal) -> {
			if(!ignoreHistory)
				dbTaskQueue.submit(new DBTask("Write to playlist history", () -> {
					queueDao.writeToHistory(Arrays.asList(newVal));
					songDao.calculatePoints(Arrays.asList(newVal.getSong()));
				}));
		});
	}

	
	public boolean ShouldignoreHistory() {
		return ignoreHistory;
	}

	
	public void setignoreHistory(boolean ignoreHistory) {
		this.ignoreHistory = ignoreHistory;
	}


	public ObjectProperty<QueueEntry> getNowPlayingProperty() {
		return nowPlayingProperty;
	}


	public void setDbTaskQueue(ExecutorService dbTaskQueue) {
		this.dbTaskQueue = dbTaskQueue;
	}
}
