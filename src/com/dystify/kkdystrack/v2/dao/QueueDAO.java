package com.dystify.kkdystrack.v2.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.dystify.kkdystrack.v2.core.exception.QueueNotFoundException;
import com.dystify.kkdystrack.v2.manager.ViewerManager;
import com.dystify.kkdystrack.v2.model.QueueEntry;
import com.dystify.kkdystrack.v2.model.Song;
import com.dystify.kkdystrack.v2.model.Viewer;
import com.dystify.kkdystrack.v2.model.queue.StdSongQueue;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;



public class QueueDAO 
{
	private NamedParameterJdbcTemplate jdbcTemplate;
	private Logger log = LogManager.getLogger(getClass());


	public static class QueueEntryRowMapper implements RowMapper<QueueEntry>
	{
		@Override public QueueEntry mapRow(ResultSet rs, int rowNum) throws SQLException {
			QueueEntry q = new QueueEntry();

			// extract the subsidiary parts as regular rows
			Song s = new SongDAO.SongRowMapper().mapRow(rs, rowNum);
			Viewer v = new ViewerDAO.ViewerRowMapper().mapRow(rs, rowNum);

			q.setSong(s);
			q.setViewer(v);
			q.setTimeRequested(new Date(rs.getLong("time_requested") * 1000L));
			q.setOwningQueue(rs.getString("queue_id"));

			return q;
		}
	}






	public QueueDAO(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}




	/**
	 * performs a swap of two QueueEntries at a database level. NOTE: will not
	 * check to make sure the indexes are in range!
	 * @param a the first list_order, assumed to be in range of the system
	 * @param b the second list_order, assumed to be in range of the system
	 * @throws QueueNotFoundException 
	 */
	public void swapQueueEntries(StdSongQueue queue, int a, int b) throws QueueNotFoundException
	{
		int size = queue.queueSizeProperty().get();
		if(a == b) // no operations need to be done, has no effect
			return;
		else if(a<size && a>=0  &&  b<size && b>=0) { // both numbers are in range
			ObservableList<QueueEntry> contents = queue.getQueue();
			QueueEntry tmp = contents.get(a);
			contents.set(a, contents.get(b));
			contents.set(b, tmp);
			String sql = "	UPDATE queue_" +queue.getQueueId()+ "\r\n" + 
					"		SET list_order = CASE\r\n" + 
					"			WHEN list_order=:a THEN :b\r\n" + 
					"			ELSE :a\r\n" + 
					"		WHERE list_order IN (:a, :b)";

			MapSqlParameterSource psrc = new MapSqlParameterSource();
			psrc.addValue("a", a);
			psrc.addValue("b", b);

			try { 
				jdbcTemplate.update(sql, psrc); 
			}
			catch(DataAccessException e) {
				if(isTblNotFoundErr(e))
					throw new QueueNotFoundException("Queue \"" +queue.getQueueId()+ "\" was not found!");
				else {
					throw e;
				}
			}
		} else {
			log.error("Attempted to swap out of bounds queue entries on queue " +queue.getQueueId());
		}
	}




