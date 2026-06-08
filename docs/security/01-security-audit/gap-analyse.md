# 5.5 — Gap-analyse: NEN-7510:2024-2

## Doel

Vergelijk de huidige staat van de `webservices.rest`-module (v3.2.0) met drie
NEN-7510:2024-2 controls en leg per control vast: **aanwezig / gedeeltelijk /
afwezig** + concreet bewijs (coderegel of screenshot).

## Module

`org.openmrs.module:webservices.rest` v3.2.0  
Repository: [GrannyGuard/webservices-rest-audit](https://github.com/GrannyGuard/webservices-rest-audit)

---

## A.8.3 — Toegangsbeveiliging (Access Restriction)

**Eis (NEN-7510:2024-2 A.8.3):** Toegang tot informatie en systemen moet worden
beperkt overeenkomstig het toegangsbeheerbeleid; gebruikers krijgen alleen toegang
tot de informatie die ze nodig hebben voor hun taak.

| Aspect | Status | Bewijs |
|--------|--------|--------|
| IP-gebaseerde toegangsbeperking | ✅ Aanwezig | `omod-common/.../filter/AuthorizationFilter.java:69–75` — globale property `REST_ALLOWED_IPS` begrenst toegang op netwerkniveau; bij niet-toegestaan IP → HTTP 403 |
| Privilege-gebaseerde resource-toegang | ⚠️ Gedeeltelijk | `omod-common/.../resource/impl/BaseDelegatingResource.java:875` — `Context.hasPrivilege(RestConstants.PRIV_SET_AUDIT_DATA)` controleert één specifiek recht; alle overige resource-methoden delegeren impliciet naar de OpenMRS service-laag |
| Declaratieve access control op REST-laag | ❌ Afwezig | `AuthorizationFilter.java:33–36` (Javadoc): *"It will not fail on invalid or missing credentials. We count on the API to throw exceptions …"* — geen `@Secured`, Spring Security-annotaties of pre-/post-autorisatie op endpoints |
| Onbeveiligd diagnostics-endpoint | ❌ Afwezig | `omod/.../controller/openmrs1_9/SessionController1_9.java:169–182` — `/session/diag` is expliciet gedocumenteerd als *"No authorization check — accessible to any caller"*, maar toont bij een actieve sessie ook `userRoles` en `userPrivileges` |
| Repository-toegangsbeveiliging (SDLC) | ✅ Aanwezig | GitHub Ruleset "main protection" actief: force pushes geblokkeerd, 1 verplichte review, CodeQL als required status check — zie `docs/security/02-secure-pipelines/repository-inrichting.md` |

**Eindoordeel A.8.3:** ⚠️ **Gedeeltelijk** — IP-filtering en OpenMRS-privileges zijn
aanwezig, maar de REST-laag heeft geen declaratieve toegangscontrole en het
`/session/diag`-endpoint vormt een onbewachte informatielekkage.

---

## A.8.5 — Authenticatie (Secure Authentication)

**Eis (NEN-7510:2024-2 A.8.5):** Authenticatieprocedures moeten identiteiten op
veilige wijze vaststellen en ongeautoriseerde toegang voorkomen; dit omvat sterke
authenticatiemechanismen en bescherming tegen misbruik van inlogprocedures.

| Aspect | Status | Bewijs |
|--------|--------|--------|
| HTTP Basic Auth (Base64) | ✅ Aanwezig | `AuthorizationFilter.java:86–114` — `Authorization: Basic`-header wordt Base64-gedecodeerd en doorgegeven aan `Context.authenticate(username, password)` |
| Sessie-invalidatie bij uitloggen | ✅ Aanwezig | `SessionController1_9.java:129–134` — `Context.logout()` gevolgd door `session.invalidate()` |
| Multi-Factor Authenticatie (MFA) | ❌ Afwezig | Geen TOTP, SMS-token of hardware-key-ondersteuning in de module; OpenMRS core biedt hier geen standaard integratie |
| Brute-force / rate-limiting bescherming | ❌ Afwezig | Geen account-lockout of request-throttling in `AuthorizationFilter`; het filter itereert onbeperkt `Context.authenticate()`-aanroepen |
| Sessie-fixatie-mitigatie | ❌ Afwezig | Sessie-ID wordt *niet* ververst na succesvolle authenticatie — geen `session.invalidate()` + nieuwe sessie aangemaakt bij login in `AuthorizationFilter` |
| Logging van mislukte aanmeldpogingen | ❌ Afwezig (praktisch) | `AuthorizationFilter.java:113` — mislukte authenticatie gelogd als `log.debug("authentication exception")` — **DEBUG-niveau**, onzichtbaar bij standaard INFO/WARN productie-logconfiguratie |
| Transport-encryptie (TLS) afdwingbaar | ⚠️ Buiten scope module | De module zelf dwingt geen HTTPS af; afhankelijk van deployment-configuratie (servlet-container / reverse proxy) |

**Eindoordeel A.8.5:** ❌ **Onvoldoende** — Basisauthenticatie is aanwezig, maar
MFA ontbreekt volledig, brute-force bescherming is er niet, sessie-fixatie is niet
gemitigeerd en mislukte aanmeldpogingen zijn niet zichtbaar in productielogs. Voor
een systeem met medische persoonsgegevens zijn dit kritieke tekortkomingen.

---

## A.8.15 — Logging (Security Event Logging)

**Eis (NEN-7510:2024-2 A.8.15):** Security-relevante gebeurtenissen moeten worden
gelogd zodat accountabiliteit geborgd is en incidentonderzoek (forensisch) mogelijk
wordt; logs moeten beschermd zijn tegen manipulatie.

| Aspect | Status | Bewijs |
|--------|--------|--------|
| Application-level logging aanwezig | ✅ Aanwezig | SLF4J + `LoggerFactory` overal gebruikt; `AuthorizationFilter.java:42` — `private static final Logger log` |
| Succesvolle authenticatie gelogd | ⚠️ Gedeeltelijk | `AuthorizationFilter.java:108` — `log.debug("authenticated [{}]", userAndPass[0])` — **DEBUG-niveau**, niet beschikbaar in productie |
| Mislukte authenticatie gelogd | ❌ Afwezig (praktisch) | `AuthorizationFilter.java:113` — `log.debug("authentication exception")` — **DEBUG-niveau**; geen gebruikersnaam of remote IP in het log-bericht |
| Autorisatieweigeringen (403/401) gelogd | ❌ Afwezig | Geen expliciete logging van afgewezen verzoeken door de module; HTTP 403 bij IP-blokkering (`AuthorizationFilter.java:73`) bevat wél IP in HTTP-response, maar niet in application-log op INFO+ |
| Centralised security audit trail | ❌ Afwezig | Geen afzonderlijk security-logbestand of gestructureerd audit event; REST-toegang wordt niet vastgelegd (endpoint, gebruiker, tijdstip) |
| Log-entries forensisch bruikbaar | ❌ Afwezig | Auth-log-entries missen remote IP-adres en benaderd endpoint; onvoldoende voor incidentonderzoek conform NEN-7510 |
| CI/CD pipeline logt security-scans | ✅ Aanwezig | CodeQL SARIF-resultaten in GitHub Security-tab; SBOM als 90-dagen CI-artifact (`sbom.yml`) — traceerbaarheid van kwetsbaarheden |

**Eindoordeel A.8.15:** ❌ **Onvoldoende** — Basis application logging is aanwezig
maar alle security-relevante events (auth-successen, -failures, access denials)
worden op DEBUG gelogd en zijn onzichtbaar in productie. Een forensisch bruikbaar
audit trail ontbreekt volledig.

---

## Samenvatting

| NEN-7510 Control | Oordeel | Kritiekste gap |
|------------------|---------|----------------|
| **A.8.3** Toegangsbeveiliging | ⚠️ Gedeeltelijk | Geen declaratieve access control op REST-laag; onbeveiligd `/session/diag` endpoint lekt gebruikersrollen |
| **A.8.5** Authenticatie | ❌ Onvoldoende | Geen MFA, geen brute-force bescherming, geen sessie-fixatie mitigatie, auth-events niet zichtbaar in productielogs |
| **A.8.15** Logging | ❌ Onvoldoende | Auth-events op DEBUG-niveau, geen security audit trail, log-entries niet forensisch bruikbaar |

### Aanbevolen prioritering van verbeteringen

1. **Hoog — A.8.5 + A.8.15:** Auth-logging verhogen naar INFO/WARN met remote IP en endpoint; rate-limiting toevoegen
2. **Hoog — A.8.3:** `/session/diag` beveiligen of verwijderen
3. **Middel — A.8.5:** Sessie-fixatie mitigeren (sessie-ID vernieuwen na login)
4. **Laag — A.8.3:** Declaratieve access control overwegen (Spring Security pre-/post-authorize)

---

*Analyse uitgevoerd op: 2026-06-03*  
*Analist: Wassim Balouda*  
*Gebaseerd op: broncode-analyse webservices.rest v3.2.0*
