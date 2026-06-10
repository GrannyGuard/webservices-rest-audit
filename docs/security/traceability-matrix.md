# Traceability Matrix

Rubric: *"Verbeteronderzoek security"* — Sprint 4 vereiste (≥3 NEN-7510:2024-2
controls, elk met een traceerbaar bewijsartefact).

---

## Matrix

| NEN-7510:2024-2 control | Maatregel | Implementatie | Bewijsartefact | Status |
|---|---|---|---|---|
| **A.8.3** Toegangsbeveiliging | Branch protection op `main` (1 verplichte review, geen directe pushes) + GitHub Environment `prod` vereist goedkeuring van een van twee reviewers vóór deploy | GitHub Ruleset *"main protection"*; Environment `prod` met `required_reviewers: DanielvG-IT, BaasW` | `gh api repos/GrannyGuard/webservices-rest-audit/environments/prod` (geverifieerd 2026-06-10); zie [02.md §2, §4.1](02-secure-pipelines/02.md#2-omgevingsscheiding-otap--github-environments) | ✅ Geïmplementeerd |
| **A.8.5** Authenticatie | Org-brede 2FA-verplichting voor alle leden; secret scanning + push protection op de repo | GitHub-org policy `two_factor_requirement_enabled` | `gh api orgs/GrannyGuard` → `"two_factor_requirement_enabled": true` (geverifieerd 2026-06-10); secret-scanning alerts: 0 open; zie [02.md §4.2–4.3](02-secure-pipelines/02.md#42-mfa-voor-alle-leden) | ✅ Geïmplementeerd |
| **A.8.8** Beheer van technische kwetsbaarheden | SBOM (CycloneDX) op elke push; SCA via Dependabot + OSV-Scanner + Snyk; contextuele risicoscore en patch/suppress/accept-besluiten per kwetsbaarheid; updates handmatig (geen `dependabot.yml`-auto-PR's, zie `e724ba1`) | `.github/workflows/sbom.yml`; `cyclonedx-maven-plugin` → `sbom.cdx.json` (238 packages) | [`sbom.cdx.json`](03-sbom-en-cve-advies/sbom.cdx.json); 264 open Dependabot-alerts → 75 unieke CVE's, geprioriteerde backlog in [03.md §4–5](03-sbom-en-cve-advies/03.md#4-geprioriteerde-security-backlog-top-8-gesorteerd-op-contextuele-score) | ✅ Geïmplementeerd (advies/uitvoering van patches: zie §6/§7 van 03.md) |
| **A.8.15** Logging | CodeQL `security-extended` als security-SAST; CWE-117 log-injection-bevindingen gemitigeerd via centrale `RestUtil.sanitizeForLog()` | `.github/workflows/codeql.yml:55` (`queries: security-extended`, [ADR-001](02-secure-pipelines/adr-001-codeql-query-suite-scope.md)); fix in commit `831dafb` | CodeQL-run [`27280969725`](https://github.com/GrannyGuard/webservices-rest-audit/actions/runs/27280969725) op commit `e476489` (2026-06-10): alle 9 `java/log-injection`-alerts (#7–15) op `state: fixed`; zie [06-mitigatie-en-validatie §1](06-mitigatie-en-validatie/README.md#1-cwe-117-log-injection) | ✅ Geïmplementeerd en geverifieerd |
| **A.8.25** Veilige ontwikkelcyclus (SDLC) | Wijzigingen aan `main` uitsluitend via PR met verplichte review; CodeQL als required status check blokkeert merge bij security-findings | GitHub Ruleset `required_approving_review_count: 1`, `required_status_checks: ["Analyze Java (java)"]` | GitHub Settings → Rules → Rulesets → *"main protection"*; zie [02.md §4.1, §8](02-secure-pipelines/02.md#41-branch-protection--main) | ✅ Geïmplementeerd |

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
- [ ] **Pentest-control toevoegen** (bv. A.8.29 — beveiligingstests) zodra
  [05-penetration-tests](05-penetration-tests/README.md) is uitgevoerd.
