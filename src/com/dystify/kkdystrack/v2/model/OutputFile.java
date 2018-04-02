package com.dystify.kkdystrack.v2.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;


/**
 * represents an auto-updating file on the filesystem, bound to a StringProperty. Whenever that 
 * property updates, the contents of the file will be replaced with that of the StringProperty. This
 * is to send updates to OBS and fussbot easily, albeit crude. It will also deliberately restrict update
 * rates to some predefined limit in order to reduce drive wear.
 * @author Duemmer
 *
 */
public class OutputFile 
{
	protected StringProperty boundProp;
	protected int minUpdateMillis;
	protected long lastUpdate = 0;
	protected Path targetFile;
	protected Logger log;
	
	public OutputFile(String targetPath) {
		log = LogManager.getFormatterLogger("OutputFile_" +targetPath);
		boundProp = new SimpleStringProperty();
		this.targetFile = Paths.get(targetPath);
	}
	
	
	
	@PostConstruct private void init() throws IOException {
		bindFileWriteListener();
	}



	/**
	 * Adds a file writing listener to the bound property to start writing file changes. 
	 * Will automatically create the necessary directory tree
	 * @throws IOException if the directory couldn't be created
	 */
	protected void bindFileWriteListener() throws IOException {
		Files.createDirectories(targetFile.getParent());
		boundProp.addListener((obs, oldVal, newVal) -> {
			if(System.currentTimeMillis() - lastUpdate > minUpdateMillis) {
				lastUpdate = System.currentTimeMillis();
				try { Files.write(targetFile, newVal.getBytes()); } 
				catch (IOException e) {
					log.error("Error writing to target file \"" +targetFile.toString()+"\"", e);
				}
			}
		});
	}
	

	public StringProperty BoundProp() {
		return boundProp;
	}


	public void setMaxUpdateRate(int maxUpdateRate) {
		this.minUpdateMillis = maxUpdateRate;
	}	
}












