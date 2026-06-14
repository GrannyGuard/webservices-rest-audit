# Traceability Matrix

Rubric: *"Verbeteronderzoek security"* — Sprint 4 vereiste (≥3 NEN-7510:2024-2
controls, elk met een traceerbaar bewijsartefact).

Elke rij volgt de keten **maatregel → NEN-7510-control → bewijsartefact** en is, waar
van toepassing, gekoppeld aan het risico/de bevinding die de maatregel adresseert
(`SQ*`/`TM-*`/`CD*`/`CQL*` uit de risicomatrix in
[`01-security-audit/01.md §4`](01-security-audit/01.md#4-risico-evaluatie)). Het
bewijsartefact is bedoeld om reproduceerbaar te zijn (commit-SHA, CI-run, `gh api`-
output of bronbestand). De matrix dekt momenteel **11 controls**; dit overstijgt de
minimumeis van 3 ruim. Bewust niet opgenomen: controls waarvoor (nog) geen hard
bewijsartefact bestaat — zie *Restpunten*.

---

## Matrix

| NEN-7510:2024-2 control | Maatregel | Implementatie | Bewijsartefact | Status |
|---|---|---|---|---|
| **A.8.3** Toegangsbeveiliging | Branch protection op `main` (1 verplichte review, geen directe pushes) + GitHub Environment `prod` vereist goedkeuring van een van twee reviewers vóór deploy | GitHub Ruleset *"main protection"*; Environment `prod` met `required_reviewers: DanielvG-IT, BaasW` | `gh api repos/GrannyGuard/webservices-rest-audit/environments/prod` (geverifieerd 2026-06-10); zie [02.md §2, §4.1](02-secure-pipelines/02.md#2-omgevingsscheiding-otap--github-environments) | ✅ Geïmplementeerd |
| **A.8.5** Authenticatie | Org-brede 2FA-verplichting voor alle leden; secret scanning + push protection op de repo | GitHub-org policy `two_factor_requirement_enabled` | `gh api orgs/GrannyGuard` → `"two_factor_requirement_enabled": true` (geverifieerd 2026-06-10); secret-scanning alerts: 0 open; zie [02.md §4.2–4.3](02-secure-pipelines/02.md#42-mfa-voor-alle-leden) | ✅ Geïmplementeerd |
| **A.8.8** Beheer van technische kwetsbaarheden | SBOM (CycloneDX) op elke push; SCA via Dependabot + OSV-Scanner + Snyk; contextuele risicoscore en patch/suppress/accept-besluiten per kwetsbaarheid; updates handmatig (geen `dependabot.yml`-auto-PR's, zie `e724ba1`) | `.github/workflows/sbom.yml`; `cyclonedx-maven-plugin` → `sbom.cdx.json` (238 packages) | [`sbom.cdx.json`](03-sbom-en-cve-advies/sbom.cdx.json); 264 open Dependabot-alerts → 75 unieke CVE's, geprioriteerde backlog in [03.md §4–5](03-sbom-en-cve-advies/03.md#4-geprioriteerde-security-backlog-top-8-gesorteerd-op-contextuele-score) | ✅ Geïmplementeerd (advies/uitvoering van patches: zie §6/§7 van 03.md) |
| **A.8.15** Logging | CodeQL `security-extended` als security-SAST; CWE-117 log-injection-bevindingen gemitigeerd via centrale `RestUtil.sanitizeForLog()` | `.github/workflows/codeql.yml:55` (`queries: security-extended`, [ADR-001](02-secure-pipelines/adr-001-codeql-query-suite-scope.md)); fix in commit `831dafb` | CodeQL-run [`27280969725`](https://github.com/GrannyGuard/webservices-rest-audit/actions/runs/27280969725) op commit `e476489` (2026-06-10): alle 9 `java/log-injection`-alerts (#7–15) op `state: fixed`; zie [06-mitigatie-en-validatie §1](06-mitigatie-en-validatie/06.md#1-cwe-117-log-injection) | ✅ Geïmplementeerd en geverifieerd |
| **A.8.25** Veilige ontwikkelcyclus (SDLC) | Wijzigingen aan `main` uitsluitend via PR met verplichte review; CodeQL als required status check blokkeert merge bij security-findings | GitHub Ruleset `required_approving_review_count: 1`, `required_status_checks: ["Analyze Java (java)"]` | GitHub Settings → Rules → Rulesets → *"main protection"*; zie [02.md §4.1, §8](02-secure-pipelines/02.md#41-branch-protection--main) | ✅ Geïmplementeerd |
| **A.8.2 / A.8.3** Privileged access / least privilege (database) | Twee-account-DB-model: privileged migratie-account (Liquibase/DDL) gescheiden van een DML-only runtime-account (`SELECT/INSERT/UPDATE/DELETE`, kan nooit `DROP`) | DB-init-script provisioneert het runtime-account bij eerste init; prod-wiring + `SHOW GRANTS`-verificatie | [`docker/db-init/10-runtime-least-privilege.sh`](../../docker/db-init/10-runtime-least-privilege.sh), [`docker-compose.yml`](../../docker-compose.yml), [`docker/prod/docker-compose.yml`](../../docker/prod/docker-compose.yml), [`docker/README.md`](../../docker/README.md); hardening checklist [§2](01-security-audit/threat-model/hardening-checklist.md#2-least-privilege-databasegebruiker-kan-nooit-drop) | ✅ Mechanisme (PoC); prod-runtime-switch = deploy-stap, niet container-geverifieerd |
| **A.8.20 / A.8.26** Netwerk- & applicatiebeveiliging (Host-validatie) | OpenAPI-spec reflecteert `Host`/`Scheme` niet langer ongevalideerd: Host-allow-list + scheme-sanitisatie (SQ9-fix); Swagger-endpoints uitschakelbaar (A.8.3 least functionality, SQ8-blootstelling) | `RestUtil.resolveSwaggerHost()`/`sanitizeScheme()`/`isSwaggerDocsEnabled()` + global properties; controllers gegate | [`SwaggerSpecificationController.java`](../../omod/src/main/java/org/openmrs/module/webservices/rest/web/controller/SwaggerSpecificationController.java), [`SwaggerDocController.java`](../../omod/src/main/java/org/openmrs/module/webservices/rest/web/controller/SwaggerDocController.java), [`config.xml`](../../omod/src/main/resources/config.xml), unit tests `SwaggerHardeningRestUtilTest` (8/8 groen); hardening checklist [§1](01-security-audit/threat-model/hardening-checklist.md#1-onnodige-features-uitschakelen) + [§10](01-security-audit/threat-model/hardening-checklist.md#10-openapi-specificatie-alleen-naar-specifieke-allowed-hosts) | ✅ Geïmplementeerd + getest (SQ9); SQ8 output-encoding nog open |
| **A.5.23 / A.8.32** Supply chain security / change management | Reproduceerbare, verifieerbare releases: keyless cosign-signing + SLSA build-provenance + CycloneDX-SBOM naast de `.omod`; alle CI-actions SHA-pinned (was 1/9) | `release-sign.yml` (cosign `sign-blob`, `attest-build-provenance`); SHA-pins in alle workflows | [`.github/workflows/release-sign.yml`](../../.github/workflows/release-sign.yml), [`.github/workflows/`](../../.github/workflows/); hardening checklist [§8](01-security-audit/threat-model/hardening-checklist.md#8-signed-artifacts) + [§9](01-security-audit/threat-model/hardening-checklist.md#9-pinned-versies--git-hashes-voor-cicd-acties) | ✅ Geïmplementeerd (workflow); eerste signed release verifieert downstream met `cosign verify-blob` / `gh attestation verify` |
| **A.8.28** Veilig coderen | Server-side validatie i.p.v. client-side: Swagger Host-allow-list + scheme-sanitisatie (adresseert SQ9) en centrale CWE-117 log-injectie-neutralisatie (`RestUtil.sanitizeForLog()`, CQL1) | `RestUtil.resolveSwaggerHost()` / `sanitizeScheme()` / `sanitizeForLog()` + unit tests | [`RestUtil.java`](../../omod-common/src/main/java/org/openmrs/module/webservices/rest/web/RestUtil.java); tests `SwaggerHardeningRestUtilTest` (8/8 groen) + `RestUtilTest`; CWE-117-fix commit `831dafb` (zie [04-code-review §2.1](04-code-review/04.md)) | ✅ Geïmplementeerd + getest |
| **A.8.31** Scheiding ontwikkel-, test- en productieomgevingen | Aparte compose-overrides per omgeving + GitHub Environments (dev/test/prod), gescheiden secrets; prod publiceert de DB niet en faalt fast (`:?`) op ontbrekende secrets | `docker/{dev,test,prod}/docker-compose.yml`; GitHub Environments met protection rules | [`docker/README.md`](../../docker/README.md), [`docker/prod/docker-compose.yml`](../../docker/prod/docker-compose.yml); zie [02.md §2](02-secure-pipelines/02.md#2-omgevingsscheiding-otap--github-environments) | ✅ Geïmplementeerd |
| **A.8.29** Beveiligingstests | Penetratietest op de development/test-fork: ZAP-scan + handmatige tests tegen alle endpoint-categorieën; bevindingen F1–F9 gekoppeld aan de SQ*/TM-*-risico's uit de threat-modelling en aan de B-001..B-005-bevindingen in het audit-rapport | ZAP-scan + handmatige verificatie per endpoint-categorie | [`05-penetration-tests/PentestReport-OpenMRS.md`](05-penetration-tests/PentestReport-OpenMRS.md) (F1–F9), [`zap-report.html`](05-penetration-tests/zap-report.html), [`endpoints.md`](05-penetration-tests/endpoints.md) | ✅ Uitgevoerd en gedocumenteerd |

---

## Restpunten

- [ ] **Screenshots toevoegen** als visueel bewijs naast de `gh api`-output voor
  A.8.3 (Environments-instellingen) en A.8.5 (org 2FA-policy in Settings →
  Authentication security) — zie ook de TODO's in
  [02.md §8](02-secure-pipelines/02.md#8-nen-7510-compliance-overzicht).
- [ ] **A.8.8 uitbreiden**: zodra de twee "quick win" patches uit
  [03.md §7](03-sbom-en-cve-advies/03.md#7-kostenraming-indicatief)
  (`snakeyaml` → 2.0, `spring-web` → 5.3.34) zijn doorgevoerd, de PR + hernieuwde
  scan-resultaten als extra bewijsartefact toevoegen.
- [x] **Pentest-control toegevoegd** (A.8.29 — beveiligingstests, hierboven) na
  afronding van [05-penetration-tests](05-penetration-tests/PentestReport-OpenMRS.md)
  (F1–F9).
- [ ] **A.8.9 / A.8.24** (gap-analyse, zie `01-security-audit/01.md` §3.4–3.5)
  zijn bewust niet als "geïmplementeerd" opgenomen — de bijbehorende
  bevindingen (B-001 resp. TM-I4) staan nog open. Zodra gemitigeerd: rij
  toevoegen met bewijsartefact.
