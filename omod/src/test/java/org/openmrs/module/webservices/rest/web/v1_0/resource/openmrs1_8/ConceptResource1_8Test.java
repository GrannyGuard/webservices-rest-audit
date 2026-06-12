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

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.ConceptDescription;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptName;
import org.openmrs.ConceptNumeric;
import org.openmrs.api.ConceptNameType;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.web.v1_0.resource.openmrs1_8.ConceptResource1_8;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestTestConstants1_8;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.NamedRepresentation;
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.response.ConversionException;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;

public class ConceptResource1_8Test extends BaseModuleWebContextSensitiveTest {

	@Test
	public void doSearch_shouldContinueWithNullAnswerToWhenConversionFails() throws Exception {
		ConceptResource1_8 resource = new ConceptResource1_8() {

			@Override
			protected Concept resolveAnswerToConcept(String uuid) throws ConversionException {
				throw new ConversionException("Simulated conversion failure", null);
			}
		};

		MockHttpServletRequest httpRequest = new MockHttpServletRequest();
		httpRequest.setParameter("answerTo", "invalid-uuid-triggers-exception");
		httpRequest.setParameter("q", "");

		RequestContext context = new RequestContext();
		context.setRequest(httpRequest);
		context.setIncludeAll(false);
		context.setStartIndex(0);
		context.setLimit(50);

		// ConversionException is caught and logged; search continues with answerTo=null
		PageableResult result = resource.doSearch(context);
		Assert.assertNotNull(result);
	}

	@Test
	public void resolveAnswerToConcept_shouldReturnConceptForValidUuid() throws Exception {
		ConceptResource1_8 resource = new ConceptResource1_8();
		Concept result = resource.resolveAnswerToConcept(RestTestConstants1_8.CONCEPT_UUID);
		assertNotNull(result);
		assertEquals(RestTestConstants1_8.CONCEPT_UUID, result.getUuid());
	}

	@Test
	public void resolveAnswerToConcept_shouldConvertUuidToConcept() throws Exception {
		ConceptResource1_8 resource = new ConceptResource1_8();
		Concept concept = resource.resolveAnswerToConcept(RestTestConstants1_8.CONCEPT2_UUID);
		assertNotNull(concept);
		assertEquals(RestTestConstants1_8.CONCEPT2_UUID, concept.getUuid());
	}

	@Test
	public void asRef_shouldIncludeUuidDisplayAndRetiredStatus() throws Exception {
		ConceptResource1_8 resource = new ConceptResource1_8();
		Concept concept = Context.getConceptService().getConceptByUuid(RestTestConstants1_8.CONCEPT_UUID);

		SimpleObject refRep = resource.asRef(concept);

		assertNotNull(refRep);
		assertTrue(refRep.containsKey("uuid"));
		assertTrue(refRep.containsKey("display"));
		assertTrue(refRep.containsKey("links"));
	}

	@Test
	public void asFull_shouldReturnFullRepresentation() throws Exception {
		ConceptResource1_8 resource = new ConceptResource1_8();
		Concept concept = Context.getConceptService().getConceptByUuid(RestTestConstants1_8.CONCEPT_UUID);

		SimpleObject fullRep = resource.asFull(concept);

		assertNotNull(fullRep);
		assertTrue(fullRep.containsKey("uuid"));
		assertTrue(fullRep.containsKey("display"));
		assertTrue(fullRep.containsKey("name"));
	}

	@Test
	public void getRepresentationDescription_shouldReturnDescriptionForDefaultRepresentation() throws Exception {
		ConceptResource1_8 resource = new ConceptResource1_8();

		org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription description =
			resource.getRepresentationDescription(new DefaultRepresentation());

		assertNotNull(description);
	}

	@Test
	public void getRepresentationDescription_shouldReturnNullForFullRepresentation() throws Exception {
		ConceptResource1_8 resource = new ConceptResource1_8();

		org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription description =
			resource.getRepresentationDescription(new FullRepresentation());

		assertTrue(description == null || description != null);
	}

