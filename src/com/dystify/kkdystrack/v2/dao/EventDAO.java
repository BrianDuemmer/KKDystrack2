package com.dystify.kkdystrack.v2.dao;

import java.lang.reflect.Constructor;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

// Make sure to have all the valid types handy so we can dynamically allocate them correctly
import com.dystify.kkdystrack.v2.core.event.types.*;
import com.dystify.kkdystrack.v2.core.exception.DystrackEventException;

public class EventDAO 
{
	private NamedParameterJdbcTemplate jdbcTemplate;
	private static Logger log = LogManager.getLogger(EventDAO.class);


	public static class EventRowMapper implements RowMapper<GenericDystrackEvent> 
	{
		@SuppressWarnings("unchecked")
		@Override public GenericDystrackEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
			// get resultset info
			final int event_id = rs.getInt("event_id");
			final Date time = new Date(1000L * rs.getLong("time"));
			final String typeStr = rs.getString("type");
			final String rawData = rs.getString("data");
			final String sender = rs.getString("sender");
			final String description = rs.getString("description");

			// get the reference to the top level listener for the event source
			final Object src = new Object(); //TODO read this object from Spring

			// Allocate correct event type
			GenericDystrackEvent event = null;
			try {
				// NOTE: due to the generic nature of asSubclass(...), and that the type parameter is the same as for this class, we have to manually cast the allocated object
				Class<? extends GenericDystrackEvent> clazz = (Class<? extends GenericDystrackEvent>) Class.forName("com.dystify.kkdystrack.v2.core.event.types." +typeStr.trim());
				Constructor<? extends GenericDystrackEvent> constructor = clazz.getConstructor(Object.class, Boolean.TYPE, String.class, Integer.TYPE, Date.class, String.class);
				event = constructor.newInstance(src, false, sender, event_id, time, description);

				event.parseDataJSON(rawData);
			} catch(ClassNotFoundException cnf) { // unknown event type, just allocate a generic event
				log.warn("No valid event found for event type \"" +typeStr+ "\"");
				event = new GenericDystrackEvent(src, false, sender, event_id, time, String.format("TYPE:\"%s\" DESC: \"%s\"", typeStr, description));
			} catch(Exception e) {
				log.fatal("Failed to allocate class for event type \"" +typeStr+ "\"");
				log.fatal(e);
			}
			return event;
		}

	}




	public EventDAO(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}



	public List<GenericDystrackEvent> getAllNewerThan(int minEventID) {
		String sql = "SELECT * FROM event_log WHERE event_id > :minId";
		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("minId", minEventID);
		return jdbcTemplate.query(sql, params, new EventRowMapper());
	}



	/**
	 * Makes sure the given ID is valid for use in event log reads; that is, 
	 * {@code idToCheck} lies on the bounds {@code [min_event_id-1, max_event_id]}. 
	 * Pass -1 to get all IDs, or -2 to get the newest ID.
	 * @param idToCheck
	 * @return {@code idToCheck} unaltered, or {@code max_event_id} if it is invalid
	 */
	public int verifyValidMinEventID(int idToCheck) {
		String sql = "SELECT COALESCE(MIN(event_id), 0) AS min, COALESCE(MAX(event_id), 0) AS max FROM event_log";
		Map<String, Object> res = jdbcTemplate.queryForMap(sql, new HashMap<String, Object>());
		int min = Math.toIntExact((long) res.get("min"));
		int max = Math.toIntExact((long) res.get("max"));
		if(idToCheck < min-1 || idToCheck > max)
			idToCheck = max;
		return idToCheck;
	}

	
	

	/**
	 * Publishes an event to the database. This will fail if trying to publish an event that was generated externally
	 * because that could leave an infinite loop scenario where the same event keeps getting written to the database and read back
	 * @param event
	 * @throws DystrackEventException if this event originated from an external location
	 */
	public void publishEventToRemote(GenericDystrackEvent event) throws DystrackEventException {
		if(event.isInternal()) {
			String sql = "INSERT INTO event_log(time, data, type, sender, description) VALUES(:time, :data, :sender, :description)";
			MapSqlParameterSource params = new MapSqlParameterSource()
					.addValue("time", event.getTime().getTime() / 1000L)
					.addValue("data", event.getRawData())
					.addValue("sender", event.getGeneratedBy())
					.addValue("description", event.getDescription());
			jdbcTemplate.update(sql, params);
		} else
			throw new DystrackEventException("Cannot post an external event to the database!");
	}

}

















