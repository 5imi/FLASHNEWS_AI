package com.example.baseredy.flashnews.core.network

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

data class RssItem(
    val title: String,
    val link: String,
    val description: String,
    val pubDate: String,
    val sourceName: String,
    val region: String,
    val category: String, // NOU: Domeniul de interes (Business, Sport etc.)
    val imageUrl: String? = null
)

data class RssSource(
    val name: String,
    val url: String,
    val region: String,
    val category: String
)

object RssClient {
    // Surse RSS mapate pe domenii de interes
    private val sources = listOf(
        // Fiscalitate & Contabilitate
        RssSource("Avocatnet",    "https://www.avocatnet.ro/rss",                      "RO", "Fiscalitate"),
        RssSource("CECCAR",       "https://www.ceccarbusinessmagazine.ro/feed/",       "RO", "Fiscalitate"),
        RssSource("Contzilla",    "https://www.contzilla.ro/feed/",                    "RO", "Fiscalitate"),
        
        // Business & Economie
        RssSource("ZF",           "https://www.zf.ro/rss",                             "RO", "Business"),
        RssSource("Economica",    "https://www.economica.net/rss",                     "RO", "Business"),
        RssSource("Profit.ro",    "https://www.profit.ro/rss",                         "RO", "Business"),

        // Sport
        RssSource("Digi Sport",   "https://www.digisport.ro/rss",                      "RO", "Sport"),
        RssSource("GSP",          "https://www.gsp.ro/rss",                            "RO", "Sport"),

        // Tehnologie
        RssSource("Go4IT",        "https://www.go4it.ro/rss",                          "RO", "Tehnologie"),
        RssSource("ZonaIT",       "https://zonait.ro/feed/",                           "RO", "Tehnologie"),

        // Politică (Investigații)
        RssSource("G4Media",      "https://www.g4media.ro/feed",                       "RO", "Politică"),

        // General
        RssSource("Digi24",       "https://www.digi24.ro/rss",                         "RO", "General"),
        RssSource("HotNews",      "https://www.hotnews.ro/rss",                        "RO", "General"),
        RssSource("ProTV",        "https://stirileprotv.ro/rss",                       "RO", "General"),
        RssSource("Libertatea",   "https://www.libertatea.ro/rss",                     "RO", "General"),

        // Internațional
        RssSource("BBC World",    "https://feeds.bbci.co.uk/news/world/rss.xml",       "GLOBAL", "General"),
        RssSource("Al Jazeera",   "https://www.aljazeera.com/xml/rss/all.xml",         "ME", "General"),
        RssSource("Ukrinform",    "https://www.ukrinform.net/block/rss",               "UA", "General")
    )

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS    = 15_000
    private const val MAX_ITEMS_PER_SOURCE = 10

    suspend fun fetchRssNews(): List<RssItem> = withContext(Dispatchers.IO) {
        val allItems = mutableListOf<RssItem>()
        sources.forEach { source ->
            try {
                println("RSS_DEBUG: Fetching from ${source.name} at ${source.url}")
                val results = parseRss(source.url, source.name, source.region, source.category)
                println("RSS_DEBUG: Got ${results.size} items from ${source.name} [${source.category}]")
                allItems.addAll(results)
            } catch (e: Exception) {
                println("RSS_DEBUG: Error fetching ${source.name}: ${e.javaClass.simpleName} — ${e.message}")
            }
        }
        allItems.sortedByDescending { it.pubDate }
    }

