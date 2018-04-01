package com.dystify.kkdystrack.v2.core.task;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
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
	private boolean clearNonPlaylist = false;


	public PlaylistGenerator(String title, int uiUpdateRate, Image favicon) throws IOException {
		super(title, uiUpdateRate, favicon);
	}


	/**
	 * Sets the playlist generator running
	 * @param clearNonPlaylist if true, will wipe any songs that weren't in the filesystem from the
	 * playlist
	 */
	public void startTask(boolean clearNonPlaylist) {
		this.clearNonPlaylist = clearNonPlaylist;
		super.startTask();
	}


	@Override
	public void startTask() {
		this.clearNonPlaylist = false; // default to false
		super.startTask();
	}


	@Override
	protected void runTask() {
		log.info("Starting playlist Generator...");
		List<Song> loadedSongs = loadAllSongs(playlistRoot);
		numTotal = loadedSongs.size();

		// Write all the song IDs to the song id temp table
		isUploading = true;

		//			int numNotInTmpBefore = 0;
		if(clearNonPlaylist) {
			isUploadingText = "Recording which songs will be added...";
			log.info(isUploadingText);
			playlistManager.writeAllToSongIdTempTable(loadedSongs);
			//				numNotInTmpBefore = songDao.getCountSongsInPlaylistNotInTempTable();
		}

		// push all songs to the playlist table
		isUploadingText = "Writing new songs to DB";
		log.info(isUploadingText);
		List<Song> addedNew = writeSongsToDB(loadedSongs, true);

		// remove all that stuff we just committed from the list of stuff we want to add, and add the rest
		for(Song s : addedNew) { loadedSongs.remove(s); }
		isUploadingText = "Updating all songs...";
		log.info(isUploadingText);
		writeSongsToDB(loadedSongs, false);

		// remove the songs from the playlist
		if(clearNonPlaylist) {
			isUploadingText = "Removing non-playlis songs from playlist table";
			log.info(isUploadingText);
			int numDropped = songDao.dropSongsNotInSongIdTempTable();
			log.info(String.format("Removed %d songs", numDropped));
		}

		// at this point this task is essentially done, and we can exit as we want. Reload the OST tree and refresh it here
		playlistManager.generateOstTree();
		playlistManager.refreshOstTree();

		// prompt to run point calc routines, if necessary
		if(addedNew.size() > 0) {
			Platform.runLater(() -> {
				promptAndRunPointCalc(addedNew);
			});
		}
		isDone = true;
	}




	/**
	 * Takes all the song objects specified, and writes those that aren't already there to the database
	 * @param loadedSongs all the correctly loaded songs that need to be written
	 * @param skipDuplicates if true, will only write songs that aren't already in the playlist
	 * @return a List of all the songs that were added new, i.e. they weren't in the database before 
	 * now
	 */
	private List<Song> writeSongsToDB(List<Song> loadedSongs, boolean skipDuplicates) {
		// commit changes in blocks
		List<Song> addedNew = new ArrayList<>();
		numTotal = loadedSongs.size();
		numOn = 0;
		int wasOn = 0;
		do {
			if(shouldAbort) { break; }
			numOn = Math.min(numTotal-1, numOn+commitBlockSize);
			addedNew.addAll(songDao.writeToPlaylist(loadedSongs.subList(wasOn, numOn), skipDuplicates)); // first add the new stuff
			wasOn = numOn;
		} while(numOn < numTotal-1);
		return addedNew;
	}




	/**
	 * Loads all song objects from the song files, while reporting the progress via class fields
	 * @param rootDir the directory to search for songs in
	 * @return a List of all the songs that were loaded
	 */
	private List<Song> loadAllSongs(String rootDir) {
		List<File> allSongs = SongDAO.getAllSongFiles(new File(rootDir));
		numTotal = allSongs.size();

		// actually load the songs. Will implicitly check for broken files and discard them
		List<Song> loadedSongs = new ArrayList<>();


		// load each song
		log.trace("Loading songs...");
		for(numOn=0; numOn<numTotal; numOn++) {
			if(shouldAbort) { break; }
			try { 
				currSong = SongDAO.loadFromFile(allSongs.get(numOn)); 
				loadedSongs.add(currSong);
			} 
			catch (SongException e) {
				log.error("Encountered exception loading song from file at \"" +allSongs.get(numOn).getAbsolutePath()+ "\" DETAILS:");
				log.error(e);
			}
		}
		return loadedSongs;
	}




	/**
	 * Will prompt the user if they want to recalculate points, and will launch the point calculator if they do
	 * @param addedNew
	 */
	private void promptAndRunPointCalc(List<Song> addedNew) {
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
}