	/**
	 * Generates full QueueEntries from just songs. NOTE: ONLY WORKS FOR 
	 * SONGS IN THE PLAYLIST!!!
	 * @param songs
	 * @param userId
	 * @return
	 */
	public List<QueueEntry> songToQueueEntry(List<Song> songs, String userId, String queueId) {
		String sql = "SELECT \r\n" + 
				"		v.*, \r\n" + 
				"		p.*,\r\n" + 
				"		m.*,\r\n" +
				"		COUNT(r.song_id) AS rating_num, \r\n" + 
				"		COALESCE(AVG(r.rating_pct), -1) AS rating_pct,\r\n" + 
				"		MAX(h.time_played) AS last_play,\r\n" + 
				"		COUNT(h.time_played) AS times_played,\r\n" + 
				"		F_CALC_COST(p.song_id, \"\", 1) AS song_cost,		\r\n" + 
				"		UNIX_TIMESTAMP() AS time_requested,\r\n" + 
				"		0 AS list_order,\r\n" + 
				"		0 AS entry_id,\r\n" + 
				"		(\r\n" + 
				"	    	SELECT override_id FROM overrides\r\n" + 
				"	    	WHERE p.song_id LIKE CONCAT(override_id, '%')\r\n" + 
				"	    	ORDER BY CHAR_LENGTH(override_id) DESC\r\n" + 
				"			LIMIT 1\r\n" + 
				"		) AS override_id,\r\n" + 
				"		o.song_pts,\r\n" + 
				"    	o.ost_pts,\r\n" + 
				"    	o.franchise_pts,\r\n" + 
				"    	o.id,\r\n" + 
				"    	o.time_checked\r\n" + 
				" 	FROM\r\n" + 
				"		playlist p\r\n" + 
				"		LEFT JOIN play_history h ON p.song_id=h.song_id AND\r\n" + 
				"			UNIX_TIMESTAMP() - UNIX_TIMESTAMP(h.time_played) < F_READ_NUM_PARAM('times_played_check', 100000000)\r\n" + 
				"		LEFT JOIN ratings r ON p.song_id=r.song_id\r\n" + 
				"		LEFT JOIN viewers v ON v.user_id=:user_id\r\n" + 
				"		LEFT JOIN queues m ON m.queue_id=:queue_id\r\n" + 
				"		LEFT JOIN overrides o ON o.override_id=(\r\n" + 
				"            SELECT override_id FROM overrides\r\n" + 
				"            WHERE p.song_id LIKE CONCAT(override_id, '%')\r\n" + 
				"            ORDER BY CHAR_LENGTH(override_id) DESC\r\n" + 
				"            LIMIT 1\r\n" + 
				"		)\r\n" + 
				"	WHERE p.song_id IN(:songs)" + 	
				"	GROUP BY p.song_id\r\n" + 
				"	ORDER BY RAND()";
		List<String> uids = new ArrayList<>();
		songs.stream().forEach((s) -> uids.add(s.getSongId()));
		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("songs", uids)
				.addValue("queue_id", queueId)
				.addValue("user_id", userId);

		return jdbcTemplate.query(sql, params, new QueueEntryRowMapper());
	}




	/**
	 * Converts a list of plain old songs to a registered QueueEntry. Song does not need to 
	 * be registered to the playlist, and will not incur any database calls
	 * @param songs
	 * @param v
	 * @param queueId
	 * @return
	 */
	public List<QueueEntry> songToQueueEntry(List<Song> songs, Viewer v, String queueId) {
		List<QueueEntry> ret = new ArrayList<>();
		for(Song s: songs) {
			QueueEntry q = new QueueEntry();
			q.setSong(s);
			q.setViewer(v);
			q.setTimeRequested(new Date());
			q.setOwningQueue(queueId);
			ret.add(q);
		}
		return ret;
	}



	/**
	 * Adds these QueueEntries to the history queue, with a time_played of now
	 * @param entries
	 */
	public void writeToHistory(List<QueueEntry> entries) {
		String sql = "INSERT INTO play_history(user_id, time_requested, song_id, time_played) "
				+ "VALUES(:user, :time, :song, UTC_TIMESTAMP())";
		MapSqlParameterSource[] params = new MapSqlParameterSource[entries.size()];
		for(int i=0; i<entries.size(); i++)
			params[i] = new MapSqlParameterSource()
			.addValue("user", entries.get(i).getViewer().getUserId())
			.addValue("time", entries.get(i).getTimeRequested())
			.addValue("song", entries.get(i).getSong().getSongId());
		jdbcTemplate.batchUpdate(sql, params);
	}




