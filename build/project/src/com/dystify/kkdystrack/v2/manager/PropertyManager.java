package com.dystify.kkdystrack.v2.manager;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.dystify.kkdystrack.v2.core.util.Util;
import com.dystify.kkdystrack.v2.service.MusicPlayer;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class PropertyManager 
{
	private Logger log = LogManager.getLogger(getClass());
	
	// Managers to pull properties from, which can be autowired
	@Autowired private QueueManager queueManager;
	@Autowired private MusicPlayer foobar;
	@Autowired private FileManager fileManager;

	//UI Properties that need to be set manually
	private StringProperty requestModeProperty;
	private DoubleProperty maxQueueLengthProperty;
	private DoubleProperty minQueueLengthProperty;

	// Resultant merged properties
	private DoubleProperty queueLengthProperty;
	private DoubleProperty timeUntilRequestsOpenProperty;
	private DoubleProperty queueFullnessProperty;
	private BooleanProperty requestsOpenProperty;
	private BooleanProperty queueFillingProperty;

	private StringProperty requestStatusProperty;
	private StringProperty nowPlayingRatingProperty;
	private StringProperty nowPlayingViewerProperty;
	private StringProperty songCommentProperty;
	
	// Helper variables
	private boolean queueWasFilling = true;


	public PropertyManager() {
		requestModeProperty = new SimpleStringProperty();
		maxQueueLengthProperty = new SimpleDoubleProperty();
		minQueueLengthProperty = new SimpleDoubleProperty();


		queueLengthProperty = new SimpleDoubleProperty();
		timeUntilRequestsOpenProperty = new SimpleDoubleProperty();
		queueFullnessProperty = new SimpleDoubleProperty();
		requestsOpenProperty = new SimpleBooleanProperty();
		queueFillingProperty = new SimpleBooleanProperty();

		requestStatusProperty = new SimpleStringProperty();
		nowPlayingRatingProperty = new SimpleStringProperty();
		nowPlayingViewerProperty = new SimpleStringProperty();
		songCommentProperty = new SimpleStringProperty();
	}


	/**
	 * Constructs all the necessary binding chains to build the logic for the 
	 * resultant merged properties
	 */
	@PostConstruct private void bindListeners() {
		bindSystemInfoProps();
		bindFileOutputProperties();
	}


	/**
	 * Performs the necessary bindings tailored for the output files, as a final formatted string
	 */
	private void bindFileOutputProperties() {
		requestStatusProperty.bind(
				Bindings.when(requestsOpenProperty)
				.then(
						Bindings.createStringBinding(
								() -> {
									String fmt = fileManager.getRequestStatusOpenFmt();
									String queueLen = Util.intSecondsToTimeString(queueLengthProperty.get());
									String maxLen = Util.intSecondsToTimeString(maxQueueLengthProperty.get());
									return fmt.replaceAll("\\$QUEUE_LEN", queueLen)
											.replaceAll("\\$MAX_LEN", maxLen);
								},
								queueLengthProperty
						)
				).otherwise(
						Bindings.createStringBinding(
								() -> {
									String fmt = fileManager.getRequestStatusClosedFmt();
									String openTime = Util.intSecondsToTimeString(timeUntilRequestsOpenProperty.get());
									return fmt.replaceAll("\\$OPEN_TIME", openTime);
								},
								timeUntilRequestsOpenProperty
								)
						)
				);

		// bound to the current song name, capped to some predefined length, which will be truncated
		// with a `…` on the end if it's too long
		nowPlayingRatingProperty.bind(Bindings.createStringBinding(
				() -> {
					if(foobar.nowPlayingProperty().get() != null) {
						String song = foobar.nowPlayingProperty().get().getSong().getSongName();
						if(song.length() > fileManager.getMaxSongLength())
							return song.substring(0, fileManager.getMaxSongLength()).concat("…");
						return song;
					}
					return "null";
				}, 
				foobar.nowPlayingProperty()));

		// simple string property 
		nowPlayingViewerProperty.bind(Bindings.createStringBinding(
				() -> {
					if(foobar.nowPlayingProperty().get() != null)
						return foobar.nowPlayingProperty().get().getViewer().getUsername();
					return "";
				},
				foobar.nowPlayingProperty()));


		songCommentProperty.bind(Bindings.createStringBinding(
				() -> {
					String fmt = fileManager.getSongCommentFmt();;
					String songName = "null";
					String ostName = "null";
					if(foobar.nowPlayingProperty().get() != null) {
						songName = foobar.nowPlayingProperty().get().getSong().getSongName();
						ostName = foobar.nowPlayingProperty().get().getSong().getOstName();
					}
					return fmt.replaceAll("\\$OST", ostName)
							.replaceAll("\\$NAME", songName);
					
				}, 
				foobar.nowPlayingProperty()));
	}




	/**
	 * performs the necessary property bindings for the calculations of the system info
	 * properties
	 */
	private void bindSystemInfoProps() {
		queueLengthProperty.bind(Bindings.add(
				queueManager.queueLengthProperty(), 
				foobar.currentSongTimeRemaining())
				);

		timeUntilRequestsOpenProperty.bind(
				queueLengthProperty
				.subtract(minQueueLengthProperty)
				);

		// Queue Open indicator, dictates whether requests should be open. They'll be open under 2 conditions:
		// 1) they are forced open, or 
		// 2) mode is Automatic, and Queue is main, queue isn't too long, and we are in a queue fill cycle
		requestsOpenProperty.bind(
				requestModeProperty.isEqualTo("Open")
				.or(
						requestModeProperty.isEqualTo("Automatic")
						.and(queueManager.queueNameProp().isEqualTo("main"))
						.and(queueLengthProperty.lessThan(maxQueueLengthProperty))
						.and(queueFillingProperty)
						)
				);
		
		queueFullnessProperty.bind(queueLengthProperty.divide(maxQueueLengthProperty));
		
		/*
		 * Because the function to calculate queue filling status is inherintly cyclic, 
		 * we can't use normal bindings or jfx will croak trying to resolve it. Rather,
		 * pull the logic out of the binds and manually set the dependencies so as to not
		 * listen on queueFillingProperty
		 */
		queueFillingProperty.bind(
				Bindings.createBooleanBinding(() -> {
					if(queueLengthProperty.get() >= maxQueueLengthProperty.get())
						queueWasFilling = false;
					else if(queueLengthProperty.get() <= minQueueLengthProperty.get())
						queueWasFilling = true;
					return queueWasFilling;
				}, 
				queueLengthProperty,
				maxQueueLengthProperty,
				minQueueLengthProperty)
			);
		queueFillingProperty.addListener((obs, oldVal, newVal) -> log.trace("QueueFillingProp updated, is now " +newVal));
	}


	public StringProperty getRequestModeProperty() {
		return requestModeProperty;
	}


	public DoubleProperty getMaxQueueLengthProperty() {
		return maxQueueLengthProperty;
	}


	public DoubleProperty getMinQueueLengthProperty() {
		return minQueueLengthProperty;
	}


	public DoubleProperty getQueueLengthProperty() {
		return queueLengthProperty;
	}


	public DoubleProperty getTimeUntilRequestsOpenProperty() {
		return timeUntilRequestsOpenProperty;
	}


	public DoubleProperty getQueueFullnessProperty() {
		return queueFullnessProperty;
	}


	public BooleanProperty getRequestsOpenProperty() {
		return requestsOpenProperty;
	}


	public BooleanProperty getQueueFillingProperty() {
		return queueFillingProperty;
	}


	public StringProperty getRequestStatusProperty() {
		return requestStatusProperty;
	}


	public StringProperty getNowPlayingRatingProperty() {
		return nowPlayingRatingProperty;
	}


	public StringProperty getNowPlayingViewerProperty() {
		return nowPlayingViewerProperty;
	}


	public StringProperty getSongCommentProperty() {
		return songCommentProperty;
	}


	public void setQueueManager(QueueManager queueManager) {
		this.queueManager = queueManager;
	}


	public void setFoobar(MusicPlayer foobar) {
		this.foobar = foobar;
	}


	public void setFileManager(FileManager fileManager) {
		this.fileManager = fileManager;
	}
}




