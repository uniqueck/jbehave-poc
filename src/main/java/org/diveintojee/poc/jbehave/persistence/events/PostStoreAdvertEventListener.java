/*
 *
 */
package org.diveintojee.poc.jbehave.persistence.events;

import org.diveintojee.poc.jbehave.domain.Advert;
import org.diveintojee.poc.jbehave.persistence.SearchEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * @author louis.gueye@gmail.com
 */
@Component
public class PostStoreAdvertEventListener implements ApplicationListener<PostStoreAdvertEvent> {

	@Autowired
	private SearchEngine	searchEngine;

	/**
	 * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
	 */
	@Override
	public void onApplicationEvent(PostStoreAdvertEvent event) {

		if (event == null) return;

		final Advert entity = event.getSource();

		if (entity == null || entity.getId() == null) return;

		this.searchEngine.index(Advert.class, entity);

	}

}
