package com.dystify.kkdystrack.v2.dao;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class PlaylistDAO 
{
	private NamedParameterJdbcTemplate jdbcTemplate;
	public PlaylistDAO(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

}
