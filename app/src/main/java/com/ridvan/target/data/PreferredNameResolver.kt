package com.ridvan.target.data

import com.ridvan.target.data.local.entity.PreferredNameSource

fun resolvePreferredName(
    firstName: String,
    middleName: String?,
    lastName: String,
    username: String,
    source: PreferredNameSource,
    customText: String,
): String = when (source) {
    PreferredNameSource.FIRST -> firstName
    PreferredNameSource.MIDDLE -> middleName?.takeIf { it.isNotBlank() } ?: firstName
    PreferredNameSource.LAST -> lastName
    PreferredNameSource.USERNAME -> username
    PreferredNameSource.OTHER -> customText.trim()
}
