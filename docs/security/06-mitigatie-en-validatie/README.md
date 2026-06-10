# Mitigatie & validatie verbeteringen

Rubric: *"Verbeteronderzoek security"* — **Mitigatie & validatie verbeteringen** (20 pt).

---

## 1. CWE-117 Log injection

**Bevinding:** [04-code-review §2.1 — F1](../04-code-review/README.md#21-bevinding-1--log-injection-cwe-117-via-rest-request-parameters)
(CodeQL `java/log-injection`, 9 alerts, `security-extended` suite — zie
[ADR-001](../02-secure-pipelines/adr-001-codeql-query-suite-scope.md)).

**Mitigatie:**
- Nieuwe gedeelde helper `RestUtil.sanitizeForLog(String)`
  (`omod-common/.../web/RestUtil.java`) verwijdert `\r`/`\n` uit user-controlled
  waarden vóórdat ze in een logregel terechtkomen — dit is het door CodeQL
  herkende sanitization-patroon voor `java/log-injection` (taint-barrier).
- Toegepast op alle 8 instromende call-sites in
  `RestUtil.getIntegerParam`, `ClearDbCacheController2_0` (×3),
  `SearchIndexController2_0` (×3) en `ConceptResource1_8`.
- **Realisatie/tooling:** geïmplementeerd met Claude Code (Sonnet 4.6) — de
  CodeQL-alertlijst (via `gh api .../code-scanning/alerts`) is gebruikt om de
  exacte bestanden/regels te identificeren; de fix zelf (één centrale sanitizer
  + toepassing op de call-sites) is handmatig gereviewd voordat gecommit.
- **Commit:** `831dafb` — `fix(security): sanitize user input before logging
  (CWE-117 log injection)`

**Validatie (voor/na):**

Bron: [`codeql.yml` op `main`, commit `e476489`, 2026-06-10](https://github.com/GrannyGuard/webservices-rest-audit/actions/runs/27280969725)
(`gh api repos/.../code-scanning/alerts`, gefilterd op `rule.id == "java/log-injection"`).

| | Vóór | Na |
|---|---|---|
| Open `java/log-injection`-alerts | 9 | **0** |
| Alerts auto-gesloten door GitHub (`state: fixed`, 2026-06-10T13:55:43Z) | n.v.t. | **9/9** — alert-nrs `7`–`15` |

| Alert # | Bestand:regel | Status |
|---:|---|---|
| 7 | `ClearDbCacheController2_0.java:79` | ✅ fixed |
| 8 | `ClearDbCacheController2_0.java:86` | ✅ fixed |
| 9 | `ClearDbCacheController2_0.java:95` | ✅ fixed |
| 10 | `SearchIndexController2_0.java:77` | ✅ fixed |
| 11 | `SearchIndexController2_0.java:81` | ✅ fixed |
| 12 | `SearchIndexController2_0.java:81` | ✅ fixed |
| 13 | `ConceptResource1_8.java:567` | ✅ fixed |
| 14 | `RestUtil.java:521` | ✅ fixed |
| 15 | `AuthorizationFilter.java:108` | ✅ fixed |

Alle 9 oorspronkelijke `java/log-injection`-findings zijn gesloten, inclusief het
9e alert in `AuthorizationFilter.java:108` (de regel die in commit `8521cdc` voor
issue #65 al was herschreven naar parameterized logging — die herschrijving plus de
`sanitizeForLog`-call op `attemptedUser` bleek samen voldoende voor CodeQL om de
taint-flow als gemitigeerd te beschouwen).

> **Restpunt:** dezelfde scan toont 4 *nieuwe* `java/log-injection`-alerts in
> `AuthorizationFilter.java` (regels 72, 84, 115, 122 — alert-nrs `808`–`811`),
> ontstaan door de logging-herschrijving voor issue #65. Deze zijn op 2026-06-08
> door @BaasW als **"false positive"** gedismissed (geen `dismissed_comment`). Voor
> de audit-traceability moet dit nog onderbouwd worden: waaróm zijn dit
> false positives (bv. omdat de waarden al via `sanitizeForLog`/parameterized
> logging lopen)? Zie ook [F1-restpunt in 04-code-review](../04-code-review/README.md#21-bevinding-1--log-injection-cwe-117-via-rest-request-parameters).

✅ Hiermee is de detect → triage → mitigeer → verifieer-cyclus voor F1 gesloten en
kwantitatief aangetoond (9 → 0 open alerts).

---

## TODO's

- [ ] **Mitigaties documenteren** — per kwetsbaarheid uit [`05-penetration-tests`](../05-penetration-tests/) beschrijven welke maatregel is genomen en hoe die is gerealiseerd
- [ ] **Gebruik van (AI-)tooling verantwoorden** — beschrijven hoe tooling ingezet is bij de realisatie
- [ ] **Hertests uitvoeren** — dezelfde pentests herhalen ná de mitigaties en de resultaten vastleggen (vereist voor "Voldoende")
- [ ] **Aantonen dat risico's verlaagd zijn** — voor/na vergelijking per bevinding
- [ ] **Kritische reflectie** schrijven op de gekozen mitigaties en tooling (vereist voor "Goed")
- [ ] **Kwantitatief aantonen** dat de verbetering reproduceerbaar is (vereist voor "Goed")
