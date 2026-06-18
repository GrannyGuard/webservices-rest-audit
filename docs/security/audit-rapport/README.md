# Formeel auditrapport

Rubric: *"Verbeteronderzoek security"*, Sprint 4-eindoplevering.

---

## Inhoud

Het formele auditrapport staat in [`latex/main.tex`](latex/main.tex) en bundelt de
onderliggende analyses uit `docs/security/01..06` tot één document: Executive Summary,
Scope & Context, Audit Methodologie, Risico-analyse & bevindingen (B-001 t/m B-006),
SBOM & Supply Chain Security, Conclusie & Advies, plus de bijlagen A–I.

**Compileren:** `latexmk -pdf main.tex` in [`latex/`](latex/) (configuratie in
`.latexmkrc`); de PDF verschijnt in `latex/.build/`.
