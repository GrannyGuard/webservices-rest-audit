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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Verifies CWE-307 / NEN-7510 A.8.15 rate limiting in {@link AuthorizationFilter}.
 * Covers: 429 after threshold, ACCESS_BLOCKED log, IP isolation, counter cleared on success.
 */
public class AuthorizationFilterRateLimitTest extends BaseModuleWebContextSensitiveTest {

	private static final String BAD_USER = "nobody";

	private static final String BAD_PASS = "wrongpass";

	private static final String IP_A = "10.0.0.10";

	private static final String IP_B = "10.0.0.11";

	private static final String IP_C = "10.0.0.12";

	private static final String IP_D = "10.0.0.13";

	private AuthorizationFilter filter;

	private TestAppender appender;

	private Logger filterLogger;

	private static class TestAppender extends AbstractAppender {

		final List<LogEvent> events = new ArrayList<>();

		TestAppender() {
			super("rate-limit-test-appender", null, null, true, Property.EMPTY_ARRAY);
		}

		@Override
		public void append(LogEvent event) {
			events.add(event.toImmutable());
		}
	}

	@Before
	public void setUp() {
		filter = new AuthorizationFilter();
		AuthorizationFilter.resetRateLimitState();

		filterLogger = (Logger) LogManager.getLogger(AuthorizationFilter.class);
		appender = new TestAppender();
		appender.start();
		filterLogger.addAppender(appender);
		filterLogger.setLevel(Level.WARN);
	}

	@After
	public void tearDown() {
		filterLogger.removeAppender(appender);
		appender.stop();
		AuthorizationFilter.resetRateLimitState();
	}

	private MockHttpServletRequest invalidRequest(String ip) {
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setRemoteAddr(ip);
		req.setRequestURI("/ws/rest/v1/patient");
		String encoded = Base64.getEncoder().encodeToString(
		    (BAD_USER + ":" + BAD_PASS).getBytes(StandardCharsets.UTF_8));
		req.addHeader("Authorization", "Basic " + encoded);
		return req;
	}

	private MockHttpServletRequest validRequest(String ip, String user, String pass) {
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setRemoteAddr(ip);
		req.setRequestURI("/ws/rest/v1/patient");
		String encoded = Base64.getEncoder().encodeToString(
		    (user + ":" + pass).getBytes(StandardCharsets.UTF_8));
		req.addHeader("Authorization", "Basic " + encoded);
		return req;
	}

	// ── core rate limit behaviour ─────────────────────────────────────────────

	@Test
	public void doFilter_afterMaxFailedAttempts_returns429() throws Exception {
		Context.logout();
		MockHttpServletRequest req = invalidRequest(IP_A);
		for (int i = 0; i < AuthorizationFilter.RATE_LIMIT_MAX_ATTEMPTS; i++) {
			filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
		}
		MockHttpServletResponse blocked = new MockHttpServletResponse();
		filter.doFilter(req, blocked, new MockFilterChain());
		Assert.assertEquals("Expected HTTP 429 after rate limit exceeded", 429, blocked.getStatus());
	}

	@Test
	public void doFilter_belowMaxFailedAttempts_doesNotReturn429() throws Exception {
		Context.logout();
		MockHttpServletRequest req = invalidRequest(IP_A);
		// one fewer than the threshold
		for (int i = 0; i < AuthorizationFilter.RATE_LIMIT_MAX_ATTEMPTS - 1; i++) {
			filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
		}
		MockHttpServletResponse response = new MockHttpServletResponse();
		filter.doFilter(req, response, new MockFilterChain());
		Assert.assertNotEquals("Should not be rate-limited before threshold", 429, response.getStatus());
	}

	// ── logging ───────────────────────────────────────────────────────────────

	@Test
	public void doFilter_afterMaxFailedAttempts_logsAccessBlockedAtWarn() throws Exception {
		Context.logout();
		MockHttpServletRequest req = invalidRequest(IP_B);
		for (int i = 0; i < AuthorizationFilter.RATE_LIMIT_MAX_ATTEMPTS; i++) {
			filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
		}
		appender.events.clear();
		filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
		boolean found = appender.events.stream().anyMatch(e ->
		    e.getLevel() == Level.WARN
		        && e.getMessage().getFormattedMessage().contains("ACCESS_BLOCKED")
		        && e.getMessage().getFormattedMessage().contains("rate_limited=true"));
		Assert.assertTrue("Expected WARN ACCESS_BLOCKED rate_limited=true log entry", found);
	}

	@Test
	public void doFilter_afterMaxFailedAttempts_logsBlockedIp() throws Exception {
		Context.logout();
		MockHttpServletRequest req = invalidRequest(IP_B);
		for (int i = 0; i < AuthorizationFilter.RATE_LIMIT_MAX_ATTEMPTS; i++) {
			filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
		}
		appender.events.clear();
		filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
		boolean found = appender.events.stream().anyMatch(e ->
		    e.getLevel() == Level.WARN && e.getMessage().getFormattedMessage().contains(IP_B));
		Assert.assertTrue("Blocked IP must appear in the ACCESS_BLOCKED log entry", found);
	}

	// ── IP isolation ─────────────────────────────────────────────────────────

	@Test
	public void doFilter_rateLimitDoesNotAffectOtherIps() throws Exception {
		Context.logout();
		// exhaust the limit for IP_C
		MockHttpServletRequest reqC = invalidRequest(IP_C);
		for (int i = 0; i < AuthorizationFilter.RATE_LIMIT_MAX_ATTEMPTS; i++) {
			filter.doFilter(reqC, new MockHttpServletResponse(), new MockFilterChain());
		}
		// IP_D should still be allowed through
		MockHttpServletRequest reqD = invalidRequest(IP_D);
		MockHttpServletResponse responseD = new MockHttpServletResponse();
		filter.doFilter(reqD, responseD, new MockFilterChain());
		Assert.assertNotEquals("Rate limit for IP_C must not block IP_D", 429, responseD.getStatus());
	}

	// ── counter cleared on success ────────────────────────────────────────────

	@Test
	public void doFilter_successfulAuthClearsFailedAttemptCounter() throws Exception {
		String validUser = Context.getAuthenticatedUser().getUsername();
		Context.getUserService().changePassword(Context.getAuthenticatedUser(), "RateTest@Pass1");
		Context.logout();

		// 9 failed attempts — one below the threshold
		MockHttpServletRequest badReq = invalidRequest(IP_A);
		for (int i = 0; i < AuthorizationFilter.RATE_LIMIT_MAX_ATTEMPTS - 1; i++) {
			filter.doFilter(badReq, new MockHttpServletResponse(), new MockFilterChain());
		}

		// Successful auth should clear the counter
		MockHttpServletRequest goodReq = validRequest(IP_A, validUser, "RateTest@Pass1");
		filter.doFilter(goodReq, new MockHttpServletResponse(), new MockFilterChain());
		Context.logout();

		// First attempt after clear — should NOT be blocked (counter is back to 0)
		MockHttpServletResponse response = new MockHttpServletResponse();
		filter.doFilter(badReq, response, new MockFilterChain());
		Assert.assertNotEquals("Successful auth must reset the rate-limit counter", 429, response.getStatus());
	}
}
