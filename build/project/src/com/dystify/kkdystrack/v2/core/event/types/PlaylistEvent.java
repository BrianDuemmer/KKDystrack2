package com.dystify.kkdystrack.v2.core.event.types;

import java.util.Date;

/**
 * triggered when something with the playlist changes (e.g. point calculations run, songs
 * added, etc)
 * @author Duemmer
 *
 */
public class PlaylistEvent extends GenericDystrackEvent 
{
	private static final long serialVersionUID = 9194579956603203341L;

	public PlaylistEvent(Object source, boolean internal, String generatedBy, int eventId, Date time, String description) {
		super(source, internal, generatedBy, eventId, time, description);
		// TODO Auto-generated constructor stub
	}


}
