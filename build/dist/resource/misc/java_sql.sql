	
	/** Queue Entry SQL */
SELECT 
		v.*, 
		p.*,
		m.*,
		COUNT(r.song_id) AS rating_num, 
		COALESCE(AVG(r.rating_pct), -1) AS rating_pct,
		MAX(h.time_played) AS last_play,
		COUNT(h.time_played) AS times_played,
		F_CALC_COST(p.song_id, "", 1) AS song_cost,		
		q.time_requested AS time_requested,
		q.list_order AS list_order,
		q.id AS entry_id,
		(
	    	SELECT override_id FROM overrides
	    	WHERE p.song_id LIKE CONCAT(override_id, '%')
	    	ORDER BY CHAR_LENGTH(override_id) DESC
			LIMIT 1
		) AS override_id,
		o.song_pts,
    	o.ost_pts,
    	o.franchise_pts,
    	o.id,
    	o.time_checked
 	FROM
		queue_main q
		LEFT JOIN play_history h ON q.song_id=h.song_id AND
			UNIX_TIMESTAMP() - UNIX_TIMESTAMP(h.time_played) < F_READ_NUM_PARAM('times_played_check', 100000000)
		LEFT JOIN ratings r ON q.song_id=r.song_id
		INNER JOIN queues m ON m.queue_id=:name
		INNER JOIN playlist p ON p.song_id=q.song_id
		INNER JOIN viewers v ON v.user_id=q.user_id
		LEFT JOIN overrides o ON o.override_id=(
            SELECT override_id FROM overrides
            WHERE p.song_id LIKE CONCAT(override_id, '%')
            ORDER BY CHAR_LENGTH(override_id) DESC
            LIMIT 1
		)
	GROUP BY q.song_id, q.id
	ORDER BY q.list_order;
	
	
	/** Random Queue Entry */
SELECT 
		v.*, 
		p.*,
		"SimpleRandom" AS queue_id,
		0 AS delete_on_empty,
		COUNT(r.song_id) AS rating_num, 
		COALESCE(AVG(r.rating_pct), -1) AS rating_pct,
		MAX(h.time_played) AS last_play,
		COUNT(h.time_played) AS times_played,
		F_CALC_COST(p.song_id, "", 1) AS song_cost,		
		UNIX_TIMESTAMP() AS time_requested,
		0 AS list_order,
		0 AS entry_id,
		(
	    	SELECT override_id FROM overrides
	    	WHERE p.song_id LIKE CONCAT(override_id, '%')
	    	ORDER BY CHAR_LENGTH(override_id) DESC
			LIMIT 1
		) AS override_id,
		o.song_pts,
    	o.ost_pts,
    	o.franchise_pts,
    	o.id,
    	o.time_checked
 	FROM
		playlist p
		INNER JOIN (SELECT song_id FROM playlist ORDER BY RAND() LIMIT :num) sort ON sort.song_id = p.song_id
		LEFT JOIN play_history h ON p.song_id=h.song_id AND
			UNIX_TIMESTAMP() - UNIX_TIMESTAMP(h.time_played) < F_READ_NUM_PARAM('times_played_check', 100000000)
		LEFT JOIN ratings r ON p.song_id=r.song_id
		LEFT JOIN viewers v ON v.user_id=:user_id
		LEFT JOIN overrides o ON o.override_id=(
            SELECT override_id FROM overrides
            WHERE p.song_id LIKE CONCAT(override_id, '%')
            ORDER BY CHAR_LENGTH(override_id) DESC
            LIMIT 1
		)
	GROUP BY p.song_id
	
	
	/** Song SQL */
SELECT 
	p.*, 
	COUNT(r.song_id) AS rating_num, 
	COALESCE(AVG(r.rating_pct), -1) AS rating_pct,
	MAX(h.time_played) AS last_play,
	COUNT(h.time_played) AS times_played,
	F_CALC_COST(p.song_id, "", 1) AS song_cost,
	(
	    SELECT override_id FROM overrides
	    WHERE p.song_id LIKE CONCAT(override_id, '%')
	    ORDER BY CHAR_LENGTH(override_id) DESC
		LIMIT 1
	) AS override_id,
	o.song_pts,
    o.ost_pts,
    o.franchise_pts,
    o.id,
    o.time_checked
	
FROM playlist p 
    LEFT JOIN ratings r ON r.song_id = p.song_id 
    LEFT JOIN play_history h ON h.song_id = p.song_id AND 
    	UNIX_TIMESTAMP() - UNIX_TIMESTAMP(h.time_played) < F_READ_NUM_PARAM('times_played_check', 100000000)
	LEFT JOIN overrides o ON o.override_id=(
	    SELECT override_id FROM overrides
	    WHERE p.song_id LIKE CONCAT(override_id, '%')
	    ORDER BY CHAR_LENGTH(override_id) DESC
		LIMIT 1
	)
GROUP BY p.song_id


	
	
	
	
	
	/** Swap entries in queue */
	UPDATE queue_$name
		SET list_order = CASE
			WHEN list_order=$a THEN $b
			ELSE $a
		WHERE list_order IN ($a, $b);
	
	
	
	/** Clean queue insert */
	INSERT INTO queue_$name
		(user_id, time_requested, song_id, list_order) VALUES
		(:user, :time, :song, :order);
		
	
	/** Get Active Override Rules */
	SELECT p.*,    
	(
	    SELECT override_id FROM overrides
	    WHERE p.song_id LIKE CONCAT(override_id, '%')
	    ORDER BY CHAR_LENGTH(override_id) DESC
		LIMIT 1
	) AS override_id,
	o.song_pts,
	o.ost_pts,
	o.franchise_pts,
	o.time_checked,
	o.id
	FROM playlist p INNER JOIN overrides o ON o.override_id=override_id
	GROUP BY p.song_id
	HAVING override_id=:oid
	
	
	REPLACE INTO playlist (song_name, ost_name, song_franchise, song_length, song_points)
	VALUES(:songName, :ostName, :songFranchise, :songLength, :songPoints)
	WHERE song_id=:sid
	
	
	
	
	