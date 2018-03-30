package com.dystify.kkdystrack.v2.core.event.types;

import java.util.Date;

public class QueueEvent extends GenericDystrackEvent 
{
	private static final long serialVersionUID = 1015024797106107517L;

	public QueueEvent(Object source, boolean internal, String generatedBy, int eventId, Date time, String description) {
		super(source, internal, generatedBy, eventId, time, description);
		// TODO Auto-generated constructor stub
	}



}
