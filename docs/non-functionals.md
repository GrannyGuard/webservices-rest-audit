# Non-functionele eisen — webservices.rest audit

> Dit document legt de kwaliteitseisen vast voor **security** en
> **onderhoudbaarheid** die gelden voor dit auditproject. Ze dienen als
> **acceptatiecriteria** voor pull requests en sprint-reviewmomenten.

---

## 1. Security-kwaliteitseisen

### 1.1 Statische code-analyse (SAST)

| # | Eis | Tool | Drempelwaarde | Meting |
|---|-----|------|---------------|--------|
| S-1 | Geen nieuwe **Critical** of **High** CodeQL-bevindingen op `main` | CodeQL | 0 nieuwe bevindingen | GitHub Security-tab → Code scanning |
| S-2 | Geen nieuwe **Blocker** of **Critical** SonarCloud security-hotspots onopgelost | SonarCloud | 0 onbehandeld | SonarCloud dashboard → Security Hotspots |
| S-3 | CodeQL-scan geslaagd (geen build-fout) | CodeQL | Build exit 0 | GitHub Actions log |

### 1.2 Afhankelijkheden (SBOM / CVE)

| # | Eis | Tool | Drempelwaarde | Meting |
|---|-----|------|---------------|--------|
| S-4 | SBOM wordt aangemaakt als CI-artifact op elke push naar `main` | CycloneDX Maven plugin | Artifact aanwezig | `.github/workflows/sbom.yml` |
| S-5 | Geen niet-beoordeelde Dependabot-alerts met CVSS ≥ 9.0 ("Critical") ouder dan 7 dagen | Dependabot | 0 open critical > 7 d | GitHub Security → Dependabot alerts |
| S-6 | Geen niet-beoordeelde Dependabot-alerts met CVSS 7.0–8.9 ("High") ouder dan 30 dagen | Dependabot | 0 open high > 30 d | GitHub Security → Dependabot alerts |

### 1.3 Repository- en pipelinebeveiliging

| # | Eis | Maatregel | Bewijs |
|---|-----|-----------|--------|
| S-7 | `main` is beschermd — geen directe pushes | GitHub Ruleset "main protection" | `docs/repository-inrichting.md` §1 |
| S-8 | Elke code-wijziging vereist ≥ 1 goedgekeurde review | `pull_request.required_approving_review_count: 1` | Ruleset in GitHub Settings |
| S-9 | MFA verplicht voor alle organisatieleden | GitHub org MFA-policy | `docs/repository-inrichting.md` §2 |

---

## 2. Onderhoudbaarheid-kwaliteitseisen

### 2.1 SonarCloud Quality Gate

De SonarCloud **Quality Gate** geeft een geaggregeerd oordeel per PR. De volgende
drempelwaarden gelden als acceptatiecriterium:

| Metric | Drempelwaarde | Toelichting |
|--------|---------------|-------------|
| Reliability rating (nieuwe code) | **A** | Geen nieuwe Bugs |
| Security rating (nieuwe code) | **A** | Geen nieuwe Vulnerabilities |
| Maintainability rating (nieuwe code) | **A** | Code smells ≤ 5% van de technische schuld |
| Coverage (nieuwe code) | ≥ **60 %** | Ondergrens voor aangepaste klassen |
| Duplications (nieuwe code) | ≤ **3 %** | Beperkt copy-paste |

> **Let op:** de huidige `main`-branch scoort nog **D op reliability** en heeft
> 3 security hotspots. Deze moeten opgelost worden voordat SonarCloud als
> required status check wordt toegevoegd (zie `docs/repository-inrichting.md` §4).

### 2.2 Cyclomatische complexiteit

| # | Eis | Drempelwaarde | Tool |
|---|-----|---------------|------|
| M-1 | Geen nieuwe methode met cyclomatische complexiteit > 15 | Max 15 per methode | SonarCloud (Cognitive Complexity) |
| M-2 | Gemiddelde complexiteit per klasse neemt niet toe t.o.v. baseline | ≤ module-gemiddelde 5.8 (huidige staat) | SonarCloud / lokale analyse |

### 2.3 Testdekking

| # | Eis | Drempelwaarde | Tool |
|---|-----|---------------|------|
| M-3 | Nieuwe code in scope van PoC-refactoring heeft unit tests | ≥ 80% line coverage voor gewijzigde klassen | JaCoCo via Maven + SonarCloud |
| M-4 | Geen regressie — bestaande tests mogen niet falen | 0 test failures op `main` | Maven Surefire (`mvn test`) |

### 2.4 Code-stijl en leesbaarheid

| # | Eis | Toelichting |
|---|-----|-------------|
| M-5 | Nieuwe code volgt het `OpenMRSFormatter.xml`-profiel (CheckStyle/IDEA) | Formatter-config in repo-root |
| M-6 | Geen TODO/FIXME-comments geïntroduceerd door ons team in productie-code | Gecontroleerd via SonarCloud "Code smells: minor" |

---

## 3. Acceptatiecriteria per PR (samenvatting)

Een pull request naar `main` is **acceptabel** als:

- [ ] CodeQL-scan geslaagd (geen nieuwe Critical/High bevindingen)
- [ ] SonarCloud Quality Gate geslaagd (zodra als required check geconfigureerd)
- [ ] Geen Dependabot-alert met CVSS ≥ 9.0 geïntroduceerd door de wijziging
- [ ] SBOM-artifact succesvol aangemaakt
- [ ] ≥ 1 goedgekeurde review ontvangen
- [ ] Alle bestaande tests slagen (`mvn test` exit 0)

---

*Vastgelegd: 2026-06-03*  
*Versie: 1.0 (Sprint 1)*  
*Auteur: Wassim Balouda*
