# FlashNews AI - Audit & Îmbunătățiri Sistem AI

În urma testelor pe emulator, am identificat câteva puncte critice care explică de ce experiența AI nu a fost la nivelul așteptărilor. Acest document detaliază auditul și planul de acțiune.

## 1. Audit Tehnic (Ce nu a funcționat)

| Componentă | Problemă Identificată | Impact |
| :--- | :--- | :--- |
| **Gemini Client** | Eroare 404: `models/gemini-1.5-flash is not found`. | Failover constant către agenții secundari sau local. |
| **Groq/OpenRouter** | Lipsa caching-ului și a logicii de curățare a răspunsurilor markdown. | Răspunsuri uneori brute sau repetitive. |
| **Prompts** | Instrucțiuni prea scurte sau fără formatare structurată. | Rezumate care par incomplete sau "robotice". |
| **Context** | Deși am mărit limita la 1500 caractere, nu am optimizat *ce* trimitem (titlu + descriere + sursă + categorie). | AI-ul pierde uneori firul narativ principal. |

## 2. Plan de Îmbunătățire (Refining the AI)

### A. Corecție Modele & SDK
- **Gemini**: Schimbăm `gemini-1.5-flash` în `gemini-1.5-flash-latest` (standardul actual SDK).
- **Groq**: Trecem la `llama-3.3-70b-versatile` pentru viteză și logică superioară.
- **OpenRouter**: Folosim `deepseek/deepseek-chat` (cel mai bun model free/low-cost actual).

### B. Prompt Engineering (Romanian structural Focus)
Vom implementa un **sistem de roluri (System Prompts)** și formatare structurată:
- **Sumar**: Nu doar puncte, ci context: „Ce s-a întâmplat, de ce contează, ce urmează”.
- **Bias**: Instrucțiuni clare de a ignora cuvintele de umplutură și de a oferi doar eticheta.
- **Chat**: Personalitate de „asistent jurnalist expert în spațiul românesc”.

### C. Infrastructură AI
- **Caching**: Implementăm caching în memoria RAM pentru TOȚI clienții (Groq, OpenRouter) pentru a evita cereri repetate inutile.
- **Markdown Stripping**: Ne asigurăm că eliminăm ` ```json ` sau alte tag-uri care pot apărea în răspunsuri.

## 3. Acțiuni Imediate

1.  **Reparăm GeminiClient.kt**: Actualizăm string-ul modelului.
2.  **Upgrade Groq/OpenRouter**: Adăugăm rate-limiting și caching similar cu Gemini.
3.  **Refacem Prompts în NewsRepository.kt**: Prompts mai „bogate” și structurate.
