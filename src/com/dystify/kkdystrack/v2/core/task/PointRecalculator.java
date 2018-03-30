package com.dystify.kkdystrack.v2.core.task;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dystify.kkdystrack.v2.dao.SongDAO;
import com.dystify.kkdystrack.v2.model.Song;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;

public class PointRecalculator extends AbstractBackgroundTask
{
	private double timePerSong;
	private int commitBlockSize;
	private List<Song> songstoCalc;
	private int numOn;
	private Song currSong = new Song(); // set to a basic default
	private boolean shouldAbort = false;
	private boolean finished = false;
	private SongDAO songDao;

	public PointRecalculator(String title, int uiUpdateRate, Image favicon) throws IOException {
		super(title, uiUpdateRate, favicon);
	}


	@Override
	protected void start() {
		if(songstoCalc != null && songstoCalc.size() > 0) {
			Thread t = new Thread(() -> {
				int wasOn = 0;
				numOn = 0;
				do {
					if(shouldAbort ) { break; }
					currSong = songstoCalc.get(numOn);
					numOn = Math.min(songstoCalc.size()-1, wasOn+commitBlockSize);
					songDao.calculatePoints(songstoCalc.subList(wasOn, numOn));
					wasOn = numOn;
				} while(numOn < songstoCalc.size()-1);
				finished = true;
			});
			t.setDaemon(true);
			t.setName("Point Calculation");
			t.start();
		} else {
			finished = true;
			Platform.runLater(() -> {
				Alert a = new Alert(AlertType.ERROR);
				a.setTitle("Error calculating points");
				a.setHeaderText("Songs not set!");
				a.setContentText("Songs to calculate points for must be specified!");
				
				a.showAndWait();
			});
		}
	}

	@Override
	protected void abort() {
		shouldAbort = true;
	}

	@Override
	public double getPctDone() {
		return ((double)numOn) / ((double)songstoCalc.size());
	}

	@Override
	public String getDispText() {
		return String.format("Processing song %d of %d [%d%%] - %s", numOn, songstoCalc.size(), (int) (100*getPctDone()), currSong.getDispText(true));
	}

	@Override
	public String getAbortWarning() {
		return "Really abort point calculations? This will leave some songs with incorrect cost / cooldown settings.";
	}

	public List<Song> getSongstoCalc() {
		return songstoCalc;
	}

	public void setSongstoCalc(List<Song> songstoCalc) {
		this.songstoCalc = songstoCalc;
	}

	@Override
	public int getEta() {
		return (int) ((songstoCalc.size() - numOn) * timePerSong);
	}

	@Override
	public boolean isFinished() {
		return finished;
	}
	
	
	@Override
	public void reset() {
		this.finished = false;
		this.shouldAbort = false;
		
	}


	public double getTimePerSong() {
		return timePerSong;
	}


	public void setTimePerSong(double timePerSong) {
		this.timePerSong = timePerSong;
	}


	public int getCommitBlockSize() {
		return commitBlockSize;
	}


	public void setCommitBlockSize(int commitBlockSize) {
		this.commitBlockSize = commitBlockSize;
	}


	public SongDAO getSongDao() {
		return songDao;
	}


	public void setSongDao(SongDAO songDao) {
		this.songDao = songDao;
	}

}
