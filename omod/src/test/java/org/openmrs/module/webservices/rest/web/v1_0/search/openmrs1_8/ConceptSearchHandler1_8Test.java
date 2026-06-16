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

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.api.APIException;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestTestConstants1_8;
import org.openmrs.module.webservices.rest.web.response.InvalidSearchException;
import org.openmrs.module.webservices.rest.web.v1_0.controller.RestControllerTestUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

/**
 * Basis-path tests voor {@link ConceptSearchHandler1_8#search(RequestContext)}, conform de
 * basispaden P1-P10 / testcases TC1-11 uit
 * docs/onderhoudbaarheid/02-testopzet-en-testresultaten/02.md. Dit is het regressievangnet voor
 * de refactoring beschreven in docs/onderhoudbaarheid/04-aangepast-ontwerp/04.md.
 */
public class ConceptSearchHandler1_8Test extends RestControllerTestUtils {

	// "Some Standardized Terminology" / WGT234 -> WEIGHT (KG)
	private static final String SOURCE_NAME = "Some Standardized Terminology";

	private static final String SOURCE_CODE = "WGT234";

	private static final String CONCEPT_BY_MAPPING_UUID = "c607c80f-1ea9-4da3-bb88-6276ce8868dd";

	protected String getURI() {
		return "concept";
	}

	/**
	 * TC1 / pad P1: references gevuld, maar geen enkele reference resolveert.
	 *
	 * @verifies return an empty search result when no reference resolves
	 * @see ConceptSearchHandler1_8#search(RequestContext)
	 */
	@Test
	public void shouldReturnEmptySearchResultWhenNoReferenceResolves() throws Exception {
		MockHttpServletRequest req = request(RequestMethod.GET, getURI());
		req.addParameter("references", "not-a-uuid-and-no-mapping");

		SimpleObject result = deserialize(handle(req));
		List<Object> hits = result.get("results");
		Assert.assertEquals(0, hits.size());
	}

	/**
	 * TC2 / pad P2: references bevat een geldige concept-UUID.
	 *
	 * @verifies return the concept resolved by uuid reference
	 * @see ConceptSearchHandler1_8#search(RequestContext)
	 */
	@Test
	public void shouldReturnConceptResolvedByUuidReference() throws Exception {
		MockHttpServletRequest req = request(RequestMethod.GET, getURI());
		req.addParameter("references", RestTestConstants1_8.CONCEPT_UUID);

		SimpleObject result = deserialize(handle(req));
		List<Object> hits = result.get("results");
		Assert.assertEquals(1, hits.size());
	}

	/**
	 * TC3 / pad P2: references bevat een bron:code-mapping (geen UUID).
	 *
	 * @verifies return the concept resolved by source:code mapping reference
	 * @see ConceptSearchHandler1_8#search(RequestContext)
	 */
	@Test
	public void shouldReturnConceptResolvedByMappingReference() throws Exception {
		MockHttpServletRequest req = request(RequestMethod.GET, getURI());
		req.addParameter("references", SOURCE_NAME + ":" + SOURCE_CODE);

		SimpleObject result = deserialize(handle(req));
		List<Object> hits = result.get("results");
		Assert.assertEquals(1, hits.size());
	}

	/**
	 * TC4 / pad P3: searchType=fuzzy, met optionele class.
	 *
	 * @verifies return concepts matching a fuzzy name search
	 * @see ConceptSearchHandler1_8#search(RequestContext)
	 */
	@Test
	public void shouldReturnConceptsForFuzzySearch() throws Exception {
		Context.getConceptService().updateConceptIndexes();

		MockHttpServletRequest req = request(RequestMethod.GET, getURI());
		req.addParameter("name", "Asp");
		req.addParameter("searchType", "fuzzy");
		req.addParameter("class", "3d065ed4-b0b9-4710-9a17-6d8c4fd259b7"); // DRUG

		SimpleObject result = deserialize(handle(req));
		List<Object> hits = result.get("results");
		Assert.assertFalse(hits.isEmpty());
	}

