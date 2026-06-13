/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.webservices.rest.web;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;

/**
 * Tests for the Swagger/OpenAPI attack-surface-reduction helpers added to {@link RestUtil}
 * (NEN-7510 A.8.3 least functionality, A.8.20/A.8.26 Host-header validation — audit findings
 * SQ8/SQ9 in the hardening checklist).
 */
public class SwaggerHardeningRestUtilTest extends BaseModuleWebContextSensitiveTest {

	private void setGlobalProperty(String name, String value) {
		Context.getAdministrationService().saveGlobalProperty(new GlobalProperty(name, value));
	}

	// ── isSwaggerDocsEnabled (§1 — gate the docs endpoints) ──

	@Test
	public void isSwaggerDocsEnabled_shouldDefaultToTrueWhenUnset() {
		GlobalProperty gp = Context.getAdministrationService().getGlobalPropertyObject(
		    RestConstants.ENABLE_SWAGGER_DOCS_GLOBAL_PROPERTY_NAME);
		if (gp != null) {
			Context.getAdministrationService().purgeGlobalProperty(gp);
		}
		Assert.assertTrue(RestUtil.isSwaggerDocsEnabled());
	}

	@Test
	public void isSwaggerDocsEnabled_shouldReturnFalseWhenGlobalPropertyIsFalse() {
		setGlobalProperty(RestConstants.ENABLE_SWAGGER_DOCS_GLOBAL_PROPERTY_NAME, "false");
		Assert.assertFalse(RestUtil.isSwaggerDocsEnabled());
	}

	@Test
	public void isSwaggerDocsEnabled_shouldReturnTrueWhenGlobalPropertyIsTrue() {
		setGlobalProperty(RestConstants.ENABLE_SWAGGER_DOCS_GLOBAL_PROPERTY_NAME, "true");
		Assert.assertTrue(RestUtil.isSwaggerDocsEnabled());
	}

	// ── resolveSwaggerHost (§10 — Host-header allow-list, SQ9) ──

	@Test
	public void resolveSwaggerHost_shouldReturnRequestHostWhenAllowListEmpty() {
		setGlobalProperty(RestConstants.SWAGGER_ALLOWED_HOSTS_GLOBAL_PROPERTY_NAME, "");
		Assert.assertEquals("evil.example.com", RestUtil.resolveSwaggerHost("evil.example.com"));
	}

	@Test
	public void resolveSwaggerHost_shouldReturnRequestHostWhenItIsOnTheAllowList() {
		setGlobalProperty(RestConstants.SWAGGER_ALLOWED_HOSTS_GLOBAL_PROPERTY_NAME,
		    "openmrs.example.org, api.example.org:8443");
		Assert.assertEquals("api.example.org:8443", RestUtil.resolveSwaggerHost("api.example.org:8443"));
	}

	@Test
	public void resolveSwaggerHost_shouldFallBackToFirstAllowedHostWhenRequestHostNotAllowed() {
		setGlobalProperty(RestConstants.SWAGGER_ALLOWED_HOSTS_GLOBAL_PROPERTY_NAME,
		    "openmrs.example.org, api.example.org:8443");
		Assert.assertEquals("openmrs.example.org", RestUtil.resolveSwaggerHost("evil.example.com"));
	}

	// ── sanitizeScheme (§10 — only http/https may be reflected, X-Forwarded-Proto injection) ──

	@Test
	public void sanitizeScheme_shouldKeepHttpAndHttps() {
		Assert.assertEquals("http", RestUtil.sanitizeScheme("http", "https"));
		Assert.assertEquals("https", RestUtil.sanitizeScheme("HTTPS", "http"));
	}

	@Test
	public void sanitizeScheme_shouldFallBackToDefaultForUnsafeScheme() {
		Assert.assertEquals("https", RestUtil.sanitizeScheme("javascript", "https"));
		Assert.assertEquals("http", RestUtil.sanitizeScheme(null, "http"));
	}
}
