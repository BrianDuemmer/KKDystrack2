package com.dystify.kkdystrack.v2.core.event.types;

import java.util.Date;


/**
 * Indicates that the list of registered queues has changed
 * @author Duemmer
 *
 */
public class QueueListChangedEvent extends GenericDystrackEvent {

	private static final long serialVersionUID = 536197165989862550L;

	public QueueListChangedEvent(Object source, boolean internal, String generatedBy, int eventId, Date time,
			String description) {
		super(source, internal, generatedBy, eventId, time, description);
		// TODO Auto-generated constructor stub
	}

}
