# Assignment context: LU2 Verbeteronderzoek (Software Architectuur & -kwaliteit)

This repo is a 4-sprint group assignment (ATIx IN-B2.4 Softwarearchitectuur & -kwaliteit
2025-26 P4, group "LU2 Groep 17"): a Secure-SDLC audit and improvement of the chosen
OpenMRS REST webservices module, measured against NEN-7510:2024.

Two separate deliverables are submitted and graded independently (each /100, "voldoende"
threshold = 55 points):

1. **"Verbeteronderzoek security"** — the improvement-advice document (audit report).
2. **"Verbeteronderzoek onderhoudbaarheid"** — the application / proof-of-concept (PoC).

**Due:** 2026-06-19 23:59. Hard cutoff: 2026-06-20 00:30 (access to submission closes).

---

## Sprint breakdown

### Sprint 1 (lesweek 5/6) — Gap analyse & projectorganisatie
- **Gap analyse (WS1):**
  - Vind en bestudeer drie controls in de NEN-7510-2 norm die van toepassing zijn op het project.
  - Onderzoek in de OpenMRS Wiki/GitHub/broncode of en hoe deze controls zijn geïmplementeerd.
  - Bepaal wat er moet gebeuren om compliant te zijn t.o.v. de drie controls.
- **Inrichten projectorganisatie (WS2):**
  - Minimaal test- en productieomgevingen, gescheiden configuratie en secrets.
  - GitHub Environments (test + productie) met protection rules + approval gates.
  - NEN-7510 controls voor CI-CD inrichten.
  - README.md met: hoe omgevingen zijn ingericht, hoe wordt voorkomen dat testdata in
    productie terechtkomt, hoe een nieuwe ontwikkelaar ermee kan werken.

### Sprint 2 (lesweek 6/7) — Risico's, threat modelling & CI/CD scanning
- CIA/BIV-analyse: kroonjuwelen (met referenties), risicocriteria (score schaal,
  risicobereidheid, grenswaarden).
- Threat model + C4-diagrammen (context- en containerniveau, level 0/1).
- Risicomatrix met gevonden risico's; bow-tie voor de hoogste risico's
  (preventieve + correctieve maatregelen).
