package com.ridvan.target.ui.reading

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.ridvan.target.R

// The bookcase: a warm dark-wood back wall and planks, the same in light and dark mode.
private val shelfWallTop = Color(0xFF4A3628)
private val shelfWallBottom = Color(0xFF2E2117)
private val woodLight = Color(0xFFB07A4F)
private val woodMid = Color(0xFF8A5A36)
private val woodDark = Color(0xFF5C3A20)

private val SHELF_SIDE_PADDING = 12.dp
private val SPINE_GAP = 2.dp
/** Room above the tallest spine, so a pulled-out cover fits inside its own shelf. */
private val SHELF_HEADROOM = 40.dp
private val SPINE_MAX_HEIGHT = 152.dp
private val COVER_HEIGHT = SHELF_HEADROOM + SPINE_MAX_HEIGHT - 8.dp

/** Thicker books get wider spines; a book without a page count gets a middling one. */
private fun spineWidth(progress: BookProgress): Dp {
    val pages = progress.book.totalPages ?: return 30.dp
    return (20 + pages / 25).coerceIn(22, 48).dp
}

/** Heights vary a little per book (stable, from its id) so the shelf doesn't look like a ruler. */
private fun spineHeight(progress: BookProgress): Dp = SPINE_MAX_HEIGHT - (progress.book.id.mod(5) * 6).dp

/**
 * Books standing upright, side by side, wrapping onto as many shelves as needed (at most [maxRows]).
 * Tapping a spine pulls that book out — its front cover turns towards you above an empty slot.
 * Tapping the cover opens the book ([onBookClick]) and puts it back; tapping the empty slot just puts it back.
 * Only one book is out at a time: [pulledBookId] is owned by the screen.
 */
@Composable
fun SpineShelves(
    books: List<BookProgress>,
    pulledBookId: Long?,
    onPull: (Long?) -> Unit,
    onBookClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    maxRows: Int = Int.MAX_VALUE,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val fullWidth = maxWidth
        val usable = fullWidth - SHELF_SIDE_PADDING * 2
        // Pack spines into rows by their real widths.
        val rows = mutableListOf<MutableList<BookProgress>>()
        var rowWidth = 0.dp
        books.forEach { progress ->
            val w = spineWidth(progress)
            if (rows.isEmpty() || rowWidth + w > usable) {
                rows += mutableListOf(progress)
                rowWidth = w + SPINE_GAP
            } else {
                rows.last() += progress
                rowWidth += w + SPINE_GAP
            }
        }
        Column {
            rows.take(maxRows).forEach { row ->
                ShelfRow(
                    books = row,
                    rowWidth = fullWidth,
                    pulledBookId = pulledBookId,
                    onPull = onPull,
                    onBookClick = onBookClick,
                )
            }
        }
    }
}

