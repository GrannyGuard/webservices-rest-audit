# Risico-evaluatie — PatientPingeling Project (Gecombineerd Overzicht)

In dit document wordt een gecombineerde risico-evaluatie uitgevoerd voor het PatientPingeling-project. Risico's zijn afkomstig uit meerdere analyses en worden samengebracht in één overzicht. Het doel is om potentiële bedreigingen te identificeren, te beoordelen en te prioriteren.

## Naamgeving

Elk risico krijgt een prefix op basis van de bron van de bevinding:

| Prefix | Bron |
| :--- | :--- |
| **SQ** | SonarQube — statische code-analyse van de OpenMRS REST module |
| **CD** | CI/CD — risico's in het Continuous Integration en Continuous Deployment proces |
| **TM** | Threat Modeling — risico's voortkomend uit de threat model analyse *(volgt)* |

---

## 1. Geïdentificeerde Risico's

### SonarQube (SQ)

*   **SQ1: Kwetsbare sessie-validatie (Security Vulnerability)** — Het gebruik van `getRequestedSessionId()` in `AuthorizationFilter.java` maakt de applicatie kwetsbaar voor sessie-spoofing en ongeautoriseerde toegang tot patiëntdata.
*   **SQ2: Onvolledige switch-statements (Missing Default Case)** — Meerdere switch-statements missen een default-case of niet alle enum-waarden, waardoor onverwachte invoer leidt tot stille fouten of undefined gedrag.
*   **SQ3: Foute testassertions (Broken Test Logic)** — In `EncounterSearchHandler2_0Test` worden primitive waarden vergeleken met null, waardoor testcases onterecht slagen en bugs in zoeklogica ongedetecteerd blijven.
*   **SQ4: Niet-serializeerbaar validatie-veld (Serialization Risk)** — Het veld `errors` in `ValidationException.java` is niet serializable, wat runtime-fouten kan veroorzaken bij het doorgeven van validatiefouten via de API.
*   **SQ5: Extreem hoge cognitieve complexiteit (Maintainability Risk)** — Meerdere methoden overschrijden de maximale complexiteit van 15 aanzienlijk (hoogste: 78), wat onderhoud, testen en uitbreiding ernstig bemoeilijkt.
*   **SQ6: Lege methoden zonder verklaring (Intentionality Risk)** — Circa 40 methoden zijn leeg zonder commentaar of exception, waardoor niet duidelijk is of dit intentioneel gedrag betreft of onvoltooide implementatie.

### CI/CD (CD)

*   **CD1: Gecompromitteerde externe dependencies (Supply Chain Attack)** — Een door de applicatie gebruikte externe bibliotheek bevat malafide code.
*   **CD2: Lekkage van secrets (Secret / Credential Leakage)** — Het per ongeluk committen of loggen van API-keys of wachtwoorden.
*   **CD3: Ongeautoriseerde toegang tot de repository / pipeline** — Een kwaadwillende verkrijgt toegang (bijv. via gestolen sessies of gebrek aan MFA) en past de code of pipeline-configuratie aan.
*   **CD4: Foutieve beveiligingsconfiguratie (Misconfiguration)** — Beveiligingscontroles (zoals SAST of SCA) zijn verkeerd geconfigureerd of falen stil, waardoor kwetsbare code ongemerkt doorgaat.
*   **CD5: Uitval van de CI/CD-omgeving (Downtime)** — De build server (GitHub Actions of Bamboo) of een externe registry is onbereikbaar.
*   **CD6: Malafide code contributor** — Een code contributor voegt expres malafide code toe aan het project.

### Threat Modeling (TM)

*   **TM1: *(Placeholder — threat modeling analyse volgt)***

---

## 2. Risicomatrix

De risico's worden gescoord op basis van **Kans** (1 = Zeer onwaarschijnlijk, 5 = Zeer waarschijnlijk) en **Impact** (1 = Zeer laag, 5 = Zeer hoog).  
*Formule: Risicoscore = Kans × Impact*

| ID | Omschrijving Risico | Kans (1-5) | Impact (1-5) | Risicoscore | Classificatie |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **SQ1** | Kwetsbare sessie-validatie (Security Vulnerability) | 3 | 5 | **15** | 🔴 **Hoog** |
| **SQ2** | Onvolledige switch-statements (Missing Default Case) | 4 | 4 | **16** | 🔴 **Hoog** |
| **SQ3** | Foute testassertions (Broken Test Logic) | 5 | 3 | **15** | 🔴 **Hoog** |
| **SQ4** | Niet-serializeerbaar validatie-veld (Serialization Risk) | 3 | 3 | **9** | 🟠 **Midden** |
| **SQ5** | Extreem hoge cognitieve complexiteit (Maintainability Risk) | 5 | 2 | **10** | 🟠 **Midden** |
| **SQ6** | Lege methoden zonder verklaring (Intentionality Risk) | 4 | 2 | **8** | 🟡 **Laag-Midden** |
| **CD1** | Gecompromitteerde externe dependencies (Supply Chain Attack) | 3 | 5 | **15** | 🔴 **Hoog** |
| **CD2** | Lekkage van secrets (Secret / Credential Leakage) | 3 | 4 | **12** | 🔴 **Hoog** |
| **CD3** | Ongeautoriseerde toegang tot de repository / pipeline | 2 | 5 | **10** | 🟠 **Midden-Hoog** |
| **CD4** | Foutieve beveiligingsconfiguratie (Misconfiguration) | 3 | 3 | **9** | 🟠 **Midden** |
| **CD5** | Uitval van de CI/CD-omgeving (Downtime) | 4 | 2 | **8** | 🟡 **Laag-Midden** |
| **CD6** | Malafide code contributor | 2 | 5 | **10** | 🟠 **Midden** |
| **TM1** | *(Placeholder — volgt)* | — | — | **—** | ⬜ **Nader te bepalen** |

*De hoogst scorende risico's zijn **SQ2** (score 16) en **SQ1**, **SQ3**, **CD1** (score 15). Threat modeling risico's worden toegevoegd zodra de analyse beschikbaar is.*