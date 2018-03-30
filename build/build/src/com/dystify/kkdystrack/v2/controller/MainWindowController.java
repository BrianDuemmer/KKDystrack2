package com.dystify.kkdystrack.v2.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.prefs.Preferences;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.dystify.kkdystrack.v2.core.exception.OverrideRuleException;
import com.dystify.kkdystrack.v2.core.exception.QueueNotFoundException;
import com.dystify.kkdystrack.v2.core.exception.SongException;
import com.dystify.kkdystrack.v2.core.task.PlaylistGenerator;
import com.dystify.kkdystrack.v2.core.task.PointRecalculator;
import com.dystify.kkdystrack.v2.core.util.Util;
import com.dystify.kkdystrack.v2.dao.OverrideRuleDAO;
import com.dystify.kkdystrack.v2.dao.SongDAO;
import com.dystify.kkdystrack.v2.manager.FileManager;
import com.dystify.kkdystrack.v2.manager.HistoryManager;
import com.dystify.kkdystrack.v2.manager.OverrideRuleManager;
import com.dystify.kkdystrack.v2.manager.PlaylistManager;
import com.dystify.kkdystrack.v2.manager.PropertyManager;
import com.dystify.kkdystrack.v2.manager.QueueManager;
import com.dystify.kkdystrack.v2.manager.SettingsManager;
import com.dystify.kkdystrack.v2.model.OST;
import com.dystify.kkdystrack.v2.model.OverrideRule;
import com.dystify.kkdystrack.v2.model.QueueEntry;
import com.dystify.kkdystrack.v2.model.SettingVal;
import com.dystify.kkdystrack.v2.model.Song;
import com.dystify.kkdystrack.v2.model.queue.SongQueue;
import com.dystify.kkdystrack.v2.service.MusicPlayer;
import com.dystify.kkdystrack.v2.service.MusicPlayerState;
import com.sun.javafx.image.AlphaType;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Callback;

public class MainWindowController 
{
	//    @FXML private ResourceBundle resources; I have no idea where these came from
	//    @FXML private URL location;
	@FXML private AnchorPane root;

	@FXML private Button addSongBtn;
	@FXML private Button addRandomBtn;
	@FXML private Button resetFoobarBtn;
	@FXML private Button foobarPlayStatusBtn;
	@FXML private Button skipSongBtn;

	@FXML private Menu activeQueueMenu;

	@FXML private Label currentQueueLbl;
	@FXML private TableView<QueueEntry> currentQueue;
	@FXML private TableColumn<QueueEntry, String> currentQueue_NameCol;
	@FXML private TableColumn<QueueEntry, String> currentQueue_OSTCol;
	@FXML private TableColumn<QueueEntry, String> currentQueue_UsernameCol;
	@FXML private TableColumn<QueueEntry, String> currentQueue_TimeRequestedCol;
	@FXML private TableColumn<QueueEntry, String> currentQueue_LengthCol;
	@FXML private TableColumn<QueueEntry, String> currentQueue_ratingCol;


	@FXML private MenuItem queueTableMenuRemoveSong;

	@FXML private Slider ctlPctRandom;
	@FXML private TextField ctlMinQueueLength;
	@FXML private TextField ctlMaxQueueLength;
	@FXML private TextField ctlCooldownPts;
	@FXML private TextField ctlSongLeadover;
	@FXML private TextField ctlPlaylistRoot;
	@FXML private TextField ctlTimesPlayedCheck;
	@FXML private TextField ctlStreamLag;
	@FXML private ChoiceBox<String> ctlRequestMode;
	@FXML private TextField ctlBaseCost;
	@FXML private Button paramSaveBtn;
	@FXML private Button paramRevertBtn;

	@FXML private CheckBox indQueueOpen;
	@FXML private Label indQueueLen;
	@FXML private Label indQueueSize;
	@FXML private ProgressBar indQueueFullnes;
	@FXML private ProgressBar indQueueRng;

	@FXML private TreeView<OST> ostTree;

	@FXML private TableView<Song> songsTbl;
	@FXML private TableColumn<Song, String> songsTbl_songName;
	@FXML private TableColumn<Song, String> songsTbl_songLength;
	@FXML private TableColumn<Song, String> songsTbl_currPts;
	@FXML private TableColumn<Song, String> songsTbl_currCost;
	@FXML private TableColumn<Song, String> songsTbl_lastPlay;
	@FXML private TableColumn<Song, String> songsTbl_timesPlayed;
	@FXML private TableColumn<Song, String> songsTbl_rating;
	@FXML private TableColumn<Song, String> songsTbl_pts_perSong;
	@FXML private TableColumn<Song, String> songsTbl_pts_perOST;
	@FXML private TableColumn<Song, String> songsTbl_pts_perFranchise;
	@FXML private TableColumn<Song, String> songsTbl_pts_timeChecked;
	@FXML private TableColumn<Song, String> songsTbl_pts_activeRule;

	@FXML private TableView<OverrideRule> overridesTbl;
	@FXML private TableColumn<OverrideRule, String> overridesTbl_id;
	@FXML private TableColumn<OverrideRule, String> overridesTbl_songPts;
	@FXML private TableColumn<OverrideRule, String> overridesTbl_ostPts;
	@FXML private TableColumn<OverrideRule, String> overridesTbl_franchisePts;
	@FXML private TableColumn<OverrideRule, String> overridesTbl_timeChecked;