	/**
	 * commits a permutation, that is, a change of song orderings. This works by
	 * dynamically generating compounded {@code IF(...)} SQL statements covering
	 * each individual list_order change
	 * @param name the queue to manipulate
	 * @param from the start index of the permutation block, inclusive
	 * @param permutations the array of changes, with index 0 corresponding to {@code from}, 1 
	 * to {@code from+1}, etc.
	 */
	public void handlePermutation(String name, int from, int[] permutations) throws QueueNotFoundException
	{
		StringBuilder sqlBuilder = new StringBuilder("UPDATE queue_")
				.append(name)
				.append(" SET list_order=");

		MapSqlParameterSource params = new MapSqlParameterSource();

		for(int i=from; i<from+permutations.length; i++) {
			sqlBuilder.append("\n\tIF(list_order=:a")// add newline / tab for extra readability
			.append(i)
			.append(",:b")
			.append(i)
			.append(","); 
			params
			.addValue("a"+i, i)
			.addValue("b" +i, permutations[i]);
		}

		sqlBuilder.append("list_order");
		for(@SuppressWarnings("unused") int j : permutations) // append the closing parenthases
			sqlBuilder.append(")");

		try { jdbcTemplate.update(sqlBuilder.toString(), params); }
		catch (DataAccessException e) {
			if(isTblNotFoundErr(e))
				throw new QueueNotFoundException("Queue \"" +name+ "\" was not found!");
			else {
				log.fatal("Exception occured when trying to remove from queue \"" +name+ "\"");
				throw e;
			}
		}
	}





	/**
	 * Gets all of the entries in a queue
	 * @param id name of queue, not including "queue_" prefix
	 * @return a list of each entry in the queue
	 */
	public StdSongQueue getQueue(String id)  throws QueueNotFoundException
	{
		// select all the data about song / viewer at once to minimize time lost to queries
		String sql = "SELECT \r\n" + 
				"		v.*, \r\n" + 
				"		p.song_name, p.ost_name, p.song_length, p.song_franchise, p.song_points,\r\n" +
				"		q.song_id,\r\n" +
				"		m.*,\r\n" + 
				"		COUNT(r.song_id) AS rating_num, \r\n" + 
				"		COALESCE(AVG(r.rating_pct), -1) AS rating_pct,\r\n" + 
				"		MAX(h.time_played) AS last_play,\r\n" + 
				"		COUNT(h.time_played) AS times_played,\r\n" + 
				"		F_CALC_COST(p.song_id, \"\", 1) AS song_cost,		\r\n" + 
				"		q.time_requested AS time_requested,\r\n" + 
				"		q.list_order AS list_order,\r\n" + 
				"		q.id AS entry_id, \r\n" + 
				"		(\r\n" + 
				"	    	SELECT override_id FROM overrides\r\n" + 
				"	    	WHERE p.song_id LIKE CONCAT(override_id, '%')\r\n" + 
				"	    	ORDER BY CHAR_LENGTH(override_id) DESC\r\n" + 
				"			LIMIT 1\r\n" + 
				"		) AS override_id,\r\n" + 
				"		o.song_pts,\r\n" + 
				"    	o.ost_pts,\r\n" + 
				"    	o.franchise_pts,\r\n" + 
				"    	o.id,\r\n" + 
				"    	o.time_checked\r\n " +
				" 	FROM\r\n" + 
				"		queue_" +id+ " q\r\n" + // can't add queue table name using parameters
				"		LEFT JOIN play_history h ON q.song_id=h.song_id AND\r\n" + 
				"			UNIX_TIMESTAMP() - UNIX_TIMESTAMP(h.time_played) < F_READ_NUM_PARAM('times_played_check', 100000000)\r\n" + 
				"		LEFT JOIN ratings r ON q.song_id=r.song_id\r\n" + 
				"    	INNER JOIN queues m ON m.queue_id=:id \r\n" +
				"		LEFT JOIN playlist p ON p.song_id=q.song_id\r\n" + 
				"		INNER JOIN viewers v ON v.user_id=q.user_id\r\n" + 
				"		INNER JOIN overrides o ON o.override_id=(\r\n" + 
				"            SELECT override_id FROM overrides\r\n" + 
				"            WHERE q.song_id LIKE CONCAT(override_id, '%')\r\n" + 
				"            ORDER BY CHAR_LENGTH(override_id) DESC\r\n" + 
				"            LIMIT 1\r\n" + 
				"		)\r\n" + 
				"	GROUP BY q.song_id, q.id\r\n" +
				"	ORDER BY q.list_order";

		List<QueueEntry> entries = null;
		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("id", id);

		// catch any exception, in case the table isn't found. If so, throw a QueueNotFoundException
		try { 
			entries = this.jdbcTemplate.query(sql, params, new QueueEntryRowMapper()); 
			
			// right now just fetch the name arbitrarily. However, this is very slow, and leads to significant delays in loading the queue, so be sure to replace eventually
			String name = (String) jdbcTemplate.queryForMap("SELECT queue_name FROM queues WHERE queue_id=:id", new MapSqlParameterSource("id", id)).get("queue_name");
			
			return new StdSongQueue(id, name, FXCollections.observableArrayList(entries), this);
		}
		catch(DataAccessException e) {
			if(isTblNotFoundErr(e))
				throw new QueueNotFoundException("Queue \"" +id+ "\" was not found!");
			else {
				log.fatal("Exception occured when trying to get contents of queue \"" +id+ "\"");
				throw e;
			}
		}
	}



