package com.dystify.kkdystrack.v2.dao;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.dystify.kkdystrack.v2.core.exception.SongException;
import com.dystify.kkdystrack.v2.core.util.Util;
import com.dystify.kkdystrack.v2.model.OST;
import com.dystify.kkdystrack.v2.model.OverrideRule;
import com.dystify.kkdystrack.v2.model.Song;

/**
 * Acts as a communication class to translate objects of the 
 * {@link Song} Class to the playlist table in the database
 * @author Duemmer
 *
 */
public class SongDAO 
{
	private NamedParameterJdbcTemplate jdbcTemplate;
	private static Logger log = LogManager.getLogger(SongDAO.class);
	
	public static class SongRowMapper implements RowMapper<Song>
	{
		@Override public Song mapRow(ResultSet rs, int rowNum) throws SQLException {
			Song s = new Song();
			
			String songId = rs.getString("song_id");
			try {
				if(songId != null) {
					
					// Since non-playlist songs in queue are possible, their data will have to be fetched elsewhere. Filesystem is the only solid choice
					boolean shouldFileLoad = rs.getString("song_name") == null;
					
					if(!shouldFileLoad) { // data is normally present
						s.setSongName(rs.getString("song_name"));
						s.setOstName(rs.getString("ost_name"));
						s.setSongFranchise(rs.getString("song_franchise"));
						s.setSongId(songId);
						s.setSongLength(rs.getDouble("song_length"));
						s.setSongPoints(rs.getDouble("song_points"));
						s.setRatingNum(rs.getInt("rating_num"));
						s.setRatingPct(rs.getDouble("rating_pct"));
						s.setLastPlay(rs.getTimestamp("last_play"));
						s.setTimesPlayed(rs.getInt("times_played"));
						s.setSongCost(rs.getDouble("song_cost"));
						
						s.setCostRule(new OverrideRuleDAO.OverrideRuleRowMapper().mapRow(rs, rowNum));
					} else { // song isn't in the playlist, attempt to file load
						s = loadFromFile(new File(songId));
					}
				} else {
					throw new SongException("SongRowMapper - Unable to load song information, SongID was not specified!");
				}
			} catch (SongException e) {
				log.error(e);
				return null;
			}
			
			return s;
		}
		
	}
	
	
	
	public SongDAO(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}




