/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.webservices.rest.web.filter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Tests for {@link SecurityHeadersFilter} — CWE-693 / NEN-7510 A.8.23.
 */
public class SecurityHeadersFilterTest {

	private SecurityHeadersFilter filter;

	private MockHttpServletRequest req;

	private MockHttpServletResponse resp;

	private MockFilterChain chain;

	@Before
	public void setUp() {
		filter = new SecurityHeadersFilter();
		req = new MockHttpServletRequest();
		resp = new MockHttpServletResponse();
		chain = new MockFilterChain();
	}

	@Test
	public void doFilter_shouldSetXContentTypeOptionsNosniff() throws Exception {
		filter.doFilter(req, resp, chain);
		Assert.assertEquals("nosniff", resp.getHeader("X-Content-Type-Options"));
	}

	@Test
	public void doFilter_shouldSetXFrameOptionsDeny() throws Exception {
		filter.doFilter(req, resp, chain);
		Assert.assertEquals("DENY", resp.getHeader("X-Frame-Options"));
	}

	@Test
	public void doFilter_shouldSetContentSecurityPolicy() throws Exception {
		filter.doFilter(req, resp, chain);
		Assert.assertEquals("default-src 'none'", resp.getHeader("Content-Security-Policy"));
	}

	@Test
	public void doFilter_shouldSetStrictTransportSecurity() throws Exception {
		filter.doFilter(req, resp, chain);
		Assert.assertEquals("max-age=31536000; includeSubDomains",
		    resp.getHeader("Strict-Transport-Security"));
	}

	@Test
	public void doFilter_shouldSetReferrerPolicyNoReferrer() throws Exception {
		filter.doFilter(req, resp, chain);
		Assert.assertEquals("no-referrer", resp.getHeader("Referrer-Policy"));
	}

	@Test
	public void doFilter_shouldSetCacheControlNoStore() throws Exception {
		filter.doFilter(req, resp, chain);
		Assert.assertEquals("no-store", resp.getHeader("Cache-Control"));
	}

	@Test
	public void doFilter_shouldPassRequestThroughChain() throws Exception {
		filter.doFilter(req, resp, chain);
		Assert.assertNotNull("Request must reach the chain", chain.getRequest());
	}
}
