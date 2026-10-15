package com.ridvan.target.data.local

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * JVM-only guard for MIGRATION_27_28 (a real run needs an Android SQLite): the exported 27.json and
 * 28.json must differ by exactly the three routine tables, so a CREATE-only migration is complete.
 */
class Schema27To28Test {
    private fun entities(version: Int): Map<String, JsonObject> {
        val file = File("schemas/com.ridvan.target.data.local.TargetDatabase/$version.json")
        val root = Json.parseToJsonElement(file.readText()).jsonObject
        return root.getValue("database").jsonObject.getValue("entities").jsonArray
            .map { it.jsonObject }
            .associateBy { it.getValue("tableName").jsonPrimitive.content }
    }

    @Test fun onlyRoutineTablesAdded() {
        val old = entities(27)
        val new = entities(28)
        assertEquals(setOf("routines", "routine_steps", "routine_step_completions"), new.keys - old.keys)
        assertEquals(emptySet<String>(), old.keys - new.keys)
        old.forEach { (table, entity) -> assertEquals("table $table changed", entity, new.getValue(table)) }
    }
}
