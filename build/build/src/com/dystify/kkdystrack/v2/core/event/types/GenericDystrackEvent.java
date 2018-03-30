package com.dystify.kkdystrack.v2.core.event.types;

import java.util.Date;

import org.springframework.context.ApplicationEvent;

public class GenericDystrackEvent extends ApplicationEvent 
{
	private static final long serialVersionUID = 7319121509980241052L;
	
	protected boolean internal;
	protected String generatedBy;
	protected int eventId;
	protected Date time;
	protected String description;
	protected String data;
	
	
	
	
	public GenericDystrackEvent(Object source, boolean internal, String generatedBy, int eventId, Date time, String description) {
		super(source);
		this.internal = internal;
		this.generatedBy = generatedBy;
		this.eventId = eventId;
		this.time = time;
		this.description = description;
	}
	
	
	/**
	 * Parses any extra data from the <code>data</code> field of the event. While by default it
	 * will do nothing, it should be overriden by specific event classes that want to extract specific data
	 * from the event.
	 * @param data custom data for the event, in JSON form
	 */
	public void parseDataJSON(String data) {
		
	}


	public boolean isInternal() {
		return internal;
	}


	public String getGeneratedBy() {
		return generatedBy;
	}


	public int getEventId() {
		return eventId;
	}


	public Date getTime() {
		return time;
	}


	public String getDescription() {
		return description;
	}


	public void setDescription(String description) {
		this.description = description;
	}


	@Override
	public String toString() {
		return getClass().getSimpleName() +" [internal=" + internal + ", generatedBy=" + generatedBy + ", eventId=" + eventId
				+ ", time=" + time + ", description=" + description + "]";
	}


	public String getRawData() {
		return data;
	}
	
	
	

}
