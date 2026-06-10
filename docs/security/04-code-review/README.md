# Security code review & kwetsbaarheden

Rubric: *"Verbeteronderzoek security"* — **Security code review & kwetsbaarheden** (15 pt).
NEN-7510 controls: **A.8.25** (veilige ontwikkelcyclus), **A.8.28** (veilig coderen), **A.8.29** (beveiligingstests).

---

## 1. Methodologie

> **TODO:** Beschrijf hoe de code review uitgevoerd is:
> - Welke tools ingezet (CodeQL, SonarCloud, AI-tooling)?
> - Welke bestanden / klassen handmatig onderzocht?
> - Hoe zijn de tools ingezet — welke query-suites, welke prompts bij AI?
>
> Dit is vereist voor het rubric-onderdeel "gebruik van (AI-)tooling".

---

## 2. Bevindingen

> **TODO:** Documenteer per kwetsbaarheid (minimaal de SonarCloud + CodeQL bevindingen):
>
> Per bevinding opnemen:
> - Bestandsnaam + regelnummer
> - Beschrijving van het probleem (wat is er mis en waarom)
> - OWASP Top 10 (2021) classificatie
> - NEN-7510 control
> - **Risico bij niet-oplossen** — onderbouwd met een valide externe bron (OWASP, CWE, NVD)
>
> Startpunten voor bevindingen:
> - SonarCloud-dashboard → Security Hotspots + Reliability issues
> - GitHub Security-tab → CodeQL-alerts
> - Handmatige review van `AuthorizationFilter.java`, `SessionController1_9.java`, `BaseDelegatingResource.java` (zie ook risicomatrix in [`01-security-audit/01.md §4`](../01-security-audit/01.md#4-risico-evaluatie) voor SQ1–SQ6 als startpunt)

### 2.1 Bevinding 1 — *(titel)*

> **TODO:** Invullen.

### 2.2 Bevinding 2 — *(titel)*

> **TODO:** Invullen.

### 2.3 Bevinding 3 — *(titel)*

> **TODO:** Invullen. *(Voeg zoveel bevindingen toe als gevonden.)*

---

## 3. Prioriteringsoverzicht

> **TODO:** Na het documenteren van alle bevindingen, een samenvattende prioriteringstabel maken:

| ID | Bevinding | OWASP | Ernst | NEN-7510 control |
|----|-----------|-------|-------|-----------------|
| *(toe te voegen)* | | | | |

---

## 4. Bijlagen

> **TODO:**
> - [ ] Screenshot of export van **GitHub Security-tab** (CodeQL SARIF-resultaten) toevoegen als bewijs
> - [ ] Screenshot of PDF van **SonarCloud-dashboard** toevoegen (hotspots, reliability rating)
