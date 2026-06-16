/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.webservices.rest.web.v1_0.search.openmrs1_9;

import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Obs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ObsValueMatcherTest {

	@Test
	public void codedMatcher_shouldReturnFalse_whenValueCodedIsNull() {
		Obs obs = new Obs();
		obs.setValueCoded(null);

		assertFalse(new CodedObsValueMatcher().matches(obs, new String[] { "some-uuid" }));
	}

	@Test
	public void codedMatcher_shouldReturnTrue_whenValueCodedUuidMatchesAnyValue() {
		Concept concept = new Concept();
		concept.setUuid("abc-uuid");
		Obs obs = new Obs();
		obs.setValueCoded(concept);

		assertTrue(new CodedObsValueMatcher().matches(obs, new String[] { "other-uuid", "abc-uuid" }));
	}

	@Test
	public void codedMatcher_shouldReturnFalse_whenValueCodedUuidMatchesNoValue() {
		Concept concept = new Concept();
		concept.setUuid("abc-uuid");
		Obs obs = new Obs();
		obs.setValueCoded(concept);

		assertFalse(new CodedObsValueMatcher().matches(obs, new String[] { "other-uuid" }));
	}

	@Test
	public void numericMatcher_shouldReturnFalse_whenValueNumericIsNull() {
		Obs obs = new Obs();

		assertFalse(new NumericObsValueMatcher().matches(obs, new String[] { "150" }));
	}

	@Test
	public void numericMatcher_shouldReturnTrue_whenValueNumericMatchesAnyValue() {
		Obs obs = new Obs();
		obs.setValueNumeric(150.0);

		assertTrue(new NumericObsValueMatcher().matches(obs, new String[] { "175", "150" }));
	}

	@Test
	public void numericMatcher_shouldReturnFalse_whenValueNumericMatchesNoValue() {
		Obs obs = new Obs();
		obs.setValueNumeric(200.0);

		assertFalse(new NumericObsValueMatcher().matches(obs, new String[] { "150", "175" }));
	}

	@Test(expected = IllegalArgumentException.class)
	public void numericMatcher_shouldThrow_whenObsValuesContainsNonNumericString() {
		Obs obs = new Obs();
		obs.setValueNumeric(150.0);

		new NumericObsValueMatcher().matches(obs, new String[] { "abc" });
	}

	@Test
	public void textMatcher_shouldReturnTrue_whenValueTextMatchesAnyValue() {
		Obs obs = new Obs();
		obs.setValueText("PB and J");

		assertTrue(new TextObsValueMatcher().matches(obs, new String[] { "PB and J" }));
	}

	@Test
	public void textMatcher_shouldReturnFalse_whenValueTextMatchesNoValue() {
		Obs obs = new Obs();
		obs.setValueText("something else");

		assertFalse(new TextObsValueMatcher().matches(obs, new String[] { "PB and J" }));
	}
}
