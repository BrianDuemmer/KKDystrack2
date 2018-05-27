package com.dystify.kkdystrack.v2.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.dystify.kkdystrack.v2.core.exception.OverrideRuleException;
import com.dystify.kkdystrack.v2.model.OverrideRule;
import com.dystify.kkdystrack.v2.model.Song;

public class OverrideRuleDAO 
{
	private NamedParameterJdbcTemplate jdbcTemplate;
	private Logger log = LogManager.getLogger(getClass());


	public static class OverrideRuleRowMapper implements RowMapper<OverrideRule>
	{
		@Override
		public OverrideRule mapRow(ResultSet rs, int rowNum) throws SQLException {
			OverrideRule o = new OverrideRule();

			o.setFranchisePts(rs.getDouble("franchise_pts"));
			// o.setId(rs.getInt("id"));
			o.setOstPts(rs.getDouble("ost_pts"));
			o.setOverrideId(rs.getString("override_id"));
			o.setSongPts(rs.getDouble("song_pts"));
			o.setTimeChecked(rs.getDouble("time_checked"));

			return o;
		}
	}




	public OverrideRuleDAO(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}



	/**
	 * Given a list of songs, gets the applied override rules for each song. If songs is null, 
	 * returns all rules for the entire playlist
	 * @param songs
	 * @return
	 */
	public Map<Song, OverrideRule> getAppliedOverrides(List<Song> songs) {
		MapSqlParameterSource src = new MapSqlParameterSource();
		String sql = "SELECT p.*,    \r\n" + 
				"(\r\n" + 
				"    SELECT override_id FROM overrides\r\n" + 
				"    WHERE p.song_id LIKE CONCAT(override_id, '%')\r\n" + 
				"    ORDER BY CHAR_LENGTH(override_id) DESC\r\n" + 
				"	LIMIT 1\r\n" + 
				") AS override_id,\r\n" + 
				"o.song_pts,\r\n" + 
				"o.ost_pts,\r\n" + 
				"o.franchise_pts,\r\n" + 
				"o.time_checked,\r\n" + 
				"o.id\r\n" + 
				"FROM playlist p INNER JOIN overrides o ON o.override_id=override_id\r\n"
				+ "GROUP BY p.song_id ";
		if(songs != null) {
			sql += "WHERE p.song_id IN (:songs)";
			src.addValue("songs", songs);
		}

		// it's probably easier to extract duplicate info so do that
		List<Entry<Song, OverrideRule>> raw = jdbcTemplate.query(sql, src, new RowMapper<Entry<Song, OverrideRule>>() 
		{
			@Override
			public Entry<Song, OverrideRule> mapRow(ResultSet rs, int rowNum) throws SQLException {
				Song s = new SongDAO.SongRowMapper().mapRow(rs, rowNum);
				OverrideRule o = new OverrideRuleRowMapper().mapRow(rs, rowNum);
				return new AbstractMap.SimpleEntry<Song, OverrideRule>(s,o);
			}
		});

		// convert to map
		Map<Song, OverrideRule> ret = new HashMap<>();
		for(Entry<Song, OverrideRule> e : raw)
			ret.put(e.getKey(), e.getValue());
		return ret;

	}
	
	
	
	/**
	 * Commits this rule to the database. Will update or replace the rule, if it already
	 * exists, or just insert it fresh
	 * @param rule
	 */
	public void putRule(OverrideRule rule) {
		String sql = "INSERT INTO overrides (override_id,song_pts,ost_pts,franchise_pts,time_checked)"
				+ "VALUES(:oid, :songPts, :ostPts, :franchisePts, :timeChecked) "
				+ "ON DUPLICATE KEY UPDATE "
				+ "song_pts=:songPts, "
				+ "ost_pts=:ostPts, "
				+ "franchise_pts=:franchisePts, "
				+ "time_checked=:timeChecked";
		
		Map<String, Object> params = new HashMap<>();
		params.put("oid", rule.getOverrideId());
		params.put("songPts", rule.getSongPts());
		params.put("ostPts", rule.getOstPts());
		params.put("franchisePts", rule.getFranchisePts());
		params.put("timeChecked", rule.getTimeChecked());
		
		jdbcTemplate.update(sql, params);
	}
	
	
	/**
	 * Deletes a rule from the database. Note that this will NOT remove the root rule, due to the necessity
	 * that all songs must have 
	 * @param rule
	 * @throws OverrideRuleException 
	 */
	public void dropRule(OverrideRule rule) throws OverrideRuleException {
		if(rule.isRootOverride())
			throw new OverrideRuleException("Cannot delete the root override!");
		
		String sql = "DELETE FROM overrides WHERE override_id=:oid";
		Map<String, Object> params = new HashMap<>();
		params.put("oid", rule.getOverrideId());
		jdbcTemplate.update(sql, params);
	}
	
	
	
	
	public List<OverrideRule> fetchAllRules() {
		String sql = "SELECT * FROM overrides";
		return jdbcTemplate.query(sql, new OverrideRuleRowMapper());
	}
	
	
	
	
	/**
	 * Checks whether the database already contains an <code>OverrideRule</code> with the same
	 * {@code overrideId} as {@code o}
	 * @param o the override rule to check. If null, returns false
	 * @return
	 */
	public boolean ruleAlreadyExists(OverrideRule o) {
		if (o != null) {
			String sql = "SELECT COUNT(*) FROM overrides WHERE override_id=:oid";
			MapSqlParameterSource params = new MapSqlParameterSource().addValue("oid", o.getOverrideId());
			return jdbcTemplate.query(sql, params, (rs) -> {
				rs.next();
				return rs.getInt(1) > 0;
			});
		}
		return false;
	}
}












