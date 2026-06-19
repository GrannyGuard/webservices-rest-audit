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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

/**
 * CWE-1275 / NEN-7510 A.8.5: adds {@code SameSite=Strict} and {@code Secure} to every
 * {@code Set-Cookie} header emitted by the REST module for {@code /ws/rest/*} requests.
 *
 * <p>Servlet 3.1 (Tomcat 9) has no native SameSite support in the {@link Cookie} API.
 * The wrapper intercepts {@code addHeader}/{@code setHeader} calls for "Set-Cookie" and
 * the {@code addCookie} shorthand, rewriting them to include the missing attributes before
 * they reach the client. Container-managed session cookies (JSESSIONID) written directly
 * by Tomcat's internal response pipeline are not intercepted here; for those, the
 * production deployment should additionally configure
 * {@code <CookieProcessor sameSiteCookies="strict"/>} in {@code context.xml}.
 */
public class SameSiteCookieFilter implements Filter {

	private static final String SET_COOKIE = "Set-Cookie";

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// Stateless filter: no configuration or resources to initialise.
	}

	@Override
	public void destroy() {
		// Stateless filter: no resources to release.
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
	        throws IOException, ServletException {
		chain.doFilter(request, new SameSiteResponseWrapper((HttpServletResponse) response));
	}

	// Package-private so unit tests can exercise the wrapper directly.
	static class SameSiteResponseWrapper extends HttpServletResponseWrapper {

		SameSiteResponseWrapper(HttpServletResponse response) {
			super(response);
		}

		@Override
		public void addHeader(String name, String value) {
			super.addHeader(name, SET_COOKIE.equalsIgnoreCase(name) ? withSameSite(value) : value);
		}

		@Override
		public void setHeader(String name, String value) {
			super.setHeader(name, SET_COOKIE.equalsIgnoreCase(name) ? withSameSite(value) : value);
		}

		@Override
		public void addCookie(Cookie cookie) {
			// Cookie API in Servlet 3.1 has no SameSite attribute — emit a raw Set-Cookie
			// header instead so SameSite=Strict and Secure are included in the response.
			super.addHeader(SET_COOKIE, buildCookieHeader(cookie));
		}

		static String withSameSite(String setCookieValue) {
			if (setCookieValue == null) {
				return null;
			}
			String lower = setCookieValue.toLowerCase();
			StringBuilder sb = new StringBuilder(setCookieValue);
			if (!lower.contains("samesite")) {
				sb.append("; SameSite=Strict");
			}
			if (!lower.contains("; secure")) {
				sb.append("; Secure");
			}
			return sb.toString();
		}

		static String buildCookieHeader(Cookie cookie) {
			StringBuilder sb = new StringBuilder(cookie.getName())
			        .append('=').append(cookie.getValue() != null ? cookie.getValue() : "");
			if (cookie.getPath() != null) {
				sb.append("; Path=").append(cookie.getPath());
			}
			if (cookie.getDomain() != null) {
				sb.append("; Domain=").append(cookie.getDomain());
			}
			if (cookie.getMaxAge() >= 0) {
				sb.append("; Max-Age=").append(cookie.getMaxAge());
			}
			sb.append("; HttpOnly");
			sb.append("; SameSite=Strict");
			sb.append("; Secure");
			return sb.toString();
		}
	}
}
