package com.dystify.kkdystrack.v2.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import com.dystify.kkdystrack.v2.core.util.SelfManagedFxmlUI;
import com.dystify.kkdystrack.v2.core.util.Util;
import com.dystify.kkdystrack.v2.model.OverrideRule;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;

public class AddOverrideRuleController extends SelfManagedFxmlUI
{
	@FXML private ResourceBundle resources;
	@FXML private URL location;

	@FXML private TextField ctlOverrideID;
	@FXML private TextField ctlSongPoints;
	@FXML private TextField ctlOstPoints;
	@FXML private TextField ctlFranchisePts;
	@FXML private TextField ctlTimeChecked;

	private boolean wasAborted = false;
	private double timeChecked = 0;

	@FXML void addRule(ActionEvent event) {
		stage.close();
	}

	@FXML void cancel(ActionEvent event) {
		wasAborted = true;
		stage.close();
	}

	@FXML void initialize() {
		System.out.println(location.toString());
		assert ctlOverrideID != null : "fx:id=\"ctlOverrideID\" was not injected: check your FXML file 'addOverrideRuleView.fxml'.";
		assert ctlSongPoints != null : "fx:id=\"ctlSongPoints\" was not injected: check your FXML file 'addOverrideRuleView.fxml'.";
		assert ctlOstPoints != null : "fx:id=\"ctlOstPoints\" was not injected: check your FXML file 'addOverrideRuleView.fxml'.";
		assert ctlFranchisePts != null : "fx:id=\"ctlFranchisePts\" was not injected: check your FXML file 'addOverrideRuleView.fxml'.";
		assert ctlTimeChecked != null : "fx:id=\"ctlTimeChecked\" was not injected: check your FXML file 'addOverrideRuleView.fxml'.";

		Util.configTextFieldAsNumericInput(ctlFranchisePts, true, (num)->{return null;});
		Util.configTextFieldAsNumericInput(ctlOstPoints, true, (num)->{return null;});
		Util.configTextFieldAsNumericInput(ctlSongPoints, true, (num)->{return null;});

		Util.configTextFieldAsTimeIntervalInput(ctlTimeChecked, (secs)->{timeChecked = (double) secs; return null;} );
	}



	public AddOverrideRuleController(String fxmlLocation, String title, Image favicon) throws IOException {
		super(fxmlLocation, favicon);
		stage.setTitle(title);
		stage.setOnCloseRequest((event) -> {
			cancel(null);
		});
	}



	public OverrideRule promptForRule() {
		stage.showAndWait();
		OverrideRule fromInput = null;
		if(!wasAborted) {
			fromInput = fromCtls();
		}
		reset(); // make sure it is initialized for repeat calls
		return fromInput;
	}
	
	
	
	public OverrideRule editRule(OverrideRule orig, boolean allowOverrideIdEdit) {
		ctlOverrideID.setDisable(!allowOverrideIdEdit); // can't change the ID of the rule, or it'll become a whole new rule, so restrict if necessary
		setCtlsFromRule(orig);
		stage.showAndWait();
		if(!wasAborted)
			orig = fromCtls();
		reset();
		return orig;
		
	}
	
	
	/**
	 * generates a new overrideRule based on the current values of the controle
	 * @return
	 */
	private OverrideRule fromCtls() {
		OverrideRule fromInput = new OverrideRule();
		fromInput.setOverrideId(ctlOverrideID.getText());
		fromInput.setTimeChecked(timeChecked);
		fromInput.setSongPts(Double.parseDouble(ctlSongPoints.getText()));
		fromInput.setOstPts(Double.parseDouble(ctlOstPoints.getText()));
		fromInput.setFranchisePts(Double.parseDouble(ctlFranchisePts.getText()));
		
		return fromInput;
	}
	
	
	private void setCtlsFromRule(OverrideRule o) {
		ctlFranchisePts.setText(String.valueOf(o.getFranchisePts()));
		ctlSongPoints.setText(String.valueOf(o.getSongPts()));
		ctlOstPoints.setText(String.valueOf(o.getOstPts()));
		ctlOverrideID.setText(o.getOverrideId());
		ctlTimeChecked.setText(Util.secondsToTimeString(o.getTimeChecked()));
	}



	@Override public void reset() {
		wasAborted = false;
		ctlOverrideID.setDisable(false);
		ctlFranchisePts.setText("");
		ctlOstPoints.setText("");
		ctlOverrideID.setText("");
		ctlSongPoints.setText("");
		ctlTimeChecked.setText("");
	}


}





