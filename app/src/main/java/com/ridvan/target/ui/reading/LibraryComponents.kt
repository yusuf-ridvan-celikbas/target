package com.ridvan.target.ui.reading

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ridvan.target.R
import com.ridvan.target.data.local.entity.Book
import com.ridvan.target.data.local.entity.BookGenre

/** Derived from the two flags on Book: finished wins, then want-to-read, otherwise reading. */
enum class BookStatus { READING, WANT_TO_READ, FINISHED }

val Book.status: BookStatus
    get() = when {
        isFinished -> BookStatus.FINISHED
        isWantToRead -> BookStatus.WANT_TO_READ
        else -> BookStatus.READING
    }

fun Book.withStatus(status: BookStatus): Book =
    copy(isFinished = status == BookStatus.FINISHED, isWantToRead = status == BookStatus.WANT_TO_READ)

@Composable
fun bookStatusLabel(status: BookStatus): String = stringResource(
    when (status) {
        BookStatus.READING -> R.string.book_status_reading
        BookStatus.WANT_TO_READ -> R.string.book_status_want
        BookStatus.FINISHED -> R.string.book_status_finished
    },
)

@Composable
fun bookGenreLabel(genre: BookGenre?): String = stringResource(
    when (genre) {
        BookGenre.NOVEL -> R.string.book_genre_novel
        BookGenre.CLASSIC -> R.string.book_genre_classic
        BookGenre.SCIENCE_FICTION -> R.string.book_genre_science_fiction
        BookGenre.FANTASY -> R.string.book_genre_fantasy
        BookGenre.MYSTERY -> R.string.book_genre_mystery
        BookGenre.HISTORY -> R.string.book_genre_history
        BookGenre.BIOGRAPHY -> R.string.book_genre_biography
        BookGenre.SCIENCE -> R.string.book_genre_science
        BookGenre.PHILOSOPHY -> R.string.book_genre_philosophy
        BookGenre.PSYCHOLOGY -> R.string.book_genre_psychology
        BookGenre.SELF_HELP -> R.string.book_genre_self_help
        BookGenre.POETRY -> R.string.book_genre_poetry
        BookGenre.RELIGION -> R.string.book_genre_religion
        BookGenre.CHILDREN -> R.string.book_genre_children
        BookGenre.COMICS -> R.string.book_genre_comics
        BookGenre.TEXTBOOK -> R.string.book_genre_textbook
        BookGenre.OTHER -> R.string.book_genre_other
        null -> R.string.library_no_genre
    },
)

/** Cloth-binding colours for generated covers. Fixed in light and dark mode — a book's colour is its own. */
val coverPalette = listOf(
    Color(0xFF8E2C2C), // oxblood
    Color(0xFF243B6B), // navy
    Color(0xFF2F5D45), // forest
    Color(0xFFC49A2C), // mustard
    Color(0xFF1F6F72), // teal
    Color(0xFF5B3A6E), // plum
    Color(0xFFB4562A), // burnt orange
    Color(0xFF4A5560), // slate
    Color(0xFF6E2240), // maroon
    Color(0xFF6B6B2A), // olive
    Color(0xFF5E86A8), // dusty blue
    Color(0xFFD9CBB0), // cream
)

fun coverColorOf(book: Book): Color = coverPalette[book.coverColor.mod(coverPalette.size)]

/**
 * A generated cover: the book's colour with a darker spine, two thin rules, the title in the
 * middle and the author at the bottom. Finished books get a ✓ badge; books being read a thin
 * progress bar. [compact] shrinks the type for small thumbnails.
 */
