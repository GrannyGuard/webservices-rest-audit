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
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * CWE-693 / NEN-7510 A.8.23: adds mandatory HTTP security headers to every response
 * emitted by the REST module ({@code /ws/rest/*}).
 *
 * <p>Headers applied:
 * <ul>
 *   <li>{@code X-Content-Type-Options: nosniff} — prevents MIME-type sniffing</li>
 *   <li>{@code X-Frame-Options: DENY} — prevents clickjacking</li>
 *   <li>{@code Content-Security-Policy: default-src 'none'} — REST API serves no
 *       HTML/JS/CSS; an empty allow-list is appropriate and stops reflected-content attacks</li>
 *   <li>{@code Strict-Transport-Security: max-age=31536000; includeSubDomains} — forces
 *       TLS for one year; safe to set here because production must run behind TLS</li>
 *   <li>{@code Referrer-Policy: no-referrer} — prevents URL leakage to third parties</li>
 *   <li>{@code Cache-Control: no-store} — patient data must not be cached by proxies
 *       or browsers (NEN-7510 A.8.10 — information deletion)</li>
 * </ul>
 */
public class SecurityHeadersFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
	        throws IOException, ServletException {
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		httpResponse.setHeader("X-Content-Type-Options", "nosniff");
		httpResponse.setHeader("X-Frame-Options", "DENY");
		httpResponse.setHeader("Content-Security-Policy", "default-src 'none'");
		httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
		httpResponse.setHeader("Referrer-Policy", "no-referrer");
		httpResponse.setHeader("Cache-Control", "no-store");
		chain.doFilter(request, response);
	}
}
