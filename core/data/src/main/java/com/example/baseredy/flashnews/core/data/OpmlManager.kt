package com.example.baseredy.flashnews.core.data

import com.example.baseredy.flashnews.core.database.CustomRssFeedEntity
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node

data class OpmlFeed(
    val title: String,
    val xmlUrl: String,
    val htmlUrl: String = "",
    val category: String = "General"
)

object OpmlManager {

    fun exportToOpml(feeds: List<CustomRssFeedEntity>, title: String = "FlashNews RSS Feeds"): String {
        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
        val now = dateFormat.format(Date())

        val grouped = feeds.groupBy { it.category }

        return buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            append("<opml version=\"2.0\">\n")
            append("  <head>\n")
            append("    <title>${escapeXml(title)}</title>\n")
            append("    <dateCreated>$now</dateCreated>\n")
            append("    <docs>http://opml.org/spec2.opml</docs>\n")
            append("  </head>\n")
            append("  <body>\n")
            for ((category, categoryFeeds) in grouped) {
                val catName = if (category.isBlank()) "General" else category
                append("    <outline text=\"${escapeXml(catName)}\" title=\"${escapeXml(catName)}\">\n")
                for (feed in categoryFeeds) {
                    val name = escapeXml(feed.name)
                    val url = escapeXml(feed.url)
                    val cat = escapeXml(catName)
                    append("      <outline type=\"rss\" text=\"$name\" title=\"$name\" xmlUrl=\"$url\" category=\"$cat\"/>\n")
                }
                append("    </outline>\n")
            }
            append("  </body>\n")
            append("</opml>")
        }
    }

    fun parseOpml(xmlContent: String): List<OpmlFeed> {
        val result = mutableListOf<OpmlFeed>()
        if (xmlContent.isBlank()) return result

        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = false
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            val builder = factory.newDocumentBuilder()
            val input = ByteArrayInputStream(xmlContent.toByteArray(StandardCharsets.UTF_8))
            val doc = builder.parse(input)
            doc.documentElement.normalize()

            val outlines = doc.getElementsByTagName("outline")
            for (i in 0 until outlines.length) {
                val node = outlines.item(i)
                if (node.nodeType == Node.ELEMENT_NODE) {
                    val element = node as Element
                    val xmlUrl = element.getAttribute("xmlUrl")
                    if (!xmlUrl.isNullOrBlank()) {
                        val text = element.getAttribute("text")
                        val title = element.getAttribute("title")
                        val catAttr = element.getAttribute("category")
                        val htmlUrl = element.getAttribute("htmlUrl")

                        var parentCategory = ""
                        val parent = element.parentNode
                        if (parent != null && parent.nodeType == Node.ELEMENT_NODE) {
                            val parentElem = parent as Element
                            if (parentElem.tagName.equals("outline", ignoreCase = true)) {
                                parentCategory = parentElem.getAttribute("title").ifBlank { parentElem.getAttribute("text") }
                            }
                        }

                        val feedTitle = when {
                            !title.isNullOrBlank() -> title
                            !text.isNullOrBlank() -> text
                            else -> "Flux RSS"
                        }

                        val finalCategory = when {
                            !catAttr.isNullOrBlank() -> catAttr
                            parentCategory.isNotBlank() -> parentCategory
                            else -> "General"
                        }

                        result.add(
                            OpmlFeed(
                                title = feedTitle.trim(),
                                xmlUrl = xmlUrl.trim(),
                                htmlUrl = htmlUrl.orEmpty().trim(),
                                category = finalCategory.trim()
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Regex fallback if parser encounters malformed OPML
            val regex = Regex("""<outline[^>]*xmlUrl=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
            val titleRegex = Regex("""(?:title|text)=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            val catRegex = Regex("""category=["']([^"']+)["']""", RegexOption.IGNORE_CASE)

            for (match in regex.findAll(xmlContent)) {
                val fullTag = match.value
                val url = match.groupValues[1]
                val titleMatch = titleRegex.find(fullTag)
                val catMatch = catRegex.find(fullTag)

                result.add(
                    OpmlFeed(
                        title = titleMatch?.groupValues?.get(1) ?: "Flux RSS",
                        xmlUrl = url.trim(),
                        category = catMatch?.groupValues?.get(1) ?: "General"
                    )
                )
            }
        }

        return result.distinctBy { it.xmlUrl.lowercase() }
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
