package com.dystify.kkdystrack.v2.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.dystify.kkdystrack.v2.model.Viewer;


/**
 * Acts as a communication class to translate objects of the 
 * {@link Viewer} Class to the viewers table in the database
 * @author Duemmer
 *
 */
public class ViewerDAO 
{
	private NamedParameterJdbcTemplate jdbcTemplate;
	
	public static class ViewerRowMapper implements RowMapper<Viewer>
	{
		@Override public Viewer mapRow(ResultSet rs, int rowNum) throws SQLException {
			Viewer v = new Viewer();
			
			v.setUsername(rs.getString("username"));
			v.setUserId(rs.getString("user_id"));
			v.setRupees(rs.getDouble("rupees"));
			v.setFavoriteSong(rs.getString("favorite_song"));
			v.setBlacklisted(rs.getBoolean("is_blacklisted"));
			v.setAdmin(rs.getBoolean("is_admin"));
			v.setRupeeDiscount(rs.getDouble("rupee_discount"));
			v.setFreeRequests(rs.getInt("free_requests"));
			v.setLoginBonusCount(rs.getInt("login_bonus_count"));
			v.setStaticRank(rs.getString("static_rank"));
			
			// WARNING: It seems a default date of "0000-00-00" will make these calls fail
			// disasterously. Since nothing with Viewer is really implemented, just comment it out
			// for now
//			v.setBirthday(new Date(rs.getDate("birthday").getTime()));
//			v.setLastbirthdayWithdraw(new Date(rs.getDate("last_birthday_withdraw").getTime()));
			v.setSongOnHold(rs.getString("song_on_hold"));
			v.setNote(rs.getString("note"));
			
			return v;
		}
		
	}

	public ViewerDAO(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
	
	
	public Viewer getByUserId(String userID) {
		String sql = "SELECT * FROM viewers WHERE user_id=:uid";
		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("uid", userID);
		List<Viewer> ret = jdbcTemplate.query(sql, params, new ViewerRowMapper());
		return ret.size() > 0 ? ret.get(0) : null;
	}
	
	
}

















