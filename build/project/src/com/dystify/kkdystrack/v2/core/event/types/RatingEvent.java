package com.dystify.kkdystrack.v2.core.event.types;

import java.util.Date;

/**
 * Triggers whenever Song Ratings are recieved, or possibly edited / removed in the future
 * @author Duemmer
 *
 */
public class RatingEvent extends GenericDystrackEvent 
{
	private static final long serialVersionUID = 377893303046279876L;

	public RatingEvent(Object source, boolean internal, String generatedBy, int eventId, Date time,
			String description) {
		super(source, internal, generatedBy, eventId, time, description);
		// TODO Auto-generated constructor stub
	}


}