	@FXML private Button clearSysoConsoleBtn;
	@FXML private Button clearSyserrConsoleBtn;
	@FXML private TextArea sysoLogTxtbox;
	@FXML private TextArea syserrLogTxtbox;
	@FXML private Label nowPlayingLabel;



	// General instance variable
	private Logger log = LogManager.getLogger(getClass());
	private String inst_lastPlaylistRootVal = ""; // value in the playlist root control from the last focus loss
	private MusicPlayer foobar;
	private SongDAO songDao;
	private OverrideRuleDAO ruleDao;

	@Autowired private Preferences prefs;
	@Autowired private AddOverrideRuleController addOverrideRuleController;
	@Autowired private PlaylistGenerator plGen;
	@Autowired private PointRecalculator pointCalc;
	@Autowired private PlaylistManager playlistManager;
	@Autowired private OverrideRuleManager ruleManager;
	@Autowired private QueueManager queueManager;
	@Autowired private SettingsManager settingsManager;
	@Autowired private HistoryManager historyManager;
	@Autowired private QueueAdder queueAdder;
	@Autowired private FileManager fileManager;
	@Autowired private PropertyManager propertyManager;




	@FXML void clearErrConsole(ActionEvent event) { syserrLogTxtbox.setText(""); }
	@FXML void clearOutConsole(ActionEvent event) { sysoLogTxtbox.setText(""); }
	@FXML void skipSong(ActionEvent event) { foobar.skipSong(); }

	/**
	 * Pops up a file dialog at the playlist root, and allows the user to select 1 
	 * or multiple song files, which will be added to the active queue
	 * @param event
	 */
	@FXML void addSongManual(ActionEvent event) {
		// open up a filechooser
		FileChooser fc = new FileChooser();
		fc.setInitialDirectory(new File(ctlPlaylistRoot.getText()));
		fc.setSelectedExtensionFilter(Util.audioFileExtensionFilter);
		fc.setTitle("Select a Song to Add");

		// extract songs
		List<File> files = fc.showOpenMultipleDialog(root.getScene().getWindow());
		if (files != null && !files.isEmpty()) {
			List<Song> songs = new ArrayList<>();
			files.stream().forEach((f) -> {
				try {
					songs.add(SongDAO.loadFromFile(f));
				} catch (SongException e) {
					log.error("Failed to load Song from file \"" + f.getAbsolutePath() + "\"");
					log.error(e);
				}
			});

			Util.runNewDaemon(() -> {
				boolean added = queueManager.addSongsToQueue(songs);
				if (!added)
					Platform.runLater(() -> {
						Alert a = new Alert(AlertType.ERROR,
								"Must select a queue to add to in the `Active Queue` menu!");
						a.setTitle("Failed to add song to queue");
						a.showAndWait();
					});
			});
		}
	}



	/**
	 * Adds a random song to the current queue
	 * @param event
	 */
	@FXML void addSongRandom(ActionEvent event) {
//		Util.runNewDaemon("Add Random Song", () -> {
			boolean added = queueManager.addRandomSongsToQueue(1);
			if (!added) {
//				Platform.runLater(() -> {
					Alert a = new Alert(AlertType.ERROR,
							"Must select a queue to add to in the `Active Queue` menu!");
					a.setTitle("Failed to add song to queue");
					a.showAndWait();
//				});
//		});
			}
	}
	
	
	
	
	@FXML void emptyCurrentQueue(ActionEvent event) {
		Alert a = new Alert(AlertType.CONFIRMATION);
		a.setContentText("Really delete contents of Queue \"" +queueManager.getActiveQueue().getQueueDispName()+ "\"?");
		Optional<ButtonType> b = a.showAndWait();
		if(b.isPresent() && b.get() == ButtonType.OK) {
//			try {
//				queueManager.clearCurrentQueue();
				queueManager.activeQueueProperty().get().getQueue().clear();
//			} catch (QueueNotFoundException e) {
//				String errTxt = "Queue \"" +queueManager.getActiveQueue().getQueueDispName()+ "\" not found in database!";
//				log.error(errTxt);
//				Alert err = new Alert(AlertType.ERROR);
//				err.setContentText(errTxt);
//				err.showAndWait();
//			}
		}
	}




	@FXML void currentQueue_removeSong(ActionEvent event) {
		int selected = currentQueue.getSelectionModel().getSelectedIndex();
		if(selected >= 0 && queueManager.activeQueueProperty().get() != null) { // don't try to delete if a queueEntry isn't selected
			currentQueue.getItems().remove(selected);
		}
	}

	@FXML void deleteForwardQueue(ActionEvent event) {
		SongQueue toDrop = queueManager.getActiveQueue();
		Alert a = new Alert(AlertType.CONFIRMATION);
		a.setContentText(String.format("Really delete queue \"%s\"?", toDrop.getQueueDispName()));
		//		a.setTitle("Drop Queue");
		//		a.setHeaderText("Drop Queue");
		if(a.showAndWait().get() == ButtonType.OK)
			try {
				queueManager.dropQueue(toDrop.getQueueId());
			} catch (QueueNotFoundException e) {
				new Alert(AlertType.ERROR, "Error Dropping the Queue!").showAndWait();
				log.error(e);
			}
	}