	/**
	 * Fetches randomly selected songs as QueueEntries, timestamped at the current time and registered 
	 * to a predefined special user
	 * @param numToGet
	 * @return
	 */
	public List<QueueEntry> getSimpleRandomQueueEntries(int numToGet) {
		String sql = "SELECT \r\n" + 
				"		v.*, \r\n" + 
				"		p.*,\r\n" + 
				"		\"SimpleRandom\" AS queue_id,\r\n" + 
				"		0 AS delete_on_empty,\r\n" + 
				"		COUNT(r.song_id) AS rating_num, \r\n" + 
				"		COALESCE(AVG(r.rating_pct), -1) AS rating_pct,\r\n" + 
				"		MAX(h.time_played) AS last_play,\r\n" + 
				"		COUNT(h.time_played) AS times_played,\r\n" + 
				"		F_CALC_COST(p.song_id, \"\", 1) AS song_cost,		\r\n" + 
				"		UNIX_TIMESTAMP() AS time_requested,\r\n" + 
				"		0 AS list_order,\r\n" + 
				"		0 AS entry_id,\r\n" + 
				"		(\r\n" + 
				"	    	SELECT override_id FROM overrides\r\n" + 
				"	    	WHERE p.song_id LIKE CONCAT(override_id, '%')\r\n" + 
				"	    	ORDER BY CHAR_LENGTH(override_id) DESC\r\n" + 
				"			LIMIT 1\r\n" + 
				"		) AS override_id,\r\n" + 
				"		o.song_pts,\r\n" + 
				"    	o.ost_pts,\r\n" + 
				"    	o.franchise_pts,\r\n" + 
				"    	o.id,\r\n" + 
				"    	o.time_checked\r\n" + 
				" 	FROM\r\n" + 
				"		playlist p\r\n" + 
				"		INNER JOIN (SELECT song_id FROM playlist ORDER BY RAND() LIMIT :num) sort ON sort.song_id = p.song_id\r\n" + 
				"		LEFT JOIN play_history h ON p.song_id=h.song_id AND\r\n" + 
				"			UNIX_TIMESTAMP() - UNIX_TIMESTAMP(h.time_played) < F_READ_NUM_PARAM('times_played_check', 100000000)\r\n" + 
				"		LEFT JOIN ratings r ON p.song_id=r.song_id\r\n" + 
				"		LEFT JOIN viewers v ON v.user_id=:user_id\r\n" + 
				"		LEFT JOIN overrides o ON o.override_id=(\r\n" + 
				"            SELECT override_id FROM overrides\r\n" + 
				"            WHERE p.song_id LIKE CONCAT(override_id, '%')\r\n" + 
				"            ORDER BY CHAR_LENGTH(override_id) DESC\r\n" + 
				"            LIMIT 1\r\n" + 
				"		)\r\n" + 
				"	GROUP BY p.song_id\r\n" + 
				"	ORDER BY RAND()"; // gotta randomize a second time because they'll be ordered by OST thanks to the native sorting / IN clause
		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("num", numToGet)
				.addValue("user_id", ViewerManager.getDystrackUserId());

		return jdbcTemplate.query(sql, params, new QueueEntryRowMapper());
	}






