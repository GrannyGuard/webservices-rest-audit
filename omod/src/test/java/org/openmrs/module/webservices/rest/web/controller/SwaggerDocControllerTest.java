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

import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Validates the SQ8 / CWE-79 mitigation in
 * {@link SwaggerDocController#debug(String, HttpServletResponse)}: the user-supplied
 * {@code tag} parameter must be HTML-encoded ({@code HtmlUtils.htmlEscape}) before it is
 * reflected into the response, so a {@code <script>} payload can no longer break out of
 * the HTML context (reflected XSS). The second test covers the defense-in-depth gating:
 * with the Swagger docs disabled the endpoint returns HTTP 404 and no body.
 * <p>
 * This is a direct method-level unit test, not an HTTP request. It therefore does
 * <em>not</em> depend on the {@code /apiDocs/debug} route being reachable on a running
 * server (which returns 404 when Swagger is disabled) — it calls the controller method
 * straight on the object. {@link BaseModuleWebContextSensitiveTest} authenticates as the
 * admin superuser, so
 * {@link org.openmrs.module.webservices.rest.web.RestUtil#isSwaggerDocsEnabled()} can
 * read the {@code webservices.rest.enableSwaggerDocs} global property.
 */
public class SwaggerDocControllerTest extends BaseModuleWebContextSensitiveTest {

	private final SwaggerDocController controller = new SwaggerDocController();

	@Test
	public void debug_shouldHtmlEncodeTagToPreventReflectedXss() throws Exception {
		Context.getAdministrationService().setGlobalProperty(
		    RestConstants.ENABLE_SWAGGER_DOCS_GLOBAL_PROPERTY_NAME, "true");

		String body = controller.debug("<script>alert(1)</script>", new MockHttpServletResponse());

		Assert.assertNotNull("a body must be returned when Swagger docs are enabled", body);
		Assert.assertTrue("the payload must be HTML-encoded (taint barrier)",
		    body.contains("&lt;script&gt;"));
		Assert.assertFalse("a raw <script> tag must never be reflected into the response",
		    body.contains("<script>"));
	}

	@Test
	public void debug_shouldGateWith404WhenSwaggerDocsDisabled() throws Exception {
		Context.getAdministrationService().setGlobalProperty(
		    RestConstants.ENABLE_SWAGGER_DOCS_GLOBAL_PROPERTY_NAME, "false");

		MockHttpServletResponse response = new MockHttpServletResponse();
		String body = controller.debug("<script>alert(1)</script>", response);

		Assert.assertNull("no body must be returned when Swagger docs are disabled", body);
		Assert.assertEquals("the endpoint must gate with HTTP 404 as defense-in-depth",
		    HttpServletResponse.SC_NOT_FOUND, response.getStatus());
	}
}
