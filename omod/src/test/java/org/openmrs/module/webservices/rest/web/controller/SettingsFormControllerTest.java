/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.webservices.rest.web.controller;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;

/**
 * Authorization tests for {@link SettingsFormController#searchProperties(String)}.
 * B-001 mitigation: an unauthenticated caller must not be able to enumerate global
 * properties; an authenticated user with the required privilege may.
 * <p>
 * {@link BaseModuleWebContextSensitiveTest} authenticates as the admin superuser by
 * default, so the positive case relies on that and the negative case logs out first.
 */
public class SettingsFormControllerTest extends BaseModuleWebContextSensitiveTest {

	private final SettingsFormController controller = new SettingsFormController();

	@Test(expected = APIAuthenticationException.class)
	public void searchProperties_shouldRejectUnauthenticatedCaller() {
		Context.logout();
		controller.searchProperties("");
	}

	@Test
	public void searchProperties_shouldReturnPropertiesForAuthorizedAdmin() {
		// default context is authenticated as the admin superuser (has "Manage RESTWS")
		Assert.assertTrue("precondition: context must be authenticated", Context.isAuthenticated());
		String result = controller.searchProperties("");
		Assert.assertNotNull(result);
		Assert.assertTrue("response must be a JSON-like array", result.startsWith("["));
	}
}
