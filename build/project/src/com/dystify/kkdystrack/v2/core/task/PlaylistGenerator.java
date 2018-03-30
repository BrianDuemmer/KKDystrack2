package com.dystify.kkdystrack.v2.core.task;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.dystify.kkdystrack.v2.core.exception.SongException;
import com.dystify.kkdystrack.v2.core.util.Util;
import com.dystify.kkdystrack.v2.dao.SongDAO;
import com.dystify.kkdystrack.v2.manager.PlaylistManager;
import com.dystify.kkdystrack.v2.model.Song;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;



/**
 * Serves to rebuild the playlist based on the filesystem. When run, it will do the following:
 * <ul>
 * <li>recursively scan all valid song files from the filesystem based on the root directory</li>
 * <li>record metadata info for each song file</li>
 * <li>Construct Song objects for each song found, populating defaults for rating / play info</li>
 * <li>Push that info to the playlist table in the database, optionally overwriting existing songs</li>
 * <li>Will record how many songs need to recalculate cost</li>
 * <li>prompt to recalculate cost for all those songs</li>
 * </ul>
 * @author Duemmer
 *
 */
public class PlaylistGenerator extends AbstractBackgroundTask
{
	private int commitBlockSize;
	private double timePerSong;
	private String ostTreeRegenAddress;
	@Autowired private PointRecalculator pointRecalculator;
	@Autowired private PlaylistManager playlistManager;
	
	private Thread taskThread;
	private String playlistRoot;
	private SongDAO songDao;
	
	private boolean shouldAbort = false;
	private boolean isDone = false;
	
	private int numTotal; // total number of songs to process
	private int numOn; // current song that's being worked on, or -1 for indeterminate
	private String isUploadingText = ""; // display text for when an upload is in progress
	private boolean isUploading = false;
	private Song currSong = new Song(); // current song under processing
	private int totalEta = -1;
	
	
	public PlaylistGenerator(String title, int uiUpdateRate, Image favicon) throws IOException {
		super(title, uiUpdateRate, favicon);
	}
	
	
	@Override
	protected void start() {
		log.info("Starting playlist Generator...");
		taskThread = new Thread(() -> {
			List<File> allSongs = getAllSongFiles(new File(playlistRoot));
			numTotal = allSongs.size();
			List<Song> loadedSongs = new ArrayList<>();
			
			
			// load each song
			log.trace("Loading songs...");
			for(numOn=0; numOn<numTotal; numOn++) {
				if(shouldAbort) { break; }
				try { 
					currSong = songDao.loadFromFile(allSongs.get(numOn)); 
					loadedSongs.add(currSong);
					} 
				catch (SongException e) {
					log.error("Encountered exception loading song from file at \"" +allSongs.get(numOn).getAbsolutePath()+ "\" DETAILS:");
					log.error(e);
				}
			}
			
			// push them to the database. Do two runs, and  record the number newly added that will need point recalculations
			isUploadingText = "Committing newly added songs...";
			isUploading = true;
			log.info(isUploadingText);
			
			// commit changes in blocks
			List<Song> addedNew = new ArrayList<>();
			numOn = 0;
			int wasOn = 0;
			do {
				if(shouldAbort) { break; }
				numOn = Math.min(numTotal-1, numOn+commitBlockSize);
				addedNew.addAll(songDao.writeToPlaylist(loadedSongs.subList(wasOn, numOn), true)); // first add the new stuff
				wasOn = numOn;
			} while(numOn < numTotal-1);
			
			// remove all that stuff we just committed from the list of stuff we want to add
			for(Song s : addedNew)
				loadedSongs.remove(s);
			
			// commit the rest of them
			isUploadingText = "Updating all songs...";
			log.info(isUploadingText);
			numTotal = loadedSongs.size();
			numOn = 0;
			wasOn = 0;
			do {
				if(shouldAbort) { break; }
				numOn = Math.min(numTotal-1, numOn+commitBlockSize);
				songDao.writeToPlaylist(loadedSongs.subList(wasOn, numOn), false);
				wasOn = numOn;
			} while(numOn < numTotal-1);
			
			// at this point this task is essentially done, and we can exit as we want
			isDone = true;
			
			// we don't actually need the output, but just to regenerate it, and block until it's finished
			try { Util.getUrlContents(new URL(ostTreeRegenAddress)); } 
			catch (IOException e) { log.fatal("Failed to update OST tree"); log.fatal(e); }
			playlistManager.refreshOstTree();
			
			// prompt to run point calc routines
			if(addedNew.size() > 0) {
				Platform.runLater(() -> {
					Alert a = new Alert(AlertType.CONFIRMATION);
					a.setTitle("Confirm Point Recalculation");
					a.setHeaderText("Some songs need to have points calculated!");
					
					String content = String.format("%d songs require point calculations. This will take "
							+ "approximately %s. Run this now?", 
							addedNew.size(), 
							Util.secondsToTimeString(pointRecalculator.getTimePerSong() * addedNew.size()));
					
					a.setContentText(content);
					Optional<ButtonType> b = a.showAndWait();
					if(b.get() == ButtonType.OK) { // launch the point recalculator
						pointRecalculator.setSongstoCalc(addedNew);
						pointRecalculator.startTask();
					}
				});
			}
		});
			
			
		taskThread.setDaemon(true);
		taskThread.setName("Playlist Rebuild");
		taskThread.start();
	}

