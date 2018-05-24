package com.dystify.kkdystrack.v2.service;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dystify.kkdystrack.v2.model.QueueEntry;
import com.dystify.kkdystrack.v2.model.queue.SongQueue;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

public class FoobarCommandLine implements MusicPlayer 
{
	private int ttlStart;

	private Logger log = LogManager.getLogger(this.getClass());

	private String foobarPath;
	private ScheduledExecutorService stateSimulator;
	private QueueEmptyCallback queueEmptyCallback;

	private double leadTime;
	private ObjectProperty<SongQueue> activeQueueProperty;
	private Queue<QueueEntry> foobarQueue;
	private ReadOnlyDoubleWrapper queueTime; // amount of time left in the entire queue
	private boolean foobarOpen = false;
	private ReadOnlyObjectWrapper<MusicPlayerState> playStatus;
	private ReadOnlyObjectWrapper<QueueEntry> nowPlaying;
	private ReadOnlyStringWrapper nowPlayingIndicatorProp;

	private long lastUpdate;



	/**
	 * Designates the task to run that 'simulates' foobar itself. Since there 
	 * is no way to actually track it, we must simulate it with a daemon updating
	 * quickly
	 */
	private Runnable updateTask = () -> {
		double dt = ((double)(System.currentTimeMillis() - lastUpdate)) / 1000;
		if(playStatus.get() == MusicPlayerState.PLAYING) {
			Platform.runLater(() -> {
				queueTime.set(queueTime.get() - dt); // advance simulator
				if(queueTime.get() <= leadTime) { // gotta add the next song
					queueTime.set(0); // skipping the song, so set timer to 0
					addNextSongToQueue();
					playPlaybackCore();
					foobarQueue.poll();
				}
			});
		}
		lastUpdate = System.currentTimeMillis();
		Platform.runLater(() -> { nowPlayingIndicatorProp.set(fmtNowPlayingMsg()); });
	};

	private long foobarRestartTime;





	public FoobarCommandLine(String path) {
		this.foobarPath = path;
		foobarQueue = new LinkedList<>();
		nowPlayingIndicatorProp = new ReadOnlyStringWrapper();
		queueTime = new ReadOnlyDoubleWrapper(0);
		nowPlaying = new ReadOnlyObjectWrapper<>();
		playStatus = new ReadOnlyObjectWrapper<MusicPlayerState>(MusicPlayerState.STOPPED);
		stateSimulator = Executors.newSingleThreadScheduledExecutor();
		stateSimulator.scheduleAtFixedRate(updateTask, 100, 500, TimeUnit.MILLISECONDS);
	}





	/**
	 * proceeds the song queue, and fetches the next QueueEntry that should 
	 * play. Note that this can return null in the event that the {@code activeQueue}
	 * is not set, or is empty, and {@link QueueEmptyCallback#onQueueEmpty()} returns null
	 * @return
	 */
	private QueueEntry popNextSong() {
		QueueEntry q = null;
		if(activeQueueProperty.get() != null)
			q = activeQueueProperty.get().popNextSong();
		else
			log.warn("ActiveQueueProperty in music player not set!");
		if(q == null)
			q = queueEmptyCallback.onQueueEmpty();
		return q;
	}




	private void execFoobarCmd(String cmd)
	{
		String cmdStr = foobarPath +" "+ cmd;
		try {
			log.info(cmdStr);
			Runtime.getRuntime().exec(cmdStr);
		} catch (IOException e) {
			log.error("Failed to execute foobar command \"" +cmdStr+ "\"", e);
		}
	}




	/**
	 * Adds this song to foobar's playback queue. Note that this won't 
	 * Explicitly call {@link #play()} or remove this song from the {@code SongQueue}
	 */
	private void addToFoobarQueue(QueueEntry toPush) {
		String fooCmd = String.format("/context_command:\"add to playback queue\" \"%s\"", toPush.getSong().getSongId());
		execFoobarCmd(fooCmd);
	}



	/**
	 * pops the next song, checks if it can be read. 
	 * If so, updates the total time remaining, and adds 
	 * it to foobar's queue, as well as triggering the nowPlaying property.
	 * If not, reports an error, decreases {@code ttl}, 
	 * and goes to the next one. If {@code ttl} <= 0, just reports an error and 
	 * breaks out
	 */
	private void addNextSongToQueue(int ttl) {
		QueueEntry added = popNextSong();
		String songId = added.getSong().getSongId();
		if(new File(songId).exists()) {
			queueTime.set(queueTime.get() + added.getSong().getSongLength());
			foobarQueue.add(added);
			log.info("Set now Playing:" + added.getSong().getDispText(false));
			nowPlaying.setValue(added);
			addToFoobarQueue(added);
		} else {
			log.error("Song \"" +songId+ "\" was not found! Skipping...");
			if(ttl > 0)
				addNextSongToQueue(ttl-1);
			else
				log.fatal("TTL expired for skips! Is the playlist broken?");
		}
	}
	
	
	
	private void addNextSongToQueue() {
		addNextSongToQueue(ttlStart);
	}




