package com.dystify.kkdystrack.v2.manager;

import java.util.Arrays;

import com.dystify.kkdystrack.v2.core.util.Util;
import com.dystify.kkdystrack.v2.dao.QueueDAO;
import com.dystify.kkdystrack.v2.model.QueueEntry;

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
	private ObjectProperty<QueueEntry> nowPlayingProperty;
	private boolean ignoreHistory;
	
	public HistoryManager(QueueDAO queueDao) {
		this.queueDao = queueDao;
		this.nowPlayingProperty = new SimpleObjectProperty<>();
		
		nowPlayingProperty.addListener((obs, oldVal, newVal) -> {
			if(!ignoreHistory)
				Util.runNewDaemon("foo", () -> {
					queueDao.writeToHistory(Arrays.asList(newVal));
				});
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
}
