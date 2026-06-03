# 5.6 — Mini-complianceverslag: Pipeline-maatregelen

## Doel

Toon per NEN-7510:2024-2 control aan welke CI/CD pipeline-maatregel is getroffen,
en lever concreet bewijs (bestandspad + regelverwijzing).

---

## Tabel: NEN-7510 control → pipeline-maatregel → bewijs

| NEN-7510 control | Categorie | Pipeline-maatregel | Bewijs |
|---|---|---|---|
| **A.8.3** Toegangsbeveiliging | Repository | Branch protection ruleset `main` — directe pushes geblokkeerd, 1 verplichte PR-review, `non_fast_forward` en `deletion` regels actief | GitHub Settings → Rules → Rulesets → *"main protection"*; zie `docs/repository-inrichting.md` §1 |
| **A.8.3** Toegangsbeveiliging | SAST gate | CodeQL als **required status check** — merge naar `main` geblokkeerd bij falende scan | `.github/workflows/codeql.yml` — `required_status_checks: Analyze Java (java)` in ruleset |
| **A.8.5** Authenticatie | SAST | CodeQL met `queries: security-and-quality` detecteert hardcoded credentials, zwakke hash-algoritmen en onveilige authenticatiepatronen | `.github/workflows/codeql.yml:48` — `queries: security-and-quality` |
| **A.8.5** Authenticatie | Dependencies | Dependabot bewaakt Maven-afhankelijkheden (o.a. `commons-codec`, Spring) op gepubliceerde CVE's en opent automatisch PR's | `.github/dependabot.yml` — `package-ecosystem: maven`, dagelijkse check |
| **A.8.8** Beheer van technische kwetsbaarheden | Dependencies | Dependabot GitHub Actions-versies up-to-date houden; security updates geactiveerd | `.github/dependabot.yml` — `package-ecosystem: github-actions` |
| **A.8.8** Beheer van technische kwetsbaarheden | SBOM | CycloneDX SBOM gegenereerd op elke push naar `main`/`develop` — volledige dependency-inventaris voor CVE-tracking | `.github/workflows/sbom.yml` — artifact `sbom-cyclonedx` / `docs/sbom.cdx.json`, 90 dagen bewaard |
| **A.8.15** Logging | Traceerbaarheid | CodeQL SARIF-resultaten opgeslagen in GitHub Security-tab — bevindingen gekoppeld aan commit/PR voor volledige traceerbaarheid | `.github/workflows/codeql.yml:55–58` — `github/codeql-action/analyze` uploadt naar Security-tab |
| **A.8.15** Logging | Kwaliteitscontrole | SonarCloud analyseert elke push/PR op code smells, bugs en security hotspots; quality gate status zichtbaar per commit | `.github/workflows/sonarcloud.yml` — org `grannyguard`, project `GrannyGuard_webservices-rest-audit` |
| **A.8.25** Beheer van softwareontwikkeling | SDLC | Commits worden alleen via pull request naar `main` gemerged; twee ogen principe via verplichte review | GitHub Ruleset `pull_request.required_approving_review_count: 1` |

---

## Openstaande pipeline-gaps (t.o.v. volledige NEN-7510 dekking)

| Gap | NEN-7510 control | Prioriteit | Aanbeveling |
|-----|------------------|------------|-------------|
| Auth-failures niet op INFO+ gelogd in applicatie | A.8.15 | **Hoog** | `AuthorizationFilter.java:113` — log-level verhogen naar `WARN`; remote IP + endpoint toevoegen |
| SonarCloud Quality Gate nog niet als required check | A.8.3 | **Middel** | Toevoegen aan branch protection ruleset nadat 3 SonarCloud hotspots opgelost zijn |
| Geen DAST / dynamische pen-test in CI | A.8.5, A.8.8 | **Middel** | OWASP ZAP Baseline Scan integreren als optionele CI-workflow |
| Geen secrets scanning in pipeline | A.8.5 | **Middel** | GitHub secret scanning activeren of `truffleHog`-action toevoegen |
| Dependabot security updates niet automatisch gemerged | A.8.8 | **Laag** | Auto-merge policy voor patch-updates (semver `~`) overwegen |
| MFA niet afdwingbaar via pipeline | A.8.5 | **Laag** | GitHub organisatie MFA-verplichting activeren (zie `docs/repository-inrichting.md` §2) |

---

## Relatie met de gap-analyse

De bovenstaande pipeline-maatregelen adresseren de **SDLC-kant** van de NEN-7510
controls; de feitelijke code-kwetsbaarheden zijn vastgelegd in
[`01-gap-analyse.md`](01-gap-analyse.md). Beide documenten samen vormen het bewijs
voor de sprint 1 eindcheck.

---

*Opgesteld: 2026-06-03*  
*Versie: 1.0 (Sprint 1)*  
*Auteur: Wassim Balouda*
