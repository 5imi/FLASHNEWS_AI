# ADR-001: Arhitectură Modulară cu Focus pe Rețea

## Status
Acceptat

## Context
Aplicația depinde 100% de surse externe de date (APIs). Trebuie să fim capabili să schimbăm sursa de știri sau motorul AI fără a rescrie interfața.

## Decizie
Vom folosi:
- **Modularizare:** `:core:network` pentru API, `:core:model` pentru date shared, `:feature:feed` pentru UI.
- **Networking:** Retrofit + OkHttp (standard industrie).
- **Serialization:** KotlinX Serialization (modern și rapid).
- **Concurrency:** Kotlin Coroutines & Flow pentru date asincrone.

## Consecințe
- Codul va fi testabil (putem simula răspunsurile API).
- Putem adăuga moduri noi (ex: Audio News) ca module separate.
