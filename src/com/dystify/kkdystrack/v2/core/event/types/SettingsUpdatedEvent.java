package com.dystify.kkdystrack.v2.core.event.types;

import java.util.Date;

/**
 * Triggers whenever, either here or externally, general system settings information is updated
 * @author Duemmer
 *
 */
public class SettingsUpdatedEvent extends GenericDystrackEvent 
{
	private static final long serialVersionUID = 490838529124327166L;

	public SettingsUpdatedEvent(Object source, boolean internal, String generatedBy, int eventId, Date time,
			String description) {
		super(source, internal, generatedBy, eventId, time, description);
		// TODO Auto-generated constructor stub
	}
	
}