@Composable
fun BookCover(
    book: Book,
    modifier: Modifier = Modifier,
    percent: Int? = null,
    compact: Boolean = false,
    elevation: Dp = 4.dp,
) {
    val base = coverColorOf(book)
    val ink = if (base.luminance() > 0.5f) Color(0xFF2A2A2A) else Color(0xFFF5F1E8)
    val shape = RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp, topEnd = 6.dp, bottomEnd = 6.dp)
    Box(
        modifier = modifier
            .aspectRatio(2f / 3f)
            .shadow(elevation, shape)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(lighten(base, 0.12f), base, darken(base, 0.12f)))),
    ) {
        // Spine: a darker band with a highlight line where the cover bends.
        Row(modifier = Modifier.fillMaxHeight()) {
            Box(modifier = Modifier.fillMaxHeight().width(if (compact) 3.dp else 7.dp).background(darken(base, 0.3f)))
            Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(Color.White.copy(alpha = 0.18f)))
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = if (compact) 6.dp else 14.dp, end = if (compact) 4.dp else 8.dp, top = if (compact) 4.dp else 10.dp, bottom = if (compact) 4.dp else 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!compact) CoverRule(ink)
            Spacer(Modifier.weight(1f))
            Text(
                book.title,
                color = ink,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 8.sp else 14.sp,
                lineHeight = if (compact) 9.sp else 17.sp,
                textAlign = TextAlign.Center,
                maxLines = if (compact) 3 else 4,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            if (!compact) {
                book.author?.let {
                    Text(
                        it,
                        color = ink.copy(alpha = 0.85f),
                        fontFamily = FontFamily.Serif,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                CoverRule(ink, modifier = Modifier.padding(top = 6.dp))
            }
        }
        if (book.isFinished) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(if (compact) 2.dp else 5.dp)
                    .size(if (compact) 12.dp else 20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary),
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = stringResource(R.string.book_status_finished),
                    tint = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.size(if (compact) 9.dp else 14.dp),
                )
            }
        } else if (percent != null && book.status == BookStatus.READING) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(if (compact) 2.dp else 4.dp)
                    .background(Color.Black.copy(alpha = 0.3f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(percent / 100f)
                        .background(MaterialTheme.colorScheme.tertiary),
                )
            }
        }
    }
}

@Composable
private fun CoverRule(ink: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(0.6f).height(1.dp).background(ink.copy(alpha = 0.5f)))
}

private fun darken(color: Color, amount: Float) = Color(
    red = color.red * (1 - amount),
    green = color.green * (1 - amount),
    blue = color.blue * (1 - amount),
    alpha = color.alpha,
)

private fun lighten(color: Color, amount: Float) = Color(
    red = color.red + (1 - color.red) * amount,
    green = color.green + (1 - color.green) * amount,
    blue = color.blue + (1 - color.blue) * amount,
    alpha = color.alpha,
)

const val BOOKS_PER_SHELF = 3

// The bookcase's back wall: a warm dark wood, the same in light and dark mode.
private val shelfWallTop = Color(0xFF4A3628)
private val shelfWallBottom = Color(0xFF2E2117)
private val woodLight = Color(0xFFB07A4F)
private val woodMid = Color(0xFF8A5A36)
private val woodDark = Color(0xFF5C3A20)

/**
 * One shelf: up to [BOOKS_PER_SHELF] covers standing on a wooden plank, against a slightly
 * darker back wall. Empty slots stay empty so covers keep the same size on every shelf.
 */
@Composable
fun BookShelf(books: List<BookProgress>, onBookClick: (Long) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .background(Brush.verticalGradient(listOf(shelfWallTop, shelfWallBottom)))
                .padding(start = 16.dp, end = 16.dp, top = 14.dp),
        ) {
            repeat(BOOKS_PER_SHELF) { index ->
                val progress = books.getOrNull(index)
                Box(modifier = Modifier.weight(1f)) {
                    if (progress != null) {
                        BookCover(
                            book = progress.book,
                            percent = progress.percent,
                            modifier = Modifier.fillMaxWidth().clickable { onBookClick(progress.book.id) },
                        )
                    }
                }
            }
        }
        // The plank: a lit top surface, then the darker front edge, then a soft shadow below.
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
}

/** 1–5 stars. With [onRate], tapping a star sets that rating and tapping the current one clears it. */
@Composable
fun RatingStars(rating: Int?, modifier: Modifier = Modifier, starSize: Dp = 20.dp, onRate: ((Int?) -> Unit)? = null) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        for (star in 1..5) {
            val filled = rating != null && star <= rating
            Icon(
                if (filled) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = stringResource(R.string.cd_rating_star, star),
                tint = if (filled) Color(0xFFF2B01E) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(starSize)
                    .then(if (onRate != null) Modifier.clickable { onRate(if (rating == star) null else star) } else Modifier),
            )
        }
    }
}
