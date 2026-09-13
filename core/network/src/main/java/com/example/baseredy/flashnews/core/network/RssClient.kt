package com.example.baseredy.flashnews.core.network

import android.util.Xml
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
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
        // [INACTIV - HTTP 403] RssSource("Avocatnet",      "https://www.avocatnet.ro/rss",                      "RO", "Business & Finanțe"),
        // [INACTIV - HTTP 404] RssSource("CECCAR",         "https://www.ceccarbusinessmagazine.ro/feed/",       "RO", "Business & Finanțe"),
        RssSource("Contzilla",      "https://www.contzilla.ro/feed/",                    "RO", "Business & Finanțe"),
        RssSource("TaxNews",        "https://taxnews.ro/feed/",                          "RO", "Business & Finanțe"),
        RssSource("Curs de Guvernare", "https://cursdeguvernare.ro/feed",                 "RO", "Business & Finanțe"),
        RssSource("ZF",             "https://www.zf.ro/rss",                             "RO", "Business & Finanțe"),
        RssSource("Economica",      "https://www.economica.net/rss",                     "RO", "Business & Finanțe"),
        RssSource("Profit.ro",      "https://www.profit.ro/rss",                         "RO", "Business & Finanțe"),
        // [INACTIV - HTTP 403] RssSource("Wall-Street",    "https://www.wall-street.ro/rss",                     "RO", "Business & Finanțe"),
        // [INACTIV - HTML / Not RSS XML] RssSource("Bursa.ro",       "https://www.bursa.ro/rss",                          "RO", "Business & Finanțe"),
        RssSource("Forbes Romania", "https://www.forbes.ro/feed",                        "RO", "Business & Finanțe"),
        // [INACTIV - HTTP 410] RssSource("BusinessMag",    "https://www.businessmagazin.ro/rss",                "RO", "Business & Finanțe"),
        RssSource("Romania Insider", "https://www.romania-insider.com/feed",               "RO", "Business & Finanțe"),
        // [INACTIV - HTML / Not RSS XML] RssSource("Ziarul Financiar", "https://www.ziarulfinanciar.ro/feed/",             "RO", "Business & Finanțe"),
        // [INACTIV - HTTP 410] RssSource("HotNews Finanțe", "https://www.hotnews.ro/rss/finante",                "RO", "Business & Finanțe"),
        RssSource("Financial Intelligence", "https://financialintelligence.ro/feed/",      "RO", "Business & Finanțe"),
        RssSource("Financial Times", "https://www.ft.com/rss/home",                        "GLOBAL", "Business & Finanțe"),
        RssSource("The Economist",   "https://www.economist.com/business/rss.xml",         "GLOBAL", "Business & Finanțe"),
        RssSource("WSJ Business",    "https://feeds.a.dj.com/rss/WSJcomUSBusiness.xml",   "GLOBAL", "Business & Finanțe"),
        // [INACTIV - HTTP 503] RssSource("CNBC",           "https://search.cnbc.com/rs/search/combinedcms/view.xml?id=10000115", "GLOBAL", "Business & Finanțe"),
        RssSource("MarketWatch",    "http://feeds.marketwatch.com/marketwatch/topstories/", "GLOBAL", "Business & Finanțe"),
        // [INACTIV - HTTP 404] RssSource("Forbes US",      "https://www.forbes.com/real-time/feed2/",           "GLOBAL", "Business & Finanțe"),
        RssSource("Fortune",        "https://fortune.com/feed/",                         "GLOBAL", "Business & Finanțe"),
        RssSource("Nikkei Asia",    "https://asia.nikkei.com/rss/feed/nar",              "GLOBAL", "Business & Finanțe"),
        RssSource("CoinDesk",       "https://www.coindesk.com/arc/outboundfeeds/rss/",   "GLOBAL", "Business & Finanțe"),
        RssSource("CoinTelegraph",  "https://cointelegraph.com/feed",                    "GLOBAL", "Business & Finanțe"),
        RssSource("Quartz",         "https://qz.com/feed",                               "GLOBAL", "Business & Finanțe"),

        // Tehnologie
        RssSource("Go4IT",          "https://www.go4it.ro/rss",                          "RO", "Tehnologie"),
        RssSource("ZonaIT",         "https://zonait.ro/feed/",                           "RO", "Tehnologie"),
        // [INACTIV - HTTP 404] RssSource("Mobilissimo",    "https://www.mobilissimo.ro/stiri-telefoane/feed",   "RO", "Tehnologie"),
        RssSource("Gadget.ro",      "https://www.gadget.ro/feed/",                       "RO", "Tehnologie"),
        RssSource("Start-up.ro",    "https://start-up.ro/feed/",                         "RO", "Tehnologie"),
        RssSource("ComputerBlog",   "https://www.computerblog.ro/feed",                   "RO", "Tehnologie"),
        RssSource("TechCrunch",     "https://techcrunch.com/feed/",                      "GLOBAL", "Tehnologie"),
        RssSource("The Verge",      "https://www.theverge.com/rss/index.xml",            "GLOBAL", "Tehnologie"),
        RssSource("Ars Technica",   "https://arstechnica.com/rss/",                      "GLOBAL", "Tehnologie"),
        RssSource("Wired",          "https://www.wired.com/feed/rss",                    "GLOBAL", "Tehnologie"),
        RssSource("Hacker News",    "https://news.ycombinator.com/rss",                  "GLOBAL", "Tehnologie"),
        // [INACTIV - HTTP 403] RssSource("Engadget",       "https://www.engadget.com/rss.xml",                  "GLOBAL", "Tehnologie"),
        RssSource("MIT Tech Review", "https://www.technologyreview.com/feed/",             "GLOBAL", "Tehnologie"),
        RssSource("9to5Mac",        "https://9to5mac.com/feed/",                         "GLOBAL", "Tehnologie"),
        RssSource("IGN",            "https://feeds.feedburner.com/ign/news",             "GLOBAL", "Tehnologie"),
        RssSource("Eurogamer",      "https://www.eurogamer.net/?format=rss",             "GLOBAL", "Tehnologie"),
        RssSource("Polygon",        "https://www.polygon.com/rss/index.xml",             "GLOBAL", "Tehnologie"),
        // [INACTIV - HTTP 429] RssSource("VentureBeat",    "https://venturebeat.com/feed/",                     "GLOBAL", "Tehnologie"),
        RssSource("Gizmodo",        "https://gizmodo.com/rss",                           "GLOBAL", "Tehnologie"),
        RssSource("Mashable",       "https://mashable.com/feeds/rss/all",                "GLOBAL", "Tehnologie"),
        RssSource("The Next Web",   "https://thenextweb.com/feed/",                      "GLOBAL", "Tehnologie"),
        // [INACTIV - HTTP 404] RssSource("ZDNet",          "https://www.zdnet.com/news/rss.xml",                "GLOBAL", "Tehnologie"),
        RssSource("Tom's Hardware", "https://www.tomshardware.com/feeds/all",            "GLOBAL", "Tehnologie"),
        RssSource("TechSpot",       "https://www.techspot.com/backend.xml",              "GLOBAL", "Tehnologie"),
        RssSource("r/technology",   "https://www.reddit.com/r/technology/top/.rss?t=day", "GLOBAL", "Tehnologie"),
        // [INACTIV - HTTP 429] RssSource("r/Futurology",   "https://www.reddit.com/r/Futurology/top/.rss?t=day", "GLOBAL", "Tehnologie"),
        RssSource("Lobsters",       "https://lobste.rs/rss",                             "GLOBAL", "Tehnologie"),
        RssSource("Dev.to",         "https://dev.to/feed",                               "GLOBAL", "Tehnologie"),
        RssSource("Product Hunt",   "https://www.producthunt.com/feed",                  "GLOBAL", "Tehnologie"),
        RssSource("Slashdot",       "https://slashdot.org/slashdot.rss",                 "GLOBAL", "Tehnologie"),

        // Politică
        RssSource("G4Media",        "https://www.g4media.ro/feed",                       "RO", "Politică"),
        RssSource("PressOne",       "https://pressone.ro/feed/",                         "RO", "Politică"),
        RssSource("News.ro",        "https://www.news.ro/rss",                           "RO", "Politică"),
        RssSource("Mediafax",       "https://www.mediafax.ro/rss",                       "RO", "Politică"),
        RssSource("Context.ro",     "https://www.context.ro/feed/",                      "RO", "Politică"),
        RssSource("Recorder",       "https://recorder.ro/feed/",                         "RO", "Politică"),
        RssSource("Snoop",          "https://snoop.ro/feed/",                            "RO", "Politică"),
        RssSource("Spotmedia",      "https://spotmedia.ro/feed",                         "RO", "Politică"),
        RssSource("Politico EU",    "https://www.politico.eu/feed/",                     "GLOBAL", "Politică"),
        RssSource("Foreign Policy", "https://foreignpolicy.com/feed/",                   "GLOBAL", "Politică"),
        RssSource("RealClearPolitics", "https://www.realclearpolitics.com/index.xml",    "GLOBAL", "Politică"),
        RssSource("Guardian Politics", "https://www.theguardian.com/politics/rss",       "GLOBAL", "Politică"),
        RssSource("Le Monde Politique", "https://www.lemonde.fr/politique/rss_full.xml", "GLOBAL", "Politică"),
        // [INACTIV - HTTP 403] RssSource("Euractiv",       "https://www.euractiv.com/feed/",                    "GLOBAL", "Politică"),
        RssSource("Balkan Insight", "https://balkaninsight.com/feed/",                   "GLOBAL", "Politică"),
        RssSource("The Atlantic Politics", "https://feeds.feedburner.com/AtlanticPoliticsChannel", "GLOBAL", "Politică"),
        RssSource("Defense One",    "https://www.defenseone.com/rss/all/",               "GLOBAL", "Politică"),

        // Sport
        RssSource("Digi Sport",     "https://www.digisport.ro/rss",                      "RO", "Sport"),
        // [INACTIV - HTTP 404] RssSource("GSP",            "https://www.gsp.ro/rss",                            "RO", "Sport"),
        RssSource("Prosport",       "https://www.prosport.ro/feed",                      "RO", "Sport"),
        // [INACTIV - HTTP 404] RssSource("Eurosport",      "https://www.eurosport.ro/rss.xml",                  "RO", "Sport"),
        RssSource("Fanatik Sport",   "https://www.fanatik.ro/sport/feed",                  "RO", "Sport"),
        RssSource("BBC Sport",      "https://feeds.bbci.co.uk/sport/rss.xml",             "GLOBAL", "Sport"),
        // [INACTIV - HTML / Not RSS XML] RssSource("ESPN",           "https://www.espn.com/espn/rss/news",                "GLOBAL", "Sport"),
        RssSource("Sky Sports",     "https://www.skysports.com/rss/12040",               "GLOBAL", "Sport"),
        // [INACTIV - HTTP 404] RssSource("Bleacher Report", "https://bleacherreport.com/articles/feed",         "GLOBAL", "Sport"),
        RssSource("Marca English",  "https://e00-marca.uecdn.es/rss/en/football.xml",    "GLOBAL", "Sport"),
        // [INACTIV - HTTP 404] RssSource("Goal.com",       "https://www.goal.com/feeds/en/news",                "GLOBAL", "Sport"),
        RssSource("Guardian Sport", "https://www.theguardian.com/sport/rss",             "GLOBAL", "Sport"),
        // [INACTIV - HTTP 404] RssSource("L'Équipe",       "https://www.lequipe.fr/rss/actu_rss.xml",           "GLOBAL", "Sport"),

        // Auto
        RssSource("Automarket",     "https://www.automarket.ro/rss/",                    "RO", "Auto"),
        RssSource("Promotor",       "https://www.promotor.ro/feed",                      "RO", "Auto"),
        RssSource("Auto-Bild",      "https://www.auto-bild.ro/feed",                     "RO", "Auto"),
        RssSource("Autocritica",    "https://www.autocritica.ro/feed/",                  "RO", "Auto"),
        RssSource("Motor1",         "https://www.motor1.com/rss/news/all/",              "GLOBAL", "Auto"),
        // [INACTIV - HTTP 404] RssSource("Top Gear",       "https://www.topgear.com/rss.xml",                   "GLOBAL", "Auto"),
        RssSource("Autocar UK",     "https://www.autocar.co.uk/rss",                     "GLOBAL", "Auto"),
        RssSource("Jalopnik",       "https://jalopnik.com/rss",                          "GLOBAL", "Auto"),
        RssSource("Car and Driver", "https://www.caranddriver.com/rss/all.xml",          "GLOBAL", "Auto"),
        RssSource("Road & Track",   "https://www.roadandtrack.com/rss/all.xml",          "GLOBAL", "Auto"),
        RssSource("Electrek",       "https://electrek.co/feed/",                         "GLOBAL", "Auto"),

        // Știință & Mediu
        // [INACTIV - HTTP 404] RssSource("Scientia",       "https://www.scientia.ro/feed",                      "RO", "Știință & Mediu"),
        RssSource("Descoperă",      "https://www.descopera.ro/feed",                     "RO", "Știință & Mediu"),
        // [INACTIV - HTTP 404] RssSource("Green Report",    "https://www.greenreport.ro/feed/",                   "RO", "Știință & Mediu"),
        RssSource("Mediu.ro",        "https://www.mediu.ro/feed/",                        "RO", "Știință & Mediu"),
        RssSource("NASA",           "https://www.nasa.gov/rss/dyn/breaking_news.rss",     "GLOBAL", "Știință & Mediu"),
        RssSource("Space.com",      "https://www.space.com/feeds/all",                   "GLOBAL", "Știință & Mediu"),
        RssSource("New Scientist",  "https://www.newscientist.com/feed/home/",           "GLOBAL", "Știință & Mediu"),
        // [INACTIV - HTTP 404] RssSource("NatGeo",         "https://www.nationalgeographic.com/foundation/rss/index.html", "GLOBAL", "Știință & Mediu"),
        // [INACTIV - HTTP 404] RssSource("Live Science",   "https://www.livescience.com/home/feed/about.xml",   "GLOBAL", "Știință & Mediu"),
        RssSource("Phys.org",       "https://phys.org/rss-feed/",                        "GLOBAL", "Știință & Mediu"),
        RssSource("Science Daily",  "https://www.sciencedaily.com/rss/all.xml",          "GLOBAL", "Știință & Mediu"),
        RssSource("ESA Spațiu",     "https://www.esa.int/rssfeed/TopNews",               "GLOBAL", "Știință & Mediu"),
        RssSource("SciTechDaily",   "https://scitechdaily.com/feed/",                    "GLOBAL", "Știință & Mediu"),
        // [INACTIV - HTTP 429] RssSource("r/science",      "https://www.reddit.com/r/science/top/.rss?t=day",   "GLOBAL", "Știință & Mediu"),
        RssSource("BBC Science",    "https://feeds.bbci.co.uk/news/science_and_environment/rss.xml", "GLOBAL", "Știință & Mediu"),
        RssSource("Guardian Science", "https://www.theguardian.com/science/rss",         "GLOBAL", "Știință & Mediu"),
        RssSource("Guardian Environment", "https://www.theguardian.com/environment/rss", "GLOBAL", "Știință & Mediu"),

        // Sănătate
        RssSource("CSID",           "https://www.csid.ro/feed",                          "RO", "Sănătate"),
        RssSource("Doctorul Zilei",  "https://doctorulzilei.ro/feed/",                    "RO", "Sănătate"),
        // [INACTIV - HTTP 404] RssSource("Medical News Today", "https://www.medicalnewstoday.com/feed",         "GLOBAL", "Sănătate"),
        // [INACTIV - HTTP 404] RssSource("WHO News",       "https://www.who.int/feeds/entity/mediacentre/news/en/rss.xml", "GLOBAL", "Sănătate"),
        // [INACTIV - [Errno 11001] getaddrinfo fail] RssSource("WebMD",          "https://rssfeeds.webmd.com/rss/rss.aspx?RSSSource=RSS_PUBLIC", "GLOBAL", "Sănătate"),
        // [INACTIV - HTTP 404] RssSource("Harvard Health", "https://www.health.harvard.edu/blog/rss",           "GLOBAL", "Sănătate"),
        // [INACTIV - HTTP 410] RssSource("NHS UK News",    "https://www.nhs.uk/news/feed/",                     "GLOBAL", "Sănătate"),
        RssSource("CDC Newsroom",   "https://tools.cdc.gov/api/v2/resources/media/316422.rss", "GLOBAL", "Sănătate"),

        // Stil de viață & Cultură
        // [INACTIV - HTTP 404] RssSource("Adevărul Lifestyle", "https://adevarul.ro/lifestyle/rss",               "RO", "Lifestyle"),
        // [INACTIV - HTTP 404] RssSource("Elle Romania",      "https://www.elle.com/ro/rss/",                      "RO", "Lifestyle"),
        RssSource("Viva",           "https://www.viva.ro/feed",                          "RO", "Lifestyle"),
        RssSource("Cinemagia",      "https://www.cinemagia.ro/stiri/rss/",               "RO", "Lifestyle"),
        RssSource("Cultura.ro",      "https://www.cultura.ro/feed",                       "RO", "Lifestyle"),
        // [INACTIV - HTTP 404] RssSource("Travel Romania",  "https://www.travelromania.ro/feed/",                "RO", "Lifestyle"),
        // [INACTIV - HTTP 404] RssSource("Turism.ro",       "https://www.turism.ro/rss",                       "RO", "Lifestyle"),
        RssSource("Vogue",          "https://www.vogue.com/feed/rss",                    "GLOBAL", "Lifestyle"),
        RssSource("BBC Arts",       "https://feeds.bbci.co.uk/news/entertainment_and_arts/rss.xml", "GLOBAL", "Lifestyle"),
        RssSource("Variety",        "https://variety.com/feed/",                         "GLOBAL", "Lifestyle"),
        RssSource("Rolling Stone",  "https://www.rollingstone.com/feed/",                "GLOBAL", "Lifestyle"),
        RssSource("GQ",             "https://www.gq.com/feed/rss",                       "GLOBAL", "Lifestyle"),
        RssSource("The Atlantic Ideas", "https://www.theatlantic.com/feed/channel/ideas/", "GLOBAL", "Lifestyle"),

        // Educație
        RssSource("Edupedu",         "https://www.edupedu.ro/feed/",                      "RO", "Educație"),
        RssSource("Educație Plus",   "https://www.educatieplus.ro/feed/",                 "RO", "Educație"),
        // [INACTIV - [Errno 11001] getaddrinfo fail] RssSource("Școala și Viața",  "https://www.scoalaeviaza.ro/feed/",                 "RO", "Educație"),
        RssSource("EdSurge",        "https://www.edsurge.com/articles_rss",              "GLOBAL", "Educație"),
        // [INACTIV - HTTP 404] RssSource("Inside Higher Ed", "https://www.insidehighered.com/rss/feed/news",    "GLOBAL", "Educație"),

        // General
        RssSource("Digi24",         "https://www.digi24.ro/rss",                         "RO", "General"),
        RssSource("HotNews",        "https://www.hotnews.ro/rss",                        "RO", "General"),
        RssSource("ProTV",          "https://stirileprotv.ro/rss",                       "RO", "General"),
        // [INACTIV - HTTP 404] RssSource("Libertatea",     "https://www.libertatea.ro/rss",                     "RO", "General"),
        // [INACTIV - The read operation timed out] RssSource("Agerpres",       "https://www.agerpres.ro/rss",                       "RO", "General"),
        RssSource("Europa FM",      "https://www.europafm.ro/feed/",                     "RO", "General"),
        RssSource("Adevarul",       "https://adevarul.ro/rss/",                          "RO", "General"),
        // [INACTIV - timed out] RssSource("TVR Info",       "http://stiri.tvr.ro/rss/stiri.xml",                 "RO", "General"),
        // [INACTIV - HTTP 410] RssSource("RFI Romania",    "https://www.rfi.ro/rss-actualitate",                "RO", "General"),
        // [INACTIV - HTTP 429] RssSource("r/romania",      "https://www.reddit.com/r/romania/top/.rss?t=day",   "RO", "General"),
        RssSource("BBC World",      "https://feeds.bbci.co.uk/news/world/rss.xml",         "GLOBAL", "General"),
        // [INACTIV - HTTP 404] RssSource("Reuters",        "https://www.reutersagency.com/feed/",                 "GLOBAL", "General"),
        RssSource("The Guardian",   "https://www.theguardian.com/world/rss",               "GLOBAL", "General"),
        RssSource("CNN World",      "http://rss.cnn.com/rss/edition_world.rss",            "GLOBAL", "General"),
        RssSource("Al Jazeera",     "https://www.aljazeera.com/xml/rss/all.xml",         "GLOBAL", "General"),
        // [INACTIV - HTTP 404] RssSource("Ukrinform",      "https://www.ukrinform.net/block/rss",               "GLOBAL", "General"),
        RssSource("DW News",        "https://rss.dw.com/xml/rss-en-all",                 "GLOBAL", "General"),
        RssSource("France24",       "https://www.france24.com/en/rss",                   "GLOBAL", "General"),
        RssSource("NY Times",       "https://rss.nytimes.com/services/xml/rss/nyt/World.xml", "GLOBAL", "General"),
        RssSource("Euronews",       "https://www.euronews.com/rss?format=xml",           "GLOBAL", "General"),
        // [INACTIV - HTTP 403] RssSource("AP News",        "https://apnews.com/feed",                           "GLOBAL", "General"),
        RssSource("TIME",           "https://time.com/feed/",                            "GLOBAL", "General"),
        RssSource("Sky News UK",    "https://feeds.skynews.com/feeds/rss/home.xml",      "GLOBAL", "General"),
        RssSource("Der Spiegel",    "https://www.spiegel.de/international/index.rss",    "GLOBAL", "General"),
        // [INACTIV - HTTP 406] RssSource("CBS News",       "https://www.cbsnews.com/latest/rss/main",           "GLOBAL", "General"),
        // [INACTIV - HTML / Not RSS XML] RssSource("PBS NewsHour",   "https://www.pbs.org/newshour/feeds/rss/headlines",  "GLOBAL", "General"),
        // [INACTIV - HTTP 370] RssSource("El País English", "https://feeds.elpais.com/mrss-s/pages/ep/site/elpais.com/section/inenglish/portada", "GLOBAL", "General"),
        RssSource("Middle East Eye", "https://www.middleeasteye.net/rss.xml",            "GLOBAL", "General"),
        RssSource("Tagesschau",      "https://www.tagesschau.de/infoservices/alle-meldungen-100~rss2.xml", "GLOBAL", "General"),
        RssSource("Le Figaro",        "https://www.lefigaro.fr/rss/figaro_actualites.xml", "GLOBAL", "General"),
        RssSource("ANSA English",     "https://www.ansa.it/english/english_rss.xml",       "GLOBAL", "General"),
        RssSource("The Hill",        "https://thehill.com/feed/",                         "GLOBAL", "General"),
        // [INACTIV - HTML / Not RSS XML] RssSource("VOA News",        "https://www.voanews.com/api/zpqioumkee",            "GLOBAL", "General"),
        // [INACTIV - HTTP 404] RssSource("Meduza EN",       "https://meduza.io/en/rss/all",                     "GLOBAL", "General"),
        RssSource("Hong Kong FP",   "https://hongkongfp.com/feed/",                     "GLOBAL", "General"),
        RssSource("Hurriyet Daily",  "https://www.hurriyetdailynews.com/rss",            "GLOBAL", "General"),
        RssSource("The Diplomat",    "https://thediplomat.com/feed/",                     "GLOBAL", "General"),
        // [INACTIV - HTTP 404] RssSource("OCCRP",           "https://occrp.org/en/feed/articles",               "GLOBAL", "General"),

        // Israel & Orientul Mijlociu
        // [INACTIV - HTTP 403] RssSource("Times of Israel", "https://www.timesofisrael.com/feed/",              "GLOBAL", "General"),
        RssSource("Jerusalem Post",  "https://www.jpost.com/rss/rssfeedsfrontpage.aspx",  "GLOBAL", "General"),
        // [INACTIV - HTML / Not RSS XML] RssSource("Haaretz",         "https://www.haaretz.com/cmlink/1.4603348",          "GLOBAL", "General"),
        // [INACTIV - HTTP 403] RssSource("Al Arabiya",      "https://english.alarabiya.net/tools/rss",           "GLOBAL", "General"),
        // [INACTIV - HTTP 404] RssSource("Gulf News",       "https://gulfnews.com/rss/world",                    "GLOBAL", "General"),
        // [INACTIV - HTML / Not RSS XML] RssSource("Arab News",       "https://www.arabnews.com/cat/1/rss.xml",            "GLOBAL", "General"),
        // [INACTIV - HTTP 404] RssSource("The National",    "https://www.thenationalnews.com/rss",               "GLOBAL", "General"),

        // Pakistan & Iran
        RssSource("Dawn News",       "https://www.dawn.com/feeds/home/",                  "GLOBAL", "General"),
        RssSource("Express Tribune", "https://tribune.com.pk/feed/home",                  "GLOBAL", "General"),
        RssSource("Tehran Times",    "https://www.tehrantimes.com/rss",                   "GLOBAL", "General"),
        RssSource("IRNA",            "https://en.irna.ir/rss",                            "GLOBAL", "General"),

        // China
        RssSource("SCMP (China)",   "https://www.scmp.com/rss/91/feed",                  "GLOBAL", "General"),
        // [INACTIV - HTTP 404] RssSource("China Daily",    "https://www.chinadaily.com.cn/rss/china_rss.xml",   "GLOBAL", "General"),
        // [INACTIV - HTTP 404] RssSource("CGTN",           "https://www.cgtn.com/subscribe/rss/news.xml",       "GLOBAL", "General"),

        // India
        RssSource("Times of India", "https://timesofindia.indiatimes.com/rssfeedstopstories.cms", "GLOBAL", "General"),
        RssSource("The Hindu",      "https://www.thehindu.com/news/feeder/default.rss",  "GLOBAL", "General"),
        RssSource("NDTV",           "https://feeds.feedburner.com/ndtvnews-top-stories", "GLOBAL", "General"),

        // Rusia
        RssSource("TASS",           "https://tass.com/rss/v2.xml",                       "GLOBAL", "General"),
        RssSource("Moscow Times",   "https://www.themoscowtimes.com/rss/news",           "GLOBAL", "General"),

        // America Latină (Brazilia, Argentina, Mexic)
        // [INACTIV - HTTP 404] RssSource("Folha de S.Paulo", "https://feeds.folha.uol.com.br/emcimadagora/rss091.xml", "GLOBAL", "General"),
        RssSource("Buenos Aires Times", "https://www.batimes.com.ar/feed",                "GLOBAL", "General"),
        // [INACTIV - HTML / Not RSS XML] RssSource("MercoPress",     "https://en.mercopress.com/rss",                     "GLOBAL", "General"),
        RssSource("El País América", "https://feeds.elpais.com/mrss-s/pages/ep/site/elpais.com/section/america/portada", "GLOBAL", "General"),
        RssSource("La Nación Arg",  "https://www.lanacion.com.ar/arc/outboundfeeds/rss/", "GLOBAL", "General"),
        RssSource("Clarín Argentina", "https://www.clarin.com/rss/lo-ultimo/",             "GLOBAL", "General"),

        // SUA & Canada
        // [INACTIV - HTTP 503] RssSource("Washington Post", "https://feeds.washingtonpost.com/rss/world",        "GLOBAL", "General"),
        RssSource("NPR News",       "https://feeds.npr.org/1001/rss.xml",                "GLOBAL", "General"),
        // [INACTIV - HTTP 404] RssSource("CBC Canada",     "https://www.cbc.ca/cxml/rss/rss-topstories.xml",     "GLOBAL", "General"),
        RssSource("Global News CA", "https://globalnews.ca/feed/",                       "GLOBAL", "General"),
        RssSource("LA Times",       "https://latimes.com/world-nation/rss2.0.xml",       "GLOBAL", "General"),

        // Asia & Sud-Estul Asiei (Japonia, Coreea, Singapore, Thailanda, Vietnam, Filipine)
        RssSource("Japan Times",    "https://www.japantimes.co.jp/feed/",                "GLOBAL", "General"),
        RssSource("Yonhap (Korea)", "https://en.yna.co.kr/RSS/news.xml",                 "GLOBAL", "General"),
        RssSource("Straits Times",  "https://www.straitstimes.com/news/world/rss.xml",   "GLOBAL", "General"),
        // [INACTIV - HTTP 404] RssSource("CNA Asia",       "https://www.channelnewsasia.com/api/v1/rss-outbound/rssnews/cna_news.xml", "GLOBAL", "General"),
        RssSource("Bangkok Post",   "https://www.bangkokpost.com/rss/data/topstories.xml", "GLOBAL", "General"),
        RssSource("VnExpress Intl", "https://e.vnexpress.net/rss/news.rss",              "GLOBAL", "General"),
        // [INACTIV - HTML / Not RSS XML] RssSource("Inquirer Ph",    "https://inquirer.net/feed",                         "GLOBAL", "General"),

        // Australia & Noua Zeelandă
        RssSource("ABC Australia",  "https://www.abc.net.au/news/feed/51120/rss.xml",     "GLOBAL", "General"),
        RssSource("RNZ Pacific",    "https://www.rnz.co.nz/rss/pacific.xml",             "GLOBAL", "General"),

        // Africa
        RssSource("AllAfrica",      "https://allafrica.com/tools/headlines/rdf/latest/headlines.rdf", "GLOBAL", "General"),
        RssSource("Africanews",     "https://www.africanews.com/feed/",                  "GLOBAL", "General")
    )

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    // [OLD] - Motiv înlocuire: Optimizare viteză rețea și economie de date prin filtrare prealabilă a surselor RSS
    /*
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS    = 15_000
    private const val MAX_ITEMS_PER_SOURCE = 10
    private val semaphore = Semaphore(10) // Limit concurrency to 10 sources at a time

    suspend fun fetchRssNews(): List<RssItem> = coroutineScope {
        val deferredResults = sources.map { source ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    try {
                        parseRss(source.url, source.name, source.region, source.category)
                    } catch (e: Exception) {
                        println("RSS_DEBUG: Error fetching ${source.name}: ${e.message}")
                        emptyList<RssItem>()
                    }
                }
            }
        }
        deferredResults.awaitAll().flatten().sortedByDescending { it.pubDate }
    }
    */

    private const val CONNECT_TIMEOUT_MS = 6_000
    private const val READ_TIMEOUT_MS    = 8_000
    private const val MAX_ITEMS_PER_SOURCE = 10
    private val semaphore = Semaphore(12) // Limit concurrency to 12 sources at a time

    suspend fun fetchRssNews(targetRegion: String? = null, targetCategory: String? = null): List<RssItem> = coroutineScope {
        val filteredSources = if (targetRegion == null && targetCategory == null) {
            sources
        } else {
            sources.filter { source ->
                val regionMatch = targetRegion == null || targetRegion == "ALL" || source.region == targetRegion
                val categoryMatch = targetCategory == null || targetCategory == "Toate" || 
                                    source.category.equals(targetCategory, ignoreCase = true) || 
                                    source.category == "General" // Keep General for important context
                regionMatch && categoryMatch
            }.ifEmpty { sources }
        }

        val deferredResults = filteredSources.map { source ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    try {
                        parseRss(source.url, source.name, source.region, source.category)
                    } catch (e: Exception) {
                        println("RSS_DEBUG: Error fetching ${source.name}: ${e.message}")
                        emptyList<RssItem>()
                    }
                }
            }
        }
        deferredResults.awaitAll().flatten().sortedByDescending { it.pubDate }
    }

    private fun cleanHtml(raw: String): String {
        if (raw.isBlank()) return ""
        return raw
            .replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&rsquo;", "’")
            .replace("&lsquo;", "‘")
            .replace("&rdquo;", "”")
            .replace("&ldquo;", "“")
            .replace("&ndash;", "–")
            .replace("&mdash;", "—")
            .replace(Regex("&#8230;|&hellip;"), "…")
            .replace(Regex("&#(\\d+);")) { match ->
                val code = match.groupValues[1].toIntOrNull()
                if (code != null && code in 32..65535) code.toChar().toString() else match.value
            }
            .replace(Regex("\\s+"), " ")
            .trim()
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
                                insideItem && tagName == "title" -> currentTitle = cleanHtml(safeNextText(parser))
                                insideItem && tagName == "link" -> {
                                    val href = parser.getAttributeValue(null, "href")
                                    currentLink = if (!href.isNullOrBlank()) href else safeNextText(parser)
                                }
                                insideItem && (tagName == "description" || tagName == "summary") -> {
                                    currentDescription = cleanHtml(safeNextText(parser))
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

    fun getAllCatalogSources(): List<RssSource> = sources

    suspend fun fetchFromCustomSources(customSources: List<RssSource>): List<RssItem> = coroutineScope {
        if (customSources.isEmpty()) return@coroutineScope emptyList()
        val customSemaphore = Semaphore(8)
        val deferredList = customSources.map { source ->
            async(Dispatchers.IO) {
                customSemaphore.withPermit {
                    parseRss(source.url, source.name, source.region, source.category)
                }
            }
        }
        deferredList.awaitAll().flatten()
    }

    suspend fun validateRssFeed(urlString: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val url = URL(urlString.trim())
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 7000
            connection.readTimeout = 7000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; FlashNews/1.4)")
            connection.instanceFollowRedirects = true
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                return@withContext Pair(false, "Serverul a returnat HTTP " + responseCode)
            }
            var channelTitle = ""
            var itemCount = 0
            val parser = Xml.newPullParser()
            connection.inputStream.use { stream ->
                parser.setInput(stream, null)
                var eventType = parser.eventType
                var insideChannel = false
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    val tagName = parser.name ?: ""
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            if (tagName.equals("channel", ignoreCase = true) || tagName.equals("feed", ignoreCase = true)) {
                                insideChannel = true
                            } else if (tagName.equals("title", ignoreCase = true) && insideChannel && channelTitle.isBlank()) {
                                channelTitle = safeNextText(parser)
                            } else if (tagName.equals("item", ignoreCase = true) || tagName.equals("entry", ignoreCase = true)) {
                                itemCount++
                                if (itemCount >= 3) break
                            }
                        }
                    }
                    eventType = parser.next()
                }
            }
            if (itemCount > 0) {
                val titleDisplay = if (channelTitle.isNotBlank()) channelTitle else "Flux RSS Valid"
                Pair(true, titleDisplay)
            } else {
                Pair(false, "Nu s-au gasit articole in format RSS/Atom")
            }
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "Eroare la conectare")
        }
    }

}
