# 🤝 Contributing to FlashNews AI

Thank you for your interest in contributing to **FlashNews AI**! This guide explains our development standards, project structure, and workflow.

---

## 🛠️ Prerequisites & Setup

1. **JDK**: Version 17+ (e.g., Eclipse Temurin or Amazon Corretto).
2. **Android SDK**: Compile SDK 35, Min SDK 31.
3. **Gradle**: Managed via Gradle Wrapper (`./gradlew`).

### Build and Test
```bash
# Check Kotlin compilation across all modules
./gradlew compileDebugKotlin

# Run the complete unit test suite
./gradlew testDebugUnitTest

# Assemble debug APK
./gradlew assembleDebug
```

---

## 🏗️ Architecture & Modules

The codebase is organized into modular Gradle packages:
- `:app`: Application configuration and MainActivity entrypoint.
- `:core:model`: Pure Kotlin domain entities (`NewsArticle`, `AiAnalysis`, `AiInsight`).
- `:core:data`: `NewsRepository`, offline synchronization worker (`NewsSyncWorker`).
- `:core:network`: Ktor HTTP client, RSS Feed parsers, and Multi-Agent `AiOrchestrator`.
- `:core:database`: Room SQLite database, entities, and DAOs.
- `:core:designsystem`: Colors, typography, shared UI components, and localized string resources.
- `:feature:feed`: Vertical TikTok-style feed, bottom-sheet AI inspector, and chat.
- `:feature:search`: Real-time keyword search and category filtering.

---

## 📰 Adding New News Sources

To add a new RSS source:
1. Open `core/network/src/main/java/com/example/baseredy/flashnews/core/network/RssFeedProvider.kt`.
2. Add your source to the appropriate category and region (`RO` or `GLOBAL`):
```kotlin
RssSource(
    name = "Source Name",
    url = "https://example.com/rss",
    category = "General", // "Technology", "Business", "Sports", etc.
    region = "RO",
    logoUrl = "https://example.com/favicon.png"
)
```
3. If the source uses HTTP cleartext, add its domain to `app/src/main/res/xml/network_security_config.xml`.
4. Add the editorial bias profile in `core/network/.../LocalAiClient.kt` if applicable.

---

## 🧪 Testing & Verification

- Every new feature or repository modification must be covered by unit tests.
- Unit tests reside in `<module>/src/test/java/...`.
- Run `./gradlew testDebugUnitTest` to verify before submitting pull requests.

---

## 📜 Commit Conventions

We follow Conventional Commits:
- `feat(...)`: New feature or capability.
- `fix(...)`: Bug fix or edge case resolution.
- `ci(...)`: Changes to CI/CD workflows or build automation.
- `docs(...)`: Documentation and architectural blueprints.
- `refactor(...)`: Code adjustments that do not change external behavior.
