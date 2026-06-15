/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.webservices.rest.web.v1_0.search.openmrs1_8;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptName;
import org.openmrs.ConceptSearchResult;
import org.openmrs.ConceptSource;
import org.openmrs.api.APIException;
import org.openmrs.api.ConceptService;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.RestUtil;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.api.SearchConfig;
import org.openmrs.module.webservices.rest.web.resource.api.SearchHandler;
import org.openmrs.module.webservices.rest.web.resource.api.SearchQuery;
import org.openmrs.module.webservices.rest.web.resource.impl.EmptySearchResult;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.response.InvalidSearchException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.util.LocaleUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Allows for finding concepts by mapping or by name
 */
@Component
public class ConceptSearchHandler1_8 implements SearchHandler {
	
	@Autowired
	@Qualifier("conceptService")
	ConceptService conceptService;
	
	private final SearchConfig searchConfig = new SearchConfig("default", RestConstants.VERSION_1 + "/concept",
	        Arrays.asList("1.8.* - 9.*"),
	        Arrays.asList(
	            new SearchQuery.Builder("Allows you to find concepts by source and code").withRequiredParameters("source")
	                    .withOptionalParameters("code").build(), new SearchQuery.Builder(
	                    "Allows you to find concepts by name and class").withRequiredParameters("name")
	                    .withOptionalParameters("class", "searchType").build(), new SearchQuery.Builder(
	                    		"Allows you to find a list of concepts by passing references")
	                    .withRequiredParameters("references").build()));
	
	/**
	 * @see SearchHandler#getSearchConfig()
	 */
	@Override
	public SearchConfig getSearchConfig() {
		return searchConfig;
	}
	
	/**
	 * @see SearchHandler#search(RequestContext)
	 */
	@Override
	public PageableResult search(RequestContext context) throws ResponseException {
		String conceptReferences = context.getParameter("references");
		if (StringUtils.isNotBlank(conceptReferences)) {
			return searchByReferences(context, conceptReferences);
		}

		String searchType = context.getParameter("searchType");
		if ("fuzzy".equals(searchType)) {
			return searchByFuzzyName(context);
		}
		if (searchType != null && !"equals".equals(searchType)) {
			throw new InvalidSearchException("Invalid searchType: " + searchType
			        + ". Allowed values: \"equals\" and \"fuzzy\"");
		}

		String name = context.getParameter("name");
		if (name != null) {
			return searchByExactName(context, name);
		}

		return searchBySourceMapping(context);
	}

	/**
	 * Resolves the comma-separated {@code references} parameter to concepts, either by UUID or by
	 * a {@code source:code} mapping. Basis paths P1/P2 in
	 * docs/onderhoudbaarheid/02-testopzet-en-testresultaten/02.md.
	 */
	private PageableResult searchByReferences(RequestContext context, String conceptReferences) {
		String[] conceptReferenceStrings = conceptReferences.split(",");
		List<Concept> concepts = new ArrayList<Concept>(conceptReferenceStrings.length);

		for (String conceptReference : conceptReferenceStrings) {
			if (StringUtils.isBlank(conceptReference)) {
				continue;
			}
			// handle UUIDs
			if (RestUtil.isValidUuid(conceptReference)) {
				Concept concept = conceptService.getConceptByUuid(conceptReference);
				if (concept != null) {
					concepts.add(concept);
					continue;
				}
			}
			// handle mappings
			int idx = conceptReference.indexOf(':');
			if (idx >= 0 && idx < conceptReference.length() - 1) {
				String conceptSource = conceptReference.substring(0, idx);
				String conceptCode = conceptReference.substring(idx + 1);
				Concept concept = conceptService.getConceptByMapping(conceptCode, conceptSource, false);
				if (concept != null) {
					concepts.add(concept);
					continue;
				}
			}
		}
		if (concepts.isEmpty()) {
			return new EmptySearchResult();
		}

		return new NeedsPaging<Concept>(concepts, context);
	}

	/**
	 * Fuzzy name search, optionally scoped to a concept {@code class}. Basis path P3.
	 */
	private PageableResult searchByFuzzyName(RequestContext context) {
		String name = context.getParameter("name");
		String conceptClass = context.getParameter("class");

		List<Locale> locales = new ArrayList<Locale>(LocaleUtility.getLocalesInOrder());
		List<ConceptClass> classes = null;
		ConceptClass responseConceptClass = conceptService.getConceptClassByUuid(conceptClass);

		if (responseConceptClass != null) {
			classes = Arrays.asList(responseConceptClass);
		}

		List<ConceptSearchResult> searchResults = conceptService.getConcepts(name, locales, context.getIncludeAll(),
		    classes, null, null, null, null, context.getStartIndex(), context.getLimit());
		List<Concept> results = new ArrayList<Concept>(searchResults.size());
		for (ConceptSearchResult csr : searchResults) {
			results.add(csr.getConcept());
		}
		return new NeedsPaging<Concept>(results, context);
	}

	/**
	 * Exact name search: the concept must exist and {@code name} must be its preferred or fully
	 * specified name. Basis paths P5/P6/P7.
	 */
	private PageableResult searchByExactName(RequestContext context, String name) {
		Concept concept = conceptService.getConceptByName(name);
		if (concept == null) {
			return new EmptySearchResult();
		}

		boolean isPreferredOrFullySpecified = false;
		for (ConceptName conceptName : concept.getNames()) {
			if (conceptName.getName().equalsIgnoreCase(name)
			        && (conceptName.isPreferred() || conceptName.isFullySpecifiedName())) {
				isPreferredOrFullySpecified = true;
				break;
			}
		}
		if (!isPreferredOrFullySpecified) {
			throw new APIException("The concept name should be either a fully specified or locale preferred name");
		}

		List<Concept> concepts = new ArrayList<Concept>();
		concepts.add(concept);
		return new NeedsPaging<Concept>(concepts, context);
	}

	/**
	 * Resolves concepts via a {@code source} (and optional {@code code}) mapping. Basis paths
	 * P8/P9/P10.
	 */
	private PageableResult searchBySourceMapping(RequestContext context) {
		String source = context.getParameter("source");
		String code = context.getParameter("code");

		ConceptSource conceptSource = conceptService.getConceptSourceByUuid(source);
		if (conceptSource == null) {
			conceptSource = conceptService.getConceptSourceByName(source);
		}
		if (conceptSource == null) {
			return new EmptySearchResult();
		}

		if (code == null) {
			List<Concept> concepts = new ArrayList<Concept>();
			List<ConceptMap> conceptMaps = conceptService.getConceptMappingsToSource(conceptSource);
			for (ConceptMap conceptMap : conceptMaps) {
				if (!conceptMap.getConcept().isRetired() || context.getIncludeAll()) {
					concepts.add(conceptMap.getConcept());
				}
			}
			return new NeedsPaging<Concept>(concepts, context);
		} else {
			List<Concept> conceptsByMapping = conceptService.getConceptsByMapping(code, source, false);

			return new NeedsPaging<Concept>(conceptsByMapping, context);
		}
	}
}