	@Override
	protected void abort() {
		shouldAbort = true;
	}

	@Override
	public double getPctDone() {
		return ((double)numOn) / ((double)numTotal);
	}

	@Override
	public String getDispText() {
		if(!isUploading)
			return String.format("Processing song %d of %d [%d%%] - %s", numOn, numTotal, (int) (100*getPctDone()), currSong.getDispText(true));
		else
			return isUploadingText;
	}

	@Override
	public String getAbortWarning() {
		return "WARNING! Aborting a playlist rebuild could damage the playlist, requiring a clean build! Continue anyways?";
	}
	
	
	
	/**
	 * Recursively obtains a list of all song files under this root directory
	 * @param root
	 * @return
	 */
	private List<File> getAllSongFiles(File root) {
		
		// basic recursive directory crawler
		File[] dirs = root.listFiles(File::isDirectory);
		File[] songsInDir = root.listFiles(Util::isLegalAudioFileExtension);
		List<File> ret = new ArrayList<>();
		for(File f : songsInDir)
			ret.add(f);
		for(File f : dirs)
			ret.addAll(getAllSongFiles(f));
		return ret;
	}


	public void setLog(Logger log) {
		this.log = log;
	}


	@Override
	public int getEta() {
		return totalEta;
	}


	@Override
	public boolean isFinished() {
		return isDone;
	}
	
	
	@Override
	public void reset() {
		this.isDone = false;
		this.shouldAbort = false;
		this.isUploading = false;
	}


	public int getCommitBlockSize() {
		return commitBlockSize;
	}


	public double getTimePerSong() {
		return timePerSong;
	}


	public void setCommitBlockSize(int commitBlockSize) {
		this.commitBlockSize = commitBlockSize;
	}


	public void setTimePerSong(double timePerSong) {
		this.timePerSong = timePerSong;
	}


	public String getPlaylistRoot() {
		return playlistRoot;
	}


	public void setPlaylistRoot(String playlistRoot) {
		this.playlistRoot = playlistRoot;
	}


	public SongDAO getSongDao() {
		return songDao;
	}


	public void setSongDao(SongDAO songDao) {
		this.songDao = songDao;
	}


	public void setPlaylistManager(PlaylistManager playlistManager) {
		this.playlistManager = playlistManager;
	}


	public void setOstTreeRegenAddress(String ostTreeRegenAddress) {
		this.ostTreeRegenAddress = ostTreeRegenAddress;
	}
}








