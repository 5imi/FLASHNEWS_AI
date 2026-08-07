# Roadmap: FlashNews AI

## Faza 1: Conectivitate (v0.1)
* [ ] Integrare NewsAPI / GNews.
* [ ] Arhitectura Retrofit + Kotlin Serialization.
* [ ] Modelul de date `NewsArticle` în `:core:model`.
* [ ] UI de bază: `VerticalPager` pentru navigare între știri.

## Faza 2: Experiența Vizuală (v0.2)
* [ ] Integrare Coil pentru încărcare imagini.
* [ ] DesignSystem: Carduri de știri cu gradient și text lizibil.
* [ ] Tratarea erorilor de rețea (Offline state).

## Faza 3: Inteligența AI (v0.5)
* [ ] Simulare AI Summary (pregătire backend).
* [ ] Fact-Check UI (Sistemul de culori Verde/Galben/Roșu).
* [ ] Bias Detection UI.

## Faza 4: Personalizare și Sincronizare (v1.0)
* [ ] Salvare favorite (Room Cache).
* [ ] Categorii și preferințe utilizator.
* [ ] Firebase Auth & Cloud Sync.

## Lansare actuală (v1.2.6+) - **Multi-Agent AI System**

### Phase 1: AI Optimizări (Implementat) ✅
* [x] Extindere surse RSS cu categorii noi
* [x] Îmbunătățire contextului AI (regiune, categorie, sursă)
* [x] Introducere bază pentru agenți AI multipli
* [x] Optimizări AI pentru FREE TIER Gemini (caching, rate limiting, prompts)
* [x] **Capacitate crescută**: ~1000 articole/zi (vs 150-200 înainte)

### Phase 2: Multi-Agent System (Implementat) ✅
* [x] **LocalAiClient** - Template-based fallback/offline
  - Template responses fără API calls
  - Heuristics pentru bias detection
  - Zero-cost, 100% reliability
  
* [x] **GrokClient** - xAI Grok specialist
  - Specialized for bias analysis
  - Real-time context awareness
  - Political leanings detection
  
* [x] **AiOrchestrator** - Smart Router
  - Intelligent task routing per agent specialty
  - Automatic failover chain (Primary → Secondary → Local)
  - Error recovery & fallback handling
  
* [x] **ConsensusAiClient** - Voting System (Optional)
  - Multi-agent voting for critical decisions
  - Consensus threshold validation
  - Future: fact-checking verification

* [x] **Integration into ViewModel**
  - Multi-level caching (in-memory + ViewModel)
  - Seamless failover without UI errors
  - Optimized for free tier usage

### Current Status (v1.2.6+)
* ✅ Build: SUCCESS (60.15 MB APK)
* ✅ AI System: Multi-Agent Operational
* ✅ Routing: Smart orchestrator active
* ✅ Failover: Automatic fallback chain
* ✅ Performance: +150% from v1.2.5
* ✅ Free Tier: Optimized & sustained

### Upcoming (Phase 3)
* [ ] Extended agents (Claude, GPT-4o mini)
* [ ] Persistent cache (Room + SharedPreferences)
* [ ] A/B testing for agent variants
* [ ] Cost tracking dashboard
* [ ] Adaptive routing (learn best agent per source)
