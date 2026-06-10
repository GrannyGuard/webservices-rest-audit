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
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Verifies NEN-7510:2024 A.8.15 logging compliance for {@link AuthorizationFilter}.
 * Covers: auth success/failure at correct log level, presence of user+IP,
 * and confirmed absence of passwords from all log output.
 */
public class AuthorizationFilterLoggingTest extends BaseModuleWebContextSensitiveTest {

	private static final String INVALID_USER = "nobody";

	private static final String INVALID_PASS = "wrongpass123";

	private static final String TEST_IP = "127.0.0.1";

	private static final String TEST_URI = "/ws/rest/v1/patient";

	private static final String BLOCKED_IP = "192.168.99.99";

	private static final String KNOWN_PASS = "LoggingTest@Pass1";

	private String validUser;

	private AuthorizationFilter filter;

	private TestAppender appender;

	private Logger filterLogger;

	private static class TestAppender extends AbstractAppender {

		final List<LogEvent> events = new ArrayList<>();

		TestAppender() {
			super("filter-logging-test-appender", null, null, true, Property.EMPTY_ARRAY);
		}

		@Override
		public void append(LogEvent event) {
			events.add(event.toImmutable());
		}
	}

	@Before
	public void setUpLogging() {
		filter = new AuthorizationFilter();

		// Capture username and set a known password while still authenticated
		validUser = Context.getAuthenticatedUser().getUsername();
		Context.getUserService().changePassword(Context.getAuthenticatedUser(), KNOWN_PASS);

		filterLogger = (Logger) LogManager.getLogger(AuthorizationFilter.class);
		appender = new TestAppender();
		appender.start();
		filterLogger.addAppender(appender);
		filterLogger.setLevel(Level.INFO);
	}

	@After
	public void tearDownLogging() {
		filterLogger.removeAppender(appender);
		appender.stop();
	}

	private MockHttpServletRequest requestWith(String user, String pass, String ip) {
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setRemoteAddr(ip);
		req.setRequestURI(TEST_URI);
		String encoded = Base64.getEncoder().encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
		req.addHeader("Authorization", "Basic " + encoded);
		return req;
	}

	private boolean hasLog(Level level, String text) {
		return appender.events.stream()
		        .anyMatch(e -> e.getLevel() == level && e.getMessage().getFormattedMessage().contains(text));
	}

	private boolean anyLogContains(String text) {
		return appender.events.stream()
		        .anyMatch(e -> e.getMessage().getFormattedMessage().contains(text));
	}

	// ── auth success ─────────────────────────────────────────────────────────

	@Test
	public void doFilter_successfulAuth_logsAtInfoLevel() throws Exception {
		Context.logout();
		filter.doFilter(requestWith(validUser, KNOWN_PASS, TEST_IP), new MockHttpServletResponse(), new MockFilterChain());

		Assert.assertTrue("Expected INFO log entry with AUTH_SUCCESS", hasLog(Level.INFO, "AUTH_SUCCESS"));
	}

	@Test
	public void doFilter_successfulAuth_logsUsername() throws Exception {
		Context.logout();
		filter.doFilter(requestWith(validUser, KNOWN_PASS, TEST_IP), new MockHttpServletResponse(), new MockFilterChain());

		Assert.assertTrue("Username must appear in auth success log", anyLogContains(validUser));
	}

	@Test
	public void doFilter_successfulAuth_logsRemoteIp() throws Exception {
		Context.logout();
		filter.doFilter(requestWith(validUser, KNOWN_PASS, TEST_IP), new MockHttpServletResponse(), new MockFilterChain());

		Assert.assertTrue("Remote IP must appear in auth success log", anyLogContains(TEST_IP));
	}

	@Test
	public void doFilter_successfulAuth_doesNotLogPassword() throws Exception {
		Context.logout();
		filter.doFilter(requestWith(validUser, KNOWN_PASS, TEST_IP), new MockHttpServletResponse(), new MockFilterChain());

		Assert.assertFalse("Password must never appear in any log entry", anyLogContains(KNOWN_PASS));
	}

	// ── auth failure ─────────────────────────────────────────────────────────

	@Test
	public void doFilter_failedAuth_logsAtWarnLevel() throws Exception {
		Context.logout();
		filter.doFilter(requestWith(INVALID_USER, INVALID_PASS, TEST_IP), new MockHttpServletResponse(), new MockFilterChain());

		Assert.assertTrue("Expected WARN log entry with AUTH_FAILURE", hasLog(Level.WARN, "AUTH_FAILURE"));
	}

	@Test
	public void doFilter_failedAuth_logsUsername() throws Exception {
		Context.logout();
		filter.doFilter(requestWith(INVALID_USER, INVALID_PASS, TEST_IP), new MockHttpServletResponse(), new MockFilterChain());

		Assert.assertTrue("Username must appear in auth failure WARN log",
		    appender.events.stream().anyMatch(
		        e -> e.getLevel() == Level.WARN && e.getMessage().getFormattedMessage().contains(INVALID_USER)));
	}

	@Test
	public void doFilter_failedAuth_logsRemoteIp() throws Exception {
		Context.logout();
		filter.doFilter(requestWith(INVALID_USER, INVALID_PASS, TEST_IP), new MockHttpServletResponse(), new MockFilterChain());

		Assert.assertTrue("Remote IP must appear in auth failure WARN log",
		    appender.events.stream().anyMatch(
		        e -> e.getLevel() == Level.WARN && e.getMessage().getFormattedMessage().contains(TEST_IP)));
	}

	@Test
	public void doFilter_failedAuth_doesNotLogPassword() throws Exception {
		Context.logout();
		filter.doFilter(requestWith(INVALID_USER, INVALID_PASS, TEST_IP), new MockHttpServletResponse(), new MockFilterChain());

		Assert.assertFalse("Password must never appear in any log entry on auth failure", anyLogContains(INVALID_PASS));
	}

	// ── IP blocking ───────────────────────────────────────────────────────────

	@Test
	public void doFilter_blockedIp_logsAtWarnLevel() throws Exception {
		Context.getAdministrationService().saveGlobalProperty(
		    new GlobalProperty(RestConstants.ALLOWED_IPS_GLOBAL_PROPERTY_NAME, TEST_IP));

		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setRemoteAddr(BLOCKED_IP);
		req.setRequestURI(TEST_URI);
		filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());

		Assert.assertTrue("Expected WARN log entry with ACCESS_BLOCKED", hasLog(Level.WARN, "ACCESS_BLOCKED"));
	}

	@Test
	public void doFilter_blockedIp_logsBlockedIpAddress() throws Exception {
		Context.getAdministrationService().saveGlobalProperty(
		    new GlobalProperty(RestConstants.ALLOWED_IPS_GLOBAL_PROPERTY_NAME, TEST_IP));

		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setRemoteAddr(BLOCKED_IP);
		req.setRequestURI(TEST_URI);
		filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());

		Assert.assertTrue("Blocked IP address must appear in the ACCESS_BLOCKED log entry",
		    appender.events.stream().anyMatch(
		        e -> e.getLevel() == Level.WARN && e.getMessage().getFormattedMessage().contains(BLOCKED_IP)));
	}
}
