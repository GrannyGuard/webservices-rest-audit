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

Ondersteunt **NEN-7510 A.8.3 (toegangsbeveiliging)**: niemand kan rechtstreeks
naar `main` pushen, elke wijziging gaat via een reviewde pull request.

| Regel | Waarde | Reden |
|-------|--------|-------|
| Require a pull request before merging | ✅ | Geen directe pushes; elke change is reviewbaar. |
| Required approving reviews | **1** | Tweede persoon moet goedkeuren. |
| Dismiss stale approvals on new commits | ✅ | Nieuwe push laat oude approval vervallen. |
| Require conversation resolution | ✅ | Alle review-threads moeten opgelost zijn. |
| Require code owner review | ❌ | Nog geen `CODEOWNERS`. |
| Require signed commits | ❌ | Niet vereist voor dit project. |
| Require linear history | ❌ | Merge-commits toegestaan. |
| Allow force pushes | ❌ | History op `main` kan niet herschreven worden. |
| Allow deletions | ❌ | `main` kan niet verwijderd worden. |
| Include administrators (`enforce_admins`) | ❌ | Admins mogen in noodgevallen bypassen — zie noot. |

**Noot over administrators:** `enforce_admins` staat **uit** zodat het 2-persoons
team niet buitengesloten raakt als een reviewer afwezig is. Zet op `true` voor een
striktere, productie-achtige policy.

### Reproduceren

```bash
gh api -X PUT repos/GrannyGuard/webservices-rest-audit/branches/main/protection \
  --input - <<'JSON'
{
  "required_status_checks": null,
  "enforce_admins": false,
  "required_pull_request_reviews": {
    "required_approving_review_count": 1,
    "dismiss_stale_reviews": true,
    "require_code_owner_reviews": false
  },
  "restrictions": null,
  "required_linear_history": false,
  "allow_force_pushes": false,
  "allow_deletions": false,
  "required_conversation_resolution": true
}
JSON
```

Huidige instellingen bekijken: `gh api repos/GrannyGuard/webservices-rest-audit/branches/main/protection`
of via **Settings → Branches → Branch protection rules → `main`**.

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
