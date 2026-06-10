# Security code review & kwetsbaarheden

Rubric: *"Verbeteronderzoek security"* — **Security code review & kwetsbaarheden** (15 pt).
NEN-7510 controls: **A.8.25** (veilige ontwikkelcyclus), **A.8.28** (veilig coderen), **A.8.29** (beveiligingstests).

---

## 1. Methodologie

De code review is gelaagd opgezet: geautomatiseerde tooling vormt de eerste verdedigingslinie en menselijke beoordeling vormt de laatste poort vóór merge.

### Geautomatiseerde tooling

**SonarCloud (SAST)**
Elke pull request triggert een SonarCloud-analyse. De pipeline is geconfigureerd als *quality gate*: een pull request waarop de quality gate faalt (bv. een nieuw security hotspot of een reliability-issue met rating slechter dan A) kan niet gemerged worden totdat het probleem handmatig is opgelost of expliciet gedocumenteerd afgewogen. Dit dwingt af dat bevindingen niet stilzwijgend passeren.

**CodeQL (SAST, query-suite `security-extended`)**
GitHub Advanced Security voert bij elke push een CodeQL-scan uit met de `security-extended` query-suite (zie [ADR-001](../02-secure-pipelines/adr-001-codeql-query-suite-scope.md) voor de keuzeonderbouwing). Alerts verschijnen als *code scanning alerts* in de GitHub Security-tab. De organisatie-instelling blokkeert merge bij nieuwe hoog-risico-alerts totdat deze worden opgelost of als `false positive` gedismisst met onderbouwing.

**SBOM & CVE-check (SCA)**
Bij elke pull request wordt een CycloneDX SBOM gegenereerd en gecontroleerd op bekende CVE's (via Dependency-Review Action / Snyk). Een pull request met afhankelijkheden die een CVE bevatten boven de drempelwaarde faalt automatisch. Dit vereist handmatige actie — updaten van de afhankelijkheid of een gedocumenteerde risicoafweging — voordat merge mogelijk is.

**GitHub Advanced Security — AI-ondersteunde review (Copilot Autofix)**
GitHub's ingebouwde AI-tooling (Copilot Autofix) analyseert CodeQL-alerts en genereert suggesties voor mitigatie rechtstreeks als PR-commentaar. Deze suggesties zijn niet blindelings overgenomen: elke autofix-suggestie is handmatig beoordeeld op correctheid en contextrelevantie voordat deze werd toegepast of verworpen.

### Verplichte menselijke review

Op organisatieniveau is *required reviewers* afgedwongen: elke pull request vereist minimaal één goedkeuring van een ander teamlid voordat merge is toegestaan. Dit voorkomt dat bevindingen van de geautomatiseerde tooling worden omzeild zonder bewust menselijk oordeel. De combinatie van automatische gate (SonarCloud, CodeQL, SBOM) én menselijke goedkeuring zorgt dat kwetsbaarheden die door tooling worden gevlagd altijd een expliciete beslissing vereisen.

### Handmatig onderzochte bestanden

Naast de geautomatiseerde scans zijn de volgende klassen handmatig gereviewed, op basis van het aanvalsoppervlak-overzicht (Sprint 3) en de risicomatrix (Sprint 2):

- `AuthorizationFilter.java` — toegangscontrole en authenticatieverwerking
- `SessionController1_9.java` — sessiebeheer en inlogstroom
- `BaseDelegatingResource.java` — centrale resource-routing met brede rechtenimpact

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
