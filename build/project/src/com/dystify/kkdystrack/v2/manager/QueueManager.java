package com.dystify.kkdystrack.v2.manager;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;

import com.dystify.kkdystrack.v2.core.event.types.QueueListChangedEvent;
import com.dystify.kkdystrack.v2.core.event.types.QueueUpdatedEvent;
import com.dystify.kkdystrack.v2.core.exception.QueueNotFoundException;
import com.dystify.kkdystrack.v2.core.util.Util;
import com.dystify.kkdystrack.v2.dao.QueueDAO;
import com.dystify.kkdystrack.v2.model.QueueEntry;
import com.dystify.kkdystrack.v2.model.Song;
import com.dystify.kkdystrack.v2.model.queue.EmptySongQueue;
import com.dystify.kkdystrack.v2.model.queue.SimpleRandomSongQueue;
import com.dystify.kkdystrack.v2.model.queue.SongQueue;
import com.dystify.kkdystrack.v2.model.queue.StdSongQueue;
import com.dystify.kkdystrack.v2.service.DBTask;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Menu;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;




/**
 * Keeps track of all existing queues, their metrics, and managing 
 * the Queues on the UI. 
 * @author Duemmer
 *
 */
public class QueueManager extends AbstractManager
{
	private Logger log = LogManager.getLogger(getClass());
	private ExecutorService dbTaskQueue;
	
	@Autowired private ViewerManager viewerManager;

	private QueueDAO queueEntryDao;
	private TableView<QueueEntry> queueTbl;
	private ObjectProperty<SongQueue> activeQueue;
	private Map<String, String> allQueueIDs; // list of all database queues, namely, StdSongQueues. Does not include SimpleRandom, EmptyQueue, etc.
	private Menu activeQueueMenu;
	private int initialMenuSize;
	
	// carry through properties from the individual SongQueues
	private ReadOnlyIntegerWrapper queueSizeProp = new ReadOnlyIntegerWrapper(0);
	private ReadOnlyDoubleWrapper queueLengthProp = new ReadOnlyDoubleWrapper(0);
	private ReadOnlyDoubleWrapper rngInQueueProp = new ReadOnlyDoubleWrapper(-1);
	
	private ReadOnlyStringWrapper queueNameProp = new ReadOnlyStringWrapper("");
	
	
	private SimpleRandomSongQueue randomQueue;


	public QueueManager() {
		activeQueue = new SimpleObjectProperty<>();
		activeQueue.set(new EmptySongQueue());
		
		// add listener to rebind new queue to queue table
		activeQueue.addListener((observable, oldValue, newValue) -> {
//			newValue.getQueue().addListener((ListChangeListener.Change<? extends QueueEntry> c) -> {
//				c.next();
				queueTbl.itemsProperty().set(newValue.getQueue());
//			});
			
			// rebind carry through properties
			rngInQueueProp.unbind();
			rngInQueueProp.bind(newValue.rngInQueuePercentProperty());
			
			queueLengthProp.unbind();
			queueLengthProp.bind(newValue.queueLengthProperty());
			
			queueSizeProp.unbind();
			queueSizeProp.bind(newValue.queueSizeProperty());
			
			queueNameProp.set(newValue.getQueueId());
			
			// force an update, to reflect the new values
			newValue.updateIndicatorProps();
		});
	}
	
	
	
	/**
	 * Convenience method to add the specified songs to the current queue
	 * @param songs
	 * @return true if the songs were able to be written, false otherwise.
	 * Currently only marks a failure if the activeQueue property isn't set
	 */
	public boolean addSongsToQueue(List<Song> songs) {
		SongQueue currQueue = activeQueue.get();
		if(currQueue != null) {
			List<QueueEntry> qe = queueEntryDao.songToQueueEntry(songs, viewerManager.dystifyzerUser(), currQueue.getQueueId());
			currQueue.getQueue().addAll(qe);
			return true;
		}
		return false;
	}
	
	
	
	
	/**
	 * Creates a new database queue using a display name. It will calculate an appropriate ID for the queue, and
	 * makes sure the queue isn't there already, and the generated ID is non-empty
	 * @param name
	 * @param deleteOnEmpty
	 * @return
	 * @throws QueueNotFoundException if the queue was attempted to be created, but failed to be read back
	 */
	public StdSongQueue createDatabaseQueue(String name, boolean deleteOnEmpty) throws QueueNotFoundException {
		String id = Util.fmtSqlIdentifier(name);
		if(id.length() > 0 && !queueEntryDao.queueExists(id))
			return queueEntryDao.createQueue(id, name, deleteOnEmpty);
		return null;
	}
	
	
	
	
	/**
	 * Convenience method to add n random songs to the current queue
	 * @param numSongs the number of random songs to add
	 * @return true if the songs were able to be written, false otherwise.
	 * Currently only marks a failure if the activeQueue property isn't set
	 */
	public boolean addRandomSongsToQueue(int numSongs) {
		List<QueueEntry> songs = queueEntryDao.getSimpleRandomQueueEntries(numSongs);
		SongQueue currQueue = activeQueue.get();
		if(currQueue != null) {
			currQueue.getQueue().addAll(songs);
			return true;
		}
		return false;
	}
	
	
	
	
	/**
	 * Deletes the specified queue from the database
	 * @param queueId
	 * @throws QueueNotFoundException 
	 */
	public void dropQueue(String queueId) throws QueueNotFoundException {
		if(!queueId.equalsIgnoreCase("main") && allQueueIDs.containsKey(queueId)) { // make sure it's a database queue and not the primary request queue
			queueEntryDao.dropQueue(queueId);
			refreshQueueMenu();
			if(queueId.equalsIgnoreCase(activeQueue.get().getQueueId())) { // if it's the currently displayed queue, have to switch
				setActiveQueue(new EmptySongQueue());
			}
		}
	}


	
	
	

