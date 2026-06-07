# CIA/BIV-analyse — OpenMRS REST Webservices Module

**Module:** `org.openmrs.module:webservices.rest` v3.2.0  
**Auteur:** Wassim Balouda  
**Datum:** 2026-06-08  
**Gerelateerd aan:** NEN-7510:2024-2, AVG, WGBO

---

## 1. Inleiding

Deze CIA/BIV-analyse identificeert de kroonjuwelen van de OpenMRS REST Webservices module
en beoordeelt per kroonjuweel de impact op **Vertrouwelijkheid (C)**, **Integriteit (I)**
en **Beschikbaarheid (A)** bij een beveiligingsincident. Op basis hiervan worden
risicocriteria vastgelegd die als fundament dienen voor de risicomatrix en het threat model.

De OpenMRS REST module exposeert medische functionaliteit via een REST API. Ze vormt
de brug tussen externe systemen/clients en de OpenMRS-kern, en verwerkt daarbij
gevoelige (bijzondere) persoonsgegevens in de zin van de AVG en WGBO.

---

## 2. Kroonjuwelen

Kroonjuwelen zijn de informatie-assets die de sterkste bescherming vereisen.
Ze zijn geïdentificeerd op basis van broncode-analyse, de module-keuze-documentatie
en de NEN-7510:2024 norm.

### 2.1 Overzicht

| ID | Kroonjuweel | Omschrijving | Wettelijke grondslag |
|----|-------------|--------------|----------------------|
| **KJ1** | Patiëntidentificatiegegevens | Naam, geboortedatum, BSN, adres van patiënten | AVG Art. 9 (bijzondere persoonsgegevens), WGBO |
| **KJ2** | Medische gegevens | Diagnoses, medicatie, observaties, encounters, labwaarden, vitals | AVG Art. 9, WGBO Art. 7:454 |
| **KJ3** | Authenticatiegegevens | Gebruikerswachtwoorden (via HTTP Basic Auth), sessie-tokens, sessie-ID's | NEN-7510 A.8.5, AVG |
| **KJ4** | Autorisatiegegevens | Gebruikersrollen, privileges, toegangsrechten | NEN-7510 A.8.3 |
| **KJ5** | API-interface (REST endpoints) | De beschikbaarheid van de REST API zelf — uitval blokkeert zorgverlening | NEN-7510 A.8.14 |
| **KJ6** | Audit- en beveiligingslogs | Logbestanden van authenticatie, autorisatie en API-toegang | NEN-7510 A.8.15 |
| **KJ7** | CI/CD-secrets en deploymentconfiguratie | GitHub Actions secrets, deployment keys, omgevingsvariabelen | NEN-7510 A.8.3, CRA |

### 2.2 Referenties per kroonjuweel

**KJ1 + KJ2 — Patiënt- en medische gegevens:**
- `omod-common/src/main/java/org/openmrs/module/webservices/rest/web/resource/impl/BaseDelegatingResource.java` — verwerkt alle patiënt- en medische resources via de REST-laag
- `omod/src/main/java/org/openmrs/module/webservices/rest/web/v1_0/controller/openmrs1_9/SessionController1_9.java:169–182` — `/session/diag` endpoint lekt gebruikersrollen zonder autorisatiecheck (gap-analyse A.8.3)
- Wettelijk: AVG Art. 9 lid 1 — medische gegevens zijn "bijzondere categorieën persoonsgegevens"; WGBO Art. 7:454 — dossierplicht en geheimhoudingsplicht

**KJ3 — Authenticatiegegevens:**
- `omod-common/src/main/java/org/openmrs/module/webservices/rest/web/filter/AuthorizationFilter.java:86–114` — HTTP Basic Auth, Base64-gedecodeerde wachtwoorden in geheugen
- Gap: sessie-ID wordt niet ververst na login → sessie-fixatierisico (gap-analyse A.8.5)

**KJ4 — Autorisatiegegevens:**
- `AuthorizationFilter.java:69–75` — `REST_ALLOWED_IPS` property; globale property die IP-gebaseerde toegang regelt
- Gap: geen declaratieve access control op REST-laag (gap-analyse A.8.3)

**KJ5 — API-interface:**
- Geen rate-limiting of brute-force bescherming aanwezig (gap-analyse A.8.5)
- Uitval raakt alle systemen die op de REST API steunen voor zorgverlening

**KJ6 — Audit logs:**
- `AuthorizationFilter.java:108,113` — auth-events gelogd op DEBUG-niveau, onzichtbaar in productie (gap-analyse A.8.15)
- Forensisch audit trail ontbreekt volledig

**KJ7 — CI/CD-secrets:**
- GitHub Actions secrets voor SonarCloud, SBOM-publicatie en deployment
- Risico vastgelegd als CD2 + CD3 in `risico-matrix-full.md`