	/**
	 * Given a certain override rule, returns all songs in the playlist whose points 
	 * calculation is done off of this rule. SQL is based off of 
	 * {@link OverrideRuleDAO#getAppliedOverrides(List) OverrideRuleDAO.getAppliedOverrides(...)}
	 * @param rule
	 * @return
	 */
	public List<Song> getSongsAffectedByRule(OverrideRule rule) {
		String sql = "SELECT \r\n" + 
				"	p.*, \r\n" + 
				"	COUNT(r.song_id) AS rating_num, \r\n" + 
				"	COALESCE(AVG(r.rating_pct), -1) AS rating_pct,\r\n" + 
				"	MAX(h.time_played) AS last_play,\r\n" + 
				"	COUNT(h.time_played) AS times_played,\r\n" + 
				"	F_CALC_COST(p.song_id, \"\", 1) AS song_cost,\r\n" + 
				"	(\r\n" + 
				"	    SELECT override_id FROM overrides\r\n" + 
				"	    WHERE p.song_id LIKE CONCAT(override_id, '%')\r\n" + 
				"	    ORDER BY CHAR_LENGTH(override_id) DESC\r\n" + 
				"		LIMIT 1\r\n" + 
				"	) AS override_id,\r\n" + 
				"	o.song_pts,\r\n" + 
				"    o.ost_pts,\r\n" + 
				"    o.franchise_pts,\r\n" + 
				"    o.time_checked,\r\n" + 
				"    o.id\r\n" + 
				"	\r\n" + 
				"FROM playlist p \r\n" + 
				"    LEFT JOIN ratings r ON r.song_id = p.song_id \r\n" + 
				"    LEFT JOIN play_history h ON h.song_id = p.song_id AND \r\n" + 
				"	 	UNIX_TIMESTAMP() - UNIX_TIMESTAMP(h.time_played) < F_READ_NUM_PARAM('times_played_check', 100000000) \r\n" +
				"	 LEFT JOIN overrides o ON o.override_id=(\r\n" + 
				"	    SELECT override_id FROM overrides\r\n" + 
				"	    WHERE p.song_id LIKE CONCAT(override_id, '%')\r\n" + 
				"	    ORDER BY CHAR_LENGTH(override_id) DESC\r\n" + 
				"		LIMIT 1\r\n" + 
				"	)\r\n" + 
				"GROUP BY p.song_id \r\n" +
				"HAVING override_id=:oid";
		
		Map<String, Object> params = new HashMap<>();
		params.put("oid", rule.getOverrideId());
		return jdbcTemplate.query(sql, params, new SongRowMapper());
	}
	
	
	
	
	/**
	 * Returns the first song match with this song id, or null if it isn't found
	 * @param song_id
	 * @return
	 */
	public Song getSongBySongID(String song_id) {
		String sql = "SELECT \r\n" + 
				"	p.song_name, p.ost_name, p.song_length, p.song_franchise, p.song_points,\r\n" +
				"	:sid AS song_id,\r\n" + 
				"	COUNT(r.song_id) AS rating_num, \r\n" + 
				"	COALESCE(AVG(r.rating_pct), -1) AS rating_pct,\r\n" + 
				"	MAX(h.time_played) AS last_play,\r\n" + 
				"	COUNT(h.time_played) AS times_played,\r\n" + 
				"	F_CALC_COST(p.song_id, \"\", 1) AS song_cost,\r\n" + 
				"	(\r\n" + 
				"	    SELECT override_id FROM overrides\r\n" + 
				"	    WHERE p.song_id LIKE CONCAT(override_id, '%')\r\n" + 
				"	    ORDER BY CHAR_LENGTH(override_id) DESC\r\n" + 
				"		LIMIT 1\r\n" + 
				"	) AS override_id,\r\n" + 
				"	 o.song_pts,\r\n" + 
				"    o.ost_pts,\r\n" + 
				"    o.franchise_pts,\r\n" + 
				"    o.time_checked,\r\n" + 
				"    o.id\r\n" + 
				"	\r\n" + 
				"FROM playlist p \r\n" + 
				"    LEFT JOIN ratings r ON r.song_id = p.song_id \r\n" + 
				"    LEFT JOIN play_history h ON h.song_id = p.song_id AND " + 
				"		UNIX_TIMESTAMP() - UNIX_TIMESTAMP(h.time_played) < F_READ_NUM_PARAM('times_played_check', 100000000) " +
				"	LEFT JOIN overrides o ON o.override_id=(\r\n" + 
				"	    SELECT override_id FROM overrides\r\n" + 
				"	    WHERE p.song_id LIKE CONCAT(override_id, '%')\r\n" + 
				"	    ORDER BY CHAR_LENGTH(override_id) DESC\r\n" + 
				"		LIMIT 1\r\n" + 
				"	)\r\n" + 
				"WHERE p.song_id=:sid " +
				"GROUP BY p.song_id";
		Map<String, Object> params = new HashMap<>();
		params.put("sid", song_id);
		List<Song> hits = jdbcTemplate.query(sql, params, new SongRowMapper());
		Song ret = hits.isEmpty() ? null : hits.get(0);
		return ret;
	}
	
	
	
