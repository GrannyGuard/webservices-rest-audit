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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Tests for {@link SameSiteCookieFilter} — CWE-1275 / NEN-7510 A.8.5.
 *
 * <p>Note: {@link MockHttpServletResponse#addHeader(String, String)} internally parses
 * Set-Cookie headers and calls {@code javax.servlet.http.Cookie.isHttpOnly()}, which does
 * not exist in the Servlet 2.5 API jar used by this project. Tests that would trigger this
 * path use {@link HeaderCapture} instead, which stores header values as plain strings.
 */
public class SameSiteCookieFilterTest {

	/**
	 * Minimal response wrapper that stores Set-Cookie header values as plain strings without
	 * delegating to MockHttpServletResponse's cookie-parsing logic.
	 */
	private static class HeaderCapture extends HttpServletResponseWrapper {

		String setCookieValue;

		String customHeaderValue;

		HeaderCapture() {
			super(new MockHttpServletResponse());
		}

		@Override
		public void addHeader(String name, String value) {
			if ("Set-Cookie".equalsIgnoreCase(name)) {
				setCookieValue = value;
			} else {
				customHeaderValue = value;
			}
		}

		@Override
		public void setHeader(String name, String value) {
			addHeader(name, value);
		}
	}

	private SameSiteCookieFilter filter;

	private MockHttpServletRequest req;

	@Before
	public void setUp() {
		filter = new SameSiteCookieFilter();
		req = new MockHttpServletRequest();
	}

	// ── withSameSite static helper ───────────────────────────────────────────

	@Test
	public void withSameSite_shouldAppendSameSiteStrictAndSecure() {
		String result = SameSiteCookieFilter.SameSiteResponseWrapper
		        .withSameSite("JSESSIONID=abc; Path=/openmrs");
		Assert.assertTrue("SameSite=Strict must be appended", result.contains("SameSite=Strict"));
		Assert.assertTrue("Secure must be appended", result.contains("Secure"));
	}

	@Test
	public void withSameSite_shouldNotDuplicateSameSiteWhenAlreadyPresent() {
		String result = SameSiteCookieFilter.SameSiteResponseWrapper
		        .withSameSite("foo=bar; Path=/; SameSite=Lax");
		String lower = result.toLowerCase();
		int first = lower.indexOf("samesite");
		Assert.assertTrue("SameSite must be present", first >= 0);
		Assert.assertEquals("SameSite must appear exactly once", -1, lower.indexOf("samesite", first + 1));
	}

	@Test
	public void withSameSite_shouldNotDuplicateSecureWhenAlreadyPresent() {
		String result = SameSiteCookieFilter.SameSiteResponseWrapper
		        .withSameSite("foo=bar; Secure; SameSite=Strict");
		String lower = result.toLowerCase();
		int first = lower.indexOf("secure");
		Assert.assertTrue("Secure must be present", first >= 0);
		Assert.assertEquals("Secure must appear exactly once", -1, lower.indexOf("secure", first + 6));
	}

	// ── addHeader/setHeader via wrapper ──────────────────────────────────────

	@Test
	public void addHeader_setCookie_shouldAddSameSiteAndSecure() {
		HeaderCapture capture = new HeaderCapture();
		SameSiteCookieFilter.SameSiteResponseWrapper wrapper =
		        new SameSiteCookieFilter.SameSiteResponseWrapper(capture);
		wrapper.addHeader("Set-Cookie", "JSESSIONID=abc123; Path=/openmrs");
		Assert.assertNotNull(capture.setCookieValue);
		Assert.assertTrue("SameSite=Strict must be present", capture.setCookieValue.contains("SameSite=Strict"));
		Assert.assertTrue("Secure must be present", capture.setCookieValue.contains("Secure"));
	}

	@Test
	public void setHeader_setCookie_shouldAddSameSiteAndSecure() {
		HeaderCapture capture = new HeaderCapture();
		SameSiteCookieFilter.SameSiteResponseWrapper wrapper =
		        new SameSiteCookieFilter.SameSiteResponseWrapper(capture);
		wrapper.setHeader("Set-Cookie", "token=xyz; Path=/");
		Assert.assertNotNull(capture.setCookieValue);
		Assert.assertTrue("SameSite=Strict must be present", capture.setCookieValue.contains("SameSite=Strict"));
		Assert.assertTrue("Secure must be present", capture.setCookieValue.contains("Secure"));
	}

	@Test
	public void addHeader_nonCookieHeader_shouldNotBeModified() {
		HeaderCapture capture = new HeaderCapture();
		SameSiteCookieFilter.SameSiteResponseWrapper wrapper =
		        new SameSiteCookieFilter.SameSiteResponseWrapper(capture);
		wrapper.addHeader("X-Custom-Header", "original-value");
		Assert.assertEquals("original-value", capture.customHeaderValue);
		Assert.assertNull("Set-Cookie must not be set", capture.setCookieValue);
	}

	// ── buildCookieHeader static helper ──────────────────────────────────────

	@Test
	public void buildCookieHeader_shouldIncludeSameSiteStrictAndSecureAndHttpOnly() {
		Cookie cookie = new Cookie("session", "xyz789");
		cookie.setPath("/openmrs");
		String header = SameSiteCookieFilter.SameSiteResponseWrapper.buildCookieHeader(cookie);
		Assert.assertTrue("Cookie name=value must be present", header.startsWith("session=xyz789"));
		Assert.assertTrue("Path must be present", header.contains("Path=/openmrs"));
		Assert.assertTrue("SameSite=Strict must be present", header.contains("SameSite=Strict"));
		Assert.assertTrue("Secure must be present", header.contains("Secure"));
		Assert.assertTrue("HttpOnly must be present", header.contains("HttpOnly"));
	}

	@Test
	public void buildCookieHeader_withDomainAndMaxAge_shouldIncludeAllAttributes() {
		Cookie cookie = new Cookie("token", "abc");
		cookie.setPath("/");
		cookie.setDomain("example.com");
		cookie.setMaxAge(3600);
		String header = SameSiteCookieFilter.SameSiteResponseWrapper.buildCookieHeader(cookie);
		Assert.assertTrue(header.contains("Domain=example.com"));
		Assert.assertTrue(header.contains("Max-Age=3600"));
		Assert.assertTrue(header.contains("SameSite=Strict"));
	}

	// ── addCookie via wrapper ─────────────────────────────────────────────────

	@Test
	public void addCookie_shouldEmitRawSetCookieHeaderWithSameSiteAndSecure() {
		HeaderCapture capture = new HeaderCapture();
		SameSiteCookieFilter.SameSiteResponseWrapper wrapper =
		        new SameSiteCookieFilter.SameSiteResponseWrapper(capture);
		Cookie cookie = new Cookie("session", "xyz789");
		cookie.setPath("/openmrs");
		wrapper.addCookie(cookie);
		Assert.assertNotNull(capture.setCookieValue);
		Assert.assertTrue("SameSite=Strict must be present", capture.setCookieValue.contains("SameSite=Strict"));
		Assert.assertTrue("Secure must be present", capture.setCookieValue.contains("Secure"));
	}

	// ── doFilter integration ─────────────────────────────────────────────────

	@Test
	public void doFilter_shouldPassRequestThroughChain() throws Exception {
		MockFilterChain chain = new MockFilterChain();
		MockHttpServletResponse resp = new MockHttpServletResponse();
		filter.doFilter(req, resp, chain);
		Assert.assertNotNull("Request must reach the chain", chain.getRequest());
	}
}