- Risico-evaluatie van het CI/CD-proces (risicomatrix + bow-tie voor meest kritieke risico).
- CI/CD-tooling inrichten: SAST, SBOM, SCA — incl. omgang met false positives.
- Geprioriteerde security requirements / security backlog o.b.v. gevonden risico's.
- Pentest plannen en uitvoeren (focus op hoogste risico's), bevindingen vastleggen,
  besluiten welke worden opgelost (met onderbouwing).
- Risk Assessment Report: o.b.v. scanresultaten + security backlog, welke gevoelige
  gegevens worden verwerkt (met referenties), mitigatie + koppeling aan NEN-7510:2024-2
  maatregel per kwetsbaarheid, kostenraming (resources/tijd/budget, indicatief).

### Sprint 3 (lesweek 7) — Attack surface, logging & coverage
- **Attack Surface Mapping:** identificeer/documenteer alle ingangen tot de module,
  werk threat models bij, markeer "high risk" ingangen, neem trust op (wat wordt
  impliciet vertrouwd). Deliverables: bijgewerkt threat model + attack surface overzicht.
- **Gap analyse 'logging'** (her-evalueer Sprint 1 versie indien aanwezig):
  inventariseer logging in de module; overzicht gekoppeld aan attack surface, bv.
  `Event | Gelogd? | Gevoelige data | Compliant met NEN-7510 8.15?`. Vooral
  niet-gelogde gebeurtenissen zijn interessant. Documenteer het gat tussen huidig
  en gewenst.
- **Logging compleet maken** o.b.v. de gap analyse: compliant aan NEN-7510 8.15,
  let op gevoelige data.
- **Tests voor logging:** succesvolle én mislukte acties, afwezigheid van gevoelige
  gegevens (voor zover mogelijk), alle tests slagen.
- **Code coverage:** configureren en activeren, coverage-% bepalen en onderbouwen,
  coverage-rapport als artefact uit het CI-proces (bv. GH Action).

### Sprint 4 (lesweek 8) — Afronding & rapportage
- **Traceability matrix:** ten minste 3 NEN-7510:2024 controls, elk met een
  traceerbaar bewijsartefact.
- **Audit rapport** met secties:
  - Executive Summary
  - Scope en Context
  - Audit Methodologie
  - Risico-analyse en ten minste 4 bevindingen
  - SBOM en Supply Chain Security
  - Conclusie en Advies
  - Bijlagen: traceability matrix, SBOM (bv. CycloneDX JSON), SAST-output (bv. CodeQL/Snyk),
    risicomatrix, bow-tie diagrammen / threat models, Snyk-rapport, CRA-mapping, overige bewijsvoering.
- Leg ook vast welke items niet gedaan zijn, met onderbouwing.

---

## Grading rubrics

### Rubric 1 — "Verbeteronderzoek Security Beroepsproduct" (the audit document, /100, voldoende ≥ 55)

| Criterium | Onvoldoende (0) | Voldoende | Goed |
|---|---|---|---|
| Security audit (wetgeving & normen) | Resultaten t.a.v. NEN7510 ontbreken/onduidelijk, geen onderbouwd advies | **11** — Gap naar volledige implementatie van relevante NEN7510-2 controls vastgelegd; advies o.b.v. geprioriteerde lijst non-compliancies (risico-inschatting) | **20** — + audit is grondig, herleidbaar, onderbouwd met bronnen/normen; prioritering en aanpak expliciet gemotiveerd |
| Secure pipelines | Pipelines niet secure / OTAP onvoldoende gescheiden, documentatie onvoldoende | **8** — Pipelines secure incl. OTAP-scheiding; documentatie verantwoordt keuzes en geeft inzicht in maatregelen | **15** — + verantwoorde keuze voor niet-herleidbare data in de OTAP-omgevingen |
| Advies updates (SBOM, CVE en CVSS) | Analyse afhankelijkheden ontbreekt/onvolledig, geen onderbouwd update-advies | **8** — Duidelijke (machine)bruikbare SBOM; bruikbaar onderbouwd advies over updates o.b.v. SBOM/CVE/CVSS | **15** — + duidelijke prioritering, afweging impact/risico's, concrete implementatie-aanbevelingen |
| Security code review & kwetsbaarheden | Reviews ontbreken/onvoldoende, kwetsbaarheden niet vastgelegd/onderbouwd | **9** — Reviews uitgevoerd met (AI-)tooling; kwetsbaarheden duidelijk vastgelegd, onderbouwd, geprioriteerd; risico's bij niet-oplossen beschreven o.b.v. valide bronnen | **15** — + analyse diepgaand, goed onderbouwde risico-inschattingen, duidelijke relatie kwetsbaarheid↔systeemgebruik |
| Penetration tests (aantonen kwetsbaarheden) | Ontbreken/niet navolgbaar, misbruik niet aantoonbaar | **8** — Pentests uitgevoerd en navolgbaar gedocumenteerd; misbruik van meest kritische kwetsbaarheden aangetoond | **15** — + systematisch opgezet, reproduceerbaar, diepgaand inzicht in exploitatie en impact |
| Mitigatie & validatie verbeteringen | Niet gemitigeerd / niet aantoonbaar effectief, geen verantwoording | **11** — Kwetsbaarheden gemitigeerd; met pentests aangetoond dat risico's verlaagd zijn; realisatie verantwoord incl. (AI)tooling-gebruik | **20** — + verbetering kwantitatief en reproduceerbaar aangetoond; kritische reflectie op gekozen mitigaties en tooling |

→ Highest leverage: **Security audit** (20) and **Mitigatie & validatie verbeteringen** (20).

### Rubric 2 — "Verbeteronderzoek Onderhoudbaarheid Beroepsproduct" (the application/PoC, /100, voldoende ≥ 55)

| Criterium | Onvoldoende (0) | Voldoende | Goed |
|---|---|---|---|
| Analyse onderhoudbaarheid | Resultaten ontbreken/onduidelijk, niet bruikbaar | **11** — Resultaten van systematische analyse vastgelegd, grotendeels begrijpelijk/bruikbaar; enkele metrieken gebruikt (mogelijk niet alle van toepassing) | **20** — + analyse diepgaand, gestructureerd, onderbouwd met passende metrieken, direct toepasbaar |
| Testopzet en testresultaten | Tests ontbreken/onvoldoende uitgewerkt, resultaten slecht vastgelegd | **11** — Relevante tests opgesteld en uitgevoerd; resultaten duidelijk en bruikbaar vastgelegd | **20** — + teststrategie uitgebreid (meerdere testtypen), resultaten reproduceerbaar en goed onderbouwd |
| Verbeteringen (prioritering en onderbouwing) | Verbeteringen ontbreken/niet geprioriteerd/onvoldoende onderbouwd, geen relatie met analyse/tests | **6** — Verbeteringen geprioriteerd en onderbouwd o.b.v. eerdere analyse en testresultaten | **10** — + prioritering expliciet onderbouwd met heldere criteria (bv. impact, effort) en consistente verwijzing naar analyse/meetgegevens |
| Aangepast ontwerp | Ontbreekt/onduidelijk, onvoldoende gerelateerd aan verbeteringen, niet onderbouwd met principes/patronen | **11** — Duidelijk en bruikbaar aangepast ontwerp, onderbouwd met relevante ontwerpprincipes/patronen/refactoringpatronen | **20** — + ontwerp toont doordachte keuzes, bespreekt alternatieven en motiveert o.b.v. kwaliteitseisen |
| Realisatie (PoC) & verantwoording | PoC ontbreekt of komt niet overeen met ontwerp; toolinggebruik niet/nauwelijks verantwoord | **5** — Verbeteringen gerealiseerd in PoC grotendeels overeenkomstig ontwerp; (AI)tooling-gebruik beschreven en verantwoord | **10** — + kritische reflectie op toolinggebruik; PoC overeenkomstig ontwerp |
| Validatie verbeteringen (testen & regressie) | Niet goed aangetoond dat onderhoudbaarheid verbeterd is of regressie vermeden | **11** — Met testen aangetoond dat onderhoudbaarheid verbeterd is en geen regressie is opgetreden | **20** — + verbetering sterk onderbouwd (bv. met metrieken) en reproduceerbaar aangetoond |

→ Highest leverage: **Analyse onderhoudbaarheid** (20), **Testopzet en testresultaten** (20),
**Aangepast ontwerp** (20), **Validatie verbeteringen** (20). PoC realization itself is
worth comparatively little (10) — invest more in analysis, design rationale, and validation evidence.

---

## How to use this when working in the repo

Frame every piece of work in terms of: **which deliverable does this serve, and which
NEN-7510 control / rubric criterion does it provide traceable evidence for?** Prioritize
effort toward the 20-point criteria in both rubrics over lower-weighted ones (e.g. raw
PoC realization is only 10 points — analysis, design rationale, and validation evidence
matter more for the grade).