	@FXML void createNewQueue(ActionEvent event) {
		SongQueue s = queueAdder.promtForQueueAdd();
		if(s != null)
			queueManager.activeQueueProperty().set(s);
	}


	@FXML void exportQueue(ActionEvent event) {

	}

	@FXML void openPlaylistRootBrowse(ActionEvent event) {
		DirectoryChooser dc = new DirectoryChooser();
		File start = new File(inst_lastPlaylistRootVal);
		if(start.isDirectory())
			dc.setInitialDirectory(start);
		dc.setTitle("Select Playlist Root");
		File chosen = dc.showDialog(root.getScene().getWindow());
		if(chosen != null) {
			ctlPlaylistRoot.setText(chosen.getAbsolutePath());
			setParamBtnsEnabled(true);
		}
	}

	@FXML void openSettingsPage(ActionEvent event) {

	}


	/**
	 * Prompts the user to recalculate points for all the songs affected by this specific override rule
	 * @param rule
	 */
	private void promptForPointCalc(List<Song> affectedSongs) {
		if(!affectedSongs.isEmpty()) {
			Alert a = new Alert(AlertType.CONFIRMATION);
			a.setTitle("Confirm Point Recalculation");
			a.setHeaderText("Some songs need to have points calculated!");

			String content = String.format("%d songs require point calculations. This will take "
					+ "approximately %s. Run this now?", 
					affectedSongs.size(), 
					Util.secondsToTimeString(pointCalc.getTimePerSong() * affectedSongs.size()));

			a.setContentText(content);
			Optional<ButtonType> btn = a.showAndWait();
			if(btn.get() == ButtonType.OK) { // launch the point recalculator
				pointCalc.setSongstoCalc(affectedSongs);
				pointCalc.startTask();
			}
		}
	}



	@FXML void overridesTbl_addRule(ActionEvent event) 
	{
		boolean shouldCommit = true;
		OverrideRule fromInput = addOverrideRuleController.promptForRule();
		if(ruleDao.ruleAlreadyExists(fromInput)) {
			Alert a = new Alert(AlertType.ERROR, "", new ButtonType("Change Name"), new ButtonType("Update"), ButtonType.CANCEL);
			a.setTitle("Duplicate Rule");
			a.setHeaderText("Rule Error");
			a.setContentText("Rule with override_id \"%s\" already exists!");
			Optional<ButtonType> btn = a.showAndWait();

			if(btn.get().getText().equals("Change Name")) {// they want to try again
				overridesTbl_addRule(null); // do it through recursion as we aren't thread hopping so much here
				shouldCommit = false;
			}
			shouldCommit = btn.get().getText().equals("Update"); // ignore it, just update instead		
		} 
		if(shouldCommit) {
			Util.runNewDaemon("Commit Rule", () -> { 
				ruleDao.putRule(fromInput); 
				List<Song> affectedSongs = songDao.getSongsAffectedByRule(fromInput);
				Platform.runLater(() -> { promptForPointCalc(affectedSongs); });
			});
		}
		ruleManager.refreshRuleTableContents(); // just do an update regardless of whether or not we changed anything specific
	}





	@FXML void overridesTbl_dropRule(ActionEvent event) {
		OverrideRule toRemove = overridesTbl.getSelectionModel().getSelectedItem();
		if(!toRemove.isRootOverride()) {
			overridesTbl.getItems().remove(toRemove);
			Util.runNewDaemon("Drop Rule", ()-> {
				try { 
					List<Song> affectedSongs = songDao.getSongsAffectedByRule(toRemove);
					ruleDao.dropRule(toRemove); 
					ruleManager.refreshRuleTableContents();
					Platform.runLater(() -> { promptForPointCalc(affectedSongs); });
				} catch (OverrideRuleException e) { log.error(e); }
			});
		} else {
			Alert a = new Alert(AlertType.ERROR);
			a.setContentText("Cannot delete root override rule!");
			a.showAndWait();
		}
		ruleManager.refreshRuleTableContents(); // just do an update regardless of whether or not we changed anything specific
	}






	@FXML void overridesTbl_editRule(ActionEvent event) {
		OverrideRule selectedRule = overridesTbl.getSelectionModel().getSelectedItem();
		if(selectedRule != null) {
			OverrideRule newRule = addOverrideRuleController.editRule(selectedRule, false);
			Util.runNewDaemon("Put Override Rule", () -> {
				ruleDao.putRule(newRule); 
				List<Song> affectedSongs = songDao.getSongsAffectedByRule(newRule);
				Platform.runLater(() -> { promptForPointCalc(affectedSongs); });
			});
		} else {
			Alert a = new Alert(AlertType.ERROR);
			a.setContentText("Please select a rule to edit!");
		}
		ruleManager.refreshRuleTableContents(); // just do an update regardless of whether or not we changed anything specific
	}




	@FXML void queueTableMenuRemoveSongOnAction(ActionEvent event) {

	}

	@FXML void regenPlaylist(ActionEvent event) {
		plGen.startTask();
	}

