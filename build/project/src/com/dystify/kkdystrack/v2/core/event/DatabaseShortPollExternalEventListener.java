/**
 * 
 */
package com.dystify.kkdystrack.v2.core.event;

import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;

import com.dystify.kkdystrack.v2.core.event.types.GenericDystrackEvent;
import com.dystify.kkdystrack.v2.dao.EventDAO;

/**
 * @author Duemmer
 *
 */
public class DatabaseShortPollExternalEventListener implements ExternalEventListener 
{
	private ApplicationEventPublisher applicationEventPublisher;
	private EventDAO eventDao;
	private Logger log = LogManager.getLogger(getClass());
	
	private int lastRegisteredEventID;
	
	
	
	public DatabaseShortPollExternalEventListener() {}
	
	
	@PostConstruct public void init() {
		// set to the current max id, we don't care about events before startup
		lastRegisteredEventID = eventDao.verifyValidMinEventID(-2);
	}
	
	
	/**
	 * periodically gets called to poll the database for new event occurrences
	 */
	@Override
	@Scheduled(fixedRate=5000)
	public void checkForNewEvents()
	{
		log.trace("Querying Database for events...");
		// read all the new events
		List<GenericDystrackEvent> events = eventDao.getAllNewerThan(eventDao.verifyValidMinEventID(lastRegisteredEventID));
		for(GenericDystrackEvent event : events) {
			log.info("Recieved event \"" +event+ "\"");
			applicationEventPublisher.publishEvent(event);
			
			// increase the event ID counter as needed
			lastRegisteredEventID = Math.max(lastRegisteredEventID, event.getEventId());
		}
	}

	
	

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}



	public ApplicationEventPublisher getApplicationEventPublisher() {
		return applicationEventPublisher;
	}



	public EventDAO getEventDao() {
		return eventDao;
	}



	public void setEventDao(EventDAO eventDao) {
		this.eventDao = eventDao;
	}

}
