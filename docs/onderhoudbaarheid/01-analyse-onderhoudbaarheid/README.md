# Analyse onderhoudbaarheid

Om de onderhoudbaarheid van de gekozen OpenMRS REST Webservices module systematisch te beoordelen, hebben wij SonarCloud toegepast op ons project. SonarCloud analyseert de broncode automatisch en levert metrieken op het gebied van complexiteit, duplicatie, code smells en technische schuld. De resultaten van deze analyse vormen de basis voor de beoordeling van de onderhoudbaarheid van de module en zijn gekoppeld aan de ISO 25010 kwaliteitskenmerken analyseerbaarheid, aanpasbaarheid, testbaarheid en modulariteit.

---

## Cyclomatic Complexity

Cyclomatic complexity (CC) meet het aantal onafhankelijke paden door een stuk code. Per extra `if`, `loop`, `case`, `&&` of `||` stijgt de waarde met 1. Een hogere CC betekent dat er meer testgevallen nodig zijn om de code volledig te dekken en dat de code moeilijker te begrijpen en aan te passen is.

De SIG/TÜViT Evaluation Criteria Trusted Product Maintainability hanteert de volgende drempelwaarden per functie:

| CC per functie | Risicocategorie |
|---|---|
| 1 – 5 | Laag risico |
| 6 – 10 | Gemiddeld risico |
| 11 – 25 | Hoog risico |
| > 25 | Zeer hoog risico |

De module bevat in totaal **2.521 functies** met een gecombineerde cyclomatic complexity van **4.756**. Dit geeft een gemiddelde van **1,89 CC per functie**, wat in de categorie *laag risico* valt.

Ondanks het lage gemiddelde zijn er duidelijke uitschieters. De vijf bestanden met de hoogste cyclomatic complexity binnen de `omod`-map zijn:

| Bestand | CC totaal | Functies | CC / functie |
|---|---|---|---|
| `ConceptResource1_8.java` | 83 | 32 | 2,59 |
| `PersonResource1_8.java` | 76 | 33 | 2,30 |
| `ObsResource1_8.java` | 71 | 20 | 3,55 |
| `VisitResource1_9.java` | 63 | 25 | 2,52 |
| `ModuleActionResource1_8.java` | 58 | 19 | 3,05 |

Ondanks de hoge totale CC vallen de gemiddelden per functie nog steeds in de *laag risico*-categorie (≤ 5) van SIG/TÜViT. De complexiteit is hier verspreid over relatief veel functies. Toch wijst de hoge totale CC op veel conditionele logica voor het verwerken van uiteenlopende invoerscenario's, wat de aanpasbaarheid en testbaarheid negatief beïnvloedt.

**Koppeling ISO 25010:** hoge cyclomatic complexity per bestand raakt twee kwaliteitskenmerken direct:
- *Testbaarheid* — elke extra branch vereist een extra testgeval voor volledige dekking; klassen met CC > 50 vereisen tientallen testgevallen voor volledige branch coverage.
- *Aanpasbaarheid (Modifiability)* — meer paden door de code betekent een grotere kans op regressie bij wijzigingen.

---

## Cognitive Complexity

Cognitive complexity is een alternatieve metriek van SonarQube die niet alleen branches telt, maar ook rekening houdt met de diepte van nesting. Diep geneste structuren worden zwaarder gewogen omdat ze voor een ontwikkelaar moeilijker te volgen zijn, ook al hebben ze formeel dezelfde cyclomatic complexity als vlakkere code.

SonarQube hanteert voor cognitive complexity de volgende drempelwaarden per functie:

| Cogn. complexity per functie | Beoordeling |
|---|---|
| 1 – 5 | Laag risico |
| 6 – 15 | Gemiddeld risico |
| > 15 | Hoog risico — SonarQube geeft automatisch een issue |

De totale cognitive complexity van de module bedraagt **3.621**, met een gemiddelde van **1,44 per functie** — eveneens laag. De verdeling is echter scheef: de `omod`-map is verantwoordelijk voor **2.324** (64%) en `omod-common` voor **1.297** (36%) van de totale cognitive complexity.

De vijf bestanden met de hoogste cognitive complexity binnen `omod` zijn:

| Bestand | Cogn. totaal | Functies | Cogn. / functie |
|---|---|---|---|
| `ObsResource1_8.java` | 103 | 20 | 5,15 |
| `EncounterSearchHandler1_9.java` | 88 | 5 | 17,6 |
| `ConceptResource1_8.java` | 69 | 32 | 2,16 |
| `PersonResource1_8.java` | 64 | 33 | 1,94 |
| `ConceptSearchHandler1_8.java` | 62 | 2 | 31,0 |

Wanneer de cognitive complexity per functie wordt bekeken, verschuift het beeld aanzienlijk. `ConceptSearchHandler1_8.java` heeft slechts 2 functies met een gemiddelde cognitive complexity van **31,0** — ruim boven de SIG-grens van 25 (zeer hoog risico). `EncounterSearchHandler1_9.java` scoort **17,6** per functie (hoog risico). Dit betekent dat deze SearchHandler-klassen, hoewel klein qua omvang, individuele functies bevatten die extreem moeilijk te begrijpen en te onderhouden zijn.

`ObsResource1_8.java` komt in beide top 5-lijsten voor met een cognitive complexity (103) die hoger ligt dan de cyclomatic complexity (71). Dit duidt op diepe nesting bovenop de vertakkingslogica, wat de leesbaarheid verder vermindert.

**Koppeling ISO 25010:** hoge cognitive complexity raakt voornamelijk:
- *Analyseerbaarheid (Analysability)* — diep geneste code is moeilijker te doorgronden bij het opsporen van defecten of het begrijpen van gedrag.
- *Aanpasbaarheid (Modifiability)* — complexe logica vergroot het risico dat een wijziging onbedoelde neveneffecten heeft.

---

## Bronnen

- SIG/TÜViT. *Evaluation Criteria for Trusted Product Maintainability.* Software Improvement Group, versie 11.0 (2023). Beschikbaar via: [https://www.softwareimprovementgroup.com](https://www.softwareimprovementgroup.com)
- SonarSource. *Cognitive Complexity — A new way of measuring understandability.* G. Ann Campbell (2023). Beschikbaar via: [https://www.sonarsource.com/resources/cognitive-complexity/](https://www.sonarsource.com/resources/cognitive-complexity/)
- ISO/IEC 25010:2023. *Systems and software engineering — Systems and software Quality Requirements and Evaluation (SQuaRE) — Product quality model.*
