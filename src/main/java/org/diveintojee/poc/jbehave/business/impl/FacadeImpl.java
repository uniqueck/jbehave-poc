/*
 *
 */
package org.diveintojee.poc.jbehave.business.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

import org.apache.commons.collections.CollectionUtils;
import org.diveintojee.poc.jbehave.domain.AbstractEntity;
import org.diveintojee.poc.jbehave.domain.Advert;
import org.diveintojee.poc.jbehave.domain.OrderBy;
import org.diveintojee.poc.jbehave.domain.SearchQuery;
import org.diveintojee.poc.jbehave.domain.SearchResult;
import org.diveintojee.poc.jbehave.domain.business.Facade;
import org.diveintojee.poc.jbehave.domain.validation.ValidationContext;
import org.diveintojee.poc.jbehave.persistence.BaseDao;
import org.diveintojee.poc.jbehave.persistence.SearchEngine;
import org.diveintojee.poc.jbehave.persistence.events.PostDeleteAdvertEvent;
import org.diveintojee.poc.jbehave.persistence.events.PostStoreAdvertEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Preconditions;

/**
 * @author louis.gueye@gmail.com
 */
@Service(Facade.BEAN_ID)
public class FacadeImpl implements Facade {

	@Autowired
	private Validator					validator;

	@Autowired
	private BaseDao						baseDao;

	@Autowired
	private ApplicationEventPublisher	eventPublisher;

	@Autowired
	SearchEngine						searchEngine;

	/**
	 * @see org.diveintojee.poc.jbehave.domain.business.Facade#createAdvert(org.diveintojee.poc.jbehave.domain.Advert)
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public Long createAdvert(final Advert advert) {

		Preconditions.checkArgument(advert != null, "Illegal call to createAdvert, advert is required");

		this.baseDao.persist(advert);

		this.eventPublisher.publishEvent(new PostStoreAdvertEvent(advert));

		return advert.getId();

	}

	/**
	 * @see org.diveintojee.poc.jbehave.domain.business.Facade#deleteAdvert(java.lang.Long)
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void deleteAdvert(final Long advertId) {

		Preconditions.checkArgument(advertId != null, "Illegal call to deleteAdvert, advert identifier is required");

		this.baseDao.delete(Advert.class, advertId);

		this.eventPublisher.publishEvent(new PostDeleteAdvertEvent(new Advert(advertId)));

	}

	/**
	 * @see org.diveintojee.poc.jbehave.domain.business.Facade#findAdvertsByCriteria(org.diveintojee.poc.jbehave.domain.Advert)
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public List<Advert> findAdvertsByCriteria(final Advert criteria) {

		Preconditions.checkArgument(criteria != null, "Illegal call to findAdvertsByCriteria, criteria is required");

		return this.baseDao.findByExample(criteria);

	}

	/**
	 * @see org.diveintojee.poc.jbehave.domain.business.Facade#findAdvertsByCriteria(java.lang.String,
	 *      java.util.Set, int, int)
	 */
	@Override
	public SearchResult findAdvertsByCriteria(final String queryString, final Set<OrderBy> orderByList,
			final int startPage, final int itemsPerPage) {

		// System.out.println("handling search request {queryString=" +
		// queryString + ", orderByList=" + orderByList
		// + ", startPage=" + startPage + ", itemsPerPage=" + itemsPerPage);

		final SearchQuery searchQuery = new SearchQuery(queryString, orderByList, startPage, itemsPerPage);

		// System.out.println("sending search query = " + searchQuery);

		final SearchResult searchResult = this.searchEngine.search(searchQuery);

		searchResult.setSearchQuery(searchQuery);

		// System.out.println("sending search result = " + searchResult);

		return searchResult;

	}

	/**
	 * @see org.diveintojee.poc.jbehave.domain.business.Facade#findProtectedAdvertsByCriteria(org.diveintojee.poc.jbehave.domain.Advert)
	 */
	@RolesAllowed("ROLE_ADMIN")
	@Override
	public List<Advert> findProtectedAdvertsByCriteria(final Advert criteria) {
		return Collections.emptyList();
	}

	/**
	 * @see org.diveintojee.poc.jbehave.domain.business.Facade#readAdvert(java.lang.Long)
	 */
	@Override
	public Advert readAdvert(final Long advertId) {

		Preconditions.checkArgument(advertId != null, "Illegal call to readAdvert, advert identifier is required");

		return this.baseDao.get(Advert.class, advertId);

	}

	/**
	 * @see org.diveintojee.poc.jbehave.domain.business.Facade#updateAdvert(org.diveintojee.poc.jbehave.domain.Advert)
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void updateAdvert(final Advert advert) {

		Preconditions.checkArgument(advert != null, "Illegal call to updateAdvert, advert is required");

		Preconditions.checkArgument(advert.getId() != null, "Illegal call to updateAdvert, advert.id is required");

		this.baseDao.merge(advert);

		this.eventPublisher.publishEvent(new PostStoreAdvertEvent(advert));

	}

	/**
	 * @see org.diveintojee.poc.jbehave.domain.business.Facade#validate(org.diveintojee.poc.jbehave.domain.AbstractEntity,
	 *      org.diveintojee.poc.jbehave.domain.validation.ValidationContext)
	 */
	@Override
	public <T extends AbstractEntity> void validate(final T type, final ValidationContext context) {

		Preconditions.checkArgument(type != null, "Illegal call to validate, object is required");

		Preconditions.checkArgument(context != null, "Illegal call to validate, validation context is required");

		final Set<ConstraintViolation<T>> constraintViolations = this.validator.validate(type, context.getContext());

		if (CollectionUtils.isEmpty(constraintViolations)) return;

		final Set<ConstraintViolation<?>> propagatedViolations = new HashSet<ConstraintViolation<?>>(
				constraintViolations.size());

		final Set<String> classNames = new HashSet<String>();

		for ( final ConstraintViolation<?> violation : constraintViolations ) {

			propagatedViolations.add(violation);

			classNames.add(violation.getLeafBean().getClass().getName());

		}

		throw new ConstraintViolationException(propagatedViolations);

	}

}
