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
import org.openmrs.api.context.Context;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import javax.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Verifies CWE-384 / NEN-7510 A.8.5 session fixation protection in {@link AuthorizationFilter}.
 * After a successful Basic-Auth login, any pre-existing session must be invalidated and a
 * fresh session must be created with a different ID.
 */
public class AuthorizationFilterSessionFixationTest extends BaseModuleWebContextSensitiveTest {

	private static final String IP = "10.0.0.99";

	private AuthorizationFilter filter;

	@Before
	public void setUp() {
		filter = new AuthorizationFilter();
		AuthorizationFilter.resetRateLimitState();
	}

	private MockHttpServletRequest buildRequest(String user, String pass) {
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setRemoteAddr(IP);
		req.setRequestURI("/ws/rest/v1/session");
		String encoded = Base64.getEncoder()
		        .encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
		req.addHeader("Authorization", "Basic " + encoded);
		return req;
	}

	@Test
	public void doFilter_successfulAuth_invalidatesPreExistingSession() throws Exception {
		String user = Context.getAuthenticatedUser().getUsername();
		Context.getUserService().changePassword(Context.getAuthenticatedUser(), "FixTest@Pass1");
		Context.logout();

		MockHttpServletRequest req = buildRequest(user, "FixTest@Pass1");
		MockHttpSession oldSession = (MockHttpSession) req.getSession(true);

		filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());

		Assert.assertTrue("Pre-existing session must be invalidated after successful login",
		    oldSession.isInvalid());
	}

	@Test
	public void doFilter_successfulAuth_createsNewSession() throws Exception {
		String user = Context.getAuthenticatedUser().getUsername();
		Context.getUserService().changePassword(Context.getAuthenticatedUser(), "FixTest@Pass2");
		Context.logout();

		MockHttpServletRequest req = buildRequest(user, "FixTest@Pass2");
		req.getSession(true); // create pre-existing session

		filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());

		HttpSession newSession = req.getSession(false);
		Assert.assertNotNull("A new session must exist after successful login", newSession);
	}

	@Test
	public void doFilter_successfulAuth_newSessionIdDiffersFromOld() throws Exception {
		String user = Context.getAuthenticatedUser().getUsername();
		Context.getUserService().changePassword(Context.getAuthenticatedUser(), "FixTest@Pass3");
		Context.logout();

		MockHttpServletRequest req = buildRequest(user, "FixTest@Pass3");
		String oldId = req.getSession(true).getId();

		filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());

		HttpSession newSession = req.getSession(false);
		Assert.assertNotNull(newSession);
		Assert.assertNotEquals("Session ID must change after login to prevent session fixation",
		    oldId, newSession.getId());
	}

	@Test
	public void doFilter_failedAuth_doesNotInvalidateExistingSession() throws Exception {
		Context.logout();

		MockHttpServletRequest req = buildRequest("nobody", "wrongpass");
		MockHttpSession session = (MockHttpSession) req.getSession(true);

		filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());

		Assert.assertFalse("Session must NOT be invalidated on failed authentication",
		    session.isInvalid());
	}

	@Test
	public void doFilter_noPreExistingSession_successfulAuth_createsSession() throws Exception {
		String user = Context.getAuthenticatedUser().getUsername();
		Context.getUserService().changePassword(Context.getAuthenticatedUser(), "FixTest@Pass4");
		Context.logout();

		MockHttpServletRequest req = buildRequest(user, "FixTest@Pass4");
		// No pre-existing session — simulates first request from a fresh client

		filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());

		Assert.assertNotNull("A session must be created after successful login even if none existed",
		    req.getSession(false));
	}
}
