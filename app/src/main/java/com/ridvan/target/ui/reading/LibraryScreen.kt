package com.ridvan.target.ui.reading

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.ui.common.AddFab

/** How the library is ordered. [groups] sorts put each value on its own labelled shelf. */
enum class LibrarySort(val labelRes: Int, val groups: Boolean, val defaultDescending: Boolean) {
    TITLE(R.string.library_sort_title, groups = false, defaultDescending = false),
    AUTHOR(R.string.library_sort_author, groups = true, defaultDescending = false),
    GENRE(R.string.library_sort_genre, groups = true, defaultDescending = false),
    STATUS(R.string.library_sort_status, groups = true, defaultDescending = false),
    PAGES(R.string.library_sort_pages, groups = false, defaultDescending = true),
    RATING(R.string.library_sort_rating, groups = false, defaultDescending = true),
    YEAR(R.string.library_sort_year, groups = false, defaultDescending = true),
    PROGRESS(R.string.library_sort_progress, groups = false, defaultDescending = true),
    ADDED(R.string.library_sort_added, groups = false, defaultDescending = true),
}

/** One shelf group: [key] is the genre/author/status it stands for (null key on an ungrouped sort = no header). */
private data class ShelfGroup(val key: Any?, val books: List<BookProgress>)

private val titleOrder = compareBy<BookProgress, String>(String.CASE_INSENSITIVE_ORDER) { it.book.title }

/** The value a sort orders by; null values always go last, whichever direction. */
private fun sortKey(sort: LibrarySort, p: BookProgress): Comparable<*>? = when (sort) {
    LibrarySort.TITLE -> p.book.title.lowercase()
    LibrarySort.AUTHOR -> p.book.author?.trim()?.lowercase()?.ifEmpty { null }
    LibrarySort.GENRE -> p.book.genre?.ordinal
    LibrarySort.STATUS -> p.book.status.ordinal
    LibrarySort.PAGES -> p.book.totalPages
    LibrarySort.RATING -> p.book.rating
    LibrarySort.YEAR -> p.book.publishedYear
    LibrarySort.PROGRESS -> p.percent
    LibrarySort.ADDED -> p.book.createdAt
}

@Suppress("UNCHECKED_CAST")
private fun sortBooks(books: List<BookProgress>, sort: LibrarySort, descending: Boolean): List<BookProgress> {
    val (keyed, unkeyed) = books.partition { sortKey(sort, it) != null }
    val byKey = compareBy<BookProgress> { sortKey(sort, it) as Comparable<Any> }
    val sorted = keyed.sortedWith(if (descending) byKey.reversed().then(titleOrder) else byKey.then(titleOrder))
    return sorted + unkeyed.sortedWith(titleOrder)
}

private fun shelfGroups(books: List<BookProgress>, sort: LibrarySort, descending: Boolean): List<ShelfGroup> {
    val sorted = sortBooks(books, sort, descending)
    if (!sort.groups) return listOf(ShelfGroup(null, sorted))
    // Same order as the sort; an author group keys on the name as typed by its first book.
    val groups = LinkedHashMap<Any?, MutableList<BookProgress>>()
    sorted.forEach { p ->
        val key: Any? = when (sort) {
            LibrarySort.AUTHOR -> p.book.author?.trim()?.ifEmpty { null }?.let { name ->
                groups.keys.firstOrNull { (it as? String)?.equals(name, ignoreCase = true) == true } ?: name
            }
            LibrarySort.GENRE -> p.book.genre
            LibrarySort.STATUS -> p.book.status
            else -> null
        }
        groups.getOrPut(key) { mutableListOf() }.add(p)
    }
    return groups.map { (key, list) -> ShelfGroup(key, list.sortedWith(titleOrder)) }
}

@Composable
private fun shelfLabel(sort: LibrarySort, key: Any?): String = when (sort) {
    LibrarySort.AUTHOR -> (key as? String) ?: stringResource(R.string.library_unknown_author)
    LibrarySort.GENRE -> bookGenreLabel(key as? com.ridvan.target.data.local.entity.BookGenre)
    LibrarySort.STATUS -> bookStatusLabel(key as BookStatus)
    else -> ""
}

