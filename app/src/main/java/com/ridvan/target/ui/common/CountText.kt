package com.ridvan.target.ui.common

import androidx.annotation.PluralsRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource

/** A number with its word in the right form — "1 test" / "2 tests" — from a `count_*` plurals resource. */
@Composable
fun countText(@PluralsRes id: Int, count: Int): String = pluralStringResource(id, count, count)
