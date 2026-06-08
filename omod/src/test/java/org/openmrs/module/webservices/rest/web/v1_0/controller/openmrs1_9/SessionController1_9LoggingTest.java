/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.webservices.rest.web.v1_0.controller.openmrs1_9;

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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Verifies NEN-7510:2024 A.8.15 logging compliance for {@link SessionController1_9}.
 * Tests cover: logout logging (level, username, IP) and /session/diag access logging,
 * including absence of sensitive data (roles, privileges) in log output.
 */
public class SessionController1_9LoggingTest extends BaseModuleWebContextSensitiveTest {

	private static final String TEST_IP = "10.0.0.42";

	private SessionController1_9 controller;

	private MockHttpServletRequest request;

	private TestAppender appender;

	private Logger controllerLogger;

	private static class TestAppender extends AbstractAppender {

		final List<LogEvent> events = new ArrayList<>();

		TestAppender() {
			super("session-logging-test-appender", null, null, true, Property.EMPTY_ARRAY);
		}

		@Override
		public void append(LogEvent event) {
			events.add(event.toImmutable());
		}
	}

	@Before
	public void setUpLogging() {
		controller = Context.getRegisteredComponents(SessionController1_9.class).iterator().next();

		request = new MockHttpServletRequest();
		request.setRemoteAddr(TEST_IP);
		request.setSession(new MockHttpSession(new MockServletContext(), "test-session-id"));

		controllerLogger = (Logger) LogManager.getLogger(SessionController1_9.class);
		appender = new TestAppender();
		appender.start();
		controllerLogger.addAppender(appender);
		controllerLogger.setLevel(Level.INFO);
	}

	@After
	public void tearDownLogging() {
		controllerLogger.removeAppender(appender);
		appender.stop();
	}

	private boolean hasLog(Level level, String text) {
		return appender.events.stream()
		        .anyMatch(e -> e.getLevel() == level && e.getMessage().getFormattedMessage().contains(text));
	}

	// ── logout ────────────────────────────────────────────────────────────────

	@Test
	public void delete_logsAtInfoLevel() {
		Assert.assertTrue(Context.isAuthenticated());
		controller.delete(request);

		Assert.assertTrue("Expected INFO log entry with AUTH_LOGOUT", hasLog(Level.INFO, "AUTH_LOGOUT"));
	}

	@Test
	public void delete_logsUsernameBeforeLogout() {
		Assert.assertTrue(Context.isAuthenticated());
		String expectedUsername = Context.getAuthenticatedUser().getUsername();
		controller.delete(request);

		Assert.assertTrue("Username must appear in AUTH_LOGOUT log entry",
		    appender.events.stream().anyMatch(
		        e -> e.getLevel() == Level.INFO && e.getMessage().getFormattedMessage().contains(expectedUsername)));
	}

	@Test
	public void delete_logsRemoteIp() {
		Assert.assertTrue(Context.isAuthenticated());
		controller.delete(request);

		Assert.assertTrue("Remote IP must appear in AUTH_LOGOUT log entry", hasLog(Level.INFO, TEST_IP));
	}

	// ── /session/diag ─────────────────────────────────────────────────────────

	@Test
	public void getDiagnostics_logsAtWarnLevel() {
		controller.getDiagnostics(request, null);

		Assert.assertTrue("Expected WARN log entry with DIAG_ACCESS", hasLog(Level.WARN, "DIAG_ACCESS"));
	}

	@Test
	public void getDiagnostics_logsRemoteIp() {
		controller.getDiagnostics(request, null);

		Assert.assertTrue("Remote IP must appear in DIAG_ACCESS log entry", hasLog(Level.WARN, TEST_IP));
	}

	@Test
	public void getDiagnostics_doesNotLogRolesOrPrivileges() {
		Assert.assertTrue(Context.isAuthenticated());
		controller.getDiagnostics(request, null);

		boolean sensitiveDataInLog = appender.events.stream().anyMatch(e -> {
			String msg = e.getMessage().getFormattedMessage();
			return msg.contains("Roles") || msg.contains("Privileges")
			        || msg.contains("ROLE_") || msg.contains("privilege");
		});
		Assert.assertFalse("Roles and privileges must not appear in any log entry", sensitiveDataInLog);
	}
}
