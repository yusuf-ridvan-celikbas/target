package com.ridvan.target.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ridvan.target.R
import com.ridvan.target.data.local.dao.TopicDao
import com.ridvan.target.data.local.entity.Topic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

private const val MAX_TOPIC_MATCHES = 8

/** Topics whose name contains [query] (case-insensitive), at most [MAX_TOPIC_MATCHES]; none for a blank query. */
fun matchingTopics(topics: List<Topic>, query: String): List<Topic> {
    val q = query.trim().lowercase(Locale.getDefault())
    if (q.isEmpty()) return emptyList()
    return topics.filter { q in it.name.lowercase(Locale.getDefault()) }.take(MAX_TOPIC_MATCHES)
}

/** True when [query] is non-blank and no topic (or pending name) has exactly that name. */
fun canAddTopic(topics: List<Topic>, query: String, pendingNames: List<String> = emptyList()): Boolean {
    val name = query.trim()
    return name.isNotEmpty() &&
        topics.none { it.name.equals(name, ignoreCase = true) } &&
        pendingNames.none { it.equals(name, ignoreCase = true) }
}

@Composable
private fun TopicSearchBox(query: String, onQueryChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text(label) },
        placeholder = { Text(stringResource(R.string.topic_search_placeholder)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun AddTopicRow(name: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            stringResource(R.string.topic_search_add, name.trim()),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/**
 * Single-topic picker: shows the chosen topic (clearable when [allowNone]) and a search box; only
 * matches are listed, never the whole topic list. A name that isn't listed can be added via [onCreate],
 * which must call back with the new topic's id (it then becomes the selection).
 */
@Composable
fun TopicSearchField(
    topics: List<Topic>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    onCreate: ((name: String, onCreated: (Long) -> Unit) -> Unit)?,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.label_topic),
    allowNone: Boolean = true,
) {
    var query by remember { mutableStateOf("") }
    val selected = topics.firstOrNull { it.id == selectedId }
    Column(modifier = modifier.fillMaxWidth()) {
        if (selected != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.topic_search_selected, selected.name),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                if (allowNone) {
                    IconButton(onClick = { onSelect(null) }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.topic_search_clear))
                    }
                }
            }
        }
        TopicSearchBox(query, { query = it }, label)
        matchingTopics(topics, query).forEach { topic ->
            Text(
                topic.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth().clickable { onSelect(topic.id); query = "" }.padding(vertical = 10.dp),
            )
        }
        if (onCreate != null && canAddTopic(topics, query)) {
            AddTopicRow(query) {
                val name = query.trim()
                query = ""
                onCreate(name) { id -> onSelect(id) }
            }
        }
        if (topics.isEmpty() && query.isBlank() && onCreate == null) {
            Text(stringResource(R.string.topic_search_none_yet), style = MaterialTheme.typography.labelSmall)
        }
    }
}

/**
 * Multi-topic picker for "add topics" dialogs: checked topics stay listed, others appear only as search
 * matches. Unlisted names are collected in [newNames] (created by the caller on confirm).
 */
@Composable
fun TopicSearchChecklist(
    topics: List<Topic>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    newNames: List<String>,
    onAddNew: (String) -> Unit,
    onRemoveNew: (String) -> Unit,
    modifier: Modifier = Modifier,
    allowNew: Boolean = true,
) {
    var query by remember { mutableStateOf("") }
    Column(modifier = modifier.fillMaxWidth()) {
        TopicSearchBox(query, { query = it }, stringResource(R.string.label_topic))
        val matches = matchingTopics(topics, query)
        val shown = topics.filter { it.id in selectedIds } + matches.filterNot { it.id in selectedIds }
        newNames.forEach { name ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onRemoveNew(name) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = true, onCheckedChange = null)
                Text(stringResource(R.string.topic_search_new_label, name))
            }
        }
        shown.forEach { topic ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggle(topic.id) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = topic.id in selectedIds, onCheckedChange = null)
                Text(topic.name)
            }
        }
        if (allowNew && canAddTopic(topics, query, newNames)) {
            AddTopicRow(query) {
                onAddNew(query.trim())
                query = ""
            }
        }
    }
}

/**
 * Adds a topic named [name] to a course or (when no course) a language and reports its id;
 * what every topic search's "Add" calls through its ViewModel.
 */
fun CoroutineScope.launchCreateTopic(topicDao: TopicDao, name: String, courseId: Long?, languageId: Long?, onCreated: (Long) -> Unit) {
    val trimmed = name.trim()
    if (trimmed.isEmpty() || (courseId == null && languageId == null)) return
    launch { onCreated(topicIdForName(topicDao, trimmed, courseId, languageId)) }
}

/** The id of the course's (or language's) topic named [name], adding it when there's none (no duplicates). */
suspend fun topicIdForName(topicDao: TopicDao, name: String, courseId: Long?, languageId: Long?): Long {
    val trimmed = name.trim()
    val existing = when {
        courseId != null -> topicDao.getByCourseId(courseId).first()
        languageId != null -> topicDao.getByLanguageId(languageId).first()
        else -> emptyList()
    }.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
    return existing?.id ?: topicDao.insert(Topic(name = trimmed, courseId = courseId, languageId = if (courseId == null) languageId else null))
}
