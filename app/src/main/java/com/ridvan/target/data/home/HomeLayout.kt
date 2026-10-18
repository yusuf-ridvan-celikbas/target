package com.ridvan.target.data.home

import com.ridvan.target.data.modules.AppModule

/** The cards Home can show; each still hides itself when it has nothing to show. */
enum class HomeCard(val modules: Set<AppModule>) {
    COUNTDOWNS(setOf(AppModule.EXAMS_STUDY)),
    PLANS(setOf(AppModule.PLANNER_ROUTINES)),
    ROUTINES(setOf(AppModule.PLANNER_ROUTINES)),
    TODAY_SO_FAR(setOf(AppModule.EXAMS_STUDY, AppModule.FOCUS, AppModule.READING, AppModule.SPORTS)),
    WORK_ON_NEXT(setOf(AppModule.EXAMS_STUDY)),
    QUICK_START(setOf(AppModule.FOCUS)),
}

/** What a shortcut can point at besides a fixed page. */
enum class ShortcutTarget { EXAM, BOOK, ROUTINE, TRIP }

/** Shortcut tiles; [target] ones point at one item (an exam, book, routine or trip). */
enum class HomeShortcut(val module: AppModule, val target: ShortcutTarget? = null) {
    EXAMS(AppModule.EXAMS_STUDY),
    COURSES(AppModule.EXAMS_STUDY),
    STUDY_RESOURCES(AppModule.EXAMS_STUDY),
    STATISTICS(AppModule.EXAMS_STUDY),
    EXAM(AppModule.EXAMS_STUDY, ShortcutTarget.EXAM),
    NEW_MOCK_EXAM(AppModule.EXAMS_STUDY, ShortcutTarget.EXAM),
    FOCUS_TIMER(AppModule.FOCUS),
    FLOW_TIMER(AppModule.FOCUS),
    STUDY_HISTORY(AppModule.FOCUS),
    LIBRARY(AppModule.READING),
    BOOK(AppModule.READING, ShortcutTarget.BOOK),
    PLANNER(AppModule.PLANNER_ROUTINES),
    ROUTINES(AppModule.PLANNER_ROUTINES),
    ROUTINE(AppModule.PLANNER_ROUTINES, ShortcutTarget.ROUTINE),
    TRIPS(AppModule.TRIPS),
    TRIP(AppModule.TRIPS, ShortcutTarget.TRIP),
    SPORTS(AppModule.SPORTS),
    LIVE_WORKOUT(AppModule.SPORTS),
}

sealed interface HomeItem {
    data class Card(val card: HomeCard) : HomeItem
    data class Shortcut(val shortcut: HomeShortcut, val targetId: Long? = null) : HomeItem
}

/** Home before the user edits it: today's cards in their usual order, no shortcuts. */
val DEFAULT_HOME_LAYOUT: List<HomeItem> = listOf(
    HomeCard.COUNTDOWNS, HomeCard.PLANS, HomeCard.ROUTINES, HomeCard.TODAY_SO_FAR, HomeCard.WORK_ON_NEXT, HomeCard.QUICK_START,
).map { HomeItem.Card(it) }

/** "card:PLANS,tile:FOCUS_TIMER,tile:BOOK:12"; null = [DEFAULT_HOME_LAYOUT]; unknown entries are skipped. */
fun parseHomeLayout(text: String?): List<HomeItem> {
    if (text == null) return DEFAULT_HOME_LAYOUT
    return text.split(',').mapNotNull { entry ->
        val parts = entry.trim().split(':')
        when (parts.getOrNull(0)) {
            "card" -> HomeCard.entries.firstOrNull { it.name == parts.getOrNull(1) }?.let { HomeItem.Card(it) }
            "tile" -> HomeShortcut.entries.firstOrNull { it.name == parts.getOrNull(1) }?.let { shortcut ->
                val id = parts.getOrNull(2)?.toLongOrNull()
                if (shortcut.target != null && id == null) null else HomeItem.Shortcut(shortcut, id.takeIf { shortcut.target != null })
            }
            else -> null
        }
    }.distinct()
}

fun serializeHomeLayout(items: List<HomeItem>): String = items.joinToString(",") { item ->
    when (item) {
        is HomeItem.Card -> "card:${item.card.name}"
        is HomeItem.Shortcut -> listOfNotNull("tile", item.shortcut.name, item.targetId?.toString()).joinToString(":")
    }
}

/** Whether the item belongs to a module that's on (cards need any of theirs). */
fun HomeItem.isEnabled(modules: Set<AppModule>): Boolean = when (this) {
    is HomeItem.Card -> card.modules.any { it in modules }
    is HomeItem.Shortcut -> shortcut.module in modules
}

/** Consecutive shortcuts grouped into one block (shown as a tile grid); each card is its own block. */
fun groupHomeBlocks(items: List<HomeItem>): List<List<HomeItem>> {
    val blocks = mutableListOf<MutableList<HomeItem>>()
    items.forEach { item ->
        val last = blocks.lastOrNull()
        if (item is HomeItem.Shortcut && last != null && last.last() is HomeItem.Shortcut) last.add(item)
        else blocks.add(mutableListOf(item))
    }
    return blocks
}