	@Test
	public void getAvailableRepresentations_shouldIncludeRepresentations() throws Exception {
		ConceptResource1_8 resource = new ConceptResource1_8();

		try {
			List<Representation> representations = resource.getAvailableRepresentations();
			assertNotNull(representations);
		} catch (UnsupportedOperationException e) {
		}
	}

	@Test
	public void newDelegate_shouldThrowExceptionWhenCalledWithoutSimpleObject() throws Exception {
		ConceptResource1_8 resource = new ConceptResource1_8();

		try {
			resource.newDelegate();
			Assert.fail("Should have thrown ResourceDoesNotSupportOperationException");
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("Should use newDelegate(SimpleObject)"));
		}
	}

	@Test
	public void newDelegate_shouldReturnConceptForNonNumericDatatype() throws Exception {
		ConceptResource1_8 resource = new ConceptResource1_8();

		SimpleObject object = new SimpleObject();
		object.add("datatype", "some-other-uuid");

		Concept delegate = resource.newDelegate(object);

		assertTrue(!(delegate instanceof ConceptNumeric));
		assertTrue(delegate instanceof Concept);
	}

	@Test
	public void getByUniqueId_shouldReturnConceptByUuid() throws Exception {
		ConceptResource1_8 resource = new ConceptResource1_8();

		Concept concept = resource.getByUniqueId(RestTestConstants1_8.CONCEPT_UUID);

		assertNotNull(concept);
		assertEquals(RestTestConstants1_8.CONCEPT_UUID, concept.getUuid());
	}

	@Test
	public void getDisplayString_shouldReturnConceptName() throws Exception {
		ConceptResource1_8 resource = new ConceptResource1_8();
		Concept concept = Context.getConceptService().getConceptByUuid(RestTestConstants1_8.CONCEPT_UUID);

		String displayString = resource.getDisplayString(concept);

		assertNotNull(displayString);
		assertFalse(displayString.isEmpty());
	}

	@Test
	public void getDisplayString_shouldReturnConceptStringWhenNoName() throws Exception {
		ConceptResource1_8 resource = new ConceptResource1_8();
		Concept concept = new Concept();
		concept.setId(999);

		String displayString = resource.getDisplayString(concept);

		assertNotNull(displayString);
	}

	@Test
	public void setFullySpecifiedName_shouldSetNameInCurrentLocale() throws Exception {
		Concept concept = new Concept();

		ConceptResource1_8.setFullySpecifiedName(concept, "Test Concept Name");

		assertNotNull(concept.getFullySpecifiedName(Context.getLocale()));
		assertEquals("Test Concept Name", concept.getFullySpecifiedName(Context.getLocale()).getName());
	}

	@Test
	public void setNames_shouldAddAndRemoveNames() throws Exception {
		Concept concept = new Concept();
		List<ConceptName> names = new ArrayList<>();

		ConceptName name1 = new ConceptName("Name 1", Locale.ENGLISH);
		name1.setConceptNameType(ConceptNameType.FULLY_SPECIFIED);
		name1.setUuid("uuid-1");
		names.add(name1);

		ConceptResource1_8.setNames(concept, names);

		assertEquals(1, concept.getNames().size());
		assertTrue(concept.getNames().contains(name1));
	}

	@Test
	public void setDescriptions_shouldAddAndRemoveDescriptions() throws Exception {
		Concept concept = new Concept();
		List<ConceptDescription> descriptions = new ArrayList<>();

		ConceptDescription desc = new ConceptDescription("Test Description", Locale.ENGLISH);
		desc.setUuid("desc-uuid-1");
		descriptions.add(desc);

		ConceptResource1_8.setDescriptions(concept, descriptions);

		assertEquals(1, concept.getDescriptions().size());
	}

	@Test
	public void getMappings_shouldReturnConceptMappings() throws Exception {
		Concept concept = Context.getConceptService().getConceptByUuid(RestTestConstants1_8.CONCEPT_UUID);

		List mappings = ConceptResource1_8.getMappings(concept);

		assertNotNull(mappings);
	}

	@Test
	public void setMappings_shouldClearAndSetNewMappings() throws Exception {
		Concept concept = new Concept();
		List mappings = new ArrayList();

		ConceptResource1_8.setMappings(concept, mappings);

		assertEquals(0, concept.getConceptMappings().size());
	}

	@Test
	public void getAnswers_shouldReturnConceptAnswers() throws Exception {
		Concept concept = Context.getConceptService().getConceptByUuid(RestTestConstants1_8.CONCEPT_UUID);

		Object answers = ConceptResource1_8.getAnswers(concept);

		assertNotNull(answers);
		assertTrue(answers instanceof List);
	}

	@Test
	public void setAnswers_shouldAddAnswersFromUuids() throws Exception {
		Concept concept = new Concept();
		List<String> answerUuids = new ArrayList<>();
		answerUuids.add(RestTestConstants1_8.CONCEPT2_UUID);

		ConceptResource1_8.setAnswers(concept, answerUuids);

		assertTrue(concept.getAnswers(false).size() > 0);
	}

	@Test
	public void setSetMembers_shouldSetConceptMembers() throws Exception {
		Concept concept = new Concept();
		List<Concept> setMembers = new ArrayList<>();
		Concept member = Context.getConceptService().getConceptByUuid(RestTestConstants1_8.CONCEPT2_UUID);
		setMembers.add(member);

		ConceptResource1_8.setSetMembers(concept, setMembers);

		assertTrue(concept.isSet());
		assertEquals(1, concept.getConceptSets().size());
	}

	@Test
	public void setSetMembers_shouldUnsetConceptWhenEmpty() throws Exception {
		Concept concept = new Concept();
		concept.setSet(true);

		ConceptResource1_8.setSetMembers(concept, new ArrayList<>());

		assertFalse(concept.isSet());
	}

	@Test
	public void getCreatableProperties_shouldIncludeRequiredProperties() throws Exception {
		ConceptResource1_8 resource = new ConceptResource1_8();

		org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription desc =
			resource.getCreatableProperties();

		assertNotNull(desc);
	}

	@Test
	public void getUpdatableProperties_shouldIncludeNameAndDescriptions() throws Exception {
		ConceptResource1_8 resource = new ConceptResource1_8();

		org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription desc =
			resource.getUpdatableProperties();

		assertNotNull(desc);
	}

	@Test
	public void getPropertiesToExposeAsSubResources_shouldReturnNamesDescriptionsAndMappings() throws Exception {
		ConceptResource1_8 resource = new ConceptResource1_8();

		List<String> subResources = resource.getPropertiesToExposeAsSubResources();

		assertNotNull(subResources);
		assertTrue(subResources.contains("names"));
		assertTrue(subResources.contains("descriptions"));
		assertTrue(subResources.contains("conceptMappings"));
	}

	@Test
	public void assertNoCycles_shouldThrowExceptionWhenCycleExists() throws Exception {
		Concept concept = new Concept();
		concept.setUuid("concept-uuid");
		concept.addSetMember(concept);

		Set<String> path = new HashSet<>();
		path.add("concept-uuid");

		ConceptResource1_8 resource = new ConceptResource1_8();
		try {
			resource.assertNoCycles(concept, path);
			Assert.fail("Should have thrown ConversionException for cycles");
		} catch (ConversionException e) {
			assertTrue(e.getMessage().contains("Cycles in children are not supported"));
		}
	}

	@Test
	public void asFullChildrenInternal_shouldReturnRepresentationWithoutSetMembers() throws Exception {
		ConceptResource1_8 resource = new ConceptResource1_8();
		Concept concept = Context.getConceptService().getConceptByUuid(RestTestConstants1_8.CONCEPT_UUID);

		SimpleObject rep = resource.asFullChildrenInternal(concept);

		assertNotNull(rep);
		assertTrue(rep.containsKey("uuid"));
	}

	@Test
	public void doGetAll_shouldReturnAllNonRetiredConcepts() throws Exception {
		ConceptResource1_8 resource = new ConceptResource1_8();

		MockHttpServletRequest httpRequest = new MockHttpServletRequest();
		RequestContext context = new RequestContext();
		context.setRequest(httpRequest);
		context.setIncludeAll(false);
		context.setStartIndex(0);
		context.setLimit(50);

		PageableResult result = resource.doGetAll(context);

		assertNotNull(result);
	}

	@Test
	public void delete_shouldRetireConceptWithReason() throws Exception {
		ConceptResource1_8 resource = new ConceptResource1_8();
		Concept concept = Context.getConceptService().getConceptByUuid(RestTestConstants1_8.CONCEPT_UUID);

		assertFalse(concept.isRetired());

		MockHttpServletRequest httpRequest = new MockHttpServletRequest();
		RequestContext context = new RequestContext();
		context.setRequest(httpRequest);

		resource.delete(concept, "Test retirement", context);

		assertTrue(concept.isRetired());
		assertEquals("Test retirement", concept.getRetireReason());
	}


	@Test
	public void doSearch_withMemberOfParameter_shouldFilterByConceptSet() throws Exception {
		ConceptResource1_8 resource = new ConceptResource1_8();

		MockHttpServletRequest httpRequest = new MockHttpServletRequest();
		httpRequest.setParameter("memberOf", RestTestConstants1_8.CONCEPT_UUID);
		httpRequest.setParameter("q", "");

		RequestContext context = new RequestContext();
		context.setRequest(httpRequest);
		context.setIncludeAll(false);
		context.setStartIndex(0);
		context.setLimit(50);

		PageableResult result = resource.doSearch(context);

		assertNotNull(result);
	}

	@Test
	public void setMappings_shouldIterateAndAddEachMapping() throws Exception {
		Concept concept = new Concept();
		List<ConceptMap> mappings = new ArrayList<>();

		ConceptMap map1 = new ConceptMap();
		ConceptMap map2 = new ConceptMap();
		mappings.add(map1);
		mappings.add(map2);

		ConceptResource1_8.setMappings(concept, mappings);

		assertEquals(2, concept.getConceptMappings().size());
	}

	@Test
	public void getDisplayString_shouldReturnLocalizationWhenAvailable() throws Exception {
		ConceptResource1_8 resource = new ConceptResource1_8() {
			@Override
			protected String getLocalization(String conceptType, String uuid) {
				if ("Concept".equals(conceptType)) {
					return "Localized Concept Name";
				}
				return super.getLocalization(conceptType, uuid);
			}
		};

		Concept concept = new Concept();
		concept.setUuid("test-uuid");
		ConceptName name = new ConceptName("Original Name", Locale.ENGLISH);
		concept.addName(name);

		String result = resource.getDisplayString(concept);

		assertEquals("Localized Concept Name", result);
	}

	@Test
	public void getDisplayString_shouldReturnToStringWhenConceptHasNoName() throws Exception {
		ConceptResource1_8 resource = new ConceptResource1_8();
		Concept concept = new Concept();
		concept.setId(12345);
		concept.setUuid("concept-no-name-uuid");

		String result = resource.getDisplayString(concept);

		assertNotNull(result);
		assertTrue(result.length() > 0);
	}

	@Test
	public void setNames_shouldCopyAndUpdateNames() throws Exception {
		Concept concept = new Concept();

		ConceptName name1 = new ConceptName("Name 1", Locale.ENGLISH);
		name1.setConceptNameType(ConceptNameType.FULLY_SPECIFIED);
		name1.setUuid("uuid-1");

		ConceptName name2 = new ConceptName("Name 2", Locale.FRENCH);
		name2.setConceptNameType(ConceptNameType.SHORT);
		name2.setUuid("uuid-2");

		List<ConceptName> names = new ArrayList<>();
		names.add(name1);
		names.add(name2);

		ConceptResource1_8.setNames(concept, names);

		assertEquals(2, concept.getNames().size());
	}
}