	@FXML void recalcAllPoints(ActionEvent event) {
		pointCalc.setSongstoCalc(songDao.getAllSongs());
		pointCalc.startTask();
	}

	@FXML void resetFoobar(ActionEvent event) {
		foobar.reset();
	}


	@FXML void foobarPlayStatusBtnPressed(ActionEvent event) {
		MusicPlayerState state = foobar.playStatusProperty().get();
		if(state == MusicPlayerState.STOPPED)
			foobar.startPlayback();
		else if(state == MusicPlayerState.PLAYING)
			foobar.pausePlayback();
		else
			foobar.playPlayback();
	}


	@FXML void showCredits(ActionEvent event) {

	}




	/** pulls the settings values from the database and writes  */
	@FXML void revertParams(ActionEvent event) {
		log.info("Loading Settings...");
		setParamBtnsEnabled(false);

		// write PlaylistRoot seperately
		ctlPlaylistRoot.setText(prefs.get("playlist_root", ""));
		inst_lastPlaylistRootVal = ctlPlaylistRoot.getText();
		settingsManager.readSettings();
	}





	@FXML void saveParams(ActionEvent event) {
		log.info("Writing Settings");
		setParamBtnsEnabled(false);

		// write PlaylistRoot seperately
		prefs.put("playlist_root", inst_lastPlaylistRootVal);

		// convenience copy 
		Map<String, SettingVal> m = new HashMap<>();

		m.put("percent_random", new SettingVal(ctlPctRandom.getValue()));
		m.put("min_queue_length", new SettingVal(Util.parseTimeIntervalFromString(ctlMinQueueLength.getText())));
		m.put("max_queue_length", new SettingVal(Util.parseTimeIntervalFromString(ctlMaxQueueLength.getText())));
		m.put("times_played_check", new SettingVal(Util.parseTimeIntervalFromString(ctlTimesPlayedCheck.getText())));
		m.put("song_leadover", new SettingVal(Double.parseDouble(ctlSongLeadover.getText())));
		m.put("cooldown_pts", new SettingVal(Double.parseDouble(ctlCooldownPts.getText())));
		m.put("base_cost", new SettingVal(Double.parseDouble(ctlBaseCost.getText())));
		m.put("stream_lag", new SettingVal(Double.parseDouble(ctlStreamLag.getText())));
		m.put("request_mode", new SettingVal(ctlRequestMode.getSelectionModel().getSelectedItem()));


		settingsManager.getSettings().putAll(m);
		settingsManager.writeSettings();
	}




	@FXML void addSongToCurrentQueue(ActionEvent event) {
		Song currSong = songsTbl.getSelectionModel().getSelectedItem();
		Util.runNewDaemon(() -> { 
			boolean added = queueManager.addSongsToQueue(Arrays.asList(currSong));
			if(!added)
				Platform.runLater(() -> {
					Alert a = new Alert(AlertType.ERROR, "Must select a queue to add to in the `Active Queue` menu!");
					a.setTitle("Failed to add song to queue");
					a.showAndWait();
				});
		});
	}
	
	
	
	
	@FXML void removeOst(ActionEvent e) {
		
	}







	@PostConstruct void initialize() {
		System.out.println("Initializing Main Window Controller...");
		doAsserts();

		Util.initLoggers(sysoLogTxtbox, syserrLogTxtbox);

		// init the tree
		playlistManager.setOstTree(ostTree);
		playlistManager.setPlaylistContents(songsTbl.getItems());
		playlistManager.refreshOstTree();
		ostTree.setShowRoot(false);
		ostTree.getSelectionModel().selectedItemProperty().addListener((observable, oldVal, newVal) -> {
			playlistManager.refreshSongList();
		});

		// prepare the tableViews
		initQueueTbl();
		initOverridesTbl();
		initSongsTbl();

		// init cost rule manager
		ruleManager.setRuleTblContents(overridesTbl.getItems());
		ruleManager.refreshRuleTableContents();

		// init queue manager
		queueManager.setQueueTbl(currentQueue);
		queueManager.setActiveQueueMenu(activeQueueMenu);
		queueManager.refreshQueueMenu();
		queueManager.activeQueueProperty().addListener((obs, oldVal, newVal) -> currentQueueLbl.setText("Current Queue: " +newVal.getQueueDispName()));
		queueManager.activeQueueProperty().addListener((obs, oldVal, newVal) -> {
			settingsManager.putSetting("active_queue", new SettingVal(newVal.getQueueId()));
		});


		// init controls
		ctlAddListeners();
		bindSettingsMapListeners();
		revertParams(null); // revert all params instead of just settingsManager params at startup
		ctlRequestMode.setItems(FXCollections.observableArrayList("Open", "Closed", "Automatic"));


		initFoobar();
		initPropertyManager();

		// init history manager
		historyManager.getNowPlayingProperty().bind(foobar.nowPlayingProperty());

		// setup default values
		queueManager.setActiveQueue("main");
		
		// force an update on this property. Needed because if the queue is filled at app start, 
		// the requestStatus file won't update and will give an invalid output
		String ctlRequestModeVal = ctlRequestMode.getValue();
		ctlRequestMode.setValue("invalid");
		ctlRequestMode.setValue(ctlRequestModeVal);
	}

	
	
	
	
	

