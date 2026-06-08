# Begrippenkader — Security audit & NEN-7510

Dit document legt het gedeelde begrippenkader vast voor de security-audit van de
`webservices.rest`-module. Het is de WS01-deliverable ("Wet- & regelgeving,
NEN-7510, audit mindset") en dient als naslagwerk voor de terminologie die in de
overige audit-documenten (`docs/security/`) wordt gebruikt.

---

## 1. Wet- en regelgeving

| Begrip | Betekenis |
|---|---|
| **NEN-7510:2024** | De Nederlandse norm voor informatiebeveiliging in de zorg, gebaseerd op ISO 27001/27002 maar aangevuld met zorgspecifieke eisen. Deel -2 bevat de concrete *controls* (beheersmaatregelen). |
| **AVG (GDPR)** | Algemene Verordening Gegevensbescherming — EU-privacywetgeving. Art. 9 classificeert medische gegevens als *bijzondere persoonsgegevens*, met een verzwaard beschermingsregime en meldplicht datalekken (Art. 33). |
| **WGBO** | Wet op de Geneeskundige Behandelingsovereenkomst — regelt o.a. het medisch beroepsgeheim en het dossierrecht van patiënten (Art. 7:454 BW). |
| **CRA** | Cyber Resilience Act — EU-verordening die eisen stelt aan de cyberveiligheid van producten met digitale elementen gedurende hun hele levenscyclus, incl. een verplicht overzicht van gebruikte software (vergelijkbaar met de SBOM-eis). |
| **Meldplicht datalekken** | Verplichting (AVG Art. 33/34) om datalekken met persoonsgegevens binnen 72 uur te melden bij de Autoriteit Persoonsgegevens, en bij hoog risico ook aan betrokkenen. |

## 2. Normen, frameworks & audit-mindset

| Begrip | Betekenis |
|---|---|
| **ISO 27001 / 27002** | Internationale norm voor informatiebeveiligingsmanagementsystemen (ISMS) resp. de bijbehorende beheersmaatregelen — de basis waarop NEN-7510 voortbouwt. |
| **ISO 27005** | Norm voor risicomanagement van informatiebeveiliging; methodologische basis voor de risicocriteria, risicomatrix en risicobehandeling in dit project. |
| **Control (beheersmaatregel)** | Een concrete, toetsbare eis uit een norm (bv. NEN-7510 **A.8.15** "Logging"). Een audit toetst de implementatie van de organisatie/het systeem tegen deze controls. |
| **Audit mindset** | Evidence-based, kritisch en herleidbaar redeneren: claims ("we loggen authenticatie") worden pas geaccepteerd als ze met concreet bewijs (coderegel, configuratie, screenshot) zijn onderbouwd — vergelijkbaar met de drie-statussen-aanpak *aanwezig / gedeeltelijk / afwezig* die in de gap-analyse wordt gebruikt. |
| **Compliance / non-compliance** | De mate waarin een systeem voldoet aan de eisen van een control. Een *gap* is het verschil tussen de gewenste (compliant) en de huidige situatie. |
| **Gap-analyse** | Systematische vergelijking van de huidige staat van een systeem met de norm-eisen, resulterend in een lijst non-compliancies met onderbouwing en prioritering. |
| **Traceability matrix** | Document dat elke compliance-claim koppelt aan een concreet, verifieerbaar bewijsartefact (coderegel, workflow-bestand, scanrapport, testresultaat) — het sluitstuk van de audit (WS06). |
| **Secure SDLC / DevSecOps** | Het structureel inbedden van security-activiteiten in elke fase van de software-ontwikkelcyclus (in plaats van security pas bij oplevering te toetsen). |
| **Shift left** | Het principe om beveiligingscontroles zo vroeg mogelijk in het ontwikkelproces uit te voeren (bv. SAST in de PR-pipeline i.p.v. een pentest vlak voor release) — een laat gevonden kwetsbaarheid is exponentieel duurder om te herstellen dan een vroeg gevonden. |
| **OTAP** | Ontwikkel-, Test-, Acceptatie-, Productie-omgevingen — gescheiden omgevingen met eigen configuratie/secrets om te voorkomen dat wijzigingen ongetest in productie belanden. |

## 3. Risicobegrippen (CIA/BIV & risicomanagement)

| Begrip | Betekenis |
|---|---|
| **CIA / BIV** | **C**onfidentiality/Integrity/Availability, in het Nederlands **B**eschikbaarheid, **I**ntegriteit, **V**ertrouwelijkheid — de drie kernwaarden van informatiebeveiliging die per informatie-asset worden beoordeeld (bv. op een schaal Laag/Midden/Hoog/Kritiek). |
| **Kroonjuweel** | Een informatie-asset die de sterkste bescherming verdient omdat schending ervan de organisatie het meeste schaadt (bv. patiëntgegevens, authenticatiegegevens). Vormt het startpunt van de risicoanalyse — risico's worden aan kroonjuwelen gekoppeld om hun impact te bepalen. |
| **Hack-driehoek** | Model dat een veiligheidsincident beschrijft als de combinatie van drie elementen: een **kroonjuweel** (wat is waardevol), een **kwetsbaarheid** (hoe kan het geraakt worden) en een **kwaadwillende** (wie wil het misbruiken). Alle drie moeten aanwezig zijn voor een geslaagde aanval. |
| **Risico** | De kans dat een dreiging een kwetsbaarheid misbruikt, vermenigvuldigd met de impact daarvan: **Risicoscore = Kans × Impact**. |
| **Risicomatrix** | Tabel/grid die risico's positioneert op basis van kans en impact, met een classificatie (bv. 🟢 Laag / 🟡 Laag-Midden / 🟠 Midden / 🔴 Hoog) die bepaalt of en hoe snel actie vereist is. |
| **Risicocriteria** | De vooraf vastgelegde schalen (kans, impact), kleurclassificaties en grenswaarden waarmee risico's consistent beoordeeld en vergeleken kunnen worden. |
| **Risicobereidheid (risk appetite)** | De mate waarin een organisatie bereid is risico te accepteren, vastgelegd als expliciete grens (bv. *"geen risico's met score ≥ 10 op vertrouwelijkheid van patiëntgegevens"*). Bepaalt wanneer een risico "acceptabel" versus "onacceptabel" is. |
| **Risicobeheersing — de vier opties** | De vier manieren om met een geïdentificeerd risico om te gaan: **vermijden** (activiteit stopzetten), **verminderen** (mitigerende maatregelen treffen), **overdragen** (bv. verzekeren of uitbesteden), **accepteren** (bewust risico laten bestaan, onderbouwd binnen de risicobereidheid). |
| **Bow-tie analyse** | Gestructureerde, visuele weergave van een risico: links de **oorzaken** met **preventieve barrières**, in het midden de **kritieke gebeurtenis** (top event), rechts de **gevolgen** met **reactieve/correctieve maatregelen**. Maakt in één oogopslag zichtbaar welke maatregelen een risico vóór en ná het optreden ervan beheersen. |
| **Threat model / threat modelling** | Het systematisch identificeren van mogelijke dreigingen tegen een systeem, vaak ondersteund door architectuurdiagrammen (bv. C4-model) om aanvalsvectoren en vertrouwensgrenzen in kaart te brengen. |
| **Attack surface** | De optelsom van alle punten waar een aanvaller een systeem kan benaderen of beïnvloeden (endpoints, interfaces, configuratie) — inclusief impliciet vertrouwde elementen ("trust boundaries"). |

## 4. Scanning, testen & kwetsbaarheidsbeheer

| Begrip | Betekenis |
|---|---|
| **SAST** (Static Application Security Testing) | Analyseert broncode zonder deze uit te voeren, op zoek naar kwetsbare patronen (bv. CodeQL, SonarCloud). |
| **DAST** (Dynamic Application Security Testing) | Test een draaiende applicatie van buitenaf op kwetsbaarheden (bv. OWASP ZAP). |
| **SCA** (Software Composition Analysis) | Analyseert gebruikte (open-source) afhankelijkheden op bekende kwetsbaarheden (bv. Dependabot, Snyk). |
| **SBOM** (Software Bill of Materials) | Machine-leesbare inventarisatie van alle software-componenten en hun versies in een product (bv. CycloneDX-formaat) — wettelijk vereist onder de CRA en NEN-7510. |
| **CVE** (Common Vulnerabilities and Exposures) | Uniek identificatienummer voor een publiek bekende kwetsbaarheid (bv. `CVE-2024-XXXXX`). |
| **CVSS** (Common Vulnerability Scoring System) | Gestandaardiseerde score (0.0–10.0) die de ernst van een CVE uitdrukt op basis van factoren als aanvalsvector, complexiteit en impact. Een hoge CVSS-score betekent niet automatisch een hoog *risico* voor dít systeem — context (is de kwetsbare functie bereikbaar/gebruikt?) bepaalt de werkelijke prioriteit. |
| **CWE** (Common Weakness Enumeration) | Categorisering van *typen* softwarezwaktes (bv. CWE-79 Cross-Site Scripting) — beschrijft het patroon achter een kwetsbaarheid, in tegenstelling tot CVE dat een specifiek concreet geval identificeert. |
| **OWASP Top 10** | Lijst van de tien meest voorkomende kwetsbaarheidscategorieën in webapplicaties (bv. Broken Access Control, Injection), gebruikt als referentiekader voor secure-code-reviews. |
| **Pentest (penetratietest)** | Gecontroleerde, geautoriseerde poging om kwetsbaarheden in een systeem daadwerkelijk te misbruiken, om aan te tonen dat een risico reëel is (en na mitigatie: dat het risico verminderd is). |
| **Security backlog** | Geprioriteerde lijst van security-verbeteracties, afgeleid uit risicoanalyse en scanresultaten, die als input dient voor sprintplanning. |
| **False positive** | Een door een scanner gerapporteerde kwetsbaarheid die bij nadere analyse niet (in deze context) daadwerkelijk uitbuitbaar of relevant blijkt — vereist een onderbouwd besluit (en documentatie) om de melding te onderdrukken zonder het echte risico te verbergen. |
| **Mitigatie** | Een maatregel die de kans op of impact van een risico vermindert (bv. invoeren van rate-limiting verlaagt de kans dat brute-forcing slaagt). |
| **Responsible disclosure** | Procedure waarbij een ontdekte kwetsbaarheid eerst vertrouwelijk aan de eigenaar van het systeem wordt gemeld, met een redelijke termijn om deze te verhelpen, vóórdat (indien ooit) details openbaar worden gemaakt. |
| **DPIA** (Data Protection Impact Assessment) | Verplichte risicobeoordeling (AVG Art. 35) bij verwerkingen die waarschijnlijk een hoog privacyrisico opleveren — zoals grootschalige verwerking van bijzondere persoonsgegevens (medische data). |
| **Privacy by Design** | Ontwerpprincipe waarbij gegevensbescherming vanaf het begin in de architectuur wordt ingebouwd (bv. dataminimalisatie, pseudonimisering) in plaats van achteraf toegevoegd. |

---

*Opgesteld als onderdeel van WS01 (Wet- & regelgeving, NEN-7510, audit mindset).*
*Termen zijn afgestemd op het gebruik in `docs/security/01-security-audit/01.md` en de overige audit-documenten — zie die documenten voor de toepassing op de `webservices.rest`-module.*