	/**
	 * Checks to see if there is an instance of foobar2000 running in the system, via shell commands.
	 * This is a fairly slow (~100ms) call, so only run when resetting
	 * @return true if foobar is running, false otherwise
	 */
	public boolean isFoobarRunning() {
		try {
			String command = "tasklist /FI \"IMAGENAME eq foobar2000.exe\"" ;
			Process proc = Runtime.getRuntime().exec(command);
			proc.waitFor();
			Scanner s = new Scanner(proc.getInputStream());
			s.useDelimiter("\\A"); // read the entire input stream
			String stdOut = "";
			if(s.hasNext()) 
				stdOut = s.next();
			s.close();
			return stdOut.contains("foobar2000.exe");
		} catch (IOException | InterruptedException e) {
			log.error("Failed to check foobar running status!", e);
		}

		return false;
	}





	@Override public void reset() {
		log.info("Resetting Foobar");
		foobarOpen = isFoobarRunning();
		if(foobarOpen) {// have to give it some time to register the shutdown before we can restart it
			execFoobarCmd("/exit");
			try { Thread.sleep(foobarRestartTime); } catch (InterruptedException e) {}
		}
		execFoobarCmd("/stop"); // just load up the program
		foobarQueue = new LinkedList<>();
		queueTime.set(0);
		foobarOpen = true;
		playStatus.set(MusicPlayerState.STOPPED);

	}




	@Override public void startPlayback() 
	{
		//		if(playStatus.get() != MusicPlayerState.PLAYING) {
		//			QueueEntry nowPlaying = popNextSong();
		//			if(nowPlaying != null) {
		//				foobarQueue.add(nowPlaying);
		//				queueTime = nowPlaying.getSong().getSongLength();
		//				playStatus.set(MusicPlayerState.PLAYING);
		//				execFoobarCmd("/play \"" + nowPlaying.getSong().getSongId() +"\"");
		//				lastUpdate = System.currentTimeMillis();
		//			} else {
		/** Foobar will play its own randoms in the event the queue ever goes empty. 
		 * It's better to flag to dystrack that the player stopped and let foobar keep playing
		 * as a fallback vs. having the music stop entirely */
		//				// if there's no next song then you can't play so halt the player
		//				playStatus.set(MusicPlayerState.STOPPED);
		//				execFoobarCmd("/stop");

		addNextSongToQueue();
		playPlaybackCore();
		//				log.fatal("Unable to aquire song for queueing, removing Dystrack control from Foobar...");
		//				playStatus.set(MusicPlayerState.STOPPED);
		//			}
		//		}
	}




	@Override public void pausePlayback() {
		if(playStatus.get() == MusicPlayerState.PLAYING) {// have to pause
			playStatus.set(MusicPlayerState.PAUSED);
			execFoobarCmd("/pause");
			lastUpdate = System.currentTimeMillis();
		}
	}



	@Override public void playPlayback() {
		if(playStatus.get() == MusicPlayerState.PAUSED) // have to play
			playPlaybackCore();
		else if(playStatus.get() == MusicPlayerState.STOPPED)
			startPlayback();
	}




	/**
	 * Updates the play status to playing and executes {@code /play}, regardless of the 
	 * current player state
	 */
	private void playPlaybackCore() {
		playStatus.set(MusicPlayerState.PLAYING);
		execFoobarCmd("/play");
		lastUpdate = System.currentTimeMillis();
	}




	@Override public void skipSong() {
		queueTime.set(0);
		addNextSongToQueue();
		if(playStatus.get() == MusicPlayerState.PAUSED) { // behavior is different for being in playing vs. paused state
			execFoobarCmd("/next");
			execFoobarCmd("/pause"); // have to re-pause
		}
		else
			playPlaybackCore();
		foobarQueue.poll();
		

		//		queueTime = 0;
		//		for(QueueEntry q : foobarQueue)
		//			queueTime += q.getSong().getSongLength();
	}


	@Override public QueueEntry getNowPlaying() { return foobarQueue.peek(); }
	@Override public void setLeadTime(double leadTime) { this.leadTime = leadTime; }
	@Override public void setQueueProperty(ObjectProperty<SongQueue> queue) { this.activeQueueProperty = queue; }


	@Override public ReadOnlyDoubleProperty currentSongTimeRemaining() { 
		return queueTime.getReadOnlyProperty(); 
	}





	@Override public ReadOnlyObjectProperty<MusicPlayerState> playStatusProperty() {
		return playStatus.getReadOnlyProperty();
	}





	@Override public ReadOnlyStringProperty nowPlayingTextProperty() {
		return nowPlayingIndicatorProp.getReadOnlyProperty();
	}




	@Override public ReadOnlyObjectProperty<QueueEntry> nowPlayingProperty() {
		return nowPlaying.getReadOnlyProperty();
	}





	public void setFoobarRestartTime(long foobarRestartTime) {
		this.foobarRestartTime = foobarRestartTime;
	}





	@Override public void setQueueEmptyCallback(QueueEmptyCallback queueEmptyCallback) {
		this.queueEmptyCallback = queueEmptyCallback;
	}





	public void setTtlStart(int ttlStart) {
		this.ttlStart = ttlStart;
	}

}











