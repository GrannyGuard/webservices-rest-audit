# OpenMRS REST Webservices — Endpoint Overzicht

Basis URL: `http://<host>:8080/openmrs/ws/rest/v1/`

Alle resources ondersteunen standaard HTTP methoden: `GET`, `POST`, `PUT/PATCH`, `DELETE`.
Sub-resources worden benaderd als `/ws/rest/v1/{resource}/{uuid}/{subresource}`.

---

## Sessie & Authenticatie

| Endpoint | Methoden | Opmerkingen |
|---|---|---|
| `/ws/rest/v1/session` | GET, POST, DELETE | Login, logout, sessie checken |

---

## Patiënt & Persoon

| Endpoint | Sub-resources |
|---|---|
| `/ws/rest/v1/patient` | `identifier`, `allergy`, `address` |
| `/ws/rest/v1/patientidentifiertype` | — |
| `/ws/rest/v1/patientdiagnoses` | — |
| `/ws/rest/v1/person` | `name`, `address`, `attribute` |
| `/ws/rest/v1/personattributetype` | — |
| `/ws/rest/v1/relationship` | — |
| `/ws/rest/v1/relationshiptype` | — |

---

## Klinisch

| Endpoint | Sub-resources |
|---|---|
| `/ws/rest/v1/encounter` | `encounterprovider` |
| `/ws/rest/v1/encountertype` | — |
| `/ws/rest/v1/encounterrole` | — |
| `/ws/rest/v1/obs` | `referencerange` |
| `/ws/rest/v1/condition` | — |
| `/ws/rest/v1/visit` | `attribute` |
| `/ws/rest/v1/visittype` | — |
| `/ws/rest/v1/visitattributetype` | — |

---

## Orders & Medicatie

| Endpoint | Sub-resources |
|---|---|
| `/ws/rest/v1/order` | `fulfillerdetails` |
| `/ws/rest/v1/ordertype` | — |
| `/ws/rest/v1/ordergroup` | — |
| `/ws/rest/v1/orderset` | `ordersetmember` |
| `/ws/rest/v1/orderfrequency` | — |
| `/ws/rest/v1/orderattributetype` | — |
| `/ws/rest/v1/orderable` | — |
| `/ws/rest/v1/orderentryconfig` | — |
| `/ws/rest/v1/caresetting` | — |
| `/ws/rest/v1/drug` | `ingredient` |

---

## Concepten

| Endpoint | Sub-resources |
|---|---|
| `/ws/rest/v1/concept` | `name`, `description`, `mapping`, `attribute` |
| `/ws/rest/v1/conceptclass` | — |
| `/ws/rest/v1/conceptdatatype` | — |
| `/ws/rest/v1/conceptsource` | — |
| `/ws/rest/v1/conceptmaptype` | — |
| `/ws/rest/v1/conceptreferenceterm` | — |
| `/ws/rest/v1/conceptreferencetermmap` | — |
| `/ws/rest/v1/conceptattributetype` | — |
| `/ws/rest/v1/conceptreferencerange` | — |
| `/ws/rest/v1/conceptstopword` | — |
| `/ws/rest/v1/conceptstateconversion` | — |
| `/ws/rest/v1/conceptproposal` | — |
| `/ws/rest/v1/conceptsearch` | — |
| `/ws/rest/v1/concepttree` | — |

---

## Gebruikers & Toegang

| Endpoint | Sub-resources |
|---|---|
| `/ws/rest/v1/user` | — |
| `/ws/rest/v1/role` | — |
| `/ws/rest/v1/privilege` | — |

---

## Locatie & Provider

| Endpoint | Sub-resources |
|---|---|
| `/ws/rest/v1/location` | `attribute` |
| `/ws/rest/v1/locationtag` | — |
| `/ws/rest/v1/locationattributetype` | — |
| `/ws/rest/v1/provider` | `attribute` |
| `/ws/rest/v1/providerattributetype` | — |
| `/ws/rest/v1/providerrole` | — |

---

## Programma's & Cohort

| Endpoint | Sub-resources |
|---|---|
| `/ws/rest/v1/program` | — |
| `/ws/rest/v1/programenrollment` | `attribute`, `state` |
| `/ws/rest/v1/programattributetype` | — |
| `/ws/rest/v1/workflow` | `state` |
| `/ws/rest/v1/cohort` | `membership` |

---

## Formulieren & Taken

| Endpoint | Sub-resources |
|---|---|
| `/ws/rest/v1/form` | `formfield`, `resource` |
| `/ws/rest/v1/field` | `answer` |
| `/ws/rest/v1/fieldtype` | — |
| `/ws/rest/v1/taskdefinition` | — |
| `/ws/rest/v1/taskaction` | — |

---

## Systeem & Admin

| Endpoint | Sub-resources |
|---|---|
| `/ws/rest/v1/systeminformation` | — |
| `/ws/rest/v1/systemsetting` | `subdetails` |
| `/ws/rest/v1/module` | — |
| `/ws/rest/v1/moduleaction` | — |
| `/ws/rest/v1/serverlog` | — |
| `/ws/rest/v1/hl7` | — |
| `/ws/rest/v1/hl7source` | — |
| `/ws/rest/v1/alert` | `recipient` |
| `/ws/rest/v1/customdatatype` | `handlers` |
| `/ws/rest/v1/nametemplate` | — |
| `/ws/rest/v1/administrationlinks` | — |
| `/ws/rest/v1/obstree` | — |

---

## Swagger UI

Interactieve documentatie beschikbaar op:
```
http://localhost:8080/openmrs/module/webservices/rest/apiExplorer.htm
```
