package com.dystify.kkdystrack.v2.manager;

import java.lang.reflect.Constructor;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

import com.dystify.kkdystrack.v2.KKDystrack;
import com.dystify.kkdystrack.v2.core.event.types.GenericDystrackEvent;

public abstract class AbstractManager implements ApplicationEventPublisherAware 
{
	protected ApplicationEventPublisher applicationEventPublisher;
	protected Logger log = LogManager.getLogger(getClass());

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}
	
	
	/**
	 * Convenience method for autogenerating and sending Dystrack events. This essentially
	 * will just wrap the {@code publish(...)} method and construction of the event
	 * @param type the class of the desired target event
	 * @param data optional event-specific extra data to send with the event
	 * @param description optional text description of the event itself, for logging / display purposes
	 */
	public void sendDystrackEvent(Class<? extends GenericDystrackEvent> type, String data, String description) {
		try {
			Constructor<? extends GenericDystrackEvent> constructor = type.getConstructor(
					Object.class,
					Boolean.TYPE,
					String.class,
					Integer.TYPE,
					Date.class,
					String.class);
			GenericDystrackEvent event = constructor.newInstance(this, true, KKDystrack.APP_NAME, -1, new Date(), description);
			applicationEventPublisher.publishEvent(event);
		} catch (Exception e) {
			log.error("Failed to publish Dystrack event!", e);
		}
	}

}
