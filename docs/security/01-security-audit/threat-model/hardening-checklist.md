# Hardening Checklist — `webservices.rest`

Deze checklist is de Sprint 3-aanvulling op het STRIDE-threat-model
([`../01.md` §3.7](../01.md#37-threat-model--c4-architectuur--stride-analyse)) en de
[`attack-surface.md`](attack-surface.md) (issue #63, WS05 "Secure Coding & Privacy by
Design"). Het doel is **attack-surface-reductie**: niet elke kwetsbaarheid los
oplossen, maar het aantal en de impact van mogelijke ingangen structureel beperken.

Elke regel volgt het format:

> **Maatregel** — Status (huidige situatie) — NEN-7510 control — Koppeling naar
> threat model / risicomatrix

---

## 1. Onnodige features uitschakelen

**Status: ✅ Geïmplementeerd — Swagger/OpenAPI-endpoints zijn nu uitschakelbaar.**

De Swagger-UI en de OpenAPI-spec (`SwaggerDocController`, `SwaggerSpecificationController`
— categorie C) zijn nu conditioneel gemaakt op een nieuwe global property
`webservices.rest.enableSwaggerDocs` (default `true` om bestaand gedrag/pentests te
behouden; in productie op `false` te zetten). Bij `false` retourneren zowel
`/module/webservices/rest/apiDocs[/debug]` als `/swagger.json` **HTTP 404**.

- **Geïmplementeerd in:**
  [`SwaggerDocController.java`](../../../../omod/src/main/java/org/openmrs/module/webservices/rest/web/controller/SwaggerDocController.java),
  [`SwaggerSpecificationController.java`](../../../../omod/src/main/java/org/openmrs/module/webservices/rest/web/controller/SwaggerSpecificationController.java)
  (gate via `RestUtil.isSwaggerDocsEnabled()`), global property in
  [`config.xml`](../../../../omod/src/main/resources/config.xml), constant
  `ENABLE_SWAGGER_DOCS_GLOBAL_PROPERTY_NAME` in `RestConstants.java`. Unit tests:
  `SwaggerHardeningRestUtilTest`.
- **NEN-7510:** A.8.3 (Toegangsbeveiliging) — least functionality.
- **Koppeling:** vermindert de productie-blootstelling van SQ8/SQ9 (TM-T2, TM-S4); de
  onderliggende XSS-output-encoding-fix voor SQ8 blijft een aparte code-review-actie.
- **Restpunt:** `SettingsFormController` (SQ7) is **niet** door deze vlag gedekt en
  blijft de hoogste open prioriteit (§7 attack surface).

---

## 2. Least-privilege databasegebruiker (kan nooit `DROP`)

**Status: ✅ Mechanisme geïmplementeerd — twee-account-model voorzien.**

De applicatie verbond voorheen uitsluitend met `MYSQL_USER`/`DB_USER`, dat via het
standaard MariaDB-imagegedrag **volledige rechten (incl. `DROP`, `ALTER`, `GRANT`) op
de eigen database** krijgt. De `openmrs/openmrs-core`-image gebruikt dat ene account
zowel voor Liquibase (DDL bij opstarten) als voor runtime-verkeer; daarom is een
twee-account-model geïntroduceerd:

- **`DB_USER`** — migratie/admin-account (volledige rechten), uitsluitend voor
  schema-creatie en Liquibase-upgrades op deploy-moment.
- **`DB_APP_USER`** (`openmrs_app`) — runtime-account met **alleen
  `SELECT, INSERT, UPDATE, DELETE`** op de database; kan nooit `DROP`/`ALTER`/
  `CREATE`/`GRANT`.

- **Geïmplementeerd in:**
  [`docker/db-init/10-runtime-least-privilege.sh`](../../../../docker/db-init/10-runtime-least-privilege.sh)
  (provisioneert het runtime-account bij eerste DB-init), gemount via
  [`docker-compose.yml`](../../../../docker-compose.yml); prod-wiring + verificatie
  (`SHOW GRANTS`) in [`docker/prod/docker-compose.yml`](../../../../docker/prod/docker-compose.yml)
  en [`docker/README.md`](../../../../docker/README.md) → "Least-privilege runtime
  database user".
- **NEN-7510:** A.8.3 (least privilege), A.8.2 (Privileged access rights).
- **Koppeling:** beperkt de **impact** van elke SQL-injectie- of
  privilege-escalatieketen (TM-E2, SQ7) — zelfs bij applicatielaag-DB-toegang kan de
  runtime-gebruiker geen tabellen droppen of het schema wijzigen (mitigeert mede
  CD1/CD6 supply-chain-impact, §4.4).
- **Restpunt:** de productie-`openmrs`-connectie staat bij eerste deploy nog op
  `DB_USER` (Liquibase heeft DDL nodig); de documented steady-state-switch naar
  `DB_APP_USER` is een operationele deploy-stap (niet container-geverifieerd in deze
  omgeving — Docker-daemon was niet beschikbaar).

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
- **Koppeling:** TM-S3 (§3.7.3), prio "te verifiëren in #63" — nu expliciet
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
- **Koppeling:** TM-R2/TM-R3 (§3.7.3) — reeds gemitigeerd.

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

**Status: ✅ Geïmplementeerd — keyless cosign-signing + SLSA-provenance + SBOM.**

Een release-workflow signeert de gebouwde `.omod` keyless (Sigstore/OIDC, géén
langlevende sleutels), maakt een SLSA build-provenance-attestatie en genereert een
CycloneDX-SBOM, en publiceert alle drie naast het `.omod`-bestand.

- **Geïmplementeerd in:**
  [`.github/workflows/release-sign.yml`](../../../../.github/workflows/release-sign.yml)
  — `cosign sign-blob` (detached `.sig` + certificaat),
  `actions/attest-build-provenance` (verifieerbaar met `gh attestation verify`), en
  `cyclonedx-maven-plugin` voor de SBOM. Assets worden bij een gepubliceerde Release
  aangehangen, of als workflow-artefact gepubliceerd bij handmatige runs.
- **Verificatie downstream:** `cosign verify-blob --signature <omod>.sig
  --certificate <omod>.pem --certificate-oidc-issuer
  https://token.actions.githubusercontent.com ...` (zie commentaar in de workflow).
- **NEN-7510:** A.8.32 (Change management) / A.5.23 (Supply chain security),
  CRA-conformiteit (zie [`begrippenkader.md`](../../../00-project-context/begrippenkader.md)).
- **Koppeling:** CD1 (Supply Chain Attack, §4.4) — signed artifacts geven
  downstream-gebruikers een verifieerbare garantie dat het `.omod`-bestand
  ongewijzigd uit déze pipeline komt.

---

## 9. Pinned versies / git hashes voor CI/CD-acties

**Status: ✅ Geïmplementeerd — alle third-party actions zijn SHA-pinned.**

Alle `uses:`-referenties in `codeql.yml`, `sbom.yml`, `sonarcloud-coverage.yml` en de
nieuwe `release-sign.yml` zijn gepind op hun volledige commit-SHA met de versie als
`# vX`-commentaar (bv. `actions/checkout@df4cb1c069e1874edd31b4311f1884172cec0e10  # v6`).
Eerder was slechts `snyk/actions/setup` SHA-gepind.

- **Geïmplementeerd in:** alle vier workflows in
  [`.github/workflows/`](../../../../.github/workflows/). Versie-tags zoals `@v6` zijn
  muteerbaar door de upstream-maintainer (of een aanvaller die hun account
  compromitteert) — een SHA is dat niet.
- **NEN-7510:** A.5.23 (Supply chain security), A.8.32.
- **Onderhoud:** bij het verhogen van een action-versie moet de SHA opnieuw worden
  geresolved (`git ls-remote --tags <repo> <tag>`), bij voorkeur via Dependabot's
  `github-actions`-ecosystem dat SHA-pins automatisch bijwerkt.
- **Koppeling:** CD1/CD6 (Supply Chain Attack, §4.4) — sluit direct aan bij de
  bestaande SBOM/SCA-mitigaties; SonarCloud signaleerde dit patroon al eerder
  (vandaar "zoals SonarQube ook zei").

---

## 10. OpenAPI-specificatie alleen naar specifieke ALLOWED hosts

**Status: ✅ Geïmplementeerd — Host-allow-list + scheme-sanitisatie (SQ9-fix).**

`SwaggerSpecificationController.getSwaggerSpecification()` nam de client-aangeleverde
`Host`- en `X-Forwarded-Proto`-headers voorheen **ongevalideerd** over in het
`host`/`scheme`-veld van de OpenAPI-spec. Dit is nu gevalideerd:

- nieuwe global property `webservices.rest.swaggerAllowedHosts` (comma-separated
  allow-list, analoog aan `REST_ALLOWED_IPS`). Bij een lege lijst blijft het
  legacy-gedrag behouden; bij een niet-toegestane `Host` valt de spec terug op de
  eerste geconfigureerde (canonieke) host i.p.v. de client-waarde
  (`RestUtil.resolveSwaggerHost()`);
- het scheme wordt gesaneerd tot uitsluitend `http`/`https`
  (`RestUtil.sanitizeScheme()`), wat `X-Forwarded-Proto`-injectie blokkeert.

- **Geïmplementeerd in:**
  [`SwaggerSpecificationController.java`](../../../../omod/src/main/java/org/openmrs/module/webservices/rest/web/controller/SwaggerSpecificationController.java),
  helpers in `RestUtil.java`, global property in
  [`config.xml`](../../../../omod/src/main/resources/config.xml). Unit tests:
  `SwaggerHardeningRestUtilTest` (`resolveSwaggerHost*`, `sanitizeScheme*`).
- **NEN-7510:** A.8.20 (Netwerkbeveiliging), A.8.26 (Application security
  requirements).
- **Koppeling:** **SQ9 / TM-S4** (§3.7.3, §3.7.4) — prioriteit 9 in §5.1 van het
  hoofddocument; nu gemitigeerd.

---

## 11. Checklist naast het threat model bijhouden

**Status: ✅ Dit document.**

Deze checklist wordt vanuit [`../01.md` §3.7.4](../01.md#37-threat-model--c4-architectuur--stride-analyse)
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
| 1 | Onnodige features uitschakelen (Swagger-gating) | ✅ | A.8.3 | SQ8/SQ9 (categorie C) |
| 2 | Least-privilege DB-gebruiker (geen `DROP`) | ✅ (mechanisme; prod-switch = deploy-stap) | A.8.2/A.8.3 | CD1/CD6, SQ7/TM-E2 |
| 3 | Endpoints minimaliseren / IP-whitelisting | 🟡 | A.8.3/A.8.20 | TM-S3, TB4, TB6 |
| 4 | Server-side validatie | ✅ (1 restpunt) | A.8.28 | `REST_ALLOWED_IPS`-validatie |
| 5 | Geen stack traces naar browser | ✅ (1 restpunt) | A.8.28/A.5.1 | TM-E2 |
| 6 | WARN-only logging productie | ✅ | A.8.15 | TM-R2/TM-R3 |
| 7 | Default admin-wachtwoord wijzigen | ⚠️ (OTAP-actie) | A.8.5 | SQ1/TM-S1/TM-S2 |
| 8 | Signed artifacts (cosign keyless + provenance + SBOM) | ✅ | A.5.23/A.8.32 | CD1 |
| 9 | Pinned CI/CD-actie-versies (SHA) | ✅ (alle workflows) | A.5.23/A.8.32 | CD1/CD6 |
| 10 | OpenAPI ALLOWED-hosts | ✅ | A.8.20/A.8.26 | SQ9/TM-S4 — prio 9 (§5.1) |
| 11 | Checklist naast threat model | ✅ | — | dit document |