	/**
	 * Takes a list of songs and uploads them to the database.
	 * <p/><b>NOTE:</b> This will not update Song Points, as they are designed 
	 * to only be written to by the (very time intensive) point calculation routine
	 * @param songs all of the songs to upload
	 * @param skipDuplicates if true, will not perform any updates if the same song is found already
	 * @return the list of songs that were added / updated
	 */
	public List<Song> writeToPlaylist(List<Song> songs, boolean skipDuplicates) {
		String sql = "REPLACE INTO playlist (song_name, ost_name, song_franchise, song_length, song_id)\r\n" + 
				"	VALUES(:songName, :ostName, :songFranchise, :songLength, :sid)\r\n";/* + 
				"	WHERE song_id=:sid";*/
		
		// we don't want to deal with duplicates, so remove any from the pool
		List<Song> alreadyThere = new ArrayList<>();
		if(skipDuplicates) 
		{
			// All we need are song IDs to be able to use Song.equals(), so just work off of that
			alreadyThere = jdbcTemplate.query("SELECT song_id FROM playlist", new RowMapper<Song>() {
				@Override public Song mapRow(ResultSet rs, int rowNum) throws SQLException {
					Song s = new Song();
					s.setSongId(rs.getString("song_id"));
					return s;
				}
			});
			
			// can't just delete matches, as it messes up the source list in PlaylistGenerator. Instead just keep track of which need to be added and whichh don't
//			for(Song t : alreadyThere) // clean out any matches
//				songs.remove(t);
		}
		
		List<MapSqlParameterSource> params = new ArrayList<>();
		List<Song> addedNew = new ArrayList<>();
		for(Song s : songs) {
			boolean shouldSkip = skipDuplicates && alreadyThere.contains(s);
			if(!shouldSkip) {
				MapSqlParameterSource m = new MapSqlParameterSource()
						.addValue("sid", s.getSongId())
						.addValue("songName", s.getSongName())
						.addValue("ostName", s.getOstName())
						.addValue("songFranchise", s.getSongFranchise())
						.addValue("songLength", s.getSongLength());
				params.add(m);
				addedNew.add(s);
			}
		}
		
		//All the songs that made it here so far should have been updated / added, so just retun the list of all of them
		jdbcTemplate.batchUpdate(sql, params.toArray(new MapSqlParameterSource[params.size()]));
		return addedNew;
	}
	
	
	
	
	/**
	 * Loads song data based on information from an audio file
	 * @param songFile
	 * @return
	 */
	public static Song loadFromFile(File songFile) throws SongException {
		Song s = new Song();
		
		try {
			AudioFile f = AudioFileIO.read(songFile);
			Tag t = f.getTag();

			if(t != null) {
				s.setSongName(t.getFirst(FieldKey.TITLE));
				s.setOstName(t.getFirst(FieldKey.ALBUM));
			} else {
				log.warn("No tag found for song \"" +songFile.getAbsolutePath()+ "\""); // alert that a broken file was encountered
			}
			
			s.setSongLength(f.getAudioHeader().getTrackLength());
			s.setSongId(songFile.getAbsolutePath());
			s.setSongFranchise(songFile.getParentFile().getParentFile().getName());

			// Just use file / directory names for song / ost names if they are empty (not an MP3 or missing tag data)
			if(s.getSongName() == null || s.getSongName().isEmpty())
				s.setSongName(songFile.getName().substring(0, songFile.getName().lastIndexOf('.'))); // remove extension

			if(s.getOstName() == null || s.getOstName().isEmpty())
				s.setOstName(songFile.getParentFile().getName());
		} catch (Exception e) { // propagate any errors
			e.printStackTrace();
			throw new SongException(e);
		}
		
		return s;
	}
	
	
	
	
	/**
	 * Will call the proper database routines and recalculate points for each provided song.
	 * <p><b>NOTE:</b> This just appends the statement calls for each song, max about 600 bytes each.
	 * Because of this, there is an upper limit to how many can be committed at once, which happens
	 * to be about 25000 songs, conservative. Commiting more than that can cause serious issues! That being
	 * said, it is probably more advisable to do it in smaller, more manageble chunks, of perhaps 50 - 100
	 * @param songs the songs to calculate points for
	 */
	public void calculatePoints(List<Song> songs) {
		String sql = "CALL P_CALC_PTS_HELP(:sid)";
		MapSqlParameterSource[] params = new MapSqlParameterSource[songs.size()];
		
		for(int i=0; i<songs.size(); i++) {
			MapSqlParameterSource p = new MapSqlParameterSource()
					.addValue("sid", songs.get(i).getSongId());
			params[i] = p;
		}
		
		jdbcTemplate.batchUpdate(sql, params);
	}
	
	
	
