package com.ridvan.target.data.modules

/** The parts of the app an account can switch on or off. Home, Settings, Help and My Account are always on. */
enum class AppModule { EXAMS_STUDY, PLANNER_ROUTINES, FOCUS, READING, TRIPS, SPORTS, MONEY }

val ALL_MODULES: Set<AppModule> = AppModule.entries.toSet()

/** Stored as comma-separated names; null (or nothing readable) means everything is on. */
fun parseModules(stored: String?): Set<AppModule> {
    if (stored == null) return ALL_MODULES
    val parsed = stored.split(',').mapNotNull { name -> AppModule.entries.firstOrNull { it.name == name.trim() } }.toSet()
    return parsed.ifEmpty { ALL_MODULES }
}

/** Null when everything is on, so a default account keeps a NULL column. */
fun serializeModules(modules: Set<AppModule>): String? =
    if (modules.isEmpty() || modules.containsAll(ALL_MODULES)) null
    else AppModule.entries.filter { it in modules }.joinToString(",") { it.name }

/** Flips [module]; the last remaining module can't be switched off. */
fun toggleModule(current: Set<AppModule>, module: AppModule): Set<AppModule> =
    if (module in current) (if (current.size <= 1) current else current - module) else current + module
