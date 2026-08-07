package com.example.baseredy.flashnews.core.network

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class RssItem(
    val title: String,
    val link: String,
    val description: String,
    val pubDate: String,
    val sourceName: String,
    val region: String,
    val category: String,
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
        // Business & Finanțe (Comasate)
        RssSource("Avocatnet",      "https://www.avocatnet.ro/rss",                      "RO", "Business & Finanțe"),
        RssSource("CECCAR",         "https://www.ceccarbusinessmagazine.ro/feed/",       "RO", "Business & Finanțe"),
        RssSource("Contzilla",      "https://www.contzilla.ro/feed/",                    "RO", "Business & Finanțe"),
        RssSource("TaxNews",        "https://taxnews.ro/feed/",                          "RO", "Business & Finanțe"),
        RssSource("Curs de Guvernare", "https://cursdeguvernare.ro/feed",                 "RO", "Business & Finanțe"),
        RssSource("ZF",             "https://www.zf.ro/rss",                             "RO", "Business & Finanțe"),
        RssSource("Economica",      "https://www.economica.net/rss",                     "RO", "Business & Finanțe"),
        RssSource("Profit.ro",      "https://www.profit.ro/rss",                         "RO", "Business & Finanțe"),
        RssSource("Wall-Street",    "https://www.wall-street.ro/rss",                     "RO", "Business & Finanțe"),
        RssSource("Bursa.ro",       "https://www.bursa.ro/rss",                          "RO", "Business & Finanțe"),
        RssSource("Forbes Romania", "https://www.forbes.ro/feed",                        "RO", "Business & Finanțe"),
        RssSource("BusinessMag",    "https://www.businessmagazin.ro/rss",                "RO", "Business & Finanțe"),
        RssSource("Romania Insider", "https://www.romania-insider.com/feed",               "RO", "Business & Finanțe"),
        RssSource("Ziarul Financiar", "https://www.ziarulfinanciar.ro/feed/",             "RO", "Business & Finanțe"),
        RssSource("HotNews Finanțe", "https://www.hotnews.ro/rss/finante",                "RO", "Business & Finanțe"),
        RssSource("Financial Times", "https://www.ft.com/rss/home",                        "GLOBAL", "Business & Finanțe"),
        RssSource("The Economist",   "https://www.economist.com/business/rss.xml",         "GLOBAL", "Business & Finanțe"),

        // Tehnologie
        RssSource("Go4IT",          "https://www.go4it.ro/rss",                          "RO", "Tehnologie"),
        RssSource("ZonaIT",         "https://zonait.ro/feed/",                           "RO", "Tehnologie"),
        RssSource("Mobilissimo",    "https://www.mobilissimo.ro/stiri-telefoane/feed",   "RO", "Tehnologie"),
        RssSource("Gadget.ro",      "https://www.gadget.ro/feed/",                       "RO", "Tehnologie"),
        RssSource("Start-up.ro",    "https://start-up.ro/feed/",                         "RO", "Tehnologie"),
        RssSource("ComputerBlog",   "https://www.computerblog.ro/feed",                   "RO", "Tehnologie"),
        RssSource("TechCrunch",     "https://techcrunch.com/feed/",                      "GLOBAL", "Tehnologie"),
        RssSource("The Verge",      "https://www.theverge.com/rss/index.xml",            "GLOBAL", "Tehnologie"),
        RssSource("Ars Technica",   "https://arstechnica.com/rss/",                      "GLOBAL", "Tehnologie"),

        // Politică
        RssSource("G4Media",        "https://www.g4media.ro/feed",                       "RO", "Politică"),
        RssSource("PressOne",       "https://pressone.ro/feed/",                         "RO", "Politică"),
        RssSource("News.ro",        "https://www.news.ro/rss",                           "RO", "Politică"),
        RssSource("Mediafax",       "https://www.mediafax.ro/rss",                       "RO", "Politică"),
        RssSource("Context.ro",     "https://www.context.ro/feed/",                      "RO", "Politică"),

        // Sport
        RssSource("Digi Sport",     "https://www.digisport.ro/rss",                      "RO", "Sport"),
        RssSource("GSP",            "https://www.gsp.ro/rss",                            "RO", "Sport"),
        RssSource("Prosport",       "https://www.prosport.ro/feed",                      "RO", "Sport"),
        RssSource("Eurosport",      "https://www.eurosport.ro/rss.xml",                  "RO", "Sport"),
        RssSource("Fanatik Sport",   "https://www.fanatik.ro/sport/feed",                  "RO", "Sport"),

        // Auto
        RssSource("Automarket",     "https://www.automarket.ro/rss/",                    "RO", "Auto"),
        RssSource("Promotor",       "https://www.promotor.ro/feed",                      "RO", "Auto"),
        RssSource("Auto-Bild",      "https://www.auto-bild.ro/feed",                     "RO", "Auto"),

        // Știință & Mediu
        RssSource("Scientia",       "https://www.scientia.ro/feed",                      "RO", "Știință & Mediu"),
        RssSource("Descoperă",      "https://www.descopera.ro/feed",                     "RO", "Știință & Mediu"),
        RssSource("Green Report",    "https://www.greenreport.ro/feed/",                   "RO", "Știință & Mediu"),
        RssSource("Mediu.ro",        "https://www.mediu.ro/feed/",                        "RO", "Știință & Mediu"),

        // Sănătate
        RssSource("CSID",           "https://www.csid.ro/feed",                          "RO", "Sănătate"),
        RssSource("Doctorul Zilei",  "https://doctorulzilei.ro/feed/",                    "RO", "Sănătate"),

        // Stil de viață & Cultură
        RssSource("Adevărul Lifestyle", "https://adevarul.ro/lifestyle/rss",               "RO", "Lifestyle"),
        RssSource("Elle Romania",      "https://www.elle.com/ro/rss/",                      "RO", "Lifestyle"),
        RssSource("Viva",           "https://www.viva.ro/feed",                          "RO", "Lifestyle"),
        RssSource("Cinemagia",      "https://www.cinemagia.ro/stiri/rss/",               "RO", "Lifestyle"),
        RssSource("Cultura.ro",      "https://www.cultura.ro/feed",                       "RO", "Lifestyle"),
        RssSource("Travel Romania",  "https://www.travelromania.ro/feed/",                "RO", "Lifestyle"),
        RssSource("Turism.ro",       "https://www.turism.ro/rss",                       "RO", "Lifestyle"),

        // Educație
        RssSource("Educație Plus",   "https://www.educatieplus.ro/feed/",                 "RO", "Educație"),
        RssSource("Școala și Viața",  "https://www.scoalaeviaza.ro/feed/",                 "RO", "Educație"),

        // General
        RssSource("Digi24",         "https://www.digi24.ro/rss",                         "RO", "General"),
        RssSource("HotNews",        "https://www.hotnews.ro/rss",                        "RO", "General"),
        RssSource("ProTV",          "https://stirileprotv.ro/rss",                       "RO", "General"),
        RssSource("Libertatea",     "https://www.libertatea.ro/rss",                     "RO", "General"),
        RssSource("Agerpres",       "https://www.agerpres.ro/rss",                       "RO", "General"),
        RssSource("Europa FM",      "https://www.europafm.ro/feed/",                     "RO", "General"),
        RssSource("Adevarul",       "https://adevarul.ro/rss/",                          "RO", "General"),
        RssSource("TVR Info",       "http://stiri.tvr.ro/rss/stiri.xml",                 "RO", "General"),
        RssSource("RFI Romania",    "https://www.rfi.ro/rss-actualitate",                "RO", "General"),
        RssSource("BBC World",      "https://feeds.bbci.co.uk/news/world/rss.xml",         "GLOBAL", "General"),
        RssSource("Reuters",        "https://www.reutersagency.com/feed/",                 "GLOBAL", "General"),
        RssSource("The Guardian",   "https://www.theguardian.com/world/rss",               "GLOBAL", "General"),
        RssSource("CNN World",      "http://rss.cnn.com/rss/edition_world.rss",            "GLOBAL", "General"),
        RssSource("Al Jazeera",     "https://www.aljazeera.com/xml/rss/all.xml",         "GLOBAL", "General"),
        RssSource("Ukrinform",      "https://www.ukrinform.net/block/rss",               "GLOBAL", "General"),
        RssSource("DW News",        "https://rss.dw.com/xml/rss-en-all",                 "GLOBAL", "General"),
        RssSource("France24",       "https://www.france24.com/en/rss",                   "GLOBAL", "General"),
        RssSource("NY Times",       "https://rss.nytimes.com/services/xml/rss/nyt/World.xml", "GLOBAL", "General")
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
                val results = parseRss(source.url, source.name, source.region, source.category)
                allItems.addAll(results)
            } catch (e: Exception) {
                println("RSS_DEBUG: Error fetching ${source.name}: ${e.message}")
            }
        }
        allItems.sortedByDescending { it.pubDate }
    }

    private fun parseRss(urlString: String, sourceName: String, region: String, category: String): List<RssItem> {
        val items = mutableListOf<RssItem>()
        try {
            val connection = URL(urlString).openConnection().apply {
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml, */*")
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout    = READ_TIMEOUT_MS
            }

            connection.getInputStream().use { inputStream ->
                val parser = Xml.newPullParser().apply {
                    setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
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
                                tagName == "item" || tagName == "entry" -> {
                                    insideItem = true
                                    currentTitle = ""; currentLink = ""; currentDescription = ""; currentPubDate = ""; currentImageUrl = null
                                }
                                insideItem && tagName == "title" -> currentTitle = safeNextText(parser)
                                insideItem && tagName == "link" -> {
                                    val href = parser.getAttributeValue(null, "href")
                                    currentLink = if (!href.isNullOrBlank()) href else safeNextText(parser)
                                }
                                insideItem && (tagName == "description" || tagName == "summary") -> {
                                    currentDescription = safeNextText(parser).replace(Regex("<[^>]*>"), "").trim()
                                }
                                insideItem && (tagName == "pubDate" || tagName == "published" || tagName == "updated") -> {
                                    currentPubDate = safeNextText(parser)
                                }
                                insideItem && tagName == "enclosure" -> {
                                    val type = parser.getAttributeValue(null, "type") ?: ""
                                    if (type.startsWith("image") && currentImageUrl == null) {
                                        currentImageUrl = parser.getAttributeValue(null, "url")
                                    }
                                }
                                insideItem && (tagName == "content" || tagName == "thumbnail") -> {
                                    val url = parser.getAttributeValue(null, "url")
                                    if (!url.isNullOrBlank() && currentImageUrl == null) currentImageUrl = url
                                }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if ((tagName == "item" || tagName == "entry") && insideItem) {
                                insideItem = false
                                if (currentTitle.isNotBlank() && currentLink.isNotBlank()) {
                                    items.add(RssItem(
                                        title       = currentTitle.trim(),
                                        link        = currentLink.trim(),
                                        description = currentDescription.ifBlank { currentTitle },
                                        pubDate     = formatToIso(currentPubDate),
                                        sourceName  = sourceName,
                                        region      = region,
                                        category    = category,
                                        imageUrl    = currentImageUrl
                                    ))
                                }
                                if (items.size >= MAX_ITEMS_PER_SOURCE) return items
                            }
                        }
                    }
                    eventType = parser.next()
                }
            }
        } catch (e: Exception) {
            println("RSS_DEBUG: Parser error for $sourceName: ${e.message}")
        }
        return items.take(MAX_ITEMS_PER_SOURCE)
    }

    private fun safeNextText(parser: XmlPullParser): String {
        return try { parser.nextText() ?: "" } catch (e: Exception) { "" }
    }

    private fun formatToIso(pubDate: String): String {
        if (pubDate.isBlank()) return ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val formatters = listOf(
            DateTimeFormatter.RFC_1123_DATE_TIME,
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"),
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
        )
        for (formatter in formatters) {
            try {
                return ZonedDateTime.parse(pubDate.trim(), formatter).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            } catch (_: Exception) { }
        }
        return ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }
}
