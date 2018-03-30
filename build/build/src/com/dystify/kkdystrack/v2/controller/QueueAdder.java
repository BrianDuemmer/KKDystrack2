package com.dystify.kkdystrack.v2.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javax.annotation.PostConstruct;

import com.dystify.kkdystrack.v2.core.exception.QueueNotFoundException;
import com.dystify.kkdystrack.v2.core.util.SelfManagedFxmlUI;
import com.dystify.kkdystrack.v2.manager.QueueManager;
import com.dystify.kkdystrack.v2.model.queue.SongQueue;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;

public class QueueAdder extends SelfManagedFxmlUI
{
    @FXML private ResourceBundle resources;
    @FXML private URL location;
    @FXML private TextField queueName;
    @FXML private CheckBox deleteOnEmpty;
    
    private QueueManager queueManager;
    
    private boolean wasCanceled = false;
    
    
	public QueueAdder(String fxmlLocation, String title, Image favicon) throws IOException {
		super(fxmlLocation, favicon);
		stage.setOnCloseRequest((event) -> cancel(null));
		stage.setTitle(title);
	}
    

    @FXML void addQueue(ActionEvent event) {
    	wasCanceled = false;
    	stage.close();
    }

    @FXML void cancel(ActionEvent event) {
    	wasCanceled = true;
    	stage.close();
    }
    
    
    
    /**
     * Pops up a message box prompting the user to enter details to create a new queue
     * @return
     */
    public SongQueue promtForQueueAdd() {
    	reset();
    	stage.showAndWait();
    	if(!wasCanceled) {
    		SongQueue s;
			try {
				s = queueManager.createDatabaseQueue(queueName.getText(), deleteOnEmpty.isSelected());
				if(s == null) {
	    			Alert a = new Alert(AlertType.ERROR);
	    			a.setTitle("Queue Error");
	    			a.setHeaderText("Unable to create queue \"" +queueName.getText()+ "\"");
	    			a.setContentText("Queue name should be unique and should contain at least one alphanumeric character!");
	    			a.showAndWait();
	    			s = promtForQueueAdd(); // prompt again
	    		}
			} catch (QueueNotFoundException e) {
				Alert a = new Alert(AlertType.ERROR);
    			a.setTitle("Queue Error");
    			a.setHeaderText("Unable to create queue \"" +queueName.getText()+ "\"");
    			a.setContentText("Internal Queue creation error! See error log.");
    			return null;
			}
			return s;
    	}
    	return null;
    }
	

	@Override
	public void reset() {
		wasCanceled = false;
		queueName.setText("");
		deleteOnEmpty.setSelected(false);
	}


	public void setQueueManager(QueueManager queueManager) {
		this.queueManager = queueManager;
	}

}
