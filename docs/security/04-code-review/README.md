# Security code review & kwetsbaarheden

Rubric: *"Verbeteronderzoek security"* — **Security code review & kwetsbaarheden** (15 pt).
NEN-7510 controls: **A.8.25** (veilige ontwikkelcyclus), **A.8.28** (veilig coderen), **A.8.29** (beveiligingstests).

---

## 1. Methodologie

> **TODO:** Beschrijf hoe de code review uitgevoerd is:
> - Welke tools ingezet (CodeQL, SonarCloud, AI-tooling)?
> - Welke bestanden / klassen handmatig onderzocht?
> - Hoe zijn de tools ingezet — welke query-suites, welke prompts bij AI?
>
> Dit is vereist voor het rubric-onderdeel "gebruik van (AI-)tooling".

---

## 2. Bevindingen

> **TODO:** Documenteer per kwetsbaarheid (minimaal de SonarCloud + CodeQL bevindingen):
>
> Per bevinding opnemen:
> - Bestandsnaam + regelnummer
> - Beschrijving van het probleem (wat is er mis en waarom)
> - OWASP Top 10 (2021) classificatie
> - NEN-7510 control
> - **Risico bij niet-oplossen** — onderbouwd met een valide externe bron (OWASP, CWE, NVD)
>
> Startpunten voor bevindingen:
> - SonarCloud-dashboard → Security Hotspots + Reliability issues
> - GitHub Security-tab → CodeQL-alerts
> - Handmatige review van `AuthorizationFilter.java`, `SessionController1_9.java`, `BaseDelegatingResource.java` (zie ook risicomatrix in [`01-security-audit/01.md §4`](../01-security-audit/01.md#4-risico-evaluatie) voor SQ1–SQ6 als startpunt)

### 2.1 Bevinding 1 — Log injection (CWE-117) via REST-request-parameters

- **Bron:** CodeQL `java/log-injection`, query-suite `security-extended` (zie
  [ADR-001](../02-secure-pipelines/adr-001-codeql-query-suite-scope.md))
- **Bestanden + regels (vóór fix, commit `8521cdc`):**
  - `omod-common/.../web/RestUtil.java:521`
  - `omod/.../v1_0/controller/openmrs2_0/ClearDbCacheController2_0.java:79,86,95`
  - `omod/.../v1_0/controller/openmrs2_0/SearchIndexController2_0.java:77,81 (×2)`
  - `omod/.../v1_0/resource/openmrs1_8/ConceptResource1_8.java:567`
- **Beschrijving:** de REST-controllers loggen request-afgeleide waarden
  (`resource`, `subResource`, `uuid`, query-parameters) direct in `log.debug()`/
  `log.info()`/`log.error()`-statements zonder neutralisatie. Een aanvaller kan
  CR/LF-tekens (`\r\n`) in deze velden opnemen om extra, valse logregels te
  injecteren ("log forging") — bv. een nep `INFO`-regel die een succesvolle login
  suggereert, om forensisch onderzoek te misleiden.
- **OWASP Top 10 (2021):** A09:2021 – Security Logging and Monitoring Failures
  (gecombineerd met A03:2021 – Injection als onderliggend mechanisme).
  Referentie: [OWASP — Log Injection](https://owasp.org/www-community/attacks/Log_Injection),
  [CWE-117](https://cwe.mitre.org/data/definitions/117.html).
- **NEN-7510 control:** A.8.15 (Logging) — de integriteit van het audit-logbestand
  (KJ6, zie [01-security-audit §2.1](../01-security-audit/01.md)) is een
  voorwaarde voor bruikbaarheid van logging als bewijs/forensisch middel.
- **Risico bij niet-oplossen:** een vervalst auditlog ondermijnt elk forensisch
  onderzoek na een incident — precies het scenario dat NEN-7510 A.8.15 en de
  audit-trail-eis (zie kroonjuweel KJ6) moeten voorkomen. CWE-117 wordt door
  OWASP geclassificeerd binnen A09, een categorie die volgens de OWASP Top 10
  2021 in >90% van de onderzochte applicaties aanwezig was.
- **Status:** ✅ **Gemitigeerd** in commit `831dafb`
  ("fix(security): sanitize user input before logging (CWE-117 log injection)").
  Zie [Mitigatie & validatie §1](../06-mitigatie-en-validatie/README.md#1-cwe-117-log-injection)
  voor de mitigatie en validatie-status.
- **Restpunt:** alert #15 (`AuthorizationFilter.java`, oorspronkelijk regel 108) is
  niet door deze commit aangepakt — dat bestand is in een eerdere commit
  (`8521cdc`, issue #65 logging-gap-fix) al herschreven met `log.warn(...,
  attemptedUser, ...)`. Of die nieuwe code nog steeds als `log-injection` flagt
  (de gebruikersnaam uit Basic Auth is ook user-controlled) moet na de
  eerstvolgende CodeQL-scan geverifieerd worden — zie
  [ADR-001 §Validatie](../02-secure-pipelines/adr-001-codeql-query-suite-scope.md#validatie-na-eerstvolgende-scan-op-main).

### 2.2 Bevinding 2 — *(titel)*

> **TODO:** Invullen.

### 2.2 Bevinding 2 — *(titel)*

> **TODO:** Invullen.

### 2.3 Bevinding 3 — *(titel)*

> **TODO:** Invullen. *(Voeg zoveel bevindingen toe als gevonden.)*

---

## 3. Prioriteringsoverzicht

> **TODO:** Na het documenteren van alle bevindingen, een samenvattende prioriteringstabel maken:

| ID | Bevinding | OWASP | Ernst | NEN-7510 control | Status |
|----|-----------|-------|-------|-----------------|--------|
| F1 | Log injection (CWE-117) — 9× in REST-controllers | A09:2021 | Medium | A.8.15 | ✅ Gemitigeerd (`831dafb`) |
| *(toe te voegen)* | | | | | |

---

## 4. Bijlagen

> **TODO:**
> - [ ] Screenshot of export van **GitHub Security-tab** (CodeQL SARIF-resultaten) toevoegen als bewijs
> - [ ] Screenshot of PDF van **SonarCloud-dashboard** toevoegen (hotspots, reliability rating)
