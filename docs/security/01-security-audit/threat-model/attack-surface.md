# Attack Surface Mapping — `webservices.rest`

Dit document is de Sprint 3-deliverable voor issue #63 ("Attack Surface Mapping").
Het bouwt voort op het STRIDE-threat-model in
[`../01.md` §3.5](../01.md#35-threat-model--c4-architectuur--stride-analyse) en op de
endpoint-inventarisatie in
[`../../05-penetration-tests/endpoints.md`](../../05-penetration-tests/endpoints.md).
Volgens het begrippenkader is de **attack surface** *"de optelsom van alle punten
waar een aanvaller een systeem kan benaderen of beïnvloeden (endpoints, interfaces,
configuratie) — inclusief impliciet vertrouwde elementen ('trust boundaries')"*.

---

## 1. Categorieën van de attack surface

De module exposeert HTTP-endpoints via **drie verschillende URL-prefixen**, die
**niet** dezelfde beveiligingslaag doorlopen. Dit onderscheid is de kern van deze
mapping en is in Sprint 3 ontdekt via de STRIDE-analyse (TM-S4, TM-I2, TM-T2 —
zie §3.5.4 in het hoofddocument).

| Categorie | URL-prefix | Loopt door `AuthorizationFilter` (TB1)? | Component (C4) |
|---|---|:---:|---|
| **A. Resource Framework** (CRUD op klinische/administratieve resources) | `/ws/rest/v1/<resource>` | ✅ Ja | Resource Framework, `MainResourceController` |
| **B. Version-Specific Controllers** (sessie, wachtwoord, HL7, cache, etc.) | `/ws/rest/v1/<endpoint>` | ✅ Ja | Version-Specific Controllers |
| **C. Module-admin controllers** (settings, Swagger) | `/module/webservices/rest/<endpoint>` | ❌ **Nee** | `SettingsFormController`, `SwaggerDocController`, `SwaggerSpecificationController` |
| **D. Andere OpenMRS-modules** (consumers van deze REST-laag) | n.v.t. (Java-API / interne aanroepen) | n.v.t. | Buiten C4-scope, wel relevant als aanvalsvector |

> **Waarom dit ertoe doet:** categorie C ligt **buiten** de servlet-filterketen die
> in `omod/src/main/resources/config.xml` is geconfigureerd
> (`<url-pattern>/ws/rest/*</url-pattern>` voor `REST Web Service Authorization`,
> `REST Web Service Content-Type`, `compressionFilter` en `shallowEtagHeaderFilter`).
> Endpoints in categorie C krijgen dus **geen** IP-allowlisting, **geen** Basic
> Auth-afdwinging en **geen** content-type-validatie — ze zijn zo "veilig" als de
> controller-code zelf, zonder enig vangnet. Dit is exact de oorzaak van SQ7, SQ8
> en SQ9.

---

## 2. Categorie A — Resource Framework (`/ws/rest/v1/<resource>`)

Voor de volledige lijst van ~50 resources (patiënt, persoon, encounter, obs, order,
concept, gebruikers, locatie, etc.) zie
[`endpoints.md`](../../05-penetration-tests/endpoints.md). Kernpunten voor de attack
surface:

- **Authenticatie/autorisatie:** `AuthorizationFilter` (TB1) verplicht een geldige
  IP (allow-list) en accepteert optioneel HTTP Basic Auth; verdere autorisatie
  gebeurt **ad-hoc** in de service-laag (zie TM-E1, gap A.8.3).
- **Input:** elke resource ondersteunt `GET`/`POST`/`PUT`/`DELETE` met
  query-parameters (`v`, `q`, `limit`, `startIndex`, custom search-parameters per
  `SearchHandler`). Zoek-/filterparameters voeden de switch-statements uit TM-T1
  (SQ2).
- **Implicit trust:** de Resource Framework vertrouwt erop dat **elke**
  resource-implementatie zelf privilege-checks uitvoert waar nodig — er is geen
  centrale, declaratieve enforcement (TM-E1). Nieuwe of door de community
  bijgedragen resources erven dus geen garanties.
- **High risk binnen categorie A:**
  - `/ws/rest/v1/user`, `/ws/rest/v1/role`, `/ws/rest/v1/privilege` — direct
    gekoppeld aan KJ3 (authenticatiegegevens); compromittering hier is een
    directe opmaat naar privilege-escalatie (TM-E2).
  - `/ws/rest/v1/patient*`, `/ws/rest/v1/person*`, `/ws/rest/v1/obs`,
    `/ws/rest/v1/encounter*` — KJ1/KJ2 (bijzondere persoonsgegevens, AVG art. 9);
    elke ongeautoriseerde toegang hier is een meldplichtig datalek.
  - `/ws/rest/v1/systemsetting`, `/ws/rest/v1/serverlog`,
    `/ws/rest/v1/administrationlinks` — administratieve/diagnostische data
    (KJ4, KJ7); zelfde risicocategorie als SQ7 maar **wél** achter
    `AuthorizationFilter`.

---

## 3. Categorie B — Version-Specific Controllers (`/ws/rest/v1/<endpoint>`)

Deze controllers liggen **wél** achter `AuthorizationFilter` (TB1) maar bieden
**geen aanvullende privilege-check** bovenop de basis-authenticatie — ze vertrouwen
volledig op "je bent ingelogd" zonder te toetsen *welke* rol/privilege vereist is
(TM-E3). Geverifieerd in deze sprint:

| Endpoint | Methode(n) | Auth-check naast `AuthorizationFilter`? | Risico-inschatting |
|---|---|:---:|---|
| `/ws/rest/v1/session` | GET, POST, DELETE | Gedeeltelijk — `Context.isAuthenticated()` bepaalt response-inhoud, geen privilege-check | Normaal — kernfunctie (login/logout) |
| `/ws/rest/v1/session/diag` | GET | ❌ Nee — `SessionController1_9.getDiagnostics()` toont `userRoles`/`userPrivileges` aan elke (ook anonieme) caller die door TB1 komt | 🔴 **Hoog** — TM-I1, gap A.8.3, prio 4 (§5.1) |
| `/ws/rest/v1/password` (eigen wachtwoord) | POST | ✅ `Context.isAuthenticated()` check aanwezig | Normaal |
| `/ws/rest/v1/password/{userUuid}` (andermans wachtwoord) | POST | ❌ Geen expliciete `hasPrivilege`-check gevonden naast service-laag | 🟠 **Midden** — afhankelijk van service-laag enforcement; nader te onderzoeken |
| `/ws/rest/v1/passwordreset[/{key}]` | POST | n.v.t. (bedoeld voor anonieme wachtwoord-reset-flow) | 🟡 Laag — standaardpatroon, mits rate-limiting (TM-D1) |
| `/ws/rest/v1/hl7` | GET, POST | ❌ Geen expliciete check gevonden | 🟠 **Midden** — HL7-berichten bevatten klinische data (KJ2); POST zonder privilege-check is een tamper-vector |
| `/ws/rest/v1/cleardbcache` | POST | ❌ Geen expliciete check gevonden | 🟠 **Midden** — DoS-potentieel (KJ5) als elke geauthenticeerde gebruiker de cache kan legen |
| `/ws/rest/v1/searchindexupdate` | POST | ❌ Geen expliciete check gevonden | 🟠 **Midden** — zelfde DoS-patroon als hierboven |
| `/ws/rest/v1/loggedinusers` | GET | ❌ Geen expliciete check gevonden | 🟠 **Midden** — lekt informatie over actieve gebruikers (KJ4) |
| `/ws/rest/v1/implementationid` | GET, POST | ❌ Geen expliciete check gevonden op POST | 🟡 Laag-Midden — wijzigen van implementation-ID is een integriteitsrisico, lage directe impact |

> **Conclusie categorie B:** TM-E3 (uit §3.5.3) wordt door deze inventarisatie
> **bevestigd**: meerdere administratieve Version-Specific Controllers voeren geen
> eigen privilege-check uit. Dit is een **systemisch patroon** (gap A.8.3, prio 11
> in §5.1: declaratieve access control), niet een eenmalige bug — vandaar de
> aanbeveling voor declaratieve, centrale autorisatie i.p.v. ad-hoc checks per
> controller.

---

## 4. Categorie C — Module-admin controllers (`/module/webservices/rest/<endpoint>`) — ⚠️ HIGH RISK

Dit is de categorie waar de drie nieuwe kritieke bevindingen (SQ7, SQ8, SQ9) uit
voortkomen. **Geen van deze endpoints loopt door `AuthorizationFilter`.**

| Endpoint | Methode | Auth? | Bevinding | Risicoscore |
|---|---|:---:|---|:---:|
| `GET /module/webservices/rest/settings.form/search?prefix=` | GET | ❌ Geen | **SQ7** — retourneert alle global properties (naam+waarde) incl. mogelijke secrets en `REST_ALLOWED_IPS` | 🔴 **20** |
| `POST /module/webservices/rest/settings.form` | POST | ❌ Geen (Spring MVC form-binding) | Niet apart gescoord, maar zelfde blootstelling: het admin-instellingenformulier (incl. `REST_ALLOWED_IPS`, `MAX_RESULTS_*`) is bereikbaar zonder login | ⚠️ Te beoordelen — zie aanbeveling §5 hieronder |
| `GET /module/webservices/rest/apiDocs/debug?tag=` | GET | ❌ Geen | **SQ8** — reflected XSS, `tag`-parameter ongesanitized in HTML | 🟠 **12** |
| `GET /module/webservices/rest/apiDocs` | GET | ❌ Geen | Swagger-UI HTML-pagina; an sich publieke documentatie maar bevestigt dat de hele `/apiDocs`-boom buiten TB1 valt | — |
| `GET /module/webservices/rest/swagger.json` | GET | ❌ Geen | **SQ9** — Host-/Scheme-headers ongevalideerd in gepubliceerde OpenAPI-spec | 🟠 **9** |

> **Sprint 3-mitigaties (categorie C):** de Swagger-UI en OpenAPI-spec
> (`/apiDocs[/debug]`, `/swagger.json`) zijn nu uitschakelbaar via
> `webservices.rest.enableSwaggerDocs` (hardening checklist §1) en SQ9 (Host-/Scheme-
> reflectie) is gemitigeerd met een Host-allow-list + scheme-sanitisatie (§10). De
> XSS-output-encoding van SQ8 en de **autorisatie op `settings.form` (SQ7 — hoogste
> prioriteit)** blijven open.

**Implicit trust (categorie C):** de aanwezigheid van `AuthorizationFilter` op
`/ws/rest/*` wekt de indruk dat *"alle REST-endpoints van deze module"*
beveiligd zijn. Categorie C toont dat dit **niet klopt** — elke nieuwe controller
die niet expliciet onder `/ws/rest/*` wordt geregistreerd, erft **geen enkele**
beveiligingsmaatregel (geen IP-filter, geen auth, geen content-type-check, geen
compressie-/ETag-afhandeling). Dit is de belangrijkste **trust-boundary-bevinding**
van deze sprint en is toegevoegd als TB6 (zie §5).

---

## 5. Bijgewerkte trust boundaries (uitbreiding op §3.5.2)

Naast de in §3.5.2 beschreven TB1–TB5 introduceert deze attack-surface-analyse:

- **TB6 — Servlet-filterketen vs. controller-registratie:** de grens tussen
  endpoints die via `<url-pattern>/ws/rest/*</url-pattern>` door
  `AuthorizationFilter`/`ContentTypeFilter`/`compressionFilter` lopen (categorieën
  A & B), en endpoints die via `@RequestMapping("/module/webservices/rest/...")`
  buiten die filterketen om direct door Spring MVC worden bediend (categorie C).
  **Impliciet vertrouwd:** dat contributors zich bewust zijn van dit onderscheid bij
  het toevoegen van nieuwe controllers. SQ7/SQ8/SQ9 bewijzen dat dit vertrouwen
  niet houdbaar is.

**TB4 (reverse proxy / `isIpAllowed`)** — TM-S3 status-update na deze mapping: de
IP allow-list (`RestUtil.isIpAllowed()`) wordt **alleen** toegepast op categorie A
en B (via TB1). Categorie C is dus **dubbel** kwetsbaar: geen IP-filter **en** geen
authenticatie. Bij het mitigeren van TB4 (bv. `X-Forwarded-For`-aware IP-resolutie)
moet expliciet besloten worden of categorie C in dezelfde filterketen wordt
opgenomen — de aanbeveling in de hardening checklist (§6) is om categorie C
endpoints óf te verwijderen, óf onder `/ws/rest/*` te laten vallen, óf zelf een
equivalente check uit te voeren.

**TM-E3 (Version-Specific Controllers)** — status-update: bevestigd in §3 hierboven;
`/cleardbcache`, `/searchindexupdate`, `/loggedinusers`, `/implementationid` en
`/hl7` (POST) missen een privilege-check naast de basisauthenticatie van TB1.

---

## 6. Categorie D — Andere OpenMRS-modules als aanvalsvector

De `webservices.rest`-module wordt door vrijwel elke andere OpenMRS-module gebruikt
als integratielaag (bv. via `RestService`, `BaseDelegatingResource`-subclasses die
door modules van derden worden geregistreerd). Dit betekent:

- **Resource- en SearchHandler-registratie is open**: elke module kan eigen
  resources/representaties registreren bij de Resource Framework. Een
  kwaadwillende of slecht geschreven module-bijdrage kan zo:
  - een resource registreren die de switch-statement-zwakte (TM-T1/SQ2) erft;
  - een `SearchHandler` registreren zonder de privilege-checks die de
    kernresources wél (deels) hebben — vergelijkbaar met TM-E1.
- **Impliciet vertrouwd:** dat geïnstalleerde modules "vertrouwde code" zijn.
  NEN-7510 A.8.3 (least privilege) en de CI/CD supply-chain-mitigaties (CD1, §4.4)
  zijn de enige barrières hiertegen — er is geen module-sandboxing op REST-niveau.
- Dit sluit aan bij de workshop-aanwijzing *"andere OpenMRS-modules die proberen
  onze module te misbruiken"* als expliciete attack surface.

---

## 7. Samenvatting — "high risk" ingangen

| Ingang | Categorie | Risico | Vervolgactie (§5.1 in hoofddocument) |
|---|---|---|---|
| `GET /module/webservices/rest/settings.form/search` | C | 🔴 20 (SQ7) | Prioriteit 1 — auth toevoegen of verwijderen |
| `GET /ws/rest/v1/session/diag` | B | 🔴 15 (gap A.8.3) | Prioriteit 4 — beveiligen of verwijderen |
| `GET /module/webservices/rest/apiDocs/debug` | C | 🟠 12 (SQ8) | 🟡 Deels — endpoint nu uitschakelbaar (§1 checklist); output-encoding nog open |
| `GET /module/webservices/rest/swagger.json` | C | 🟠 9 (SQ9) | ✅ Gemitigeerd — Host-allow-list + scheme-sanitisatie (§10 checklist) |
| `/ws/rest/v1/cleardbcache`, `/searchindexupdate`, `/loggedinusers`, `/implementationid`, `/hl7` (POST) | B | 🟠 Midden (TM-E3) | Onderdeel van prioriteit 11 — declaratieve access control |
| `POST /module/webservices/rest/settings.form` | C | ⚠️ Te beoordelen | Zie hardening checklist — categorie C structureel beveiligen |

Zie [`hardening-checklist.md`](hardening-checklist.md) voor de concrete
reductiemaatregelen die uit deze mapping volgen.

---

## 8. OWASP-koppeling van de attack surface

De STRIDE-tabellen in [`../01.md` §3.5.3](../01.md#353-stride-threatanalyse) mappen
**per threat** op OWASP. Onderstaande tabel geeft het **omgekeerde overzicht** dat
hoort bij een attack-surface-analyse: per OWASP-categorie welke ingang(en) van deze
module geraakt worden. Primaire referentie is de
[OWASP API Security Top 10:2023](https://owasp.org/API-Security/editions/2023/en/0x11-t10/)
(`APIx:2023`) omdat de module een REST-API is; web-specifieke items (zoals XSS) zijn
gekoppeld aan de [OWASP Top 10:2021](https://owasp.org/Top10/) (`Ax:2021`).

> **Bewust meerdere OWASP-edities (2021 + 2023 + 2025).** Dit onderzoek mapt findings
> opzettelijk over drie OWASP-edities: de **Top 10:2021** (web-breed), de **API Security
> Top 10:2023** (deze REST-module) en de **Top 10:2025**-conceptcategorieën (gebruikt in
> het pentestrapport). De edities overlappen maar zijn niet identiek — eenzelfde finding
> kan onder verschillende categorienamen vallen. Dat is **geen inconsistentie maar
> bredere dekking**: elke editie belicht een ander deel van het aanvalsoppervlak (bv.
> "Improper Inventory Management" bestaat alléén in de API-editie, en de 2025-editie
> voegt o.a. een expliciete supply-chain-categorie toe). De combinatie geeft daardoor
> een vollediger, beter onderbouwd beeld dan één enkele editie.

| OWASP API Security Top 10:2023 | Geraakte ingang(en) (categorie uit §1) | Bevinding(en) | Status |
|---|---|---|:---:|
| **API1 — Broken Object Level Authorization** | Cat. A resources (`/patient*`, `/obs`, `/encounter*`) | TM-E2 (keten) | ❌ open |
| **API2 — Broken Authentication** | Cat. A/B (`AuthorizationFilter`, `/session`, `/password*`, `/passwordreset`) | TM-S1, TM-S2, AT3 | ❌ open |
| **API3 — Broken Object Property Level Authorization** | Cat. B `/session/diag` | TM-I1 | ❌ open |
| **API4 — Unrestricted Resource Consumption** | Cat. B/C (`AuthorizationFilter`, `/settings.form/search`, `/cleardbcache`, `/searchindexupdate`) | TM-D1, TM-D2, AT2 | ❌ open |
| **API5 — Broken Function Level Authorization** | Cat. A Resource Framework + Cat. B admin-controllers | TM-E1, TM-E3 | ❌ open / 🟡 |
| **API6 — Unrestricted Access to Sensitive Business Flows** | Cat. A bulk-patiëntendpoints | AT2 | ❌ open |
| **API7 — Server Side Request Forgery** | — geen server-side fetch van client-aangeleverde URL's aangetroffen | — | n.v.t. |
| **API8 — Security Misconfiguration** | Cat. C (buiten filterketen, TB6) + `/settings.form/search` | **SQ7** (TM-I2), TM-S3 | SQ7 ❌; Swagger-gating ✅ |
| **API9 — Improper Inventory Management** | Cat. C Swagger-UI + OpenAPI-spec | AT1, **SQ9** (TM-S4) | ✅ gemitigeerd (gating §1 + host-allow-list §10) |
| **API10 — Unsafe Consumption of APIs** | Cat. D andere OpenMRS-modules | §6 | ⚠️ impliciet vertrouwd |

> **XSS valt buiten de API-Top-10:** **SQ8** (reflected XSS in `/apiDocs/debug`, TM-T2/TM-I3)
> mapt op **A03:2021 — Injection**. Productie-blootstelling is nu gereduceerd doordat de
> hele `/apiDocs`-boom achter de `webservices.rest.enableSwaggerDocs`-vlag is gezet
> (hardening checklist §1); de output-encoding-fix zelf blijft een open code-review-actie.

**Conclusie:** de attack surface raakt **8 van de 10** OWASP API-categorieën. De zwaarst
geraakte cluster is **API8/API9** (Security Misconfiguration + Improper Inventory
Management) — exact de categorie C-endpoints buiten `AuthorizationFilter` (TB6). De
Sprint 3-mitigaties (Swagger-gating + Host-header-allow-list) verlagen API9 van "open"
naar "gemitigeerd"; API8 (SQ7) blijft de hoogste open prioriteit (§7).
