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

| | Vóór | Na |
|---|---|---|
| Open `java/log-injection`-alerts | 9 | _TBD — vul in na eerstvolgende CodeQL-scan op `main`_ |
| Alerts auto-gesloten door GitHub | n.v.t. | _TBD — alert-nummers + datum noteren_ |

> **TODO:** zodra de CodeQL-workflow (`security-extended`) opnieuw op `main`
> draait, hier de alert-IDs invullen die zijn gesloten en een screenshot/links
> toevoegen als bewijs. Het 9e alert (`AuthorizationFilter.java`, oorspr.
> regel 108) is niet door deze fix geraakt — apart valideren of de
> elders-herschreven logging daar nog steeds flagt (zie
> [ADR-001 §Validatie](../02-secure-pipelines/adr-001-codeql-query-suite-scope.md#validatie-na-eerstvolgende-scan-op-main)).
> **Niet als "afgerond" markeren totdat de alerts daadwerkelijk gesloten zijn.**

---

## TODO's

- [ ] **Mitigaties documenteren** — per kwetsbaarheid uit [`05-penetration-tests`](../05-penetration-tests/) beschrijven welke maatregel is genomen en hoe die is gerealiseerd
- [ ] **Gebruik van (AI-)tooling verantwoorden** — beschrijven hoe tooling ingezet is bij de realisatie
- [ ] **Hertests uitvoeren** — dezelfde pentests herhalen ná de mitigaties en de resultaten vastleggen (vereist voor "Voldoende")
- [ ] **Aantonen dat risico's verlaagd zijn** — voor/na vergelijking per bevinding
- [ ] **Kritische reflectie** schrijven op de gekozen mitigaties en tooling (vereist voor "Goed")
- [ ] **Kwantitatief aantonen** dat de verbetering reproduceerbaar is (vereist voor "Goed")