	/**
	 * Binds all the needed properties for the PropertyManager from the UI that couldn't be
	 * autowired in from the start, as well as all binding all of the target properties to
	 * the computed joint properties held by propertyManager
	 */
	private void initPropertyManager() {
		// bind the source properties for the propertyManager
		propertyManager.getMaxQueueLengthProperty().bind(
				Bindings.createDoubleBinding(
						() -> Util.parseTimeIntervalFromString(ctlMaxQueueLength.getText()), 
						ctlMaxQueueLength.textProperty()));

		propertyManager.getMinQueueLengthProperty().bind(
				Bindings.createDoubleBinding(
						() -> Util.parseTimeIntervalFromString(ctlMinQueueLength.getText()), 
						ctlMinQueueLength.textProperty()));	

		propertyManager.getRequestModeProperty().bind(ctlRequestMode.valueProperty());


		// bind any listeners to propertyManager's joint properties as needed
		indQueueFullnes.progressProperty().bind(propertyManager.getQueueFullnessProperty());
		indQueueOpen.selectedProperty().bind(propertyManager.getRequestsOpenProperty());
		indQueueLen.textProperty().bind(
				Bindings.createStringBinding(
						() -> Util.intSecondsToTimeString(propertyManager.getQueueLengthProperty().get()), 
						propertyManager.getQueueLengthProperty()));
		
		propertyManager.getRequestsOpenProperty().addListener(
				(obs, oldVal, newVal) -> Util.runNewDaemon(
						() -> settingsManager.putSetting("requests_open", new SettingVal(newVal)) 
				));


		// add any other vanilla listeners not through the propertyManager
		indQueueRng.progressProperty().bind(queueManager.rngInQueuePercentProperty());
		indQueueSize.textProperty().bind(Bindings.convert(queueManager.queueSizeProperty()));
		foobar.nowPlayingProperty().addListener(
				(obs, oldVal, newVal) -> {
					if(newVal != null) 
						Util.runNewDaemon(() -> {
							Map<String, SettingVal> settings = new HashMap<>();
							settings.put("now_playing_song", new SettingVal(newVal.getSong().getSongId()));
							settings.put("now_playing_length", new SettingVal(newVal.getSong().getSongLength()));
							settings.put("now_playing_update", new SettingVal(System.currentTimeMillis() / 1000L));
							settingsManager.putMultipleSettings(settings); 
						});
				});
		
		// bind file properties
		fileManager.getNowPlayingRating().BoundProp().bind(propertyManager.getNowPlayingRatingProperty());
		fileManager.getRequestedBy().BoundProp().bind(propertyManager.getNowPlayingViewerProperty());
		fileManager.getRequestStatus().BoundProp().bind(propertyManager.getRequestStatusProperty());
		fileManager.getSongComment().BoundProp().bind(propertyManager.getSongCommentProperty());
	}





	/**
	 * Performs secondary initialization of the music player, namely wiring it up to the other 
	 * managers / the User Interface
	 */
	private void initFoobar() {
		// for updating the controls on a change to 
		foobar.playStatusProperty().addListener((obs, oldVal, newVal) -> {
			switch(newVal) {
			case PLAYING: {
				foobarPlayStatusBtn.setText("Pause");
				break;
			} case PAUSED: {
				foobarPlayStatusBtn.setText("Play");
				break;
			} case STOPPED: {
				foobarPlayStatusBtn.setText("Start");
				break;
			}
			}
		});


		foobar.setQueueProperty(queueManager.activeQueueProperty());
		nowPlayingLabel.textProperty().bind(foobar.nowPlayingTextProperty());

		// make sure the skip button is disabled when not playing, as that isn't implemented, and write out the updated play status
		foobar.playStatusProperty().addListener((obs, oldVal, newVal) -> {
			skipSongBtn.setDisable(newVal != MusicPlayerState.PLAYING);
			Util.runNewDaemon("Update is_playing", () -> settingsManager.putSetting("is_now_playing", new SettingVal(newVal == MusicPlayerState.PLAYING)));
		});

		foobar.reset();
	}





	/**
	 * Adds listeners for settings controls to listen for changes
	 * in the map of settings themselves
	 */
	private void bindSettingsMapListeners() {
		final Callback<Map<String,SettingVal>, Void> updateSettingsCtls = (c) -> {

			// just set each control's value, don't bother trying to figure out each change
			ctlPctRandom.setValue(c.getOrDefault("percent_random", SettingVal.defaultVal).getNumericVal());
			ctlMinQueueLength.setText(Util.secondsToTimeString(c.getOrDefault("min_queue_length", SettingVal.defaultVal).getNumericVal()));
			ctlMaxQueueLength.setText(Util.secondsToTimeString(c.getOrDefault("max_queue_length", SettingVal.defaultVal).getNumericVal()));
			ctlCooldownPts.setText(String.valueOf(c.getOrDefault("cooldown_pts", SettingVal.defaultVal).getNumericVal()));
			ctlStreamLag.setText(String.valueOf(c.getOrDefault("stream_lag", SettingVal.defaultVal).getNumericVal()));
			ctlSongLeadover.setText(String.valueOf(c.getOrDefault("song_leadover", SettingVal.defaultVal).getNumericVal()));
			ctlBaseCost.setText(String.valueOf(c.getOrDefault("base_cost", SettingVal.defaultVal).getNumericVal()));
			ctlTimesPlayedCheck.setText(Util.secondsToTimeString(c.getOrDefault("times_played_check", SettingVal.defaultVal).getNumericVal()));
			ctlRequestMode.getSelectionModel().select(c.getOrDefault("request_mode", new SettingVal("Closed")).getStringVal());
			return null;
		};

		settingsManager.setOnDBUpdated(updateSettingsCtls);
	}