	/**
	 * Queries the playlist table and retrieves a flat list of all songs registered 
	 * in the playlist
	 * @return
	 */
	public List<Song> getAllSongs() {
		String sql = "SELECT \r\n" + 
				"	p.*, \r\n" + 
				"	COUNT(r.song_id) AS rating_num, \r\n" + 
				"	COALESCE(AVG(r.rating_pct), -1) AS rating_pct,\r\n" + 
				"	MAX(h.time_played) AS last_play,\r\n" + 
				"	COUNT(h.time_played) AS times_played,\r\n" + 
				"	F_CALC_COST(p.song_id, \"\", 1) AS song_cost,\r\n" + 
				"	(\r\n" + 
				"	    SELECT override_id FROM overrides\r\n" + 
				"	    WHERE p.song_id LIKE CONCAT(override_id, '%')\r\n" + 
				"	    ORDER BY CHAR_LENGTH(override_id) DESC\r\n" + 
				"		LIMIT 1\r\n" + 
				"	) AS override_id,\r\n" + 
				"	o.song_pts,\r\n" + 
				"    o.ost_pts,\r\n" + 
				"    o.franchise_pts,\r\n" + 
				"    o.time_checked,\r\n" + 
				"    o.id\r\n" + 
				"	\r\n" + 
				"FROM playlist p \r\n" + 
				"    LEFT JOIN ratings r ON r.song_id = p.song_id \r\n" + 
				"    LEFT JOIN play_history h ON h.song_id = p.song_id AND " + 
				"	 	UNIX_TIMESTAMP() - UNIX_TIMESTAMP(h.time_played) < F_READ_NUM_PARAM('times_played_check', 100000000) " +
				"	 LEFT JOIN overrides o ON o.override_id=(\r\n" + 
				"	    SELECT override_id FROM overrides\r\n" + 
				"	    WHERE p.song_id LIKE CONCAT(override_id, '%')\r\n" + 
				"	    ORDER BY CHAR_LENGTH(override_id) DESC\r\n" + 
				"		LIMIT 1\r\n" + 
				"	)\r\n" + 
				"GROUP BY p.song_id";
		
		return jdbcTemplate.query(sql, new SongRowMapper());
	}
	
	
	
	
	/**
	 * Retrieves all of the songs registered under an OST
	 * @param ost
	 * @return
	 */
	public List<Song> getAllFromOST(OST ost) {
		String sql = "SELECT \r\n" + 
				"	p.*, \r\n" + 
				"	COUNT(r.song_id) AS rating_num, \r\n" + 
				"	COALESCE(AVG(r.rating_pct), -1) AS rating_pct,\r\n" + 
				"	MAX(h.time_played) AS last_play,\r\n" + 
				"	COUNT(h.time_played) AS times_played,\r\n" + 
				"	F_CALC_COST(p.song_id, \"\", 1) AS song_cost,\r\n" + 
				"	(\r\n" + 
				"	    SELECT override_id FROM overrides\r\n" + 
				"	    WHERE p.song_id LIKE CONCAT(override_id, '%')\r\n" + 
				"	    ORDER BY CHAR_LENGTH(override_id) DESC\r\n" + 
				"		LIMIT 1\r\n" + 
				"	) AS override_id,\r\n" + 
				"	o.song_pts,\r\n" + 
				"    o.ost_pts,\r\n" + 
				"    o.franchise_pts,\r\n" + 
				"    o.time_checked, \r\n" + 
				"    o.id\r\n" + 
				"	\r\n" + 
				"FROM playlist p \r\n" + 
				"    LEFT JOIN ratings r ON r.song_id = p.song_id \r\n" + 
				"    LEFT JOIN play_history h ON h.song_id = p.song_id AND " + 
				"	 	UNIX_TIMESTAMP() - UNIX_TIMESTAMP(h.time_played) < F_READ_NUM_PARAM('times_played_check', 100000000) " +
				"	LEFT JOIN overrides o ON o.override_id=(\r\n" + 
				"	    SELECT override_id FROM overrides\r\n" + 
				"	    WHERE p.song_id LIKE CONCAT(override_id, '%')\r\n" + 
				"	    ORDER BY CHAR_LENGTH(override_id) DESC\r\n" + 
				"		LIMIT 1\r\n" + 
				"	)\r\n" + 
				"WHERE p.ost_name=:ost " +
				"GROUP BY p.song_id";
		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("ost", ost.getOstName());
		return jdbcTemplate.query(sql, params, new SongRowMapper());
	}
	
	
	
