package com.dystify.kkdystrack.v2.manager;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.event.EventListener;

import com.dystify.kkdystrack.v2.core.event.types.SettingsUpdatedEvent;
import com.dystify.kkdystrack.v2.dao.SettingsDAO;
import com.dystify.kkdystrack.v2.model.SettingVal;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.util.Callback;

public class SettingsManager extends AbstractManager 
{
	private SettingsDAO settingsDao;
	private ObservableMap<String, SettingVal> settings;
	private Callback<Map<String, SettingVal>, Void> onDBUpdated;
	
	public SettingsManager() {
		settings = FXCollections.observableHashMap();
		
	}
	
	
	/**
	 * Writes the current settings out to the database
	 */
	public void writeSettings() {
		settingsDao.putSettings(settings);
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
		Map<String, SettingVal> m = new HashMap<>(1);
		m.put(key, value);
		settingsDao.putSettings(m);
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
		settings.putAll(settingsDao.getAllSettings());
		if(onDBUpdated != null)
			onDBUpdated.call(settings);
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
}
