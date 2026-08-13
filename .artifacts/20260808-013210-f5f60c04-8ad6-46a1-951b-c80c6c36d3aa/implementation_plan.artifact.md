# FlashNews AI - Advanced Technical Remediation & Optimization

This plan addresses the technical debt and implements advanced optimizations for FlashNews AI, including AI failover pools, deep linking, concurrent RSS synchronization, and AI result persistence.

## Proposed Changes

### AI Clients & Orchestration

#### [AiClient.kt](file:///D:/Proiecte/FLASHNEWS_AI/core/network/src/main/java/com/example/baseredy/flashnews/core/network/AiClient.kt)
- Refine `isValid` and `runWithFailover` to better handle API errors (like 429).

#### [GeminiClient.kt](file:///D:/Proiecte/FLASHNEWS_AI/core/network/src/main/java/com/example/baseredy/flashnews/core/network/GeminiClient.kt)
- Increase context truncation limit from 300/400 to 1500 characters.

#### [GrokClient.kt](file:///D:/Proiecte/FLASHNEWS_AI/core/network/src/main/java/com/example/baseredy/flashnews/core/network/GrokClient.kt)
- Increase context truncation limit to 1500 characters.

#### [LocalAiClient.kt](file:///D:/Proiecte/FLASHNEWS_AI/core/network/src/main/java/com/example/baseredy/flashnews/core/network/LocalAiClient.kt)
- Improve Romanian heuristic responses for better fallback utility.

#### [FeedViewModel.kt](file:///D:/Proiecte/FLASHNEWS_AI/feature/feed/src/main/java/com/example/baseredy/flashnews/feature/feed/FeedViewModel.kt)
- Fix `AiOrchestrator` constructor call (provide all 5 agents).
- Initialize `GroqClient` and `OpenRouterClient` using `BuildConfig`.

#### [SearchViewModel.kt](file:///D:/Proiecte/FLASHNEWS_AI/feature/search/src/main/java/com/example/baseredy/flashnews/feature/search/SearchViewModel.kt)
- Fix `AiOrchestrator` constructor call (provide all 5 agents).

---

### Notifications & Deep Linking

#### [MainActivity.kt](file:///D:/Proiecte/FLASHNEWS_AI/app/src/main/java/com/example/baseredy/flashnews/MainActivity.kt)
- Implement `onNewIntent` to handle deep links while the app is running.
- Extract `article_url` and trigger navigation/article display.

---

### RSS Synchronization

#### [RssClient.kt](file:///D:/Proiecte/FLASHNEWS_AI/core/network/src/main/java/com/example/baseredy/flashnews/core/network/RssClient.kt)
- Refactor `fetchRssNews` to use `coroutineScope`, `async`, and `awaitAll`.
- Use `Semaphore(10)` to limit concurrency and prevent network congestion.

---

### Database & Repository

#### [NewsRepository.kt](file:///D:/Proiecte/FLASHNEWS_AI/core/data/src/main/java/com/example/baseredy/flashnews/core/data/NewsRepository.kt)
- Ensure AI result reuse is consistent across all data fetching paths.
- Verify title-based deduplication logic.

#### [build.gradle.kts](file:///D:/Proiecte/FLASHNEWS_AI/app/build.gradle.kts)
- Add `GROQ_API_KEY` and `OPENROUTER_API_KEY` to `buildConfigField`.

---

### Security & UI

#### [network_security_config.xml](file:///D:/Proiecte/FLASHNEWS_AI/app/src/main/res/xml/network_security_config.xml)
- Broaden cleartext support if necessary for additional legacy RSS sources.

## Verification Plan

### Automated Tests
- Run `.\gradlew.bat assembleDebug` to ensure all constructor fixes and build config changes are correct.

### Manual Verification
- **AI Failover**: Temporarily invalidate `GEMINI_API_KEY` to verify it falls back to Groq/OpenRouter.
- **Deep Linking**: Trigger a notification via `adb shell am broadcast` or wait for sync, then click it to verify the article opens.
- **RSS Speed**: Log the time taken for `fetchRssNews` before and after refactoring to verify the performance gain.
- **Persistence**: Analyze database content after sync to ensure AI fields are populated.
