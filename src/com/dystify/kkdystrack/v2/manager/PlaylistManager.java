package com.dystify.kkdystrack.v2.manager;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.event.EventListener;

import com.dystify.kkdystrack.v2.core.event.types.PlaylistEvent;
import com.dystify.kkdystrack.v2.core.util.Util;
import com.dystify.kkdystrack.v2.dao.SongDAO;
import com.dystify.kkdystrack.v2.model.OST;
import com.dystify.kkdystrack.v2.model.Song;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

public class PlaylistManager extends AbstractManager
{
	private SongDAO songDao;
	private TreeView<OST> ostTree;
	private ObservableList<Song> playlistContents;
	private String ostTreeLocation;
	private StringProperty playlistRoot;
	
	private int songIdTempTblCommitBlockSize;
	private String ostTreeRegenAddress;


	public PlaylistManager(String ostTreeLocation) {
		this.ostTreeLocation = ostTreeLocation;
	}


	public TreeItem<OST> getOstTree() throws IOException {
		String jsonData = Util.getUrlContents(new URL(ostTreeLocation));
		TreeItem<OST> ret = populate(new TreeItem<OST>(new OST("root")), new JsonParser().parse(jsonData));

		//shouldn't be possible to have more than one child under root, so we can make that assumption
		condenseRoot(ret.getChildren().get(0));
		return ret;
	}



	/**
	 * Recursive function to traverse the json tree, and replicate that structure into a
	 * logical javafx tree
	 * @param master
	 * @param root
	 * @return
	 */
	private TreeItem<OST> populate(TreeItem<OST> master, JsonElement root) {
		if(root.isJsonObject()) {
			Set<Entry<String, JsonElement>> children = root.getAsJsonObject().entrySet();
			List<TreeItem<OST>> childOSTs = new ArrayList<>();
			for(Entry<String, JsonElement> e : children) {
				OST o = new OST(e.getKey());
				TreeItem<OST> t = populate(new TreeItem<OST>(o), e.getValue());
				childOSTs.add(t);
			}
			master.getChildren().addAll(childOSTs);
		}
		return master;
	}



	/**
	 * Starting from {@code root}, takes all trailing treeItems with single children and condenses it
	 * into a single element
	 * @param root
	 */
	private void condenseRoot(TreeItem<OST> root) {
		if(root.getChildren().size() == 1) { // one child, can condense
			TreeItem<OST> child = root.getChildren().get(0);
			String newName = root.getValue().getOstName() + "\\" + child.getValue().getOstName();
			root.getValue().setOstName(newName);
			root.getChildren().remove(0);
			root.getChildren().addAll(child.getChildren());
			condenseRoot(root);
		}
	}



	@EventListener
	public void handlePlaylistUpdate(PlaylistEvent event) {
		refreshOstTree(); // for now do this, may get smarter later
	}



	public void refreshOstTree() {
		Util.runNewDaemon("OST tree refresh", () -> {
			try {
				TreeItem<OST> newRoot = getOstTree();
				Platform.runLater(() -> {
					ostTree.setRoot(newRoot);
					Util.expandTreeView(newRoot);	
				});
			} catch (IOException e) {
				log.error(e);
			}			
		});
	}
	
	
	/**
	 * Calls the PHP script that regenerates the OST tree
	 */
	public void generateOstTree() {
		// we don't actually need the output, but just to regenerate it, and block until it's finished
		try { Util.getUrlContents(new URL(ostTreeRegenAddress)); } 
		catch (IOException e) { log.fatal("Failed to update OST tree at " +ostTreeRegenAddress); log.fatal(e); }
	}
	
	
	
	
	public void dropOst() {
//		String 
//		Util.runNewDaemon("Drop OST", () -> {
//			int numSongs = songDao
//		});
	}
	
	
	/**
	 * Writes all the songs in {@code allSongs} to the song id temp table. This will create, 
	 * but not delete, the table
	 */
	public void writeAllToSongIdTempTable(List<Song> allSongs) {
		int total = allSongs.size();
		int numOn = 0;
		int onNext = 0;
		while(numOn < total) {
			// get SIDs to commit this iteration
			onNext = Math.min(total, numOn + this.songIdTempTblCommitBlockSize);
			List<Song> songsThisIteration = allSongs.subList(numOn, onNext);
			List<String> songIDsThisIteration = new LinkedList<>();
			songsThisIteration.forEach((fil) -> songIDsThisIteration.add(fil.getSongId()));
			
			this.songDao.writeSongIDsToTempTbl(songIDsThisIteration, numOn == 0); // only clean on the first iteration
			log.info(String.format("Wrote songs %d - %d of %d", numOn, onNext, total));
			numOn = onNext;
		}
		
		// clear the playlist
		int numDropped = songDao.dropSongsNotInSongIdTempTable();
		log.info(String.format("Removed %d songs from the playlist", numDropped));
	}




	/**
	 * Takes the currently selected OST in the treeview and populates the song list to display the correct songs
	 * @param tbl
	 */
	public void refreshSongList() {
		OST ost = ostTree.getSelectionModel().getSelectedItem().getValue();
		Util.runNewDaemon("SongList Refresh", () -> {
			List<Song> songs = songDao.getAllFromOST(ost);
			Platform.runLater(() -> { playlistContents.setAll(songs); });
		});
	}



	public void setSongDao(SongDAO songDao) {
		this.songDao = songDao;
	}


	public void setOstTree(TreeView<OST> ostTree) {
		this.ostTree = ostTree;
	}


	public ObservableList<Song> getPlaylistContents() {
		return playlistContents;
	}


	public void setPlaylistContents(ObservableList<Song> playlistContents) {
		this.playlistContents = playlistContents;
	}


	public void setOstTreeLocation(String ostTreeLocation) {
		this.ostTreeLocation = ostTreeLocation;
	}


	public void setPlaylistRoot(StringProperty playlistRoot) {
		this.playlistRoot = playlistRoot;
	}


	public void setSongIdTempTblCommitBlockSize(int songIdTempTblCommitBlockSize) {
		this.songIdTempTblCommitBlockSize = songIdTempTblCommitBlockSize;
	}


	public void setOstTreeRegenAddress(String ostTreeRegenAddress) {
		this.ostTreeRegenAddress = ostTreeRegenAddress;
	}

}
















