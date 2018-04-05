package com.dystify.kkdystrack.v2.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Special type of OutputFile that in addition to the main OutputFile, includes a following file.
 * Every time the main file is updated, before it updates it is renamed to the file specified
 * by targetPath, and then the normal update of the main file takes place. This effectively 
 * leaves the follower 1 update behind the main file.
 * @author Duemmer
 *
 */
public class OutputFileWithFollower extends OutputFile 
{
	private Path lastPlayingPath;
	
	public OutputFileWithFollower(String mainFilePath, String followingFilePath) {
		super(mainFilePath);
		lastPlayingPath = Paths.get(followingFilePath);
	}
	
	
	@Override protected void bindFileWriteListener() throws IOException {
		Files.createDirectories(targetFile.getParent());
		Files.createDirectories(lastPlayingPath.getParent());
		boundProp.addListener((obs, oldVal, newVal) -> {
			if(System.currentTimeMillis() - lastUpdate > minUpdateMillis) {
				lastUpdate = System.currentTimeMillis();
				try { // first replace the follower file with the current main file, before changing the main file
					if(targetFile.toFile().isFile())// make sure the main file exists before attempting to move it
						Files.copy(targetFile, lastPlayingPath, StandardCopyOption.REPLACE_EXISTING);
					Files.write(targetFile, newVal.getBytes()); 
				} 
				catch (IOException e) {
					log.error("Error writing to target file", e);
				}
			}
		});
	}

}
