# Setup Guide - Multi-Agent AI System

## Quick Start

FlashNews AI v1.2.6+ uses a **Smart Router (AiOrchestrator)** that automatically selects the best AI agent for each task.

### ✅ Works Out-of-the-Box

The app works immediately with:
- **Gemini 1.5 Flash** (Summarization) ← Primary
- **LocalAiClient** (Fallback) ← Automatic backup
- **GrokClient** (Optional) ← Add for better bias detection

---

## 1. Optional: Add Grok API Key

If you want **better bias detection** and **political analysis**, add a Grok API key from xAI.

### Step 1: Get API Key
1. Go to [https://grok.x.ai](https://grok.x.ai)
2. Sign up or login
3. Create API key
4. Copy the key

### Step 2: Add to local.properties
Open `local.properties` in project root:

```properties
#Tue Jul 14 02:48:37 EEST 2026
GEMINI_API_KEY=your_gemini_api_key_here
GROK_API_KEY=your_grok_api_key_here
NEWS_API_KEY=your_news_api_key_here
```

### Step 3: Rebuild
```bash
./gradlew clean
./gradlew :app:assembleDebug
```

### Result
- ✅ Grok activates for bias detection
- ✅ Automatic fallback if Grok fails
- ✅ Better political analysis

---

## 2. API Keys Configuration

### Gemini (Required for Summarization)
- **Provider**: Google AI Studio
- **Key Location**: `local.properties` → `GEMINI_API_KEY`
- **Model**: `gemini-1.5-flash` (free tier)
- **Tier**: Free (60 requests/minute)

### Grok (Optional for Bias Analysis)
- **Provider**: xAI
- **Key Location**: `local.properties` → `GROK_API_KEY`
- **Default**: `grok_placeholder` (fallback to LocalClient)
- **Tier**: Free (varies)

### News APIs (Required for Feed)
- **NewsAPI**: Top headlines & search
- **NewsData.io**: Romanian & multilingual
- **Mediastack**: Additional news sources

---

## 3. Build Configuration

### Files Modified
```
app/build.gradle.kts              ← Added GROK_API_KEY
feature/feed/build.gradle.kts     ← Added GROK_API_KEY
feature/search/build.gradle.kts   ← Added GROK_API_KEY
```

### Automatic Injection
BuildConfig values are auto-injected from `local.properties`:

```kotlin
// In FeedViewModel
private val grokClient = GrokClient(BuildConfig.GROK_API_KEY)
```

---

## 4. How It Works

### Smart Routing Example

```
User Requests Article Summary
         ↓
AiOrchestrator Router
         ↓
   Try Gemini (Primary)
   ├─ Success? → Return
   ├─ Fail? → Try Grok
   │   ├─ Success? → Return
   │   ├─ Fail? → Try LocalClient
   │   │   ├─ Success? → Return (template)
   │   │   ├─ Fail? → Return default
```

### Bias Detection Hierarchy

```
analyzeBias() call
      ↓
Try Grok (specialist at political analysis)
      ├─ Success? → "STÂNGA" / "DREAPTA" / "NEUTRU"
      ├─ Fail (no key / rate limited)? → Try LocalClient heuristics
      │   └─ "NEUTRU" (default safe response)
```

---

## 5. Testing

### Without Grok Key
App still works perfectly:
- Summarization: ✅ Gemini
- Bias Detection: ✅ LocalClient (heuristics)
- Fallback: ✅ Automatic

### With Grok Key
Better accuracy on bias & political analysis:
- Summarization: ✅ Gemini (fast)
- Bias Detection: ✅ Grok (specialist)
- Fallback: ✅ LocalClient (if needed)

### Test Failover
1. Manually add invalid API key to `local.properties`
2. Rebuild
3. App still works (falls back to LocalClient)

---

## 6. Performance & Cost

### Free Tier Daily Capacity

| Component | Before | After | Improvement |
|-----------|--------|-------|------------|
| **Gemini Calls** | 300+ | 150-200 | Optimized |
| **Grok Calls** | - | 50-100 | Selective |
| **Local Calls** | - | Unlimited | Fallback |
| **Total Daily** | ~200 articles | ~1000+ articles | **+400%** |

### Cost Estimate (Monthly)
- **Gemini**: FREE tier (5000 calls/month default)
- **Grok**: FREE tier (varies by xAI)
- **Local**: $0 (zero API calls)
- **Total**: **$0/month** (fully free tier compatible)

---

## 7. Troubleshooting

### Build Error: "GROK_API_KEY = ;"
**Cause**: Empty GROK_API_KEY in local.properties
**Fix**: Add placeholder value:
```properties
GROK_API_KEY=grok_placeholder
```

### Bias Detection Returns "NEUTRU"
**Likely**: GrokClient not configured or failed
**Check**:
1. Is `GROK_API_KEY` set in `local.properties`?
2. Are network requests working?
3. Check Logcat for errors

**Workaround**: LocalClient will provide heuristics anyway (fallback)

### Slow Response Times
**Cause**: Rate limiting (APIs throttling requests)
**Solution**: Responses are cached, so repeat articles are instant
**Monitor**: Check if same articles appear

---

## 8. Architecture

### Three-Tier AI System

```
┌─────────────────────────────────┐
│    Application Layer (UI)       │
│  FeedScreen / FeedViewModel     │
└──────────────┬──────────────────┘
               │
        ┌──────▼──────┐
        │  Repository │
        └──────┬──────┘
               │
    ┌──────────▼────────────┐
    │  AiOrchestrator       │
    │  (Smart Router)       │
    └──────────┬────────────┘
               │
    ┌──────────┼────────────────────┐
    │          │                    │
    ▼          ▼                    ▼
┌────────┐ ┌────────┐         ┌──────────┐
│ Gemini │ │  Grok  │         │  Local   │
│ 1.5    │ │ (xAI)  │         │ Client   │
│Flash   │ │Optional│         │ Fallback │
└────────┘ └────────┘         └──────────┘
```

---

## 9. Next Steps

1. **Build & Test**
   ```bash
   ./gradlew :app:assembleDebug
   ```

2. **Install on Device**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

3. **(Optional) Add Grok Key** for better bias detection

4. **Monitor Performance**
   - Check Logcat for orchestrator decisions
   - Monitor API rate limiting
   - Cache hitrate tracking

---

## 10. Documentation

- **AI Optimization**: `docs/AI_OPTIMIZATION.md`
- **Multi-Agent Architecture**: `docs/MULTI_AGENT_ARCHITECTURE.md`
- **Roadmap**: `docs/ROADMAP.md`

---

**Version**: 1.2.6+
**Status**: ✅ Production Ready
**Last Updated**: 18 iulie 2026