	/**
	 * Takes the specified queueEntries and enters them into the remote queue
	 * 	 * 
	 * @param name the queue that these songs will be added to
	 * @param entries the {@link QueueEntry QueueEntries} to add to the queue
	 * @param from the start index to add elements at, inclusive. set to {@code entries.size()} to
	 * add to the end of the list
	 */
	@SuppressWarnings("unchecked")
	public void addToQueue(String name, List<QueueEntry> entries, int from, boolean overwrite) throws QueueNotFoundException 
	{
		String sql = "INSERT INTO queue_" +name+ 
				"		(user_id, time_requested, song_id, list_order) VALUES\r\n" + 
				"		(:user, :time, :song, :order)";

		try {
			if(overwrite) { // delete if necessary
				jdbcTemplate.update(
						"DELETE FROM queue_" +name+ " WHERE list_order BETWEEN :low AND :up", 
						new MapSqlParameterSource()
						.addValue("low", from)
						.addValue("up", from + entries.size()-1)
						);
			} else { // or otherwise shift everything past and including `from`
				jdbcTemplate.update(
						"UPDATE queue_" +name+" SET list_order=list_order+:num WHERE list_order>=:from", 
						new MapSqlParameterSource().addValue("num", entries.size()).addValue("from", from)
						);
			}

			// add in the rest
			List<Map<String, Object>> batchVals = new ArrayList<>(entries.size());
			for(int i=0; i<entries.size(); i++) {
				QueueEntry entry = entries.get(i);
				batchVals.add(
						new MapSqlParameterSource("user", entry.getViewer().getUserId()) // add each parameter, make sure to use getValues() at the end!
						.addValue("time", entry.getTimeRequested().getTime() / 1000L)
						.addValue("song", entry.getSong().getSongId())
						.addValue("order", from + i)
						.getValues()
						);
			}

			jdbcTemplate.batchUpdate(sql, batchVals.toArray(new Map[entries.size()]));
		}catch (DataAccessException e) {
			if(isTblNotFoundErr(e))
				throw new QueueNotFoundException("Queue \"" +name+ "\" was not found!");
			else {
				log.fatal("Exception occured when trying to add to queue \"" +name+ "\"");
				throw e;
			}
		}
	}



	/**
	 * Removes a set of QueueEntries from the queue specified by {@code name}
	 * @param name the name of the queue to remove songs from
	 * @param from the start index of the remove, inclusive
	 * @param to the end index of the remove, also inclusive
	 * @throws QueueNotFoundException if there is no queue registered to {@code name} in the database
	 */
	public void removeFromQueue(String name, int from, int to) throws QueueNotFoundException
	{
		String sqlRemove = "DELETE FROM queue_" +name+ " WHERE list_order BETWEEN :from AND :to";
		MapSqlParameterSource paramsRemove = new MapSqlParameterSource()
				.addValue("from", from)
				.addValue("to", to); /*// BETWEEN's end is inclusive, to is exclusive*/

		String sqlUpdate = "UPDATE queue_" +name+ " SET list_order = list_order-:size WHERE list_order > :to";
		MapSqlParameterSource paramsUpdate = new MapSqlParameterSource()
				.addValue("from", from)
				.addValue("to", to)
				.addValue("size", to-from + 1);

		try { 
			jdbcTemplate.update(sqlRemove, paramsRemove);
			jdbcTemplate.update(sqlUpdate, paramsUpdate);
		}
		catch (DataAccessException e) {
			if(isTblNotFoundErr(e))
				throw new QueueNotFoundException("Queue \"" +name+ "\" was not found!");
			else {
				log.fatal("Exception occured when trying to remove from queue \"" +name+ "\"");
				throw e;
			}
		}
	}




