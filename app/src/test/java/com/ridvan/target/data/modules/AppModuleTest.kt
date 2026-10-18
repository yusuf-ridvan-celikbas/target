package com.ridvan.target.data.modules

import com.ridvan.target.ui.shell.AgendaGroup
import com.ridvan.target.ui.shell.ShellTab
import com.ridvan.target.ui.shell.agendaGroupVisible
import com.ridvan.target.ui.shell.availableStatsSources
import com.ridvan.target.ui.shell.visibleTabs
import com.ridvan.target.ui.statistics.StatsSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppModuleTest {
    @Test
    fun nullOrUnreadableMeansEverythingOn() {
        assertEquals(ALL_MODULES, parseModules(null))
        assertEquals(ALL_MODULES, parseModules(""))
        assertEquals(ALL_MODULES, parseModules("NOPE,ALSO_NOPE"))
    }

    @Test
    fun parsesNamesIgnoringUnknownOnes() {
        assertEquals(setOf(AppModule.READING, AppModule.TRIPS), parseModules("READING, TRIPS,OLD_MODULE"))
    }

    @Test
    fun serializeRoundTripsAndKeepsAllAsNull() {
        assertNull(serializeModules(ALL_MODULES))
        val subset = setOf(AppModule.TRIPS, AppModule.EXAMS_STUDY)
        assertEquals("EXAMS_STUDY,TRIPS", serializeModules(subset))
        assertEquals(subset, parseModules(serializeModules(subset)))
    }

    @Test
    fun lastModuleCannotBeSwitchedOff() {
        val only = setOf(AppModule.READING)
        assertEquals(only, toggleModule(only, AppModule.READING))
        assertEquals(setOf(AppModule.READING, AppModule.FOCUS), toggleModule(only, AppModule.FOCUS))
        assertEquals(only, toggleModule(setOf(AppModule.READING, AppModule.FOCUS), AppModule.FOCUS))
    }

    @Test
    fun tabVisibilityFollowsModules() {
        assertEquals(ShellTab.entries.toList(), visibleTabs(ALL_MODULES))
        assertEquals(listOf(ShellTab.HOME, ShellTab.FOCUS, ShellTab.STATS), visibleTabs(setOf(AppModule.READING)))
        assertEquals(listOf(ShellTab.HOME, ShellTab.PLAN), visibleTabs(setOf(AppModule.TRIPS)))
        assertEquals(listOf(ShellTab.HOME, ShellTab.FOCUS), visibleTabs(setOf(AppModule.FOCUS)))
        assertEquals(listOf(ShellTab.HOME, ShellTab.PLAN), visibleTabs(setOf(AppModule.PLANNER_ROUTINES)))
        assertEquals(listOf(ShellTab.HOME, ShellTab.STUDY, ShellTab.STATS), visibleTabs(setOf(AppModule.EXAMS_STUDY)))
        assertEquals(listOf(ShellTab.HOME, ShellTab.FOCUS), visibleTabs(setOf(AppModule.SPORTS)))
    }

    @Test
    fun sportsModuleIsOnByDefaultAndGatesItsAgendaGroup() {
        assertTrue(AppModule.SPORTS in parseModules(null))
        assertTrue(agendaGroupVisible(AgendaGroup.SPORTS, setOf(AppModule.SPORTS)))
        assertFalse(agendaGroupVisible(AgendaGroup.SPORTS, setOf(AppModule.FOCUS)))
        assertEquals(setOf(AppModule.SPORTS), parseModules(serializeModules(setOf(AppModule.SPORTS))))
    }

    @Test
    fun statsSourcesAndAgendaGroups() {
        assertEquals(listOf(StatsSource.READING), availableStatsSources(setOf(AppModule.READING)))
        assertEquals(
            listOf(StatsSource.PRACTICE_SESSIONS, StatsSource.PRACTICE_EXAMS, StatsSource.MOCK_EXAMS),
            availableStatsSources(setOf(AppModule.EXAMS_STUDY, AppModule.TRIPS)),
        )
        assertEquals(4, availableStatsSources(ALL_MODULES).size)
        val trips = setOf(AppModule.TRIPS)
        assertTrue(agendaGroupVisible(AgendaGroup.TRIPS, trips))
        assertFalse(agendaGroupVisible(AgendaGroup.ROUTINES, trips))
        assertFalse(agendaGroupVisible(AgendaGroup.PRACTICE, trips))
        assertTrue(agendaGroupVisible(AgendaGroup.EXAMS, setOf(AppModule.EXAMS_STUDY)))
    }
}
