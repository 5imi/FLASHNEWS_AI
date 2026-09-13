# ⚡ FlashNews AI

[![CI & Quality Pipeline](https://github.com/baseredy/FLASHNEWS_AI/actions/workflows/ci.yml/badge.svg)](https://github.com/baseredy/FLASHNEWS_AI/actions/workflows/ci.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg)](https://kotlinlang.org)
[![Android SDK](https://img.shields.io/badge/Min%20SDK-31-green.svg)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Compose-Material%203-blueviolet.svg)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20Modular-orange.svg)](docs/ARCHITECTURE.md)

**FlashNews AI** is an intelligent, high-performance Android news aggregation and analysis platform. It combines a seamless TikTok-style vertical news feed with a **5-Tier Resilient Multi-Agent AI System** that provides bullet-point digests, editorial bias detection, Romanian local impact analysis, and interactive Q&A.

---

## ✨ Key Features

- 📱 **TikTok-Style Vertical Feed**: Smooth 60fps vertical paging powered by Jetpack Compose & Paging 3.
- 🤖 **5-Tier Multi-Agent AI Engine**:
  1. **Google Gemini** — High-speed multimodal summarization.
  2. **xAI Grok** — In-depth editorial bias & political perspective detection.
  3. **Groq (Llama 3)** — Ultra low-latency fallback.
  4. **OpenRouter** — Universal LLM gateway.
  5. **Local AI Engine** — 100% offline heuristic engine with Romanian media bias matrix & impact detection.
- 🇷🇴 **Comprehensive Romanian & Global Coverage**: Over 150 curated RSS feeds covering National, Economy, Tech, Defence, Sports, and International news.
- 💾 **Offline-First Persistence**: Room database caching with smart deduplication (`insertArticlesIfAbsent`).
- ♿ **WCAG Accessible & Localized**: Full TalkBack accessibility semantics, 48dp+ tap targets, and bilingual resource support.
- 🔒 **Enterprise Security**: Strict HTTPS defaults in `network_security_config.xml` and defensive JSON parsing.

---

## 🏗️ Architecture

```
FLASHNEWS_AI
├── app                  # Application entry point & configuration
├── core
│   ├── model            # Pure Kotlin domain entities & AI contracts
│   ├── data             # NewsRepository & WorkManager background sync
│   ├── network          # Ktor client, RSS parsers & Multi-Agent AI Orchestrator
│   ├── database         # Room SQLite entities, DAOs & persistence
│   └── designsystem     # Theme, components, typography & strings
├── feature
│   ├── feed             # FeedScreen, NewsCard & AI bottom-sheet inspector
│   └── search           # Real-time search & category filtering
└── docs
    └── ARCHITECTURE.md  # Detailed architectural specifications & Mermaid diagrams
```

---

## 🚀 Quick Start

### 1. Build & Run Tests
```bash
# Run unit tests across all modules
./gradlew testDebugUnitTest

# Assemble Debug APK
./gradlew assembleDebug
```

The compiled APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

### 2. Configure API Keys (Optional)
Add any of the following to your `local.properties` file:
```properties
GEMINI_API_KEY=your_gemini_api_key
GROK_API_KEY=your_grok_api_key
GROQ_API_KEY=your_groq_api_key
OPENROUTER_API_KEY=your_openrouter_api_key
```
*Note: The app runs 100% autonomously with its built-in Local AI Engine even without external API keys.*

---

## 🧪 Testing

```bash
./gradlew testDebugUnitTest --info
```
All unit tests in `:core:model`, `:core:network`, `:core:data`, and `:feature:feed` are executed automatically on every GitHub pull request.

---

## 📖 Documentation
- [Architecture & Diagrams](docs/ARCHITECTURE.md)
- [Contributing Guide](CONTRIBUTING.md)
- [Quick Start Guide](QUICK_START.md)
