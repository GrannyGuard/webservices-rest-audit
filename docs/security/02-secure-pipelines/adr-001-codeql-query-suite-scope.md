# ADR-001: CodeQL query suite scope — `security-and-quality` → `security-extended`

**Status:** Accepted
**Datum:** 2026-06-10
**Auteurs:** Granny Guard

## Context

De CodeQL-workflow ([`.github/workflows/codeql.yml`](../../../.github/workflows/codeql.yml))
draaide met `queries: security-and-quality`. Deze suite bevat naast security-queries ook
een groot aantal pure code-quality/style-queries (bv. *"Boxed variable is never null"*,
*"Cast from abstract to concrete collection"*, *"Reference equality test of boxed types"*,
*"Container contents are never accessed"*).

Op het moment van schrijven stonden er **739 open code-scanning alerts** in de GitHub
Security-tab (`category: /language:java`). Een volledige export van alle alerts via de
GitHub API (`gh api repos/.../code-scanning/alerts`, gegroepeerd op `rule.id` en
`rule.tags`) laat zien:

| Groep | Aantal | Query-id's (CWE) |
|---|---|---|
| **Security** (`tags` bevat `security`) | **16** | `log-injection` ×9 (cwe-117), `tainted-arithmetic` ×2 (cwe-190/191), `overly-large-range` ×2 (cwe-020), `xss` ×1 (cwe-079), `user-controlled-bypass` ×1 (cwe-290/807), `sensitive-log` ×1 (cwe-532) |
| **Quality** (geen `security`-tag) | **723** | `deprecated-call` ×576, `missing-override-annotation` ×38, `unused-parameter` ×22, `unknown-javadoc-parameter` ×20, `local-variable-is-never-read` ×16, `non-static-nested-class` ×12, en 13 kleinere query-types |

**Belangrijk:** de scheiding security/quality loopt via de `tags`-array van de query
(`security` vs. niet), **niet via `rule.severity`**. Severity is geen bruikbare proxy:
binnen de 723 quality-findings zitten ook 4 alerts met severity `error`
(`reference-equality-of-boxed-types` ×2, `unused-container` ×1, `sleep-with-lock-held`
×1) — qua severity-badge identiek aan de echte security-findings. Een aanpak die op
severity zou filteren (bv. "alleen Error/Warning tonen") zou deze 4 quality-issues
dus ten onrechte als security-relevant blijven tonen, en zou tegelijk de `warning`-
severity security-findings (`tainted-arithmetic`, `overly-large-range`, `sensitive-log`
— 4 van de 16) niet onderscheiden van de `warning`-severity quality-ruis. Vandaar dat
de oplossing op **query-suite-niveau** moet: welke queries draaien, niet welke severity
getoond wordt.

Deze ruis heeft drie gevolgen:

1. De Security-tab is praktisch onbruikbaar als triage-bron voor echte kwetsbaarheden —
   precies het instrument dat NEN-7510 A.8.15 (traceerbaarheid) en de
   "Security code review & kwetsbaarheden"-rubriek (auditrapport) als bewijs verwacht.
