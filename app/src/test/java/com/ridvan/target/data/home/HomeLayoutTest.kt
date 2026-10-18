package com.ridvan.target.data.home

import com.ridvan.target.data.modules.AppModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutTest {
    @Test
    fun nullIsTheDefaultLayout() {
        assertEquals(DEFAULT_HOME_LAYOUT, parseHomeLayout(null))
    }

    @Test
    fun roundTripsCardsAndShortcuts() {
        val items = listOf(
            HomeItem.Shortcut(HomeShortcut.FOCUS_TIMER),
            HomeItem.Card(HomeCard.PLANS),
            HomeItem.Shortcut(HomeShortcut.BOOK, 12),
        )
        assertEquals(items, parseHomeLayout(serializeHomeLayout(items)))
        assertEquals(emptyList<HomeItem>(), parseHomeLayout(""))
    }

    @Test
    fun skipsUnknownAndTargetlessEntries() {
        assertEquals(
            listOf(HomeItem.Card(HomeCard.PLANS)),
            parseHomeLayout("card:GONE,tile:BOOK,card:PLANS,tile:NOPE:3,card:PLANS"),
        )
    }

    @Test
    fun groupsConsecutiveShortcuts() {
        val a = HomeItem.Shortcut(HomeShortcut.FOCUS_TIMER)
        val b = HomeItem.Shortcut(HomeShortcut.LIBRARY)
        val card = HomeItem.Card(HomeCard.PLANS)
        assertEquals(listOf(listOf(a, b), listOf(card), listOf(a)), groupHomeBlocks(listOf(a, b, card, a)))
    }

    @Test
    fun followsModules() {
        assertTrue(HomeItem.Card(HomeCard.TODAY_SO_FAR).isEnabled(setOf(AppModule.SPORTS)))
        assertFalse(HomeItem.Shortcut(HomeShortcut.TRIPS).isEnabled(setOf(AppModule.SPORTS)))
    }
}