/** Every book as covers on wooden shelves (or a list), with search, status filter and sorting. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBack: () -> Unit,
    onBookClick: (Long) -> Unit,
    viewModel: LibraryViewModel = viewModel(),
) {
    val books by viewModel.books.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var statusFilter by rememberSaveable { mutableStateOf<BookStatus?>(null) }
    var sort by rememberSaveable { mutableStateOf(LibrarySort.STATUS) }
    var descending by rememberSaveable { mutableStateOf(sort.defaultDescending) }
    var listView by rememberSaveable { mutableStateOf(false) }
    var showAddBook by remember { mutableStateOf(false) }

    val visible = books.filter { p ->
        (statusFilter == null || p.book.status == statusFilter) &&
            (query.isBlank() || listOfNotNull(p.book.title, p.book.author, p.book.publisher).any { it.contains(query.trim(), ignoreCase = true) })
    }
    val groups = shelfGroups(visible, sort, descending)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.library_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { listView = !listView }) {
                        if (listView) {
                            Icon(Icons.Filled.ViewModule, contentDescription = stringResource(R.string.cd_library_shelf_view))
                        } else {
                            Icon(Icons.AutoMirrored.Filled.ViewList, contentDescription = stringResource(R.string.cd_library_list_view))
                        }
                    }
                },
            )
        },
        floatingActionButton = { AddFab(onClick = { showAddBook = true }) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        ) {
            item {
                Text(
                    stringResource(
                        R.string.library_summary,
                        books.size,
                        books.count { it.book.status == BookStatus.READING },
                        books.count { it.book.status == BookStatus.WANT_TO_READ },
                        books.count { it.book.status == BookStatus.FINISHED },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.library_search)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = if (query.isNotEmpty()) {
                        { IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.common_clear)) } }
                    } else null,
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp),
                ) {
                    FilterChip(
                        selected = statusFilter == null,
                        onClick = { statusFilter = null },
                        label = { Text(stringResource(R.string.library_filter_all)) },
                    )
                    listOf(BookStatus.READING, BookStatus.WANT_TO_READ, BookStatus.FINISHED).forEach { status ->
                        FilterChip(
                            selected = statusFilter == status,
                            onClick = { statusFilter = if (statusFilter == status) null else status },
                            label = { Text(bookStatusLabel(status)) },
                        )
                    }
                }
            }
            item {
                SortRow(
                    sort = sort,
                    descending = descending,
                    onSortChange = {
                        sort = it
                        descending = it.defaultDescending
                    },
                    onToggleDirection = { descending = !descending },
                )
            }

            when {
                books.isEmpty() -> item { EmptyText(stringResource(R.string.library_empty_shelf)) }
                visible.isEmpty() -> item { EmptyText(stringResource(R.string.library_no_match)) }
                listView -> {
                    items(groups.flatMap { it.books }, key = { it.book.id }) { progress ->
                        LibraryListRow(progress, onClick = { onBookClick(progress.book.id) })
                        HorizontalDivider()
                    }
                }
                else -> groups.forEach { group ->
                    if (sort.groups) {
                        item(key = "header-${group.key}") {
                            Text(
                                stringResource(R.string.library_shelf_header, shelfLabel(sort, group.key), group.books.size),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                            )
                        }
                    }
                    val shelves = group.books.chunked(BOOKS_PER_SHELF)
                    items(shelves.size, key = { index -> "shelf-${group.key}-$index" }) { index ->
                        BookShelf(
                            books = shelves[index],
                            onBookClick = onBookClick,
                            modifier = Modifier.padding(top = if (!sort.groups && index == 0) 16.dp else 0.dp, bottom = 12.dp),
                        )
                    }
                }
            }
        }
    }

    if (showAddBook) {
        BookDialog(
            initial = null,
            onConfirm = { book ->
                viewModel.addBook(book)
                showAddBook = false
            },
            onDismiss = { showAddBook = false },
        )
    }
}

@Composable
private fun SortRow(
    sort: LibrarySort,
    descending: Boolean,
    onSortChange: (LibrarySort) -> Unit,
    onToggleDirection: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(stringResource(R.string.library_sort_label, stringResource(sort.labelRes)))
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                LibrarySort.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(stringResource(option.labelRes)) },
                        onClick = {
                            onSortChange(option)
                            expanded = false
                        },
                    )
                }
            }
        }
        IconButton(onClick = onToggleDirection) {
            Icon(
                if (descending) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                contentDescription = stringResource(if (descending) R.string.cd_sort_descending else R.string.cd_sort_ascending),
            )
        }
    }
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 24.dp),
    )
}

/** List view: a small cover, title/author, genre · pages · year, stars and progress. */
@Composable
private fun LibraryListRow(progress: BookProgress, onClick: () -> Unit) {
    val book = progress.book
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { BookCover(book, percent = progress.percent, compact = true, elevation = 2.dp, modifier = Modifier.width(44.dp)) },
        headlineContent = { Text(book.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column {
                book.author?.let { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                val details = listOfNotNull(
                    book.genre?.let { bookGenreLabel(it) },
                    book.totalPages?.let { stringResource(R.string.book_pages_count, it) },
                    book.publishedYear?.toString(),
                )
                if (details.isNotEmpty()) {
                    Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Text(
                        if (book.status == BookStatus.READING) bookProgressLabel(progress) else bookStatusLabel(book.status),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f),
                    )
                    if (book.rating != null) RatingStars(book.rating, starSize = 14.dp)
                }
                if (book.status == BookStatus.READING) {
                    progress.percent?.let { percent ->
                        LinearProgressIndicator(
                            progress = { percent / 100f },
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        )
                    }
                }
            }
        },
    )
    Spacer(Modifier.height(2.dp))
}
