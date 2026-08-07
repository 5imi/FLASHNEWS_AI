# AGENT.md

# Project Rules

These rules apply to every task unless the user explicitly instructs otherwise.

---

## 1. Think Before Coding

Before making any changes:

* Understand the existing implementation.
* Search for related code.
* Identify the smallest safe modification.
* Reuse existing code whenever possible.

Do not start writing new code before understanding the current implementation.

---

## 2. Preserve Previous Implementations

When replacing existing logic, **never permanently delete the previous implementation** unless explicitly instructed.

Instead:

* Comment it out.
* Keep it directly above or near the new implementation.
* Mark it as `[OLD]` or `[LEGACY]`.
* Add a short reason for the replacement.

Minor cosmetic edits (formatting, whitespace, imports, comments, variable renames without logic changes) do not require preserving the previous version.

---

## 3. Make the Smallest Safe Change

Modify only the code required to complete the requested task.

Do not refactor, reorganize, optimize, or clean unrelated code.

---

## 4. Preserve Existing Behaviour

Do not change existing functionality unless explicitly required.

New features must not break existing workflows.

---

## 5. Reuse Before Creating

Before adding new code, verify that similar functionality does not already exist.

Prefer extending existing code over creating duplicate implementations.

---

## 6. Respect Project Conventions

Follow the existing:

* architecture
* folder structure
* naming conventions
* coding style
* formatting

Do not introduce a different style.

---

## 7. Protect Existing Interfaces

Do not rename or remove existing:

* APIs
* routes
* exported functions
* configuration keys
* database fields
* public methods

unless explicitly instructed.

---

## 8. Database, Data & Security Safety

Never perform destructive database changes unless explicitly requested.

Do not automatically:

* drop tables
* remove columns
* delete data
* change column types

Additionally:
* Never log sensitive data (PINs, passwords, API tokens, personal client data) to console, Logcat, or log files.
* Always use secure storage mechanisms (e.g. EncryptedSharedPreferences, environment variables) for credentials and sensitive configuration.

---

## 9. Do Not Guess

If requirements are unclear or confidence is low:

* stop;
* explain the uncertainty;
* ask the user for clarification.

Never invent missing requirements.

---

## 10. Propose New Ideas

Before implementing any idea, optimization, or feature that was not explicitly requested:

* notify the user;
* explain the benefits and potential impact;
* wait for explicit approval.

Never implement "stealth" features or changes based on personal preference without consultation.

---

## 11. Keep Code Maintainable & Robust

Write code that is:

* readable
* simple
* consistent
* maintainable
* modular (isolated features to ensure they are easy to replace or maintain)

Additionally:
* Never swallow exceptions silently in business logic (invoice saving, calculations, data sync). Any failure must be explicitly handled and clearly communicated to the user.

Avoid unnecessary complexity.

---

## 12. Verify Before Finishing & Unit Tests

Before completing a task, verify that:

* the requested feature works;
* existing behaviour has been preserved;
* no unrelated code was modified;
* no duplicate logic was introduced;
* the implementation remains consistent with the project.

Additionally:
* Any modification to price, VAT, discount, or tax calculation algorithms must be covered by unit tests to prevent financial regressions.

---

## 13. User Instructions Override Everything

If any rule conflicts with an explicit user instruction, always follow the user's instruction.

---

## 14. Key Moment Backups & Interactive Workflow

* Create periodic backups/saves of the project in key moments (before major backend/dashboard changes and after verified milestones).
* Collaboration workflow: Guide the user step-by-step with clear ideas and precise instructions, implement code changes when instructed, and verify every modification rigorously.

