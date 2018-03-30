package com.dystify.kkdystrack.v2.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.dystify.kkdystrack.v2.model.SettingVal;

public class SettingsDAO {
	private NamedParameterJdbcTemplate jdbcTemplate;
	
	public static class SettingsResultSetMapper implements ResultSetExtractor<Map<String, SettingVal>> {

		@Override public Map<String, SettingVal> extractData(ResultSet rs) throws SQLException, DataAccessException {
			Map<String, SettingVal> settings = new HashMap<>();
			while(rs.next()) {
				double numericVal = rs.getDouble("num_val");
				String strVal = rs.getString("str_val");
				String setting = rs.getString("setting");
				
				settings.put(setting, new SettingVal(numericVal, strVal));
			}
			return settings;
		}
	}
	
	
	
	
	
	public SettingsDAO(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
	
	
	
	
	
	/**
	 * Fetches all registered settings from the database
	 * @return
	 */
	public Map<String, SettingVal> getAllSettings() {
		return jdbcTemplate.query("SELECT * FROM general_settings", new SettingsResultSetMapper());
	}



	/**
	 * Writes all of the provided settings to the database
	 * @param settings
	 */
	public void putSettings(Map<String, SettingVal> settings) {
		String sql = "REPLACE INTO general_settings (setting, num_val, str_val) VALUES (:setting, :num, :str)";
		MapSqlParameterSource[] params = new MapSqlParameterSource[settings.size()];
		
		int i = 0;
		for(Entry<String, SettingVal> e : settings.entrySet()) {
			params[i] = new MapSqlParameterSource()
					.addValue("setting", e.getKey())
					.addValue("num", e.getValue().getNumericVal())
					.addValue("str", e.getValue().getStringVal());
			i++;
		}
		jdbcTemplate.batchUpdate(sql, params);	
	}
}