@Composable
private fun ShelfRow(
    books: List<BookProgress>,
    rowWidth: Dp,
    pulledBookId: Long?,
    onPull: (Long?) -> Unit,
    onBookClick: (Long) -> Unit,
) {
    val pulledHere = books.firstOrNull { it.book.id == pulledBookId }
    // Kept until the put-back animation ends, so the slot stays empty while the book goes back in.
    var shown by remember { mutableStateOf<BookProgress?>(null) }
    val out = remember { Animatable(0f) }
    LaunchedEffect(pulledHere?.book?.id) {
        if (pulledHere != null) {
            shown = pulledHere
            out.snapTo(0f)
            out.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow))
        } else if (shown != null) {
            out.animateTo(0f, tween(220))
            shown = null
        }
    }
    val current = shown?.let { s -> books.firstOrNull { it.book.id == s.book.id } ?: s }

    Box(modifier = Modifier.fillMaxWidth().zIndex(if (current != null) 1f else 0f)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(SPINE_GAP),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SHELF_HEADROOM + SPINE_MAX_HEIGHT)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(Brush.verticalGradient(listOf(shelfWallTop, shelfWallBottom)))
                    .padding(horizontal = SHELF_SIDE_PADDING),
            ) {
                books.forEach { progress ->
                    if (progress.book.id == current?.book?.id) {
                        EmptySlot(progress, onClick = { onPull(null) })
                    } else {
                        BookSpine(progress, onClick = { onPull(progress.book.id) })
                    }
                }
            }
            Plank()
        }
        if (current != null) {
            // The cover comes out next to its empty slot (never over it, so the slot stays tappable),
            // turning from edge-on to face the reader: right of the slot on the left half, else left of it.
            val index = books.indexOfFirst { it.book.id == current.book.id }.coerceAtLeast(0)
            val slotLeft = SHELF_SIDE_PADDING + books.take(index).fold(0.dp) { acc, p -> acc + spineWidth(p) + SPINE_GAP }
            val slotRight = slotLeft + spineWidth(current)
            val coverWidth = COVER_HEIGHT * 2f / 3f
            val preferred = if (slotLeft + slotRight < rowWidth) slotRight + 10.dp else slotLeft - 10.dp - coverWidth
            val x = preferred.coerceIn(4.dp, rowWidth - coverWidth - 4.dp)
            val density = LocalDensity.current
            val progress = out.value
            BookCover(
                book = current.book,
                percent = current.percent,
                elevation = 12.dp,
                modifier = Modifier
                    .offset(x = x, y = SHELF_HEADROOM + SPINE_MAX_HEIGHT - COVER_HEIGHT)
                    .height(COVER_HEIGHT)
                    .graphicsLayer {
                        rotationY = -90f * (1f - progress)
                        cameraDistance = 12f * density.density
                        translationY = with(density) { (16.dp * (1f - progress)).toPx() }
                        alpha = progress.coerceIn(0f, 1f)
                    }
                    .clickable {
                        // The book goes back on the shelf while its page opens, so it's there on return.
                        onPull(null)
                        onBookClick(current.book.id)
                    },
            )
        }
    }
}

@Composable
private fun BookSpine(progress: BookProgress, onClick: () -> Unit) {
    val book = progress.book
    val base = coverColorOf(book)
    val ink = if (base.luminance() > 0.5f) Color(0xFF2A2A2A) else Color(0xFFF5F1E8)
    Box(
        modifier = Modifier
            .width(spineWidth(progress))
            .height(spineHeight(progress))
            .shadow(3.dp, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
            // Rounded spine: lit in the middle, darker at both edges.
            .background(Brush.horizontalGradient(listOf(darken(base, 0.35f), lighten(base, 0.12f), base, darken(base, 0.35f))))
            .clickable(onClick = onClick),
    ) {
        // Two thin bands near the top and bottom, like a cloth binding.
        Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp).fillMaxWidth().height(2.dp).background(ink.copy(alpha = 0.45f)))
        Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp).fillMaxWidth().height(2.dp).background(ink.copy(alpha = 0.45f)))
        Text(
            book.title,
            color = ink,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .verticalText()
                // Top-to-bottom text: "end" is the spine's foot, where a finished book's ✓ sits.
                .padding(start = 18.dp, end = if (book.isFinished) 36.dp else 18.dp),
        )
        if (book.isFinished) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary),
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = stringResource(R.string.book_status_finished),
                    tint = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.size(10.dp),
                )
            }
        }
    }
}

/** Where a pulled-out book stood: a darker gap with a faint outline. Tapping it puts the book back. */
@Composable
private fun EmptySlot(progress: BookProgress, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(spineWidth(progress))
            .height(spineHeight(progress))
            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    )
}

/** Rotates text to run top-to-bottom along a spine, swapping its measured width and height. */
private fun Modifier.verticalText(): Modifier = this
    .layout { measurable, constraints ->
        val placeable = measurable.measure(
            Constraints(maxWidth = constraints.maxHeight, maxHeight = constraints.maxWidth),
        )
        layout(placeable.height, placeable.width) {
            placeable.place(
                x = -(placeable.width - placeable.height) / 2,
                y = (placeable.width - placeable.height) / 2,
            )
        }
    }
    .rotate(90f)

/** The plank: a lit top surface, the darker front edge, then a soft shadow below. */
@Composable
private fun Plank() {
    Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Brush.verticalGradient(listOf(woodLight, woodMid))))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
            .background(Brush.verticalGradient(listOf(woodMid, woodDark))),
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .padding(horizontal = 6.dp)
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent))),
    )
}