	/**
	 * Fetches a list of all the registered queues in the database
	 * @return
	 */
	public List<String> getAllQueueIDs() {
		String sql = "SELECT queue_id FROM queues";
		return jdbcTemplate.query(sql, (rs, rowNum) -> {
			return rs.getString(1);
		});
	}
	
	
	/**
	 * Fetches a list of all the registered queues in the database, along with
	 * their names. Similar to {@link #getAllQueueIDs()}, but for convenience 
	 * this method bundles the display names along with the IDs
	 * @return A {@link Map} with the queue ID serving as the keys, and queue name
	 * as values
	 */
	public Map<String, String> getAllQueueIDsWithNames() {
		String sql = "SELECT queue_id, queue_name FROM queues";
		
		return jdbcTemplate.query(sql, (rs) -> {
			Map<String, String> queues = new HashMap<>();
			while(rs.next()) {
				String id = rs.getString("queue_id");
				String name = rs.getString("queue_name");
				queues.put(id, name);
			}
			return queues;
		});
	}
	
	
	
	/**
	 * Creates a new Queue, and returns it, or returns null if the queue already exists
	 * queue if it already exists
	 * @param queueID
	 * @param queueName
	 * @param deleteOnEmpty
	 * @return
	 * @throws QueueNotFoundException if the queue failed to be created, IE attempting to read the newly created queue failed
	 */
	public StdSongQueue createQueue(String queueID, String queueName, boolean deleteOnEmpty) throws QueueNotFoundException {
		if(!queueExists(queueID)) {
			String sql = "CALL P_CREATE_QUEUE(:qid, :delete, :name)";	
			MapSqlParameterSource params = new MapSqlParameterSource()
					.addValue("qid", queueID)
					.addValue("delete", deleteOnEmpty)
					.addValue("name", queueName);
			jdbcTemplate.update(sql, params);
			return getQueue(queueID); // will fail if the queue failed to be created
		}
		
		// already exists, don't touch it
		return null;
	}





	/**
	 * Writes a queue to the database. Makes sure the queue table exists, creates it if it doesn't.
	 * Then makes sure the table is empty, then writes its entire contents to that table, and finally
	 * updates the queues table
	 * @param queue
	 */
	public void putQueue(StdSongQueue queue) {
		if(queueExists(queue.getQueueId())) {
			jdbcTemplate.update("DELETE FROM queue_" +queue.getQueueId(), new MapSqlParameterSource());
		} else {

		}
	}




	public boolean queueExists(String queueName) {
		String sql = "SELECT COUNT(*) AS num FROM queues WHERE queue_id=:name";
		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("name", queueName);
		return jdbcTemplate.queryForMap(sql, params).get("num").equals(1);
	}







	/**
	 * Empties queue specified by <code>name</code>
	 * @param name the name of the queue to empty
	 * @throws QueueNotFoundException if no queue exists under the provided name
	 */
	public void clearQueue(String name) throws QueueNotFoundException 
	{
		try { jdbcTemplate.getJdbcOperations().update("DELETE FROM queue_" +name); }
		catch (DataAccessException e) {
			if(isTblNotFoundErr(e))
				throw new QueueNotFoundException("Queue \"" +name+ "\" was not found!");
			else {
				log.fatal("Exception occured when trying to empty queue \"" +name+ "\"");
				throw e;
			}
		}
	}
	
	
	
	/**
	 * Drops the specified queue from the database
	 * @param queueId
	 * @throws QueueNotFoundException
	 */
	public void dropQueue(String queueId) throws QueueNotFoundException {
		try { jdbcTemplate.update("CALL P_DROP_QUEUE(:qid)", new MapSqlParameterSource("qid", queueId)); }
		catch (DataAccessException e) {
			if(isTblNotFoundErr(e))
				throw new QueueNotFoundException("Queue \"" +queueId+ "\" was not found!");
			else {
				log.fatal("Exception occured when trying to empty queue \"" +queueId+ "\"");
				throw e;
			}
		}
	}








	/**
	 * Checks an {@link DataAccessException} and checks if the error is attributed to an unknown table SQL syntax error,
	 * which could happen if a nonexistent queue is attempted to be acted upon.
	 * though we can't check a specific exception type to see if it's a table not found,
	 * we can determine it's a missing table issue by mysql's error output
	 */
	private boolean isTblNotFoundErr(DataAccessException e) {
		return e./*getRootCause().*/getMessage().toLowerCase().matches("table .* doesn't exist");
	}


}













