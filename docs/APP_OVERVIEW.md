# ⚡ FlashNews AI — Next-Gen AI News Aggregator & Personal Radio

<div align="center">

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg)](https://kotlinlang.org)
[![Android SDK](https://img.shields.io/badge/Target%20SDK-35%20(Vanilla%20Ice%20Cream)-green.svg)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Compose-Material%203-blueviolet.svg)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20Modular-orange.svg)](docs/ARCHITECTURE.md)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Release](https://img.shields.io/badge/Latest%20Release-v1.4.0-success.svg)](releases/FlashNews_AI_v1.4.0.apk)

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
  *„Bună dimineața! Iată sinteza celor mai importante evenimente ale momentului în FlashNews: În economie (...), pe plan extern (...), iar în tehnologie (...). Te ținem la curent cu restul detaliilor în FlashNews!”*
- Player dedicat cu Play, Pauză, salt temporal și scriptul afișat sincronizat.

### 3. 💬 „Întreabă Știrea” (Chat Interactiv cu Articolul)
- Mini-asistent conversațional inteligent integrat în fiecare articol.
- **Cipuri de întrebări rapide (1-Tap)**:
  - *„Cum mă afectează direct?”*
  - *„Context istoric pe scurt”*
  - *„Explică-mi ca unui copil de 10 ani”*
  - *„Ce spun vocile critice?”*
- Câmp liber pentru orice întrebare personalizată.
- **Ascultare Audio a Răspunsului**: Fiecare răspuns primit de la AI poate fi ascultat audio dintr-o atingere la 1.35x.

### 4. 🌐 Perspectivă 360° & Anti-Manipulare (Comparație Între Ziare)
- Detectează automat când același subiect major este relatat de mai multe publicații diferite (*Digi24, HotNews, Agerpres, Libertatea etc.*).
- Butonul **„Compară ziarele (360°)”** produce o sinteză critică obiectivă:
  - **Fapte confirmate unanim**: ce este cert și raportat identic de toți.
  - **Nuanțe & Tente editoriale**: ce a evidențiat sau a omis fiecare redacție.
  - **Concluzie de neutralitate și etichetă vizuală**.

### 5. 🗂️ Catalog Complet de Surse RSS & Tab-ul „⭐ Sursele Mele”
- **Peste 150 de publicații verificate live**: organizate pe categorii (Politică, Tehnologie, Business, Sport, Auto, Presă Internațională).
- **Căutare instantă**: găsești orice ziar sau blog tastând în timp real.
- **1-Tap Follow (+)**: bifezi publicațiile preferate, iar ele intră instant în tab-ul dedicat **„⭐ Sursele Mele”**.
- **➕ Adaugă URL Propriu**: introduci orice link RSS din internet cu **validare live pe loc** (confirmare HTTP 200 și număr articole găsite).
- Control complet cu comutator activ/inactiv pe fiecare sursă fără a o șterge definitiv.

### 6. 📱 Widget Nativ pe Ecranul Principal (Glanceable Widget)
- Widget Android modern (3x2 / 4x2) pentru ecranul de pornire.
- Afișează ultima știre fierbinte și sinteza AI în 2 rânduri fără a deschide aplicația.
- Atingerea widget-ului deschide aplicația direct pe articolul respectiv.

### 7. 📲 Partajare Nativă cu un Singur Tap
- Buton de Share integrat pe toate cardurile.
- Formatează automat titlul, sinteza esențială AI și link-ul sursei originale pentru partajare pe WhatsApp, Telegram, Signal, E-mail etc.

---

## 🏗️ Arhitectură Tehnică:
- **Limbaj & UI**: 100% Kotlin, Jetpack Compose, Material 3.
- **Modularizare Clean Architecture**:
  - `:app` — Punctul de intrare, Navigation și Android Home Widget.
  - `:feature:feed` — Ecranul principal de știri, Radio Sheet, Catalog Sheet, Detalii & Chat AI.
  - `:feature:search` — Căutare avansată în presă.
  - `:core:data` — Repository, sincronizare în fundal (WorkManager), gestionare surse personalizate.
  - `:core:database` — Baza de date locală Room (`NewsDatabase`, `NewsDao`, `CustomRssFeedDao`) cu cache offline.
  - `:core:network` — Motorul RssClient, Retrofit, GeminiClient, GrokClient, GroqClient, OpenRouterClient.
  - `:core:model` — Modelele de date partajate.
  - `:core:designsystem` — Tema Material 3, culori, tipografie și componente reutilizabile.

---
---

# 🇬🇧 ENGLISH — FlashNews AI Complete Guide

**FlashNews AI** is an intelligent, high-performance Android news aggregation and cognitive analysis platform designed for Android 12 to 15 (SDK 31-35). It combines a dynamic TikTok-style vertical news feed with a **5-Tier Multi-Agent AI Engine** (Google Gemini, xAI Grok, Groq Llama 3, OpenRouter, and Local On-Device) that provides instant takeaways, editorial bias detection, Romanian local impact assessment, and interactive Q&A.

---

## 🌟 What the Application Does (Core Features):

### 1. 🎙️ On-Device Text-to-Speech (TTS) at High Speed (1.35x)
- **Listen hands-free while driving or commuting**: Built using Android's native `TextToSpeech` engine configured for Romanian (`ro-RO`) and English.
- **Optimized for 1.35x reading speed**: Fast, energetic, and natural pronunciation that saves over 30% of listening time.
- **On-the-fly speed controls**: Instant toggle between `1.0x`, `1.25x`, `1.35x (Fast)`, and `1.5x`.
- Audio toggle buttons available on every news card, in the article details modal, and on AI chat answers.

### 2. 📻 Daily Radio AI Briefing / Morning Podcast (3 Minutes with Coffee)
- Instead of manually scrolling through dozens of articles, tap the **„📻 Radio AI”** header button.
- The AI algorithm selects top 5 crucial news items across diverse categories (*Politics, Economy, Technology, World*) and scripts a **seamless radio podcast**:
  *"Good morning! Here is your FlashNews briefing on today's most important headlines: In economy (...), on the international stage (...), and in technology (...). Stay informed with FlashNews!"*
- Interactive player with Play, Pause, speed adjustments, and synchronized script display.

### 3. 💬 „Chat with Article” (Interactive Contextual Q&A)
- Conversational mini-assistant directly inside every article view.
- **1-Tap Quick Prompt Chips**:
  - *"How does this affect me directly?"*
  - *"Brief historical context"*
  - *"Explain like I'm 10 years old"*
  - *"What do critics argue?"*
- Free-text input field for custom inquiries answered strictly using article context.
- **Audio Playback for AI Answers**: Listen to any AI response instantly with one tap at 1.35x speed.

### 4. 🌐 360° Perspective & Anti-Manipulation (Cross-Outlet Comparison)
- Automatically detects when a trending event is covered by multiple publications (*e.g., Digi24, HotNews, Reuters, BBC*).
- The **„Compare Outlets (360°)”** button produces an objective debunker summary:
  - **Unanimously confirmed facts**: what all sources agree upon.
  - **Editorial nuances & selective omissions**: highlights unique angles or missing facts.
  - **Neutrality and bias index rating**.

### 5. 🗂️ Complete RSS Source Catalog & „My Sources” Personal Feed
- **150+ live-verified news outlets**: structured across categories (Politics, Tech, Business, Sports, Automotive, World).
- **Real-time search**: find any publisher or topic in seconds.
- **1-Tap Follow (+)**: bookmark your favorites and access them in the dedicated **„⭐ My Sources”** tab on the main feed.
- **➕ Add Custom RSS URL**: paste any RSS/Atom feed from the web with **instant live validation** (verifies HTTP 200 response and extracted articles count).
- Full user control with on/off toggles to mute sources without permanently deleting them.

### 6. 📱 Glanceable Android Home Screen Widget
- Sleek dark-mode Android AppWidget (3x2 / 4x2) for your phone's Home Screen.
- Displays the latest breaking headline and 2-line AI takeaway at a glance.
- Tapping the widget opens the app directly into that specific story.

### 7. 📲 One-Tap Native System Share
- Direct integration with Android's native share sheet (`Intent.ACTION_SEND`).
- Formats headline, AI core summary, and original URL ready to share to WhatsApp, Telegram, Signal, Mail, etc.

---

## 🛠️ Tech Stack & Architecture:
- **Language & UI**: 100% Kotlin, Jetpack Compose, Material 3 Design System.
- **Clean Architecture Modules**:
  - `:app` — Application entrypoint, Navigation host, and Home Screen Widget Provider.
  - `:feature:feed` — Feed pager, Radio Briefing Sheet, Catalog Sheet, Article Details & Chat.
  - `:feature:search` — Full-text and categorical search.
  - `:core:data` — Data repositories, WorkManager background synchronization, custom feeds orchestration.
  - `:core:database` — Room persistent SQLite database (`NewsDatabase`, `NewsDao`, `CustomRssFeedDao`) with offline caching.
  - `:core:network` — RSS client engine, Retrofit, GeminiClient, GrokClient, GroqClient, OpenRouterClient.
  - `:core:model` — Shared domain data models.
  - `:core:designsystem` — Theme, typography, colors, and reusable UI components.

---

## 📦 Releases & Installation:
Download the latest signed installable Android APK:
- **[Latest Release APK v1.4.0 (63.4 MB)](releases/FlashNews_AI_v1.4.0.apk)**
- Requires Android 12+ (API level 31 or higher).
