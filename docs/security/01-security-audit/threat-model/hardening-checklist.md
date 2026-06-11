# Hardening Checklist — `webservices.rest`

Deze checklist is de Sprint 3-aanvulling op het STRIDE-threat-model
([`../01.md` §3.5](../01.md#35-threat-model--c4-architectuur--stride-analyse)) en de
[`attack-surface.md`](attack-surface.md) (issue #63, WS05 "Secure Coding & Privacy by
Design"). Het doel is **attack-surface-reductie**: niet elke kwetsbaarheid los
oplossen, maar het aantal en de impact van mogelijke ingangen structureel beperken.

Elke regel volgt het format:

> **Maatregel** — Status (huidige situatie) — NEN-7510 control — Koppeling naar
> threat model / risicomatrix

---

## 1. Onnodige features uitschakelen

**Status: ❌ Niet structureel — open actiepunt.**

De drie module-admin controllers in
[categorie C van de attack surface](attack-surface.md#4-categorie-c--module-admin-controllers-modulewebservicesrestendpoint--️-high-risk)
(`SettingsFormController`, `SwaggerDocController`, `SwaggerSpecificationController`)
zijn **altijd actief**, ook in productie, en bieden geen configuratie-optie om ze uit
te schakelen.

- **Aanbeveling:** maak `/module/webservices/rest/apiDocs*` en
  `/module/webservices/rest/swagger.json` conditioneel op een global property (bv.
  `webservices.rest.enableSwaggerUI`, default `false` in productie), naar analogie
  van `webservices.rest.enableStackTraceDetails` (zie §5).
- **NEN-7510:** A.8.3 (Toegangsbeveiliging) — least functionality.
- **Koppeling:** vermindert de blootstelling van SQ8/SQ9 (TM-T2, TM-S4) zonder de
  onderliggende code-fix te vervangen.

---

## 2. Least-privilege databasegebruiker (kan nooit `DROP`)

**Status: ❌ Niet geconfigureerd — open actiepunt.**

In `docker-compose.yml` / `docker/prod/docker-compose.yml` wordt de applicatie
verbonden met `MYSQL_USER`/`DB_USER`, dat via het standaard MySQL-imagegedrag
**volledige rechten (incl. `DROP`, `ALTER`, `GRANT`) op de eigen database** krijgt.
Er is geen apart, beperkt account voor de runtime-verbinding van de
`webservices.rest`-module.

- **Aanbeveling:** introduceer een `openmrs_app`-gebruiker met alleen
  `SELECT, INSERT, UPDATE, DELETE` (geen `DROP`, `ALTER`, `CREATE`) voor de
  runtime-verbinding (`OMRS_CONFIG_CONNECTION_USERNAME`); reserveer een apart
  migratie-account met hogere rechten uitsluitend voor Liquibase-runs tijdens
  deployment.
- **NEN-7510:** A.8.3 (least privilege), A.8.2 (Privileged access rights).
- **Koppeling:** beperkt de **impact** van elke SQL-injectie- of
  privilege-escalatieketen (TM-E2, SQ7) — zelfs als een aanvaller via de
  applicatielaag database-toegang krijgt, kan deze geen tabellen droppen of het
  schema wijzigen (mitigeert mede CD1/CD6 supply-chain-impact, §4.4).

---

## 3. Minimaliseer geëxposeerde endpoints (IP-whitelisting)

**Status: 🟡 Gedeeltelijk — bestaand mechanisme dekt niet alle endpoints.**

`AuthorizationFilter` past `RestUtil.isIpAllowed(request.getRemoteAddr())` toe via
de `webservices.rest.allowedips` global property, maar:

- dit geldt **alleen** voor `/ws/rest/*` (categorieën A en B in
  [`attack-surface.md`](attack-surface.md)) — categorie C (`/module/webservices/rest/*`)
  valt hier volledig buiten (TB6, nieuw geïdentificeerd in §5 van
  `attack-surface.md`);
- achter een reverse proxy bevat `getRemoteAddr()` het proxy-IP, niet het
  client-IP (TM-S3, TB4) — de allow-list filtert dan effectief niets.

- **Aanbeveling:**
  1. Pas dezelfde IP-allow-list-logica toe op categorie C, of verwijder/relocate
     deze endpoints onder `/ws/rest/*` zodat ze door `AuthorizationFilter` lopen.
  2. Voor TB4: gebruik `X-Forwarded-For` alléén wanneer deze afkomstig is van een
     vertrouwde, geconfigureerde reverse-proxy-IP — anders blijft `getRemoteAddr()`
     leidend.
- **NEN-7510:** A.8.3, A.8.20 (Netwerkbeveiliging).
- **Koppeling:** TM-S3 (§3.5.3), prio "te verifiëren in #63" — nu expliciet
  geconcretiseerd tot bovenstaande twee acties.

---

## 4. Nooit client-side validatie (server-side enforcement)

**Status: ✅ Grotendeels in lijn — Resource Framework valideert server-side.**

De Resource Framework (categorie A) voert validatie uit via
`Validator`-implementaties en `BaseDelegatingResource` vóór persistence — er is geen
afhankelijkheid van client-side checks voor de kern-resources.

- **Uitzondering:** `SettingsFormController.GlobalPropertiesModel.validate()` bevat
  meerdere `// TODO validate legal uri prefix` /
  `// TODO validate legal comma-separated IPv4 or IPv6 addresses`-commentaren
  (regels 138, 140) — de `REST_ALLOWED_IPS`-waarde (gebruikt door §3 hierboven!)
  wordt dus **niet** server-side gevalideerd op formaat.
- **Aanbeveling:** implementeer de twee openstaande TODO's in
  `SettingsFormController.GlobalPropertiesModel.validate()` — een ongeldige
  `REST_ALLOWED_IPS`-waarde kan de IP-allow-list (maatregel §3) onbedoeld
  uitschakelen.
- **NEN-7510:** A.8.28 (Secure coding).
- **Koppeling:** indirect aan TM-S3/TB4 — een kapotte allow-list-configuratie
  ondermijnt maatregel §3.

---

## 5. Stack traces nooit naar de browser

**Status: ✅ Grotendeels gemitigeerd, met één restpunt.**

`RestUtil.wrapErrorResponse()` (`omod-common/.../RestUtil.java:821-866`) plaatst de
volledige `ExceptionUtils.getStackTrace(ex)` alleen in het `detail`-veld als de
global property `webservices.rest.enableStackTraceDetails` op `"true"` staat
(default `"false"`).

- **Restpunt:** ongeacht deze instelling wordt altijd
  `map.put("code", stackTraceElement.getClassName() + ":" + stackTraceElement.getLineNumber())`
  teruggegeven (regel 856) — dit lekt de **interne klassenaam en regelnummer** van
  waar de exceptie optrad, ook in productie.
- **Aanbeveling:** maak ook het `code`-veld afhankelijk van
  `enableStackTraceDetails`, of vervang het door een generieke foutcode
  (bv. een UUID/correlation-ID die wél in de server-logs traceerbaar is).
- **NEN-7510:** A.8.28, A.5.1 (informatie-classificatie).
- **Koppeling:** versterkt de bescherming tegen informatie-verzameling die
  voorafgaat aan TM-E2 (keten-aanval via informatie-disclosure).

---

## 6. Alleen WARN-niveau logging in productie

**Status: ✅ Geïmplementeerd in Sprint 3 (zie §3.3 / Mitigatie uitgevoerd).**

`AuthorizationFilter` gebruikt `log.warn(...)` voor `ACCESS_BLOCKED`,
`SESSION_TIMEOUT`, `AUTH_FAILURE` en `DIAG_ACCESS`, en `log.info(...)` voor
`AUTH_SUCCESS`/`AUTH_LOGOUT`. Geen van deze log-statements bevat wachtwoorden of
andere geheimen (alleen username, IP, URI, foutreden).

- **Aanbeveling:** controleer bij de productie-`log4j2`/`logback`-configuratie dat
  het root- of pakket-niveau voor `org.openmrs.module.webservices.rest` op `WARN`
  staat (niet `DEBUG`/`INFO`), zodat `AUTH_SUCCESS`/`AUTH_LOGOUT` (op `INFO`) in
  productie niet onnodig veel volume genereren — overweeg deze naar `WARN` te
  verplaatsen of de productie-drempel op `INFO` te zetten indien het audit trail
  (NEN-7510 A.8.15) deze events vereist.
- **NEN-7510:** A.8.15 (Logging).
- **Koppeling:** TM-R2/TM-R3 (§3.5.3) — reeds gemitigeerd.

---

## 7. Standaard `admin`/`admin123`-wachtwoord wijzigen

**Status: ⚠️ Buiten codebase-scope, wel een OTAP/deployment-actiepunt.**

Geen verwijzingen naar `admin123` zijn aangetroffen in de module-broncode of
configuratie van dit project — het standaardwachtwoord is een eigenschap van de
**OpenMRS-platform-seeddata**, niet van deze module.

- **Aanbeveling:** documenteer in de OTAP-omgevingsdocumentatie (Sprint 1,
  README "hoe omgevingen zijn ingericht") dat het `admin`-account in elke
  omgeving (test én productie) bij eerste deploy een uniek, gegenereerd wachtwoord
  krijgt — niet het OpenMRS-default `Admin123`. Voeg dit toe als
  post-deploy-controlepunt in de CI/CD-pipeline (bv. een check die faalt als het
  default-wachtwoord nog werkt).
- **NEN-7510:** A.8.5 (Authenticatie) — "wijzig standaardwachtwoorden vóór
  productiegebruik".
- **Koppeling:** versterkt SQ1/TM-S1/TM-S2 (sessie-fixatie en brute-force op
  Basic Auth) — een gecompromitteerd `admin`-account via een default-wachtwoord
  maakt elke andere mitigatie irrelevant.

---

## 8. Signed artifacts

**Status: ❌ Niet geconfigureerd — open actiepunt (bonus, "Marcel would love").**

Geen `maven-gpg-plugin`, `jarsigner`, of vergelijkbare signing-stap is aangetroffen
in de Maven-configuratie of GitHub Actions-workflows.

- **Aanbeveling:** voeg een signing-stap toe aan de release-workflow die de
  gebouwde `.omod`/`.jar`-artefacten signeert (bv. met `cosign` voor
  keyless-signing via GitHub OIDC, of GPG met een in GitHub Secrets opgeslagen
  sleutel). Publiceer de signature/SBOM-attestation als CI-artefact naast het
  `.omod`-bestand.
- **NEN-7510:** A.8.32 (Change management) / A.5.23 (Supply chain security),
  CRA-conformiteit (zie [`begrippenkader.md`](../../../00-project-context/begrippenkader.md)).
- **Koppeling:** CD1 (Supply Chain Attack, §4.4) — signed artifacts geven
  downstream-gebruikers een verifieerbare garantie dat het `.omod`-bestand
  ongewijzigd uit déze pipeline komt.

---

## 9. Pinned versies / git hashes voor CI/CD-acties

**Status: 🟡 Gedeeltelijk — slechts 1 van 9 third-party actions is SHA-pinned.**

In `.github/workflows/` zijn alle `actions/*`-stappen (checkout, setup-java, cache,
upload-artifact) gepind op een **versie-tag** (`@v6`, `@v5`, `@v4`), niet op een
SHA-commit-hash. Alleen `snyk/actions/setup` in `sbom.yml:68` is correct
SHA-gepind:
`uses: snyk/actions/setup@9adf32b1121593767fc3c057af55b55db032dc04  # v1.0.0`.

- **Aanbeveling:** pin alle `uses:`-referenties in `codeql.yml`, `sbom.yml` en
  `sonarcloud-coverage.yml` op hun volledige commit-SHA (met versie als
  `# vX`-commentaar, zoals nu al bij `snyk/actions/setup`). Versie-tags zoals
  `@v6` zijn muteerbaar door de upstream-maintainer (of een aanvaller die hun
  account compromitteert) — een SHA is dat niet.
- **NEN-7510:** A.5.23 (Supply chain security), A.8.32.
- **Koppeling:** CD1/CD6 (Supply Chain Attack, §4.4) — sluit direct aan bij de
  bestaande SBOM/SCA-mitigaties; SonarCloud signaleerde dit patroon al eerder
  (vandaar "zoals SonarQube ook zei").

---

## 10. OpenAPI-specificatie alleen naar specifieke ALLOWED hosts

**Status: ❌ Niet geïmplementeerd — directe oorzaak van SQ9.**

`SwaggerSpecificationController.getSwaggerSpecification()`
(`omod/.../controller/SwaggerSpecificationController.java:30-40`) neemt de
client-aangeleverde `Host`- en `X-Forwarded-Proto`-headers **ongevalideerd** over
in het gepubliceerde `host`/`scheme`-veld van de OpenAPI-spec.

- **Aanbeveling:** introduceer een global property (bv.
  `webservices.rest.swaggerAllowedHosts`, comma-separated allow-list, analoog aan
  `REST_ALLOWED_IPS`). Valideer de `Host`-header tegen deze lijst; bij een
  niet-toegestane host: gebruik een geconfigureerde default-host in plaats van de
  request-header, of retourneer een 400.
- **NEN-7510:** A.8.20 (Netwerkbeveiliging), A.8.26 (Application security
  requirements).
- **Koppeling:** **SQ9 / TM-S4** (§3.5.3, §3.5.4) — prioriteit 9 in §5.1 van het
  hoofddocument.

---

## 11. Checklist naast het threat model bijhouden

**Status: ✅ Dit document.**

Deze checklist wordt vanuit [`../01.md` §3.5.4](../01.md#35-threat-model--c4-architectuur--stride-analyse)
en [`attack-surface.md`](attack-surface.md) gelinkt, en moet worden bijgewerkt
zodra:

- een van de bovenstaande maatregelen wordt geïmplementeerd (status ❌/🟡 → ✅,
  met verwijzing naar de PR/commit als bewijsartefact voor de
  traceability matrix in Sprint 4);
- het threat model wijzigt (nieuwe componenten/endpoints in de C4-diagrammen)
  — controleer dan of nieuwe endpoints categorie A, B of C raken (zie
  `attack-surface.md` §1) en of deze checklist nieuwe items nodig heeft.

---

## Samenvattende status

| # | Maatregel | Status | NEN-7510 | Gekoppeld aan |
|---|---|:---:|---|---|
| 1 | Onnodige features uitschakelen | ❌ | A.8.3 | SQ8/SQ9 (categorie C) |
| 2 | Least-privilege DB-gebruiker (geen `DROP`) | ❌ | A.8.2/A.8.3 | CD1/CD6, SQ7/TM-E2 |
| 3 | Endpoints minimaliseren / IP-whitelisting | 🟡 | A.8.3/A.8.20 | TM-S3, TB4, TB6 |
| 4 | Server-side validatie | ✅ (1 restpunt) | A.8.28 | `REST_ALLOWED_IPS`-validatie |
| 5 | Geen stack traces naar browser | ✅ (1 restpunt) | A.8.28/A.5.1 | TM-E2 |
| 6 | WARN-only logging productie | ✅ | A.8.15 | TM-R2/TM-R3 |
| 7 | Default admin-wachtwoord wijzigen | ⚠️ (OTAP-actie) | A.8.5 | SQ1/TM-S1/TM-S2 |
| 8 | Signed artifacts | ❌ | A.5.23/A.8.32 | CD1 |
| 9 | Pinned CI/CD-actie-versies (SHA) | 🟡 (1/9) | A.5.23/A.8.32 | CD1/CD6 |
| 10 | OpenAPI ALLOWED-hosts | ❌ | A.8.20/A.8.26 | SQ9/TM-S4 — prio 9 (§5.1) |
| 11 | Checklist naast threat model | ✅ | — | dit document |
