/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.webservices.rest.web.controller;

import org.openmrs.module.webservices.rest.web.RestUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller("webservices.rest.SwaggerDocController")
@RequestMapping("/module/webservices/rest/apiDocs")
public class SwaggerDocController {

	@RequestMapping(method = RequestMethod.GET)
	public void get(HttpServletResponse response) throws IOException {
		// NEN-7510 A.8.3 — the Swagger UI can be disabled in production to reduce the attack surface.
		if (!RestUtil.isSwaggerDocsEnabled()) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	@RequestMapping(value = "/debug", method = RequestMethod.GET)
	@ResponseBody
	public String debug(@RequestParam("tag") String tag, HttpServletResponse response) throws IOException {
		if (!RestUtil.isSwaggerDocsEnabled()) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return null;
		}
		return "<h1>Debugging Tag: " + tag + "</h1>";
	}

}