	/**
	 * Adds listeners to detect value change / commitment on each control. In particular, 
	 * we want to trigger the save / revert buttons to enable whenever any values are 
	 * changed
	 */
	private void ctlAddListeners() 
	{
		final Callback<Number, Void> updateParamsCallback = (num) -> {
			setParamBtnsEnabled(true);
			return null;
		};
		// input filters
		Util.configTextFieldAsNumericInput(ctlBaseCost, true, updateParamsCallback);
		Util.configTextFieldAsNumericInput(ctlCooldownPts, true, updateParamsCallback);
		Util.configTextFieldAsNumericInput(ctlSongLeadover, true, updateParamsCallback);
		Util.configTextFieldAsNumericInput(ctlStreamLag, true, updateParamsCallback);

		// Time interval inputs
		Util.configTextFieldAsTimeIntervalInput(ctlMinQueueLength, updateParamsCallback);
		Util.configTextFieldAsTimeIntervalInput(ctlMaxQueueLength, updateParamsCallback);
		Util.configTextFieldAsTimeIntervalInput(ctlTimesPlayedCheck, updateParamsCallback);


		// other focus lost
		ctlPlaylistRoot.focusedProperty().addListener((obsVal, oldVal, newVal) -> {
			if(!newVal) { // only act when focus lost
				File f = new File(ctlPlaylistRoot.getText());
				if(f.isDirectory()) { //if the path is valid, update
					inst_lastPlaylistRootVal = ctlPlaylistRoot.getText();
					setParamBtnsEnabled(true);
				} else { // invalid playlist root
					ctlPlaylistRoot.setText(inst_lastPlaylistRootVal);
					Alert a = new Alert(AlertType.INFORMATION);
					a.setTitle("Invalid Playlist Root");
					a.setHeaderText("Please enter a directory for the playlist root");
					a.showAndWait();
				}
			}
		});


		ctlPctRandom.valueProperty().addListener((obs, oldVal, newVal) -> {
			setParamBtnsEnabled(true);
		});

		ctlSongLeadover.textProperty().addListener((obs, oldVal, newVal) -> {
			foobar.setLeadTime(Double.parseDouble(newVal));
		});

		ctlPlaylistRoot.textProperty().addListener((obs, oldVal, newVal) -> {
			plGen.setPlaylistRoot(newVal);
		});

		ctlRequestMode.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			setParamBtnsEnabled(true);
		});
	}







	private void setParamBtnsEnabled(boolean enable) {
		paramRevertBtn.setDisable(!enable);
		paramSaveBtn.setDisable(!enable);
	}






	/**
	 * Performs all initialization of the queue display table, as to map a
	 * {@link QueueEntry} object to each row
	 */
	private void initQueueTbl() 
	{
		currentQueue_LengthCol.setCellValueFactory(cellData -> {
			return new SimpleStringProperty(Util.secondsToTimeString(cellData.getValue().getSong().getSongLength()));
		});

		currentQueue_NameCol.setCellValueFactory(cellData -> {
			return new SimpleStringProperty(cellData.getValue().getSong().getSongName());
		});

		currentQueue_OSTCol.setCellValueFactory(cellData -> {
			return new SimpleStringProperty(cellData.getValue().getSong().getOstName());
		});

		currentQueue_ratingCol.setCellValueFactory(cellData -> {
			return new SimpleStringProperty(cellData.getValue().getSong().getRatingTxt());
		});

		currentQueue_TimeRequestedCol.setCellValueFactory(cellData -> {
			return new SimpleStringProperty(Util.fmtDateTimeDisp(cellData.getValue().getTimeRequested()));
		});

		currentQueue_UsernameCol.setCellValueFactory(cellData -> {
			return new SimpleStringProperty(cellData.getValue().getViewer().getUsername());
		});




		// format the queue
		currentQueue.setRowFactory(tbl -> {
			TableRow<QueueEntry> row = new TableRow<>();

			row.setOnDragDetected((MouseEvent event) -> {
				log.debug("Drag Detected");
				int idx = row.getIndex();
				Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
				db.setDragView(row.snapshot(null, null));
				ClipboardContent cb = new ClipboardContent();
				cb.putString(String.valueOf(idx));
				db.setContent(cb);
				event.consume();
			});


			row.setOnDragOver((DragEvent event) -> {
				Dragboard db = event.getDragboard();

				// only permit a transfer if it's from a different index, and the drag index is a valid number
				try { 
					if(row.getIndex() != Integer.parseInt(db.getString())) {
						event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
						event.consume();
					}
				} catch (NumberFormatException e) { log.error("Bad drag info \"" +db.getString()+ "\""); }
			});


			row.setOnDragDropped((DragEvent event) -> {
				log.debug("Drag Dropped");
				Dragboard db = event.getDragboard();

				// only accept the transfer if it's from a different index, and the drag index is a valid number
				try { 
					int from = Integer.parseInt(db.getString());
					if(row.getIndex() != from) {
						int to = row.isEmpty() ? currentQueue.getItems().size() : row.getIndex();
						QueueEntry dragged = currentQueue.getItems().remove(from); // pop the dragged element from the queue
						currentQueue.getItems().add(to, dragged);
						//						currentQueue.getItems().remove(from+1);
						event.consume();
					}
				} catch (NumberFormatException e) { log.error("Bad drag info \"" +db.getString()+ "\""); }
			});

			return row;
		});

		// configure DnD
		//		configQueueDragAndDrop();
	}





	private void initSongsTbl()
	{
		songsTbl_currCost.setCellValueFactory(cellData -> {
			return new SimpleStringProperty(String.format("%.1f", cellData.getValue().getSongCost()));
		});

		songsTbl_currPts.setCellValueFactory(cellData -> {
			return new SimpleStringProperty(String.format("%.3f", cellData.getValue().getSongPoints()));
		});

		songsTbl_lastPlay.setCellValueFactory(cellData -> {
			return new SimpleStringProperty(Util.fmtDateTimeDisp(cellData.getValue().getLastPlay()));
		});

		songsTbl_pts_perFranchise.setCellValueFactory(cellData -> {
			return new SimpleStringProperty(String.format("%.3f", cellData.getValue().getCostRule().getFranchisePts()));
		});

		songsTbl_pts_perOST.setCellValueFactory(cellData -> {
			return new SimpleStringProperty(String.format("%.3f", cellData.getValue().getCostRule().getOstPts()));
		});

		songsTbl_pts_perSong.setCellValueFactory(cellData -> {
			return new SimpleStringProperty(String.format("%.3f", cellData.getValue().getCostRule().getSongPts()));
		});

		songsTbl_pts_timeChecked.setCellValueFactory(cellData -> {
			return new SimpleStringProperty(Util.secondsToTimeString(cellData.getValue().getCostRule().getTimeChecked()));
		});

		songsTbl_pts_activeRule.setCellValueFactory(cellData -> {
			return new SimpleStringProperty(cellData.getValue().getCostRule().getOverrideId());
		});

		songsTbl_rating.setCellValueFactory(cellData -> {
			return new SimpleStringProperty(cellData.getValue().getRatingTxt());
		});

		songsTbl_songName.setCellValueFactory(cellData -> {
			return new SimpleStringProperty(cellData.getValue().getSongName());
		});

		songsTbl_songLength.setCellValueFactory(cellData -> {
			return new SimpleStringProperty(Util.secondsToTimeString(cellData.getValue().getSongLength()));
		});

		songsTbl_timesPlayed.setCellValueFactory(cellData -> {
			return new SimpleStringProperty(String.valueOf(cellData.getValue().getTimesPlayed()));
		});


		songsTbl.setRowFactory(data -> {
			TableRow<Song> row = new TableRow<>();
			return row;
		});
	}





	private void initOverridesTbl()
	{
		overridesTbl_franchisePts.setCellValueFactory(cellData -> {
			return new SimpleStringProperty(String.format("%f", cellData.getValue().getFranchisePts()));
		});

		overridesTbl_id.setCellValueFactory(cellData -> {
			return new SimpleStringProperty(cellData.getValue().getOverrideId());
		});

		overridesTbl_ostPts.setCellValueFactory(cellData -> {
			return new SimpleStringProperty(String.format("%f", cellData.getValue().getOstPts()));
		});

		overridesTbl_songPts.setCellValueFactory(cellData -> {
			return new SimpleStringProperty(String.format("%f", cellData.getValue().getSongPts()));
		});

		overridesTbl_timeChecked.setCellValueFactory(cellData -> {
			return new SimpleStringProperty(Util.secondsToTimeString(cellData.getValue().getTimeChecked()));
		});

		overridesTbl.setRowFactory(data -> {
			TableRow<OverrideRule> row = new TableRow<>();
			return row;
		});
	}









	/**
	 * checks all the introductory asserts on the fxml includes to verify that the fxml file is good
	 */
	private void doAsserts() {
		assert addSongBtn != null : "fx:id=\"addSongBtn\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert addRandomBtn != null : "fx:id=\"addRandomBtn\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert resetFoobarBtn != null : "fx:id=\"resetFoobarBtn\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert foobarPlayStatusBtn != null : "fx:id=\"foobarPlayStatusBtn\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert skipSongBtn != null : "fx:id=\"skipSongBtn\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert currentQueue != null : "fx:id=\"currentQueue\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert currentQueue_NameCol != null : "fx:id=\"currentQueue_NameCol\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert currentQueue_OSTCol != null : "fx:id=\"currentQueue_OSTCol\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert currentQueue_UsernameCol != null : "fx:id=\"currentQueue_UsernameCol\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert currentQueue_TimeRequestedCol != null : "fx:id=\"currentQueue_TimeRequestedCol\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert currentQueue_LengthCol != null : "fx:id=\"currentQueue_LengthCol\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert currentQueue_ratingCol != null : "fx:id=\"currentQueue_ratingCol\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert queueTableMenuRemoveSong != null : "fx:id=\"queueTableMenuRemoveSong\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert ctlPctRandom != null : "fx:id=\"ctlPctRandom\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert ctlMinQueueLength != null : "fx:id=\"ctlMinQueueLength\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert ctlMaxQueueLength != null : "fx:id=\"ctlMaxQueueLength\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert ctlCooldownPts != null : "fx:id=\"ctlCooldownPts\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert ctlSongLeadover != null : "fx:id=\"ctlSongLeadover\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert ctlPlaylistRoot != null : "fx:id=\"ctlPlaylistRoot\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert ctlRequestMode != null : "fx:id=\"ctlRequestMode\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert ctlBaseCost != null : "fx:id=\"ctlBaseCost\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert indQueueOpen != null : "fx:id=\"indQueueOpen\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert indQueueLen != null : "fx:id=\"indQueueLen\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert indQueueSize != null : "fx:id=\"indQueueSize\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert indQueueFullnes != null : "fx:id=\"indQueueFullnes\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert indQueueRng != null : "fx:id=\"indQueueRng\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert ostTree != null : "fx:id=\"ostTree\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert songsTbl != null : "fx:id=\"songsTbl\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert songsTbl_songName != null : "fx:id=\"songsTbl_songName\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert songsTbl_songLength != null : "fx:id=\"songsTbl_songLength\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert songsTbl_currPts != null : "fx:id=\"songsTbl_currPts\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert songsTbl_currCost != null : "fx:id=\"songsTbl_currCost\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert songsTbl_lastPlay != null : "fx:id=\"songsTbl_lastPlay\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert songsTbl_timesPlayed != null : "fx:id=\"songsTbl_timesPlayed\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert songsTbl_rating != null : "fx:id=\"songsTbl_rating\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert songsTbl_pts_perSong != null : "fx:id=\"songsTbl_pts_perSong\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert songsTbl_pts_perOST != null : "fx:id=\"songsTbl_pts_perOST\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert songsTbl_pts_perFranchise != null : "fx:id=\"songsTbl_pts_perFranchise\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert songsTbl_pts_timeChecked != null : "fx:id=\"songsTbl_pts_timeChecked\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert overridesTbl != null : "fx:id=\"overridesTbl\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert overridesTbl_id != null : "fx:id=\"overridesTbl_id\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert overridesTbl_songPts != null : "fx:id=\"overridesTbl_songPts\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert overridesTbl_ostPts != null : "fx:id=\"overridesTbl_ostPts\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert overridesTbl_franchisePts != null : "fx:id=\"overridesTbl_franchisePts\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert overridesTbl_timeChecked != null : "fx:id=\"overridesTbl_timeChecked\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert clearSysoConsoleBtn != null : "fx:id=\"clearSysoConsoleBtn\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert clearSyserrConsoleBtn != null : "fx:id=\"clearSyserrConsoleBtn\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert sysoLogTxtbox != null : "fx:id=\"sysoLogTxtbox\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert syserrLogTxtbox != null : "fx:id=\"syserrLogTxtbox\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert nowPlayingLabel != null : "fx:id=\"nowPlayingLabel\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert activeQueueMenu != null : "fx:id=\"activeQueueMenu\" was not injected: check your FXML file 'MainWindow.fxml'.";
	}
	public AnchorPane getRoot() {
		return root;
	}
	public MusicPlayer getFoobar() {
		return foobar;
	}
	public void setFoobar(MusicPlayer foobar) {
		this.foobar = foobar;
	}
	public PlaylistGenerator getPlGen() {
		return plGen;
	}
	public PointRecalculator getPointCalc() {
		return pointCalc;
	}
	public void setPlGen(PlaylistGenerator plGen) {
		this.plGen = plGen;
	}
	public void setPointCalc(PointRecalculator pointCalc) {
		this.pointCalc = pointCalc;
	}
	public SongDAO getSongDao() {
		return songDao;
	}
	public void setSongDao(SongDAO songDao) {
		this.songDao = songDao;
	}
	public void setPlaylistManager(PlaylistManager playlistManager) {
		System.out.println("Playlist Manager: " +playlistManager);
		this.playlistManager = playlistManager;
	}
	public void setRuleManager(OverrideRuleManager ruleManager) {
		this.ruleManager = ruleManager;
	}
	public void setRuleDao(OverrideRuleDAO ruleDao) {
		this.ruleDao = ruleDao;
	}
	public void setQueueManager(QueueManager queueManager) {
		this.queueManager = queueManager;
	}
	public void setSettingsManager(SettingsManager settingsManager) {
		this.settingsManager = settingsManager;
	}
	public void setHistoryManager(HistoryManager historyManager) {
		this.historyManager = historyManager;
	}
	public void setPrefs(Preferences prefs) {
		this.prefs = prefs;
	}
	public void setQueueAdder(QueueAdder queueAdder) {
		this.queueAdder = queueAdder;
	}
	public void setFileManager(FileManager fileManager) {
		this.fileManager = fileManager;
	}
	public void setPropertyManager(PropertyManager propertyManager) {
		this.propertyManager = propertyManager;
	}
}












