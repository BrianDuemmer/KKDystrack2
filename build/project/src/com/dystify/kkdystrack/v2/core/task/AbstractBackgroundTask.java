package com.dystify.kkdystrack.v2.core.task;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dystify.kkdystrack.v2.core.util.SelfManagedFxmlUI;
import com.dystify.kkdystrack.v2.core.util.Util;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;

/**
 * This bean is used to launch and deploy background tasks. This will fully manage the 
 * lifestyle of these the task it runs, from loading it to starting and stopping it.
 * It also wil automatically control the User Interface element as well
 * @author Duemmer
 *
 */
public abstract class AbstractBackgroundTask extends SelfManagedFxmlUI
{
	private int uiUpdaterMillis;
	private String title;
	private boolean aborted = false;
	
	
	@FXML private ProgressBar progressBar;
	@FXML private Label dispTxtLabel;
	@FXML private Label etaLabel;
	
	protected Logger log = LogManager.getLogger(getClass());

	
	
	/**
	 * Creates a new BackgroundTaskLauncer. Loads all the necessary UI components, which are 
	 * saved to the instance returned by this. Note that the task is not started until explicitly
	 * calling the start() method.
	 * @param favicon 
	 * @param task
	 * @return
	 * @throws IOException
	 */
	public AbstractBackgroundTask(String title, int uiUpdateRate, Image favicon) throws IOException {
		super("/ui/BackgroundTaskView.fxml", favicon);
		this.title = title;
		this.uiUpdaterMillis = uiUpdateRate;
		stage.setTitle(title);
		stage.setOnCloseRequest((event) -> {
			event.consume();
			onAbort(null);
		});
	}
	
	
	
	




	/**
	 * Makes the user learn that their actions have consequences by showing a warning
	 * popup. Will call the task's abort() if the user chooses to follow through
	 * @param event
	 */
	@FXML private void onAbort(ActionEvent event) {
		
		// popup a new warning dialogue
		Alert a = new Alert(AlertType.CONFIRMATION);
		a.setTitle("Confirm Task Abort");
		a.setHeaderText("Really abort " +title+ "?");
		a.setContentText(getAbortWarning());
		
		Optional<ButtonType> b = a.showAndWait();
		if(b.get() == ButtonType.OK) {
			aborted = true;
			abort();
		}
	}
	
	
	@PostConstruct private void initialize() {}


	/**
	 * Loads up the dialog box and fires off the given  Will also periodically check on whether the task finished, and
	 * will shut things down accordingly. This is an asynchronous call.
	 */
	public void startTask() {
		ScheduledExecutorService sex = Executors.newSingleThreadScheduledExecutor();
		start();
		stage.show();
		Runnable updater = () -> {
			Platform.runLater(() -> {
				double pct = getPctDone();
//				log.info("PERCENT: " +pct);
				dispTxtLabel.setText(getDispText());
				progressBar.setProgress(pct);
//				log.info("BAR: " +progressBar.getProgress());
				int eta = getEta();
				if(eta > 0)
					etaLabel.setText("Estimated time remaining: " +Util.secondsToTimeString(eta));
				else
					etaLabel.setText("Estimated time remaining: unknown");
			});
		};
		
		// gotta wrap the executor in another thread so we can track the task ending and stop the executor
		new Thread(()->{
			int start = (int) (System.currentTimeMillis() / 1000);
			sex.scheduleWithFixedDelay(updater, 0, uiUpdaterMillis, TimeUnit.MILLISECONDS);
			while(!isFinished()) // wait for it to finish
				try {Thread.sleep(100);}catch(Exception e) {}
			sex.shutdown();
			Platform.runLater(() -> {
				stage.close();
				if(!aborted) {
					Alert a = new Alert(AlertType.INFORMATION);
					a.setHeaderText(title);
					a.setTitle("Task Finished");
					int now = (int) (System.currentTimeMillis() / 1000);
					a.setContentText(String.format("Finished %s in %s", title, Util.secondsToTimeString(now - start)));
					a.show();
				}
			});
			reset();
			this.aborted = false;
		}).start();
		
	}
	
	
	
	/** Starts the task running. This should be an asynchronous call! */
	protected abstract void start();
	
	/** Ends the task immediately. It is safe to assume the user already authorized it after a warning*/
	protected abstract void abort();
	
	/** Returns a percent (0-1) for representing how complete the task is, or -1 for an indeterminate completion percent*/
	public abstract double getPctDone();
	
	/** This text will be displayed as well as the percent completion bar that tells at what it is working on*/
	public abstract String getDispText();
	
	/** If the user attempts to halt the task, this warning will be displayed. If null or empty, no message will be displayed*/
	public abstract String getAbortWarning();
	
	public abstract int getEta();
	
	public abstract boolean isFinished();
}
