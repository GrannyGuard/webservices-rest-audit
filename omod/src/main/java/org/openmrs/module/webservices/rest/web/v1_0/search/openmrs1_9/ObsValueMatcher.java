/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.webservices.rest.web.v1_0.search.openmrs1_9;

import org.openmrs.Obs;

/**
 * Strategy used by {@link EncounterSearchHandler1_9} to decide whether an {@link Obs} matches one
 * of the requested {@code obsValues}. Implementations are looked up per concept datatype via
 * {@link ObsValueMatcherFactory}.
 */
public interface ObsValueMatcher {
	
	boolean matches(Obs obs, String[] values);
}
