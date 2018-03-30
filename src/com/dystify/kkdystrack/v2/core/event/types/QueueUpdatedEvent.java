package com.dystify.kkdystrack.v2.core.event.types;

import java.util.Date;

public class QueueUpdatedEvent extends GenericDystrackEvent {

	private static final long serialVersionUID = -3998916832737010766L;
	private String queueId;

	public QueueUpdatedEvent(Object source, boolean internal, String generatedBy, int eventId, Date time,
			String description) {
		super(source, internal, generatedBy, eventId, time, description);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void parseDataJSON(String data) {
		this.queueId = data; 
	}

	public String getQueueId() {
		return queueId;
	}
	
	
	

}
