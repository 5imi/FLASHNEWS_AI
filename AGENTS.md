# Reguli de Dezvoltare și Control Git pentru Agenți AI (AGENTS.md)

Acest fișier conține directive OBLIGATORII pentru toți asistenții și agenții de codare (Antigravity, Gemini CLI, Claude, Copilot) care lucrează în acest repository.

---

## 🚫 Regula de Aur: Fără Fișiere Interne de Asistent sau IDE pe GitHub

Când efectuezi operațiuni Git (git add, git commit, git push), **ESTE STRICT INTERZIS** să incluzi sau să împingi pe GitHub:
1. **Foldere de asistent / AI**: .agents/, .artifacts/, .gemini/
2. **Foldere și fișiere de IDE**: .idea/, .vscode/, *.iml, local.properties
3. **Build artifacts și cache**: uild/, .gradle/, .cxx/

Pe GitHub trebuie să ajungă **DOAR codul sursă al programului**, resursele Android, testele și documentația proiectului.

---

## 🛡️ Instrucțiuni de Verificare Înainte de Orice Commit / Push

Înainte de a comite sau împinge modificări:
1. Verifică întotdeauna git status pentru a te asigura că nu există fișiere din folderele interzise pregătite pentru commit.
2. Dacă un fișier intern din greșeală a fost urmărit de Git, folosește comanda:
   `ash
   git rm -r --cached .agents .artifacts .idea .vscode .gemini
   `
   pentru a-l scoate din indexul Git fără a-l șterge de pe disc.
3. Asigură-te că fișierul .gitignore conține întotdeauna aceste excluderi.