    private fun parseRss(urlString: String, sourceName: String, region: String, category: String): List<RssItem> {
        val items = mutableListOf<RssItem>()

        val connection = URL(urlString).openConnection().apply {
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml, */*")
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout    = READ_TIMEOUT_MS
        }

        connection.getInputStream().use { inputStream ->
            val parser = Xml.newPullParser().apply {
                // FIX: activăm namespace-urile pentru a recunoaște media:content, dc:, etc.
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
                // FIX: specificăm explicit UTF-8 pentru diacritice românești
                setInput(inputStream, "UTF-8")
            }

            var eventType = parser.eventType

            var currentTitle       = ""
            var currentLink        = ""
            var currentDescription = ""
            var currentPubDate     = ""
            var currentImageUrl: String? = null
            var insideItem         = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name

                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when {
                            // Intrăm într-un element <item> sau <entry> (Atom)
                            tagName == "item" || tagName == "entry" -> {
                                insideItem = true
                                currentTitle       = ""
                                currentLink        = ""
                                currentDescription = ""
                                currentPubDate     = ""
                                currentImageUrl    = null
                            }

                            // FIX CRITIC: citim textul la START_TAG cu nextText()
                            insideItem && tagName == "title" -> {
                                currentTitle = safeNextText(parser)
                            }
                            insideItem && tagName == "link" -> {
                                // Atom <link href="..."/> vs RSS <link>url</link>
                                val href = parser.getAttributeValue(null, "href")
                                currentLink = if (!href.isNullOrBlank()) href else safeNextText(parser)
                            }
                            insideItem && (tagName == "description" || tagName == "summary") -> {
                                val raw = safeNextText(parser)
                                // Eliminăm tag-uri HTML din descriere
                                currentDescription = raw.replace(Regex("<[^>]*>"), "").trim()
                            }
                            insideItem && (tagName == "pubDate" || tagName == "published" || tagName == "updated") -> {
                                currentPubDate = safeNextText(parser)
                            }

                            // Imagini via <enclosure>
                            insideItem && tagName == "enclosure" -> {
                                val type = parser.getAttributeValue(null, "type") ?: ""
                                if (type.startsWith("image") && currentImageUrl == null) {
                                    currentImageUrl = parser.getAttributeValue(null, "url")
                                }
                            }

                            // Imagini via <media:content> sau <media:thumbnail>
                            insideItem && (tagName == "content" || tagName == "thumbnail") -> {
                                val url = parser.getAttributeValue(null, "url")
                                if (!url.isNullOrBlank() && currentImageUrl == null) {
                                    currentImageUrl = url
                                }
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        when {
                            // Finalizăm un item
                            (tagName == "item" || tagName == "entry") && insideItem -> {
                                insideItem = false
                                if (currentTitle.isNotBlank() && currentLink.isNotBlank()) {
                                    items.add(
                                        RssItem(
                                            title       = currentTitle.trim(),
                                            link        = currentLink.trim(),
                                            description = currentDescription.ifBlank { currentTitle },
                                            pubDate     = formatToIso(currentPubDate),
                                            sourceName  = sourceName,
                                            region      = region,
                                            category    = category,
                                            imageUrl    = currentImageUrl
                                        )
                                    )
                                }
                                if (items.size >= MAX_ITEMS_PER_SOURCE) return items
                            }
                        }
                    }
                }

                eventType = parser.next()
            }
        }

        return items.take(MAX_ITEMS_PER_SOURCE)
    }

    /**
     * Citire sigură a textului elementului curent.
     * Returnează șir gol în caz de excepție (ex: tag CDATA sau element gol).
     */
    private fun safeNextText(parser: XmlPullParser): String {
        return try {
            parser.nextText() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Convertim data RSS (RFC 1123 sau ISO 8601 sau altele) la ISO_OFFSET_DATE_TIME
     * pentru sortare și afișare uniformă.
     */
    private fun formatToIso(pubDate: String): String {
        if (pubDate.isBlank()) return ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        // Încercăm mai multe formate uzuale în feed-urile RSS românești
        val formatters = listOf(
            DateTimeFormatter.RFC_1123_DATE_TIME,                            // Tue, 14 Jul 2026 12:00:00 +0300
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,                          // 2026-07-14T12:00:00+03:00
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"),           // 2026-07-14T12:00:00+0300
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
        )

        for (formatter in formatters) {
            try {
                val zdt = ZonedDateTime.parse(pubDate.trim(), formatter)
                return zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            } catch (_: DateTimeParseException) { }
            catch (_: Exception) { }
        }

        // Fallback: data curentă
        println("RSS_DEBUG: Could not parse date: '$pubDate'")
        return ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }
}