	/**
	 * Removes a set of songs from the playlist given a path filter for song_ids, much like those used by
	 * OverrideRules
	 * @param songIdFilter
	 */
	public void dropSongs(String songIdFilter) {
		String sql = "DELETE FROM playlist WHERE song_id LIKE :sid";
		MapSqlParameterSource params = new MapSqlParameterSource("sid", songIdFilter +"%");
		jdbcTemplate.update(sql, params);
	}
	
	/**
	 * Writes the given Song IDs to a temp table, `song_id_tmp`
	 * @param songIds song IDs to write
	 * @param clean if true, will wipe any preexisting songs from the table. If if doesn't exist,
	 * this has no effect
	 */
	public void writeSongIDsToTempTbl(List<String> songIds, boolean clean) {
		if(clean) { // wipe it first
			dropSongIdTempTable();
		}
		
		// setup new table
		String sqlCreate = "CREATE TABLE IF NOT EXISTS song_id_tmp "
				+ "( song_id VARCHAR(255) NOT NULL, UNIQUE sid_uniq (song_id(255)) "
				+ ") ENGINE = MEMORY";
		jdbcTemplate.getJdbcOperations().execute(sqlCreate);
		
		//write SIDs
		String sqlIns = "INSERT INTO song_id_tmp VALUES(:sid)";
		List<MapSqlParameterSource> params = new LinkedList<>();
		for(String s: songIds) {
			params.add(new MapSqlParameterSource("sid", s));
		}
		jdbcTemplate.batchUpdate(sqlIns, params.toArray(new MapSqlParameterSource[params.size()]));
	}



	/**
	 * Drops the song id temp table. If that doesn't exist, this has no effect
	 */
	public void dropSongIdTempTable() {
		String sqlDrop = "DROP TABLE IF EXISTS song_id_tmp";
		jdbcTemplate.getJdbcOperations().execute(sqlDrop);
	}
	
	
	/**
	 * Removes any songs from the playlist table that aren't in the temp table of song IDs
	 * @return the number of songs dropped from the table
	 */
	public int dropSongsNotInSongIdTempTable() {
		String sql = "DELETE FROM playlist WHERE song_id NOT IN (SELECT song_id FROM song_id_tmp)";
		return jdbcTemplate.getJdbcOperations().update(sql);
	}
	
	
	
	/**
	 * Gets the number of songs that a given song_id filter applies to
	 * @param songIdFilter
	 * @return
	 */
	public int getNumSongsAffectedBySongIdFilter(String songIdFilter) {
		String sql = "SELECT COUNT(*) AS num FROM playlist WHERE song_id LIKE :sid";
		MapSqlParameterSource params = new MapSqlParameterSource("sid", songIdFilter +"%");
		return (int) jdbcTemplate.queryForMap(sql, params).get("num");
	}
	
	
	
	
	/**
	 * Recursively obtains a list of all song files under this root directory. this will be sure to 
	 * only check files with valid song file extensions
	 * @param root root directory to search from
	 */
	public static List<File> getAllSongFiles(File root) {
		
		// basic recursive directory crawler
		File[] dirs = root.listFiles(Util::isLegalSongDirectory);
		File[] songsInDir = root.listFiles(Util::isLegalAudioFileExtension);
		List<File> ret = new ArrayList<>();
		for(File f : songsInDir)
			ret.add(f);
		for(File f : dirs)
			ret.addAll(getAllSongFiles(f));
		return ret;
	}
	
	
	
}











