package com.dystify.kkdystrack.v2.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.springframework.context.event.EventListener;

import com.dystify.kkdystrack.v2.core.event.types.SettingsUpdatedEvent;
import com.dystify.kkdystrack.v2.dao.SettingsDAO;
import com.dystify.kkdystrack.v2.model.SettingVal;
import com.dystify.kkdystrack.v2.service.DBTask;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.util.Callback;

public class SettingsManager extends AbstractManager 
{
	private SettingsDAO settingsDao;
	private ObservableMap<String, SettingVal> settings;
	private Callback<Map<String, SettingVal>, Void> onDBUpdated;
	private ExecutorService dbTaskQueue;
	
	public SettingsManager() {
		settings = FXCollections.observableHashMap();
		
	}
	
	
	/**
	 * Writes the current settings out to the database
	 */
	public void writeSettings() {
		dbTaskQueue.submit(new DBTask("Write Settings", () -> {
			settingsDao.putSettings(settings);
		}));
	}
	
	
	@EventListener
	public void handleRemoteSettingsChange(SettingsUpdatedEvent event) {
		if(!event.isInternal()) {
			readSettings();
		}
	}
	
	
	
	/**
	 * Puts a singular setting to the database
	 * @param key
	 * @param value
	 */
	public void putSetting(String key, SettingVal value) {
		dbTaskQueue.submit(new DBTask("Put Setting", () -> {
			Map<String, SettingVal> m = new HashMap<>(1);
			m.put(key, value);
			settingsDao.putSettings(m);
		}));
	}
	
	
	/**
	 * Puts a set of keys to the database, independent of the settings stored internally in the settingsManager
	 * @param settings
	 */
	public void putMultipleSettings(Map<String, SettingVal> settings) {
		settingsDao.putSettings(settings);
	}
	
	
	/**
	 * Gets a singular setting value from the database, or a default if none is found
	 * @param key
	 * @return
	 */
	public SettingVal getSetting(String key) {
		return settingsDao.getAllSettings().getOrDefault(key, SettingVal.defaultVal);
	}
	
	
	
	public void readSettings() {
		dbTaskQueue.submit(new DBTask("Read Settings", () -> {
			settings.putAll(settingsDao.getAllSettings());
			Platform.runLater(() -> {
				if(onDBUpdated != null)
					onDBUpdated.call(settings);
			});
		}));
	}
	
	

	public ObservableMap<String, SettingVal> getSettings() {
		return settings;
	}

	public void setSettingsDao(SettingsDAO settingsDao) {
		this.settingsDao = settingsDao;
	}


	public void setOnDBUpdated(Callback<Map<String, SettingVal>, Void> onDBUpdated) {
		this.onDBUpdated = onDBUpdated;
	}


	public void setDbTaskQueue(ExecutorService dbTaskQueue) {
		this.dbTaskQueue = dbTaskQueue;
	}
}
