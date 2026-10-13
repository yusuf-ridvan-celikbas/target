package com.ridvan.target.res

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class StringsConsistencyTest {
    private val placeholder = Regex("%(?:(\\d+)\\$)?[-#+ 0,(]*\\d*(?:\\.\\d+)?([a-zA-Z%])")

    private fun load(path: String): Pair<Map<String, String>, Map<String, List<String>>> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(path))
        val strings = linkedMapOf<String, String>()
        val plurals = linkedMapOf<String, List<String>>()
        val nodes = doc.documentElement.childNodes
        for (i in 0 until nodes.length) {
            val e = nodes.item(i) as? Element ?: continue
            if (e.getAttribute("translatable") == "false") continue
            when (e.tagName) {
                "string" -> strings[e.getAttribute("name")] = e.textContent
                "plurals" -> {
                    val items = e.getElementsByTagName("item")
                    plurals[e.getAttribute("name")] =
                        (0 until items.length).map { items.item(it).textContent }
                }
            }
        }
        return strings to plurals
    }

    /** Sorted (position, conversion) tokens; unnumbered args get implicit order. */
    private fun sig(s: String): List<String> {
        var implicit = 0
        return placeholder.findAll(s).mapNotNull {
            val conv = it.groupValues[2]
            if (conv == "%" || conv == "n") return@mapNotNull null
            val pos = it.groupValues[1].ifEmpty { (++implicit).toString() }
            "$pos$conv"
        }.sorted().toList()
    }

    private fun union(items: List<String>) = items.flatMap { sig(it) }.toSortedSet().toList()

    @Test
    fun enAndTrAreConsistent() {
        val (enS, enP) = load("src/main/res/values/strings.xml")
        val (trS, trP) = load("src/main/res/values-tr/strings.xml")
        val problems = mutableListOf<String>()
        for (k in enS.keys - trS.keys) problems += "string '$k' missing in TR"
        for (k in trS.keys - enS.keys) problems += "string '$k' missing in EN"
        for (k in enP.keys - trP.keys) problems += "plurals '$k' missing in TR"
        for (k in trP.keys - enP.keys) problems += "plurals '$k' missing in EN"
        for (k in enS.keys.intersect(trS.keys)) {
            if (sig(enS.getValue(k)) != sig(trS.getValue(k)))
                problems += "string '$k' placeholders differ: EN=${sig(enS.getValue(k))} TR=${sig(trS.getValue(k))}"
        }
        for (k in enP.keys.intersect(trP.keys)) {
            val en = union(enP.getValue(k))
            for ((i, item) in trP.getValue(k).withIndex()) {
                val t = sig(item)
                if (!en.containsAll(t)) problems += "plurals '$k' TR item $i placeholders $t not in EN $en"
            }
        }
        assertTrue(problems.joinToString("\n", "\n"), problems.isEmpty())
    }
}
