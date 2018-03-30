package com.dystify.kkdystrack.v2.core.event.types;

import java.util.Date;

/**
 * Triggers whenever viewers are registered, changed, etc.
 * @author Duemmer
 *
 */
public class ViewerEvent extends GenericDystrackEvent 
{
	private static final long serialVersionUID = 424452347473724795L;

	public ViewerEvent(Object source, boolean internal, String generatedBy, int eventId, Date time,
			String description) {
		super(source, internal, generatedBy, eventId, time, description);
		// TODO Auto-generated constructor stub
	}

}
