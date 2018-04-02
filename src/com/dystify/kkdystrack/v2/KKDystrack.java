package com.dystify.kkdystrack.v2;
	
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.io.IoBuilder;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.dystify.kkdystrack.v2.controller.MainWindowController;
import com.dystify.kkdystrack.v2.core.util.Util;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;


public class KKDystrack extends Application 
{ 
	public static final String APP_NAME = "KKDystrack_master";
	public static AbstractApplicationContext appCtx;
	public static MainWindowController mainWindowController;
	private static URL log4jConfigLocation;
	private static URL appCtxLocation;
	
	@Override
	public void start(Stage primaryStage) {
		try {
			appCtx = new FileSystemXmlApplicationContext(appCtxLocation.getPath());
			mainWindowController = appCtx.getBean("mainWindowController", MainWindowController.class);
			AnchorPane root = mainWindowController.getRoot();
			Scene scene = new Scene(root);
//			scene.getStylesheets().add(getClass().getResource("core/application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.show();
			primaryStage.setTitle("K. K. Dystrack Master");
			primaryStage.getIcons().add(appCtx.getBean("favicon", Image.class));
			primaryStage.setOnCloseRequest((event) -> {
				appCtx.registerShutdownHook();
				System.err.println("Shutting down");
				primaryStage.close();
				System.exit(0);
			});
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	
	
	
	public static void main(String[] args) throws Exception {
		System.out.println("Starting app...");
		try {
			// Load all the confifg files
			log4jConfigLocation = Util.loadFile("/config/log4j2.xml");
			appCtxLocation = Util.loadFile("/config/Beans.xml");
			
			// configure Log4j
			System.out.println("log4j2 config file: " +log4jConfigLocation.toString());
			
			// redirect syso / syserr to log4j
			PipedInputStream sysoIn = new PipedInputStream();
			System.setOut(new PrintStream(new PipedOutputStream(sysoIn)));
			IoBuilder.forLogger("System_out").setLevel(Level.INFO).filter(sysoIn);
			
			PipedInputStream syserrIn = new PipedInputStream();
			System.setOut(new PrintStream(new PipedOutputStream(syserrIn)));
			IoBuilder.forLogger("System_err").setLevel(Level.ERROR).filter(syserrIn);
			
			
			
			LoggerContext.getContext(false).setConfigLocation(log4jConfigLocation.toURI());
			
		} catch (FileNotFoundException | URISyntaxException e) {
			System.err.println("Failed to Load core configuration data!");
			e.printStackTrace();
			System.exit(1);
		}
		launch(args);
	}
}