---

## 3. BIV-classificatie per kroonjuweel

De BIV-classificatie geeft per kroonjuweel aan hoe groot de schade is als één van de
drie aspecten wordt aangetast. Gebruikte schaal: **Laag / Midden / Hoog / Kritiek**.

| ID | Kroonjuweel | Vertrouwelijkheid (V) | Integriteit (I) | Beschikbaarheid (B) | Toelichting |
|----|-------------|----------------------|-----------------|----------------------|-------------|
| **KJ1** | Patiëntidentificatiegegevens | **Kritiek** | **Hoog** | Midden | Lekkage = meldplicht datalek (AVG Art. 33), reputatieschade, boete AP. Wijziging = foutieve zorgverlening. |
| **KJ2** | Medische gegevens | **Kritiek** | **Kritiek** | **Hoog** | Lekkage = schending medisch beroepsgeheim. Wijziging = verkeerde diagnose/medicatie → direct gevaar voor patiënt. Uitval = belemmerde zorgverlening. |
| **KJ3** | Authenticatiegegevens | **Kritiek** | **Hoog** | Midden | Lekkage geeft volledige toegang tot patiëntdata. Sessie-fixatie maakt overname actieve sessie mogelijk. |
| **KJ4** | Autorisatiegegevens | **Hoog** | **Hoog** | Laag | Lekkage via `/session/diag` — rollen en privileges zichtbaar zonder login. Wijziging geeft privilege escalation. |
| **KJ5** | API-interface | Laag | Midden | **Kritiek** | Uitval blokkeert alle zorgverlening via de REST-laag. Geen rate-limiting → kwetsbaar voor DoS. |
| **KJ6** | Audit logs | Midden | **Hoog** | Midden | Ontbreken van logs maakt forensisch onderzoek na incident onmogelijk. NEN-7510 A.8.15 non-compliant. |
| **KJ7** | CI/CD-secrets | **Hoog** | **Hoog** | Midden | Lekkage geeft aanvaller controle over deploymentproces en codebase. Supply chain risico. |

### Prioritering kroonjuwelen

Op basis van de BIV-classificatie heeft **KJ2 (Medische gegevens)** de hoogste prioriteit:
alle drie de BIV-aspecten scoren Hoog of Kritiek. Daarna **KJ1** en **KJ3** als kritieke
vertrouwelijkheidssrisico's.

| Prioriteit | Kroonjuweel | Reden |
|-----------|-------------|-------|
| 1 | KJ2 — Medische gegevens | V=Kritiek, I=Kritiek, B=Hoog. Direct gevaar voor patiëntveiligheid bij schending. |
| 2 | KJ1 — Patiëntidentificatiegegevens | V=Kritiek. AVG-meldplicht + reputatieschade bij lekkage. |
| 3 | KJ3 — Authenticatiegegevens | V=Kritiek. Compromittering geeft directe toegang tot KJ1 + KJ2. |
| 4 | KJ5 — API-interface | B=Kritiek. Uitval raakt alle zorgverlening. |
| 5 | KJ7 — CI/CD-secrets | V=Hoog, I=Hoog. Supply chain impact. |
| 6 | KJ4 — Autorisatiegegevens | V=Hoog, I=Hoog. Privilege escalation mogelijk. |
| 7 | KJ6 — Audit logs | I=Hoog. NEN-7510 compliance vereist. |

---

## 4. Risicocriteria

### 4.1 Scoreschaal

Risico's worden gescoord op twee dimensies. De risicoscore wordt berekend als:

**Risicoscore = Kans × Impact**

**Kansschaal (K):**

| Score | Label | Omschrijving |
|-------|-------|--------------|
| 1 | Zeer onwaarschijnlijk | Incident verwacht minder dan eens per 5 jaar |
| 2 | Onwaarschijnlijk | Incident verwacht eens per 2–5 jaar |
| 3 | Mogelijk | Incident verwacht eens per jaar |
| 4 | Waarschijnlijk | Incident verwacht meerdere keren per jaar |
| 5 | Zeer waarschijnlijk | Incident verwacht regelmatig (maandelijks of vaker) |

**Impactschaal (I):**

| Score | Label | Omschrijving |
|-------|-------|--------------|
| 1 | Verwaarloosbaar | Geen merkbare schade; intern oplosbaar zonder externe gevolgen |
| 2 | Laag | Beperkte schade; geen dataverlies, herstel binnen uren |
| 3 | Midden | Merkbare schade; mogelijke tijdelijke dataverlies of uitval, herstel binnen dagen |
| 4 | Hoog | Ernstige schade; mogelijke AVG-meldplicht, patiëntdata aangetast, uitval zorgverlening |
| 5 | Kritiek | Catastrofale schade; groot datalek medische gegevens, directe patiëntveiligheidsrisico's, hoge boetes |

