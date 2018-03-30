package com.dystify.kkdystrack.v2.core.event;

import org.springframework.context.ApplicationEventPublisherAware;

/**
 * Listens for and recieves events from an external source
 * @author Duemmer
 *
 */
public interface ExternalEventListener extends ApplicationEventPublisherAware
{
	public void checkForNewEvents();
}
