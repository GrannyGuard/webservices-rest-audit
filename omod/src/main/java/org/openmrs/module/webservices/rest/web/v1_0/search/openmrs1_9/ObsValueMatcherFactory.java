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

import org.openmrs.ConceptDatatype;

/**
 * Resolves the {@link ObsValueMatcher} for a given {@link ConceptDatatype}. Returns {@code null}
 * for datatypes that {@link EncounterSearchHandler1_9} does not (yet) support for value-based
 * filtering (e.g. date, boolean) - callers treat this as "no obs match".
 */
public final class ObsValueMatcherFactory {
	
	private ObsValueMatcherFactory() {
	}
	
	public static ObsValueMatcher forDatatype(ConceptDatatype datatype) {
		if (datatype.isNumeric()) {
			return new NumericObsValueMatcher();
		}
		if (datatype.isText()) {
			return new TextObsValueMatcher();
		}
		if (datatype.isCoded()) {
			return new CodedObsValueMatcher();
		}
		return null;
	}
}