	/**
	 * TC5 / pad P4: ongeldige searchType.
	 *
	 * @verifies throw InvalidSearchException for an unsupported searchType
	 * @see ConceptSearchHandler1_8#search(RequestContext)
	 */
	@Test(expected = InvalidSearchException.class)
	public void shouldThrowExceptionForInvalidSearchType() throws Exception {
		MockHttpServletRequest req = request(RequestMethod.GET, getURI());
		req.addParameter("name", "Aspirin");
		req.addParameter("searchType", "banana");

		handle(req);
	}

	/**
	 * TC6 / pad P5: searchType null/equals, name is de preferred/fully specified naam.
	 *
	 * @verifies return the concept with the matching preferred name
	 * @see ConceptSearchHandler1_8#search(RequestContext)
	 */
	@Test
	public void shouldReturnConceptForExactPreferredName() throws Exception {
		MockHttpServletRequest req = request(RequestMethod.GET, getURI());
		req.addParameter("name", "WEIGHT (KG)");

		SimpleObject result = deserialize(handle(req));
		List<Object> hits = result.get("results");
		Assert.assertEquals(1, hits.size());
	}

	/**
	 * TC7 / pad P6: concept gevonden, maar de opgegeven naam is niet de preferred/fully specified
	 * naam.
	 *
	 * @verifies throw APIException when the matched name is not preferred or fully specified
	 * @see ConceptSearchHandler1_8#search(RequestContext)
	 */
	@Test(expected = APIException.class)
	public void shouldThrowExceptionWhenNameIsNotPreferredOrFullySpecified() throws Exception {
		MockHttpServletRequest req = request(RequestMethod.GET, getURI());
		req.addParameter("name", "WT");

		handle(req);
	}

	/**
	 * TC8 / pad P7: name gezet, maar er bestaat geen concept met die naam.
	 *
	 * @verifies return an empty search result when no concept matches the given name
	 * @see ConceptSearchHandler1_8#search(RequestContext)
	 */
	@Test
	public void shouldReturnEmptySearchResultWhenNameNotFound() throws Exception {
		MockHttpServletRequest req = request(RequestMethod.GET, getURI());
		req.addParameter("name", "ThisConceptNameDoesNotExist12345");

		SimpleObject result = deserialize(handle(req));
		List<Object> hits = result.get("results");
		Assert.assertEquals(0, hits.size());
	}

	/**
	 * TC9 / pad P8: name leeg, source resolveert niet naar een bekende ConceptSource.
	 *
	 * @verifies return an empty search result when the source cannot be resolved
	 * @see ConceptSearchHandler1_8#search(RequestContext)
	 */
	@Test
	public void shouldReturnEmptySearchResultWhenSourceNotFound() throws Exception {
		MockHttpServletRequest req = request(RequestMethod.GET, getURI());
		req.addParameter("source", "UnknownConceptSourceXYZ");

		SimpleObject result = deserialize(handle(req));
		List<Object> hits = result.get("results");
		Assert.assertEquals(0, hits.size());
	}

	/**
	 * TC10 / pad P9: name leeg, source resolveert, code leeg -> mappings naar de bron.
	 *
	 * @verifies return concepts mapped to the source when code is not given
	 * @see ConceptSearchHandler1_8#search(RequestContext)
	 */
	@Test
	public void shouldReturnConceptsMappedToSourceWhenCodeIsMissing() throws Exception {
		MockHttpServletRequest req = request(RequestMethod.GET, getURI());
		req.addParameter("source", SOURCE_NAME);

		SimpleObject result = deserialize(handle(req));
		List<Object> hits = result.get("results");
		Assert.assertEquals(10, hits.size());
	}

	/**
	 * TC11 / pad P10: name leeg, source resolveert, code gezet -> getConceptsByMapping.
	 *
	 * @verifies return the concept matching the source and code mapping
	 * @see ConceptSearchHandler1_8#search(RequestContext)
	 */
	@Test
	public void shouldReturnConceptsBySourceAndCode() throws Exception {
		MockHttpServletRequest req = request(RequestMethod.GET, getURI());
		req.addParameter("source", SOURCE_NAME);
		req.addParameter("code", SOURCE_CODE);

		SimpleObject result = deserialize(handle(req));
		List<Object> hits = result.get("results");
		Assert.assertEquals(1, hits.size());
	}
}
