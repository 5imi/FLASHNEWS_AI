# ⚡ FlashNews AI — Next-Gen AI News Aggregator & Personal Radio

<div align="center">

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg)](https://kotlinlang.org)
[![Android SDK](https://img.shields.io/badge/Target%20SDK-35%20(Vanilla%20Ice%20Cream)-green.svg)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Compose-Material%203-blueviolet.svg)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20Modular-orange.svg)](docs/ARCHITECTURE.md)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Release](https://img.shields.io/badge/Latest%20Release-v1.5.0-success.svg)](releases/FlashNews_AI_v1.5.0.apk)

**[🇷🇴 Versiunea în Română](#-română--ghid-complet-flashnews-ai)** &nbsp;|&nbsp; **[🇬🇧 English Version](#-english--flashnews-ai-complete-guide)**

</div>

---

# 🇷🇴 ROMÂNĂ — Ghid Complet FlashNews AI

**FlashNews AI** este o platformă mobilă modernă de agregare, sinteză și analiză a știrilor, concepută pentru Android (SDK 31-35). Aplicația combină un feed vertical fluid stil TikTok/Reels cu un **motor hibrid Multi-Agent AI** (Gemini, Grok, Groq, OpenRouter și Local On-Device) capabil să extragă esența din știri, să detecteze orientarea editorială (bias politic/senzaționalism) și să ofere o experiență audio completă.

---

## 🌟 Ce face aplicația (Funcționalități Cheie):

### 1. 🎙️ Redare Audio Inteligentă & Viteză Rapidă (1.35x)
- **Ascultă știrile la volan sau pe drum**: Motor nativ Android `TextToSpeech` configurat pentru limba română (`ro-RO`).
- **Viteză optimizată la 1.35x**: Redare alertă, clară și fluentă ce economisește peste 30% din timpul utilizatorului.
- **Control flexibil**: Selector de viteză direct în interfață (`1.0x`, `1.25x`, `1.35x`, `1.5x`).
- Buton de ascultare audio pe fiecare card de știre, în panoul de detalii și pentru răspunsurile asistentului AI.

### 2. 📻 Buletinul Radio AI / Podcastul Zilei (3 Minute la Cafea)
- În loc să parcurgi zeci de articole, apeși pe **„📻 Radio AI”** din antet.
- AI-ul selectează cele mai importante 5 știri din categorii diferite (*Politică, Economie, Tehnologie, Externe*) și le transformă într-un **buletin de știri radio fluid**:
  *„Bună dimineața! Iată sinteza celor mai importante evenimente ale momentului în FlashNews: În economie, pe plan extern, iar în tehnologie. Te ținem la curent cu restul detaliilor în FlashNews!”*
- Player dedicat cu Play, Pauză, salt temporal și scriptul afișat sincronizat.

### 3. 🚗 Mod Navetă & Redare Continuă (Commute Mode)
- **Hands-Free Playlist**: Atinge pictograma de căști din antet pentru a porni fluxul continuu de știri.
- Articolele sunt narate vocal secvențial, fără întrerupere, trecând automat la următoarea știre la finalizare.
- Comenzi tactile mari și sigure pentru condus: Play/Pause, Pas Înainte, Pas Înapoi și contor de progres în playlist.
- Paginare automată a feed-ului când playlist-ul se apropie de final.

### 4. 📖 Mod Lectură Curat (Distraction-Free Smart Reader)
- Elimină reclamele, elementele de tracking și elementele vizuale aglomerate, prezentând doar conținutul editorial pur.
- **Teme de lectură personalizate**:
  - 🖤 **AMOLED Pitch Black**: contrast absolut pentru ecrane OLED și economie maximă de baterie.
  - 📜 **Sepia Cald**: tonuri calde relaxante ideale pentru lectură prelungită fără oboseală oculară.
  - 🌌 **Nocturn Indigo**: temă întunecată cu accente elegante de albastru profund.
- **Tipografie fină**: font serif premium cu scalare instantanee în 4 trepte (`A-`, `A`, `A+`, `A++`).
- Card de sinteză executivă AI integrat la începutul articolului și buton de copiere rapidă a textului curățat.

### 5. 💬 „Întreabă Știrea” cu Ton AI Personalizabil (Multi-Tone Analysis)
- Mini-asistent conversațional inteligent integrat în fiecare articol.
- **Comutator de ton la 1 tap**:
  - 💼 **Executiv**: sinteză concisă axată pe impact decizional, cifre și concluzii de business.
  - 💡 **Simplu**: explicație intuitivă pe înțelesul oricui, fără jargon tehnic (stil ELI5).
  - 🔍 **Critic**: analiză sceptică axată pe contradicții, interese ascunse și posibile omisiuni.
- Cipuri rapide de întrebări frecvente și suport complet pentru întrebări libere formulate de utilizator.
- Fiecare răspuns AI poate fi ascultat vocal la viteza optimă de 1.35x.

### 6. 🌐 Perspectivă 360° & Anti-Manipulare (Comparație Între Ziare)
- Detectează automat când același subiect major este relatat de mai multe publicații diferite (*Digi24, HotNews, Agerpres, Libertatea etc.*).
- Butonul **„Compară ziarele (360°)”** produce o sinteză critică obiectivă:
  - **Fapte confirmate unanim**: ce este cert și raportat identic de toți.
  - **Nuanțe & Tente editoriale**: ce a evidențiat sau a omis fiecare redacție.
  - **Concluzie de neutralitate și etichetă vizuală**.

### 7. 📊 Statistici Personale de Lectură (Reading Analytics & Insights)
- Panou elegant de analiză accesibil direct din antet prin pictograma de grafic.
- **Minute Economisite cu AI**: calcul inteligent al timpului salvat prin citirea sintezelor executive în loc de articole întregi.
- **Total Articole Parcurse**: contor automat ce ține evidența articolelor citite sau deschise.
- **Timp Audio Ascultat**: minute cumulate de navetă și ascultare la 1.35x.
- **Indicator de Protecție Anti-Manipulare**: monitorizează expunerea utilizatorului la surse verificate și alerte de bias.

### 8. 🔔 Alerte pe Cuvinte Cheie Urmărite (Keyword Alerts)
- Adaugă orice termen de interes major (ex: *„București”*, *„Inteligență Artificială”*, *„Bursa”*, *„Taxe”*).
- Știrile care conțin termenii urmăriți primesc automat un badge vizual pulsant **⚡ URMĂRIT: [Termen]** direct în feed.
- Sincronizarea în fundal (`SyncNewsWorker`) detectează articolele potrivite și trimite notificări prioritare locale.

### 9. 🗂️ Catalog Complet de Surse RSS & Tab-ul „⭐ Sursele Mele”
- **Peste 150 de publicații verificate live**: organizate pe categorii (Politică, Tehnologie, Business, Sport, Auto, Presă Internațională).
- **Căutare instantă**: găsești orice ziar sau blog tastând în timp real.
- **1-Tap Follow (+)**: bifezi publicațiile preferate, iar ele intră instant în tab-ul dedicat **„⭐ Sursele Mele”**.
- **➕ Adaugă URL Propriu**: introduci orice link RSS din internet cu **validare live pe loc** (confirmare HTTP 200 și număr articole găsite).
- Control complet cu comutator activ/inactiv pe fiecare sursă fără a o șterge definitiv.

### 10. 🔄 Import & Export OPML Standard
- Compatibilitate totală cu standardul internațional de abonamente RSS (OPML 2.0).
- **Export OPML**: generează un fișier standard `.opml` cu toate fluxurile tale personalizate pentru backup sau migrare.
- **Import OPML**: încarcă colecții întregi de feed-uri din Feedly, Inoreader, NetNewsWire sau Google Reader.
- Dialog interactiv integrat direct în foaia de catalog cu copiere rapidă în clipboard sau partajare de fișier.

### 11. 📱 Widget Nativ pe Ecranul Principal (Glanceable Widget)
- Widget Android modern (3x2 / 4x2) pentru ecranul de pornire.
- Afișează ultima știre fierbinte și sinteza AI în 2 rânduri fără a deschide aplicația.
- Atingerea widget-ului deschide aplicația direct pe articolul respectiv.

### 12. 📲 Partajare Vizuală Stilizată (Story / Card Quote Share)
- Buton de Share integrat pe toate cardurile și panourile de detalii.
- Formatează automat un extras stilizat de tip **Quote Card**:
  - Antet de brand `⚡ FLASHNEWS AI • Sinteză Inteligentă`
  - Titlu articol, puncte cheie, impact în România și etichetă de perspectivă editorială.
  - Link direct către sursa originală pentru combaterea dezinformării.

---

## 🏗️ Arhitectură Tehnică:
- **Limbaj & UI**: 100% Kotlin, Jetpack Compose, Material 3.
- **Modularizare Clean Architecture**:
  - `:app` — Punctul de intrare, Navigation și Android Home Widget.
  - `:feature:feed` — Ecranul principal de știri, Commute Mode, Reader Mode, Reading Stats, Radio Sheet, Catalog Sheet, Detalii & Chat AI.
  - `:feature:search` — Căutare avansată în presă și gestionarea alertelor pe cuvinte cheie.
  - `:core:data` — Repository, OPML manager, sincronizare în fundal (WorkManager), gestionare surse personalizate și preferințe utilizator.
  - `:core:database` — Baza de date locală Room (`NewsDatabase`, `NewsDao`, `CustomRssFeedDao`) cu suport complet de migrare și cache offline.
  - `:core:network` — Motorul RssClient, Retrofit, GeminiClient, GrokClient, GroqClient, OpenRouterClient.
  - `:core:model` — Modelele de date partajate.
  - `:core:designsystem` — Tema Material 3, culori, tipografie și componente reutilizabile.

---
---

# 🇬🇧 ENGLISH — FlashNews AI Complete Guide

**FlashNews AI** is an intelligent, high-performance Android news aggregation and cognitive analysis platform designed for Android 12 to 15 (SDK 31-35). It combines a dynamic TikTok-style vertical news feed with a **5-Tier Multi-Agent AI Engine** (Google Gemini, xAI Grok, Groq Llama 3, OpenRouter, and Local On-Device) that provides instant takeaways, editorial bias detection, Romanian local impact assessment, and interactive Q&A.

---

## 🌟 What the Application Does (Key Features):

### 1. 🎙️ Smart Romanian Text-to-Speech at 1.35x High Speed
- **Listen while driving or multitasking**: Android native `TextToSpeech` engine configured specifically for Romanian (`ro-RO`).
- **Tuned to 1.35x speed**: Fast, articulate, and natural voice cadence saving 30%+ reading time.
- **Dynamic speed selector**: Choose between `1.0x`, `1.25x`, `1.35x`, and `1.5x` on the fly.
- Listen buttons available on every news card, detail view, and for all AI interactive answers.

### 2. 📻 AI Radio News Bulletin (3-Minute Morning Podcast)
- Press the **„📻 Radio AI”** button in the header instead of reading dozens of individual articles.
- The AI aggregates the top 5 stories across *Politics, Economy, Technology, and World News* into a coherent morning radio broadcast.
- Features a synchronized full-text script display, play/pause controls, and forward/back navigation.

### 3. 🚗 Commute Mode & Continuous Audio Playback
- **Hands-Free Listening**: Tap the headset icon in the top bar to launch an automated continuous news stream.
- Seamlessly transitions from one article to the next without requiring manual taps.
- Large driving-friendly touch controls: Play/Pause, Skip Next, Rewind, and playlist position counter.
- Automated background pagination: dynamically fetches the next page of articles when playlist runs low.

### 4. 📖 Distraction-Free Smart Reader Mode
- Strips away ads, cookie banners, tracking scripts, and visual clutter to leave only clean editorial text.
- **Customizable Reading Themes**:
  - 🖤 **AMOLED Pitch Black**: true black background for OLED screens with maximum battery savings.
  - 📜 **Warm Sepia**: warm, paper-like tones engineered to minimize eye strain during long sessions.
  - 🌌 **Midnight Indigo**: deep blue night theme for comfortable evening reading.
- **Serif Typography Controls**: 4-step font scaling (`A-`, `A`, `A+`, `A++`) with premium serif type rendering.
- Includes executive AI summary highlights at the top and one-tap clean text clipboard copying.

### 5. 💬 "Ask the News" with Multi-Tone AI Tuning
- Interactive conversational AI assistant embedded within every article.
- **One-tap Tone Switcher**:
  - 💼 **Executive**: concise, decision-oriented summary focusing on figures, business impact, and strategic takeaways.
  - 💡 **Simple**: intuitive explanation broken down in plain language without jargon (ELI5 style).
  - 🔍 **Critical**: investigative analysis highlighting hidden assumptions, conflicts of interest, and omitted perspectives.
- Ready-to-use quick inquiry chips plus support for custom user-written prompts.
- Full 1.35x audio narration for all generated AI responses.

### 6. 🌐 360° Perspective & Editorial Bias Detection
- Automatically identifies when a major news event is covered across multiple independent publications.
- The **„Compare Media (360°)”** feature synthesizes:
  - **Unanimously verified facts**: undisputed facts reported across all outlets.
  - **Editorial nuances & slant**: unique framing, tone, or omissions from each publisher.
  - **Neutrality rating badge** to safeguard against disinformation and media bias.

### 7. 📊 Personal Reading Analytics & Insights
- Interactive dashboard accessible right from the top navigation bar.
- **Minutes Saved with AI**: algorithmically computes time saved by reading synthesized AI takeaways instead of full articles.
- **Articles Read Counter**: tracks reading volume and reading history.
- **Audio Commute Time**: logs cumulative audio minutes listened at high-speed TTS.
- **Bias Shield Indicator**: tracks exposure to fact-checked and verified news sources.

### 8. 🔔 Tracked Keyword Alerts
- Track high-priority keywords (e.g., *„București”*, *„Artificial Intelligence”*, *„Stock Market”*, *„Inflation”*).
- Matching news items in the feed display an illuminated **⚡ TRACKED: [Keyword]** chip.
- Background synchronization worker (`SyncNewsWorker`) alerts you as soon as breaking stories matching your keywords are detected.

### 9. 🗂️ Comprehensive RSS Catalog & "⭐ My Sources" Tab
- **150+ verified media outlets**: categorized across Politics, Tech, Business, Sports, Automotive, and Global press.
- **Real-time search**: find any publication or blog with instant search.
- **1-Tap Follow**: easily add publications to your curated **„⭐ My Sources”** stream.
- **➕ Add Custom RSS URL**: add any external RSS/Atom feed with **instant live validation** (HTTP 200 verification & article count).
- Individual toggle switches to temporarily pause or resume feeds without deleting them.

### 10. 🔄 Standard OPML Import & Export
- Universal compatibility with RSS standard OPML 2.0.
- **Export OPML**: generate a clean `.opml` document containing all custom feeds for backup or synchronization.
- **Import OPML**: effortlessly import existing feed collections from Feedly, Inoreader, NetNewsWire, or Google Reader.
- Built-in interactive management modal with quick clipboard copy and native file sharing.

### 11. 📱 Native Android Home Screen Widget
- Modern glanceable Android widget (sizes 3x2 / 4x2) for your device launcher.
- Shows breaking news headlines with 2-line AI takeaways updated automatically.
- Deep links directly to the selected article inside the app.

### 12. 📲 Visual Styled Quote Sharing
- Share formatted quote cards directly to WhatsApp, Telegram, Signal, or social media.
- Features formatted branding `⚡ FLASHNEWS AI • Sinteză Inteligentă`, executive takeaways, and direct links to the primary source.

---

## 🏗️ Technical Architecture:
- **Language & UI**: 100% Kotlin, Jetpack Compose, Material 3.
- **Clean Architecture Modularization**:
  - `:app` — Entry point, Navigation host, Android Home Screen Widget.
  - `:feature:feed` — Main feed, Commute Mode, Reader Mode, Reading Stats, Radio Sheet, Catalog Sheet, Article Details, Interactive AI Chat.
  - `:feature:search` — Full-text search and keyword alert monitoring.
  - `:core:data` — Repository layer, OPML manager, WorkManager background sync, custom feeds, and user preferences.
  - `:core:database` — Room persistence (`NewsDatabase`, `NewsDao`, `CustomRssFeedDao`) with auto-migrations and offline cache.
  - `:core:network` — High-resilience RSS parsing client, Retrofit, GeminiClient, GrokClient, GroqClient, OpenRouterClient.
  - `:core:model` — Shared data contracts and domain models.
  - `:core:designsystem` — Theme tokens, typography, dark/light color schemes, and reusable UI components.

---

## 🚀 Getting Started

### Prerequisites:
- Android Studio Ladybug or newer
- JDK 17+
- Android Device or Emulator with API 31+ (Android 12+)

### Build & Run:
```bash
# Clone the repository
git clone https://github.com/5imi/FLASHNEWS_AI.git
cd FLASHNEWS_AI

# Run unit tests
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug

# Install on connected device/emulator
./gradlew installDebug
```

---

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