**Risicoscore matrix:**

| | **K=1** | **K=2** | **K=3** | **K=4** | **K=5** |
|--|---------|---------|---------|---------|---------|
| **I=5** | 5 🟡 | 10 🟠 | 15 🔴 | 20 🔴 | 25 🔴 |
| **I=4** | 4 🟢 | 8 🟡 | 12 🔴 | 16 🔴 | 20 🔴 |
| **I=3** | 3 🟢 | 6 🟡 | 9 🟠 | 12 🔴 | 15 🔴 |
| **I=2** | 2 🟢 | 4 🟢 | 6 🟡 | 8 🟡 | 10 🟠 |
| **I=1** | 1 🟢 | 2 🟢 | 3 🟢 | 4 🟢 | 5 🟡 |

**Legenda:**

| Kleur | Score | Classificatie |
|-------|-------|---------------|
| 🟢 Groen | 1–4 | Laag |
| 🟡 Geel | 5–8 | Laag-Midden |
| 🟠 Oranje | 9–12 | Midden |
| 🔴 Rood | 13–25 | Hoog |

### 4.2 Risicobereidheid

De OpenMRS REST module verwerkt bijzondere persoonsgegevens (medische data) in de zin
van AVG Art. 9. In een zorginformatiecontext gelden strenge eisen aan
vertrouwelijkheid en integriteit. De risicobereidheid is daarom laag:

> **De organisatie accepteert geen risico's met een score ≥ 10 die betrekking hebben
> op de vertrouwelijkheid of integriteit van patiënt- of medische gegevens (KJ1, KJ2, KJ3).**

Voor beschikbaarheid en niet-medische assets (KJ5, KJ6, KJ7) geldt een hogere
tolerantie, mits herstel aantoonbaar geborgd is.

### 4.3 Grenswaarden

| Classificatie | Score | Actie vereist |
|---------------|-------|---------------|
| 🟢 Laag (1–4) | Acceptabel | Geen directe actie; monitoren in reguliere cyclus |
| 🟡 Laag-Midden (5–8) | Acceptabel met maatregel | Mitigatie plannen binnen 3 maanden |
| 🟠 Midden (9–12) | Niet acceptabel | Mitigatie plannen binnen 1 maand; eigenaar aanwijzen |
| 🔴 Hoog (13–25) | Onacceptabel | Directe actie vereist; escaleren naar security officer |

**Aanvullende grenswaarden voor zorgdata (KJ1, KJ2, KJ3):**
Elk risico met een score ≥ 10 dat betrekking heeft op de vertrouwelijkheid of
integriteit van patiëntdata wordt automatisch geclassificeerd als **Onacceptabel**,
ongeacht de absolute risicoscore. Dit is conform NEN-7510:2024 A.5.1 (informatiebeveiligingsbeleid)
en de meldplicht datalekken onder AVG Art. 33.

---

## 5. Relatie met risicomatrix en threat model

De kroonjuwelen uit deze analyse vormen de input voor:

- **Risicomatrix** (`risico-matrix-full.md`) — elk geïdentificeerd risico wordt gekoppeld
  aan een of meer kroonjuwelen (KJ1–KJ7) om de impact te valideren.
- **Threat model** (in ontwikkeling, issue #54) — aanvalspaden worden beoordeeld op
  welke kroonjuwelen ze in gevaar brengen.
- **Risk Assessment Report** (issue #39) — deze analyse wordt als sectie opgenomen
  ("welke gevoelige gegevens worden verwerkt") met verwijzing naar AVG en WGBO.

---

## 6. Samenvatting

| Aspect | Bevinding |
|--------|-----------|
| Hoogst prioritaire kroonjuwelen | KJ2 (medische gegevens) en KJ1 (patiëntidentificatie) — Kritiek op vertrouwelijkheid én integriteit |
| Grootste direct risico | KJ3 (authenticatiegegevens) — compromittering geeft toegang tot alle andere kroonjuwelen |
| Wettelijke verplichting | AVG Art. 9 + WGBO — lekkage van KJ1/KJ2 = meldplichtig datalek |
| NEN-7510 controls relevant | A.8.3 (toegang), A.8.5 (authenticatie), A.8.14 (beschikbaarheid), A.8.15 (logging) |
| Risicobereidheid | Laag — score ≥ 10 op KJ1/KJ2/KJ3 is onacceptabel en vereist directe actie |

---

*Analyse uitgevoerd op: 2026-06-08*  
*Analist: Wassim Balouda*  
*Gebaseerd op: broncode-analyse webservices.rest v3.2.0, gap-analyse A.8.3/A.8.5/A.8.15, NEN-7510:2024-2, AVG, WGBO*
