/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.webservices.rest.web.v1_0.resource.openmrs1_8;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestTestConstants1_8;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.response.ConversionException;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;

public class ConceptResource1_8Test extends BaseModuleWebContextSensitiveTest {

	@Test
	public void doSearch_shouldContinueWithNullAnswerToWhenConversionFails() throws Exception {
		ConceptResource1_8 resource = new ConceptResource1_8() {

			@Override
			protected Concept resolveAnswerToConcept(String uuid) throws ConversionException {
				throw new ConversionException("Simulated conversion failure", null);
			}
		};

		MockHttpServletRequest httpRequest = new MockHttpServletRequest();
		httpRequest.setParameter("answerTo", "invalid-uuid-triggers-exception");
		httpRequest.setParameter("q", "");

		RequestContext context = new RequestContext();
		context.setRequest(httpRequest);
		context.setIncludeAll(false);
		context.setStartIndex(0);
		context.setLimit(50);

		// ConversionException is caught and logged; search continues with answerTo=null
		PageableResult result = resource.doSearch(context);
		Assert.assertNotNull(result);
	}

	@Test
	public void resolveAnswerToConcept_shouldReturnConceptForValidUuid() throws Exception {
		ConceptResource1_8 resource = new ConceptResource1_8();
		Concept result = resource.resolveAnswerToConcept(RestTestConstants1_8.CONCEPT_UUID);
		Assert.assertNotNull(result);
		Assert.assertEquals(RestTestConstants1_8.CONCEPT_UUID, result.getUuid());
	}
}
