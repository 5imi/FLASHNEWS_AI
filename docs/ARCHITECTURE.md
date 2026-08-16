# 🏗️ FlashNews AI — System Architecture & Design

FlashNews AI is built with modern Android architectural principles, utilizing Jetpack Compose, Kotlin Coroutines & Flow, Room SQLite Persistence, Ktor Multiplatform Networking, and a 5-Tier Resilient Multi-Agent AI System.

---

## 📐 1. High-Level Modular Architecture

The application is structured into decoupled Gradle modules following Clean Architecture principles:

```mermaid
graph TD
    subgraph UI & Presentation Layer
        App[":app (Application Entrypoint)"]
        FeatureFeed[":feature:feed (TikTok-style Vertical Feed & AI Inspector)"]
        FeatureSearch[":feature:search (Real-time Article Search & Filtering)"]
        CoreDesign[":core:designsystem (Theme, Components, Typography, Strings)"]
    end

    subgraph Domain & Business Logic
        CoreData[":core:data (NewsRepository, Offline Sync, WorkManager)"]
        CoreModel[":core:model (Domain Entities, Enums, AI Contracts)"]
    end

    subgraph Data & Integration Layer
        CoreNetwork[":core:network (Multi-Agent AI Orchestrator, RSS Parsers, Ktor)"]
        CoreDatabase[":core:database (Room Database, DAOs, Entities, Caching)"]
    end

    App --> FeatureFeed
    App --> FeatureSearch
    App --> CoreDesign
    App --> CoreData

    FeatureFeed --> CoreModel
    FeatureFeed --> CoreDesign
    FeatureFeed --> CoreData
    FeatureFeed --> CoreNetwork

    FeatureSearch --> CoreModel
    FeatureSearch --> CoreDesign
    FeatureSearch --> CoreData

    CoreData --> CoreModel
    CoreData --> CoreNetwork
    CoreData --> CoreDatabase

    CoreNetwork --> CoreModel
    CoreDatabase --> CoreModel
```

---

## 🤖 2. Multi-Agent AI Orchestration (5-Tier Resilient Fallback)

FlashNews AI delivers uninterrupted intelligence using an adaptive failover chain:

```mermaid
sequenceDiagram
    autonumber
    participant UI as Compose UI / ViewModel
    participant Repo as NewsRepository (Cache Layer)
    participant Orch as AiOrchestrator
    participant Gemini as GeminiClient (Tier 1 - Primary)
    participant Grok as GrokClient (Tier 2 - Bias Expert)
    participant Groq as GroqClient (Tier 3 - Llama 3 Fast)
    participant OR as OpenRouterClient (Tier 4)
    participant Local as LocalAiClient (Tier 5 - Offline Heuristics)

    UI->>Repo: Request AI Analysis (title, desc, source, region)
    alt Analysis Cached (< 24h)
        Repo-->>UI: Return Room-Cached AiAnalysis
    else Cache Miss / Forced Refresh
        Repo->>Orch: analyzeNewsDynamic(...)
        alt Gemini Key Available
            Orch->>Gemini: analyzeNewsDynamic(...)
            Gemini-->>Orch: Return Structured AI Output
        else Gemini Fails / No Key
            Orch->>Grok: analyzeNewsDynamic(...)
            Grok-->>Orch: Return Structured AI Output
        else Grok Fails / No Key
            Orch->>Groq: analyzeNewsDynamic(...)
            Groq-->>Orch: Return Structured AI Output
        else Groq Fails / No Key
            Orch->>OR: analyzeNewsDynamic(...)
            OR-->>Orch: Return Structured AI Output
        else All Remote APIs Unavailable / Device Offline
            Orch->>Local: analyzeNewsDynamic(...)
            Note over Local: Media Bias Matrix + Romanian Impact Engine + Clickbait Detection
            Local-->>Orch: Return Local Heuristics Analysis
        end
        Orch-->>Repo: Return AiAnalysis
        Repo->>Repo: Save to Room DB (insertArticlesIfAbsent / update)
        Repo-->>UI: Return AiAnalysis to UI
    end
```

---

## 💾 3. Data Ingestion & Offline-First Strategy

1. **RSS & API Ingestion**: Over 150 RSS feeds spanning Romania (National, Regional, Economic, Tech, Defense) and Global outlets (Reuters, BBC, AP, Politico, TechCrunch) are fetched concurrently.
2. **Room Database Cache**:
   - `insertArticlesIfAbsent`: Preserves existing user favorites and AI summaries from being overwritten during background synchronization.
   - Paging 3 integration provides smooth 60fps vertical paging with instant cache hydration.
3. **Background Sync**: `NewsSyncWorker` runs periodically via Android WorkManager with network constraints.

---

## 🛡️ 4. Security & Network Hygiene

- **Strict HTTPS**: Enabled by default in `network_security_config.xml`.
- **Cleartext Whitelist**: Explicitly restricted only to verified Romanian news outlets lacking full HTTPS RSS endpoints.
- **Zero Sensitive Data Logging**: API keys and tokens are strictly excluded from console logs.
- **R8 / ProGuard Optimization**: Fully configured in `app/proguard-rules.pro` for release builds.