	/**
	 * Takes the current list of {@code allQueueIDs} and pushes an update.
	 * to the {@code activeQueueMenu}
	 */
	public void refreshQueueMenu() {
		dbTaskQueue.submit(new DBTask("Refresh Queue Menu", () -> {
			allQueueIDs = queueEntryDao.getAllQueueIDsWithNames();
			Platform.runLater(() -> {
				int menuSize = activeQueueMenu.getItems().size();
//				if(menuSize >= 2) { ==> was originally to handle no queues present condition, but that won't happen now because of the random queue
					activeQueueMenu.getItems().remove(0, menuSize-initialMenuSize);
					if(allQueueIDs.size() > 0) {
						ToggleGroup t = new ToggleGroup();
						
						// Add the random queue separately from the database queues
						RadioMenuItem randMenu = new RadioMenuItem(randomQueue.getQueueDispName());
						randMenu.setUserData(randomQueue.getQueueId());
						randMenu.setToggleGroup(t);
						randMenu.setOnAction((event) -> {
							activeQueue.set(randomQueue);
							updateQueueTblContents();
						});
						
						activeQueueMenu.getItems().add(0, randMenu);
						
						// add each queue menu item / bind listeners
						for(Entry<String, String> e : allQueueIDs.entrySet()) { 
							RadioMenuItem m = new RadioMenuItem();
							m.setToggleGroup(t);
							m.setText(e.getValue());
							m.setUserData(e.getKey());
							
							// fetch the desired SongQueue as needed in the listener
							m.setOnAction((event) -> { 
								setActiveQueue((String) m.getUserData());
							});

							activeQueueMenu.getItems().add(0, m);
						}
//					} 
//					else { // no registered Queues
//						MenuItem defaultItem = new MenuItem();
//						defaultItem.setDisable(true);
//						activeQueue.set(new EmptySongQueue());
//						defaultItem.setUserData(activeQueue);
//						defaultItem.setText("No Queues Detected");
//						activeQueueMenu.getItems().add(0, defaultItem);
//					}
				} else
					throw new RuntimeException("Missing key items in Active Queue Menu!");
			});
		}));
	}
	
	
	
	@EventListener
	public void handleQueueListChangeEvent(QueueListChangedEvent event) {
		log.info("Recieved QueueListChanged Event");
		refreshQueueMenu();
	}
	
	
	
	
	@EventListener
	public void handleQueueUpdatedEvent(QueueUpdatedEvent event) {
		log.info("Recieved QueueUpdated Event for queue \"" +event.getQueueId()+ "\"");
		if(event.getQueueId().equals(activeQueue.get().getQueueId())) {
			setActiveQueue(event.getQueueId());
//			Util.runNewDaemon(() -> {
//				try {
//					SongQueue s = queueEntryDao.getQueue(event.getQueueId());
//					Platform.runLater(() -> { 
//						updateQueueTblContents(); 
////						activeQueue.get().getQueue();
//						activeQueue.get().getQueue().setAll(s.getQueue());
//					});
//				} catch (QueueNotFoundException e) { log.error(e); }
//			});
		}
	}
	
	


	/**
	 * Refreshes the contents of the queue table to reflect the current
	 * {@code activeQueue}
	 */
	private void updateQueueTblContents() {
		queueTbl.getItems().setAll(activeQueue.get().getQueue());
	}



	public void setQueueEntryDao(QueueDAO queueEntryDao) {
		this.queueEntryDao = queueEntryDao;
	}


	public void setQueueTbl(TableView<QueueEntry> queueTbl) {
		this.queueTbl = queueTbl;
	}


	/**
	 * Attempts to set the current queue by name
	 * @param queueName
	 */
	public void setActiveQueue(String queueId) {
		dbTaskQueue.submit(new DBTask("Set Active Queue", () -> {
			SongQueue activeQueue = queueEntryDao.getQueue(queueId);
			Platform.runLater(() -> { setActiveQueue(activeQueue); });
		}));
	}
	
	
	
	public void clearCurrentQueue() throws QueueNotFoundException {
		queueEntryDao.clearQueue(activeQueue.get().getQueueId());
		queueTbl.getItems().clear();
	}
	
	
	
	public void setActiveQueue(SongQueue s) {
		activeQueue.set(s);
		allQueueIDs.put(s.getQueueId(), s.getQueueDispName());
		refreshQueueMenu();
	}


	public void setActiveQueueMenu(Menu activeQueueMenu) {
		this.activeQueueMenu = activeQueueMenu;
		this.initialMenuSize = activeQueueMenu.getItems().size();
	}
	
	
	public SongQueue getActiveQueue() {
		return activeQueue.get();
	}
	
	
	public ObjectProperty<SongQueue> activeQueueProperty() {
		return activeQueue;
	}


	public void setRandomQueue(SimpleRandomSongQueue randomQueue) {
		this.randomQueue = randomQueue;
	}
	
	
	public ReadOnlyIntegerProperty queueSizeProperty() {
		return queueSizeProp.getReadOnlyProperty();
	}

	
	public ReadOnlyDoubleProperty queueLengthProperty() {
		return queueLengthProp.getReadOnlyProperty();
	}

	
	public ReadOnlyDoubleProperty rngInQueuePercentProperty() {
		return rngInQueueProp.getReadOnlyProperty();
	}



	public void setViewerManager(ViewerManager viewerManager) {
		this.viewerManager = viewerManager;
	}



	public ReadOnlyStringProperty queueNameProp() {
		return queueNameProp.getReadOnlyProperty();
	}



	public void setDbTaskQueue(ExecutorService dbTaskQueue) {
		this.dbTaskQueue = dbTaskQueue;
	}

}














