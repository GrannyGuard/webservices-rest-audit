# 5.2 — Repository inrichten

> **Sprint-doel:** Onze GitHub-omgeving staat klaar en we hebben aantoonbaar
> gemaakt waar onze module staat t.o.v. NEN-7510.
>
> **Voorbeeld output (bewijs):** GitHub **Security-tab toont geen rode vlaggen**.

Dit document beschrijft de vier maatregelen van taak 5.2 en het bijbehorende bewijs.

| # | Maatregel | Issue | Status |
|---|-----------|-------|--------|
| 1 | Branch protection (`main` is protected) | #2  | ✅ Actief |
| 2 | MFA voor alle leden                     | #8  | ⬜ TODO (Wouter) |
| 3 | Dependabot alerts aan                   | #9  | ⬜ TODO (Wouter) |
| 4 | Statische code-analyse (SAST): CodeQL **+** SonarCloud | #10 | ✅ 2 workflows |

---

## 1. Branch protection — `main` (#2)

`main` wordt beschermd via een **Ruleset** ("main protection", *Settings → Rules →
Rulesets*) — de moderne opvolger van classic branch protection, met betere
audit-zichtbaarheid. Ondersteunt **NEN-7510 A.8.3 (toegangsbeveiliging)**: niemand
kan rechtstreeks naar `main` pushen, elke wijziging gaat via een reviewde pull request.

| Regel (ruleset) | Waarde | Reden |
|-----------------|--------|-------|
| `pull_request` — required approving reviews | **1** | Geen directe pushes; tweede persoon moet goedkeuren. |
| `pull_request` — dismiss stale reviews on push | ✅ | Nieuwe push laat oude approval vervallen. |
| `pull_request` — required review thread resolution | ✅ | Alle review-threads moeten opgelost zijn. |
| `pull_request` — require code owner review | ❌ | Nog geen `CODEOWNERS`. |
| `non_fast_forward` | ✅ | Force pushes geblokkeerd; history op `main` onveranderbaar. |
| `deletion` | ✅ | `main` kan niet verwijderd worden. |
| Required status checks | ❌ (nog niet) | Toevoegen zodra CodeQL/SonarCloud één keer gedraaid hebben — zie §"Toekomstige hardening". |

**Noot over bypass:** de repository-rol **Admin** staat als *bypass actor*
(`bypass_mode: always`), zodat het 2-persoons team niet buitengesloten raakt als
een reviewer afwezig is. Verwijder de bypass-actor voor een striktere policy.

### Reproduceren

```bash
gh api -X POST repos/GrannyGuard/webservices-rest-audit/rulesets --input - <<'JSON'
{
  "name": "main protection",
  "target": "branch",
  "enforcement": "active",
  "conditions": { "ref_name": { "include": ["~DEFAULT_BRANCH"], "exclude": [] } },
  "bypass_actors": [
    { "actor_id": 5, "actor_type": "RepositoryRole", "bypass_mode": "always" }
  ],
  "rules": [
    { "type": "deletion" },
    { "type": "non_fast_forward" },
    { "type": "pull_request", "parameters": {
        "required_approving_review_count": 1,
        "dismiss_stale_reviews_on_push": true,
        "require_code_owner_review": false,
        "require_last_push_approval": false,
        "required_review_thread_resolution": true,
        "allowed_merge_methods": ["merge", "squash", "rebase"]
    } }
  ]
}
JSON
```

Effectieve regels op `main` bekijken: `gh api repos/GrannyGuard/webservices-rest-audit/rules/branches/main`
of via **Settings → Rules → Rulesets → "main protection"**.

---

## 2. MFA voor alle leden (#8)

> ⬜ **TODO (Wouter, #8)** — documenteer hier:
> - Waar MFA is afgedwongen (org-niveau / repo-toegang).
> - Bevestiging dat alle leden (Wouter, Daniël, Wassim) 2FA actief hebben.
> - **Bewijs:** screenshot van het org members-overzicht met 2FA-status.

---

## 3. Dependabot alerts (#9)

> ⬜ **TODO (Wouter, #9)** — documenteer hier:
> - Dependabot **alerts** + **security updates** ingeschakeld in Security-settings.
> - `.github/dependabot.yml` aanwezig en geldig (maven / github-actions / docker).
> - **Bewijs:** screenshot van de Dependabot-instellingen / Security-tab.

---

## 4. Statische code-analyse (SAST) (#10)

We zetten **twee** SAST-tools in die elkaar aanvullen — samen dekken ze de
security- én maintainability-acceptatiecriteria uit #7.

### 4a. CodeQL — security-SAST

Diepe security-analyse (dataflow/taint) voor Java/Maven; resultaten in de
**Security-tab** (Code scanning alerts).

- Workflow: [`.github/workflows/codeql.yml`](../.github/workflows/codeql.yml)
- Triggers: push/PR op `main` + wekelijkse schedule.
- **Bewijs:** Code scanning resultaten in de Security-tab.

### 4b. SonarCloud — SAST + maintainability/quality gate

Analyseert zowel security-issues als maintainability (code smells, duplicatie,
coverage) en levert een **quality gate** op elke PR.

- Workflow: [`.github/workflows/sonarcloud.yml`](../.github/workflows/sonarcloud.yml)
- Org / project: `grannyguard` / `GrannyGuard_webservices-rest-audit`.
- Vereist secret `SONAR_TOKEN` en **Automatic Analysis uit** in SonarCloud.
- **Bewijs:** quality gate-status op de PR + SonarCloud-dashboard.

---

## Bewijs (eindcheck 5.2)

De gecombineerde maatregelen zorgen dat de **GitHub Security-tab geen rode
vlaggen** toont. Voeg per maatregel een screenshot toe in de auditmap als bewijs
voor het sprintverslag.
