package com.example.baseredy.flashnews.core.data

import com.example.baseredy.flashnews.core.database.CustomRssFeedEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpmlManagerTest {

    @Test
    fun exportToOpml_producesValidOpmlXml() {
        val feeds = listOf(
            CustomRssFeedEntity(
                id = 1,
                name = "Digi24 Actualitate",
                url = "https://www.digi24.ro/rss",
                category = "General",
                region = "RO",
                isFollowed = true
            ),
            CustomRssFeedEntity(
                id = 2,
                name = "Ziarul Financiar",
                url = "https://www.zf.ro/rss",
                category = "Business",
                region = "RO",
                isFollowed = true
            )
        )

        val xml = OpmlManager.exportToOpml(feeds, "Test Export")

        assertTrue(xml.contains("<opml version=\"2.0\">"))
        assertTrue(xml.contains("<title>Test Export</title>"))
        assertTrue(xml.contains("xmlUrl=\"https://www.digi24.ro/rss\""))
        assertTrue(xml.contains("xmlUrl=\"https://www.zf.ro/rss\""))
    }

    @Test
    fun parseOpml_parsesFeedsCorrectly() {
        val sampleOpml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="2.0">
              <head><title>My Feeds</title></head>
              <body>
                <outline text="Tehnologie" title="Tehnologie">
                  <outline type="rss" text="Hacker News" title="Hacker News" xmlUrl="https://news.ycombinator.com/rss"/>
                  <outline type="rss" text="TechCrunch" title="TechCrunch" xmlUrl="https://techcrunch.com/feed/"/>
                </outline>
                <outline type="rss" text="HotNews" title="HotNews" xmlUrl="https://www.hotnews.ro/rss" category="General"/>
              </body>
            </opml>
        """.trimIndent()

        val parsed = OpmlManager.parseOpml(sampleOpml)

        assertEquals(3, parsed.size)
        val hn = parsed.first { it.title == "Hacker News" }
        assertEquals("https://news.ycombinator.com/rss", hn.xmlUrl)
        assertEquals("Tehnologie", hn.category)

        val hotnews = parsed.first { it.title == "HotNews" }
        assertEquals("https://www.hotnews.ro/rss", hotnews.xmlUrl)
        assertEquals("General", hotnews.category)
    }

    @Test
    fun exportAndParse_roundTripMatches() {
        val originalFeeds = listOf(
            CustomRssFeedEntity(
                id = 1,
                name = "BBC World",
                url = "http://feeds.bbci.co.uk/news/world/rss.xml",
                category = "International"
            ),
            CustomRssFeedEntity(
                id = 2,
                name = "Start-up.ro",
                url = "https://start-up.ro/feed",
                category = "Tech"
            )
        )

        val exportedXml = OpmlManager.exportToOpml(originalFeeds)
        val parsed = OpmlManager.parseOpml(exportedXml)

        assertEquals(2, parsed.size)
        assertTrue(parsed.any { it.xmlUrl == "http://feeds.bbci.co.uk/news/world/rss.xml" && it.title == "BBC World" })
        assertTrue(parsed.any { it.xmlUrl == "https://start-up.ro/feed" && it.title == "Start-up.ro" })
    }
}
