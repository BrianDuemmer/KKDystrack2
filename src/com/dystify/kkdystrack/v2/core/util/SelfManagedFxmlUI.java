package com.dystify.kkdystrack.v2.core.util;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

/**
 * utility class that allows FXML GUIs like popups to function as 
 * self contained objects without the need of external helping javafx boilerplate.
 * @author Duemmer
 *
 */
public abstract class SelfManagedFxmlUI 
{
	protected Stage stage;
	protected Pane rootPane;
	
	
	public SelfManagedFxmlUI(String fxmlLocation, Image favicon) throws IOException {
		FXMLLoader loader = new FXMLLoader();
		loader.setController(this);
		loader.setLocation(Util.loadFile(fxmlLocation));
		rootPane = loader.load();
		stage = new Stage();
		stage.getIcons().add(favicon);
		stage.setScene(new Scene(rootPane));
	}
	
	
	/** Resets the task back to an initialized state, ready to run again */
	public abstract void reset();
	

}
