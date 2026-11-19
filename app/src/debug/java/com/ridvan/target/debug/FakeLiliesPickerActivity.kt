package com.ridvan.target.debug

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ridvan.target.data.extensions.LiliesLibContract as C

/**
 * Debug-only stand-in for Lilies Lib's picker (docs/lilies-lib-contract.md). Answers like the real app would,
 * including the awkward cases: numbers as text, an empty answer, a too-new contract version, cancel.
 * Not user-facing, so its labels are plain literals.
 */
class FakeLiliesPickerActivity : ComponentActivity() {
    private class Fake(val label: String, val extras: Bundle?)

    private val fakes = listOf(
        Fake("Dune — Frank Herbert, 412 p.", Bundle().apply {
            putInt(C.EXTRA_CONTRACT_VERSION, 1); putString(C.EXTRA_BOOK_ID, "fake-1"); putString(C.EXTRA_TITLE, "Dune")
            putString(C.EXTRA_AUTHOR, "Frank Herbert"); putInt(C.EXTRA_PAGE_COUNT, 412)
            putString(C.EXTRA_PUBLISHER, "Ace"); putInt(C.EXTRA_PUBLISHED_YEAR, 1965)
        }),
        Fake("Kürk Mantolu Madonna (numbers as text/double)", Bundle().apply {
            putDouble(C.EXTRA_CONTRACT_VERSION, 1.0); putDouble(C.EXTRA_BOOK_ID, 17.0); putString(C.EXTRA_TITLE, "Kürk Mantolu Madonna")
            putString(C.EXTRA_AUTHOR, "Sabahattin Ali"); putString(C.EXTRA_PAGE_COUNT, "160")
        }),
        Fake("Title only, no version", Bundle().apply { putString(C.EXTRA_BOOK_ID, "fake-3"); putString(C.EXTRA_TITLE, "Untitled notebook") }),
        Fake("OK but empty (no id/title) -> nothing", Bundle()),
        Fake("Contract v99 -> nothing", Bundle().apply {
            putInt(C.EXTRA_CONTRACT_VERSION, 99); putString(C.EXTRA_BOOK_ID, "fake-99"); putString(C.EXTRA_TITLE, "From the future")
        }),
        Fake("OK with no data -> nothing", null),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val asked = intent.getIntExtra(C.EXTRA_CONTRACT_VERSION, -1)
        if (intent.action == C.ACTION_ADD_BOOK) {
            showAddBook(asked)
            return
        }
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.safeDrawingPadding().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Fake Lilies Lib picker (debug)", style = MaterialTheme.typography.titleLarge)
                        Text("Target asked for contract v$asked", style = MaterialTheme.typography.bodySmall)
                        fakes.forEach { fake ->
                            Button(onClick = { answer(fake.extras) }, modifier = Modifier.fillMaxWidth()) { Text(fake.label) }
                        }
                        OutlinedButton(onClick = { setResult(Activity.RESULT_CANCELED); finish() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }

    /** ADD_BOOK: shows what Target sent; Add answers a new fake id, Cancel answers nothing. */
    private fun showAddBook(asked: Int) {
        val sent = intent.extras?.keySet().orEmpty().sorted().joinToString("\n") { key ->
            @Suppress("DEPRECATION")
            "$key = ${intent.extras?.get(key)}"
        }
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.safeDrawingPadding().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Fake Lilies Lib: add this book? (debug, v$asked)", style = MaterialTheme.typography.titleLarge)
                        Text(sent, style = MaterialTheme.typography.bodySmall)
                        Button(onClick = {
                            answer(Bundle().apply {
                                putInt(C.EXTRA_CONTRACT_VERSION, 1)
                                putString(C.EXTRA_BOOK_ID, "fake-sent-${System.currentTimeMillis()}")
                            })
                        }, modifier = Modifier.fillMaxWidth()) { Text("Add to library") }
                        Button(onClick = { answer(Bundle().apply { putString(C.EXTRA_BOOK_ID, "fake-1") }) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Answer an id already linked (fake-1)")
                        }
                        OutlinedButton(onClick = { setResult(Activity.RESULT_CANCELED); finish() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }

    private fun answer(extras: Bundle?) {
        setResult(Activity.RESULT_OK, extras?.let { Intent().putExtras(it) })
        finish()
    }
}
