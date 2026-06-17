/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.webservices.rest.web.v1_0.resource.openmrs1_8;

import org.openmrs.api.impl.AdministrationServiceImpl;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.resource.api.Listable;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.util.PrivilegeConstants;

import java.util.LinkedHashMap;
import java.util.Map;

@Resource(name = RestConstants.VERSION_1 + "/systeminformation", supportedClass = AdministrationServiceImpl.class, supportedOpenmrsVersions = {
       "1.8.* - 9.*" })
public class SystemInformationResource1_8 implements Listable {

	// Keys removed from the database section to prevent credential/connection disclosure.
	private static final String DB_SECTION = "SystemInfo.title.dataBaseInformation";

	private static final String DB_CONNECTION_URL = "SystemInfo.Database.connectionURL";

	private static final String DB_USERNAME = "SystemInfo.Database.userName";

	@Override
	public SimpleObject getAll(RequestContext context) throws ResponseException {
		// CWE-200 / NEN-7510 A.8.3: only users with Get Global Properties privilege may
		// retrieve system information. Throws ContextAuthenticationException → HTTP 403
		// via BaseRestController.handleException() if the privilege is absent.
		Context.requirePrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);

		Map<String, Map<String, String>> sysInfo = Context.getAdministrationService().getSystemInformation();

		// Redact database credentials and connection URL from the response so that
		// even a privileged user cannot retrieve them via the REST API.
		Map<String, String> dbSection = sysInfo.get(DB_SECTION);
		if (dbSection != null) {
			Map<String, String> redacted = new LinkedHashMap<String, String>(dbSection);
			redacted.remove(DB_CONNECTION_URL);
			redacted.remove(DB_USERNAME);
			sysInfo.put(DB_SECTION, redacted);
		}

		SimpleObject rest = new SimpleObject();
		rest.put("systemInfo", sysInfo);
		return rest;
	}
	
	@Override
	public String getUri(Object instance) {
		return RestConstants.URI_PREFIX + "/systeminformation";
	}
}
