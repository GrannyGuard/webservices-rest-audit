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

import java.io.IOException;
import java.nio.charset.Charset;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.RestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter intended for all /ws/rest calls that allows the user to authenticate via Basic
 * authentication. (It will not fail on invalid or missing credentials. We count on the API to throw
 * exceptions if an unauthenticated user tries to do something they are not allowed to do.) <br/>
 * <br/>
 * IP address authorization is also performed based on the global property:
 * {@link RestConstants#ALLOWED_IPS_GLOBAL_PROPERTY_NAME}
 */
public class AuthorizationFilter implements Filter {
	
	private static final Logger log = LoggerFactory.getLogger(AuthorizationFilter.class);
	
	/**
	 * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
	 */
	@Override
	public void init(FilterConfig arg0) throws ServletException {
		log.debug("Initializing REST WS Authorization filter");
	}
	
	/**
	 * @see javax.servlet.Filter#destroy()
	 */
	@Override
	public void destroy() {
		log.debug("Destroying REST WS Authorization filter");
	}
	
	/**
	 * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse,
	 *      javax.servlet.FilterChain)
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
	        throws IOException, ServletException {
		
		// check the IP address first.  If its not valid, return a 403
		if (!RestUtil.isIpAllowed(request.getRemoteAddr())) {
			String uri = request instanceof HttpServletRequest ? ((HttpServletRequest) request).getRequestURI() : "-";
			// GrannyGuard patch — sanitizeForLog neutralises CWE-117 (the request URI is user-controlled)
			log.warn("ACCESS_BLOCKED ip=[{}] uri=[{}]", request.getRemoteAddr(), RestUtil.sanitizeForLog(uri));
			HttpServletResponse httpresponse = (HttpServletResponse) response;
			httpresponse.sendError(HttpServletResponse.SC_FORBIDDEN,
			    "IP address '" + request.getRemoteAddr() + "' is not authorized");
			return;
		}
		
		// skip if the session has timed out, we're already authenticated, or it's not an HTTP request
		if (request instanceof HttpServletRequest) {
			HttpServletRequest httpRequest = (HttpServletRequest) request;
			// GrannyGuard patch — derive "session timed out" from server-side state instead of the
			// client-controlled getRequestedSessionId()/isRequestedSessionIdValid() pair (Sonar
			// security hotspot: forgeable session id, CWE-384). The id value is never trusted; we
			// only act on the absence of a live container session for a request that carried one.
			if (hasStaleSessionCookie(httpRequest)) {
				// GrannyGuard patch — sanitizeForLog neutralises CWE-117 (the request URI is user-controlled)
				log.warn("SESSION_TIMEOUT ip=[{}] uri=[{}]", httpRequest.getRemoteAddr(),
				    RestUtil.sanitizeForLog(httpRequest.getRequestURI()));
				HttpServletResponse httpResponse = (HttpServletResponse) response;
				httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session timed out");
				// CWE-302 (defense-in-depth, found in code review) — stop the chain after a
				// timed-out session instead of falling through to chain.doFilter() with a 401
				// already committed. Distinct from CodeQL #908 (the optional Basic-Auth branch
				// below); see docs/security/06-mitigatie-en-validatie/06.md section 3b.
				return;
			}
			
			if (!Context.isAuthenticated()) {
				String basicAuth = httpRequest.getHeader("Authorization");
				if (basicAuth != null) {
					// check that header is in format "Basic ${base64encode(username + ":" + password)}"
					if (basicAuth.startsWith("Basic")) {
						String attemptedUser = null;
						try {
							// remove the leading "Basic "
							basicAuth = basicAuth.substring(6);
							if (StringUtils.isBlank(basicAuth)) {
								HttpServletResponse httpResponse = (HttpServletResponse) response;
								httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid credentials provided");
								return;
							}

							String decoded = new String(Base64.decodeBase64(basicAuth), Charset.forName("UTF-8"));
							if (StringUtils.isBlank(decoded) || !decoded.contains(":")) {
								HttpServletResponse httpResponse = (HttpServletResponse) response;
								httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid credentials provided");
								return;
							}

							String[] userAndPass = decoded.split(":");
							attemptedUser = userAndPass[0];
							Context.authenticate(userAndPass[0], userAndPass[1]);
							// GrannyGuard patch — sanitizeForLog neutralises CWE-117 (username + URI are user-controlled)
							log.info("AUTH_SUCCESS user=[{}] ip=[{}] uri=[{}]",
							    RestUtil.sanitizeForLog(attemptedUser), httpRequest.getRemoteAddr(),
							    RestUtil.sanitizeForLog(httpRequest.getRequestURI()));
						}
						catch (Exception ex) {
							// This filter never stops execution. If the user failed to
							// authenticate, that will be caught later.
							// GrannyGuard patch — sanitizeForLog neutralises CWE-117 (username, URI, exception msg are user-controlled)
							log.warn("AUTH_FAILURE user=[{}] ip=[{}] uri=[{}] reason=[{}]",
							    RestUtil.sanitizeForLog(attemptedUser), httpRequest.getRemoteAddr(),
							    RestUtil.sanitizeForLog(httpRequest.getRequestURI()), RestUtil.sanitizeForLog(ex.getMessage()));
						}
					}
				}
			}
		}
		
		// continue with the filter chain (unless IP is not allowed)
		chain.doFilter(request, response);
	}

	/**
	 * Detects a timed-out (or invalidated) session without trusting the client-supplied session id.
	 * A request references a session only if it carries a {@code JSESSIONID} cookie; that session is
	 * considered stale when the container no longer has a live session for the request
	 * ({@link HttpServletRequest#getSession(boolean) getSession(false)} returns {@code null}). The
	 * cookie value itself is never used for any authentication or session lookup, so this cannot be
	 * abused for session fixation (CWE-384).
	 *
	 * @param httpRequest the incoming request
	 * @return {@code true} if the request presents a session cookie but has no valid server session
	 */
	private boolean hasStaleSessionCookie(HttpServletRequest httpRequest) {
		if (httpRequest.getSession(false) != null) {
			// a live server session exists — nothing has timed out
			return false;
		}
		Cookie[] cookies = httpRequest.getCookies();
		if (cookies == null) {
			return false;
		}
		for (Cookie cookie : cookies) {
			if ("JSESSIONID".equalsIgnoreCase(cookie.getName())) {
				return true;
			}
		}
		return false;
	}
}
