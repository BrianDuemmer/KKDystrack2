package com.dystify.kkdystrack.v2.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.dystify.kkdystrack.v2.model.Song;
import com.dystify.kkdystrack.v2.model.SongAlias;

public class SongAliasDAO 
{
	private NamedParameterJdbcTemplate jdbcTemplate;
	private Logger log = LogManager.getLogger(getClass());


	public static class AliasRowMapper implements RowMapper<SongAlias>
	{
		@Override
		public SongAlias mapRow(ResultSet rs, int rowNum) throws SQLException {
			Song s = new SongDAO.SongRowMapper().mapRow(rs, rowNum);
			SongAlias a = new SongAlias();
			a.setSongAlias(s);
			a.setAliasID(rs.getString("alias_name"));
			return a;
		}
	}
	
	
	public SongAliasDAO(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}


	/**
	 * adds this song to this alias rule in the database, or does nothing if it already exists
	 * @param a
	 */
	public void addRule(SongAlias a) {
		String sql = "INSERT INTO song_alias(song_id, alias_name) VALUES (:sid, :name) ON DUPLICATE KEY IGNORE";
		MapSqlParameterSource src = new MapSqlParameterSource()
				.addValue("sid", a.getSongAlias().getSongId())
				.addValue("name", a.getAliasID());
		jdbcTemplate.update(sql, src);
		log.info("Added song " +a.getSongAlias().getDispText(true) +" to alias " +a.getAliasID());
	}
	
	
	
	/**
	 * removes this song from this alias from the list of aliases
	 * @param a
	 */
	public void removeRule(SongAlias a) {
		String sql = "DELETE FROM song_alias WHERE song_id=:sid AND alias_name=:name";
		MapSqlParameterSource src = new MapSqlParameterSource()
				.addValue("sid", a.getSongAlias().getSongId())
				.addValue("name", a.getAliasID());
		jdbcTemplate.update(sql, src);
		log.info("Removed song " +a.getSongAlias().getDispText(true) +" from alias " +a.getAliasID());
	}
	
	
	
	
	
	/**
	 * Removes all songs for the given alias id
	 * @param aliasId
	 */
	public void removeAllForAlias(String aliasId) {
		String sql = "DELETE FROM song_alias WHERE alias_name=:name";
		MapSqlParameterSource src = new MapSqlParameterSource()
				.addValue("name", aliasId);
		int numRemoved = jdbcTemplate.update(sql, src);
		log.info("Removed all ("+numRemoved+")songs for alias \"" +aliasId +"\"");
	}
	
	
	
	
	
	
	/**
	 * retrieves all registered aliases for the given songs
	 * @param songs
	 * @return
	 */
	public Map<Song, List<SongAlias>> getAllAliases(List<Song> songs) {
		String sql = "SELECT p.*, a.alias_name FROM playlist p "
				+ "INNER JOIN song_alias a ON a.song_id=p.song_id "
				+ "WHERE p.song_id IN (:songs)";
		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("sonds", songs);
		
		// get all the results to start off
		List<SongAlias> raw = jdbcTemplate.query(sql, params, new SongAliasDAO.AliasRowMapper());
		
		// now group them up by song
		Map<Song, List<SongAlias>> ret = new HashMap<>();
		for(SongAlias a : raw) {
			if(!ret.containsKey(a.getSongAlias())) // make sure a list with this key is in there first
				ret.put(a.getSongAlias(), new ArrayList<SongAlias>());
			List<SongAlias> tmp = ret.get(a.getSongAlias()); // append this alias to the respective entry
			tmp.add(a);
			ret.put(a.getSongAlias(), tmp);
		}
		
		return ret;
	}
	
	
	
	/**
	 * Retrieves all songs tied to a certain alias
	 * @param aliasId
	 * @return
	 */
	public List<SongAlias> getAllUnderAlias(String aliasId)
	{
		String sql = "SELECT p.*, a.alias_name FROM playlist p INNER JOIN song_alias a "
				+ "ON a.song_id=p.song_id WHERE a.alias_name=:name";
		MapSqlParameterSource src = new MapSqlParameterSource()
				.addValue("name", aliasId);
		return jdbcTemplate.query(sql, src, new AliasRowMapper());
	}
}