2. Onderhoudbaarheidsmetrieken (complexiteit, duplicatie, code smells) horen thuis bij
   de **maintainability**-toolchain (SonarCloud, zie §5.2 van
   [secure pipelines](./02.md#5-sast-tooling)), niet bij de SAST-security-scan. Door
   beide doelen in één tool/suite te combineren ontstaat overlap zonder dat er een
   eigenaar is voor de quality-signalen.
3. CodeQL-alerts worden gebruikt als *required status check* voor `main`
   (zie §4.1 van [secure pipelines](./02.md#41-branch-protection--main)). Een suite die
   voor 98% uit niet-security findings bestaat maakt die gate ondoorzichtig: een falende
   check kan een quality-nit zijn in plaats van een kwetsbaarheid.

## Decision

We scopen de CodeQL-workflow naar `queries: security-extended`
([`.github/workflows/codeql.yml:55`](../../../.github/workflows/codeql.yml)).

`security-extended` behoudt en verbreedt de security/CWE-georiënteerde queries
(inclusief de queries die de 16 echte bevindingen opleveren — `user-controlled-bypass`,
`xss`, `log-injection`, `tainted-arithmetic`, `sensitive-log`, `overly-large-range`),
maar laat de pure stijl/quality-queries weg.

Onderhoudbaarheidsmetrieken (complexity, duplicatie, code smells, coverage) blijven de
verantwoordelijkheid van **SonarCloud** (§5.2), dat hier al specifiek voor is ingericht
en een eigen quality gate levert.

## Consequences

**Positief:**

- De Security-tab gaat van ~739 naar een hanteerbaar aantal alerts, gedomineerd door
  daadwerkelijke security-bevindingen. Dit maakt de tab weer bruikbaar als
  triage-/bewijsbron voor de "Security code review & kwetsbaarheden"-rubriek.
- De CodeQL-required-status-check op `main` wordt weer betekenisvol: een falende check
  betekent een (potentiële) kwetsbaarheid, geen stijlkwestie.
- Eén tool per concern: CodeQL = security SAST, SonarCloud = maintainability/quality.
  Geen overlappende eigenaarschap van dezelfde signalen.

**Negatief / trade-offs:**

- We verliezen de quality-queries uit `security-and-quality` (bv. boxed-type-vergelijkingen,
  collection-misuse-patronen). Dit is acceptabel omdat SonarCloud (§5.2) deze klasse van
  bevindingen al dekt vanuit het maintainability-perspectief — maar dit moet wel kloppen:
  als SonarCloud's quality gate niet (nog) als required check staat (zie openstaand punt
  #5 in [secure pipelines §9](./02.md#9-openstaande-punten-todos)), is er een tijdelijk
  gat in quality-coverage totdat dat is opgelost.
- Bestaande open CodeQL-alerts die uitsluitend door `-quality`-queries zijn gegenereerd
  sluiten niet bij het mergen van deze configuratiewijziging zelf — pas bij de
  **eerstvolgende CodeQL-scan op `main`** (push of weekly schedule) herproduceert
  CodeQL die queries niet meer, en sluit GitHub de bijbehorende alerts automatisch.
  Tot die scan gedraaid heeft, blijven de 723 quality-alerts zichtbaar in de Security-tab.

## Alternatieven overwogen

1. **Suite behouden, filteren op severity (Error/Warning) in de Security-tab-UI.**
   Verworpen: zoals hierboven aangetoond is severity geen betrouwbare scheidslijn — 4
   quality-findings hebben severity `error` en 4 security-findings hebben severity
   `warning`. Een severity-filter zou dus zowel quality-ruis doorlaten als echte
   security-findings verbergen.
2. **Alle `-quality`-only alerts in bulk dismissen ("won't fix" / "used in tests").**
   Verworpen: dit is een eenmalige opschoning van symptomen, geen structurele
   oplossing — elke nieuwe scan zou de 723 quality-findings opnieuw genereren en
   moeten worden afgewezen.
3. **Custom query-configuratie met `query-filters` (CodeQL config-bestand) op
   basis van CWE/tags.** Functioneel gelijkwaardig aan optie voor `security-extended`,
   maar vereist een apart `codeql-config.yml` en onderhoud van een eigen
   include/exclude-lijst. Verworpen ten gunste van de kant-en-klare, door GitHub
   onderhouden `security-extended`-suite, die exact dezelfde scheiding (security-tags)
   toepast zonder eigen onderhoudslast.

## Validatie (na eerstvolgende scan op `main`)

| Meting | Voor (`security-and-quality`) | Na (`security-extended`) |
|---|---|---|
| Totaal open CodeQL-alerts | 739 | _TBD — vul in na scan, datum + run-link_ |
| Security-findings behouden (16/16)? | n.v.t. | _TBD — met name `overly-large-range` (cwe-020) verifiëren, dit is qua naam de meest twijfelachtige van de 6 query-types_ |
| Quality-alerts gesloten | 0 | _TBD_ |

> **TODO:** invullen zodra de CodeQL-workflow met `security-extended` op `main` heeft
> gedraaid. Dit sluit de detect → decide → verify-cyclus voor deze ADR.

## Gerelateerd

- [Secure Pipelines §5.1 (CodeQL)](./02.md#51-codeql--security-sast) en §8
  (NEN-7510 compliance-overzicht) zijn bijgewerkt om naar `security-extended` te
  verwijzen.
- De 16 resterende echte findings worden getrieerd in
  [`docs/security/04-code-review/`](../04-code-review/README.md); een deel ervan
  (log-injection in `ClearDbCacheController2_0` / `SearchIndexController2_0`) is direct
  gemitigeerd, zie [Mitigatie & validatie](../06-mitigatie-en-validatie/README.md).
