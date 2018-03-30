package com.dystify.kkdystrack.v2.core.event.types;

import java.util.Date;

/**
 * Triggers whenver a cost override setting gets changed
 * @author Duemmer
 *
 */
public class CostOverrideEvent extends GenericDystrackEvent 
{

	private static final long serialVersionUID = -7528864518754853360L;

	public CostOverrideEvent(Object source, boolean internal, String generatedBy, int eventId, Date time,
			String description) {
		super(source, internal, generatedBy, eventId, time, description);
		// TODO Auto-generated constructor stub
	}

}
