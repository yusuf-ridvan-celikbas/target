package com.ridvan.target.ui.mockexam

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.local.entity.MockExamQuestion
import com.ridvan.target.data.local.entity.QuestionResult
import com.ridvan.target.data.local.entity.Topic
import com.ridvan.target.ui.common.FormDialogProperties
import com.ridvan.target.ui.common.SegmentedToggle
import com.ridvan.target.ui.common.SegmentedToggleOption
import com.ridvan.target.ui.common.TopicSearchField

private enum class QuestionListMode { BY_TOPIC, BY_QUESTION }

/**
 * Marks each question of a deneme part: tap a number to set its result and topic (Prev/Next walk
 * through them). The part's totals and topic results are recounted from the marks.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MockExamQuestionsScreen(
    onBack: () -> Unit,
    viewModel: MockExamQuestionsViewModel = viewModel(),
) {
    val part by viewModel.part.collectAsStateWithLifecycle()
    val questions by viewModel.questions.collectAsStateWithLifecycle()
    val topics by viewModel.topics.collectAsStateWithLifecycle()

    var editingNumber by remember { mutableStateOf<Int?>(null) }
    var listMode by remember { mutableStateOf(QuestionListMode.BY_TOPIC) }

    val count = part?.part?.questionCount ?: 0
    val inRange = questions.filterKeys { it in 1..count }.values
    val correct = inRange.count { it.result == QuestionResult.CORRECT }
    val wrong = inRange.count { it.result == QuestionResult.WRONG }
    val blank = inRange.count { it.result == QuestionResult.BLANK }
    val topicNames = topics.associate { it.id to it.name }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        part?.let { stringResource(R.string.mockexam_questions_title, partLabel(it)) }.orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                stringResource(R.string.mockexam_questions_summary, correct, wrong, blank, count - correct - wrong - blank),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(R.string.mockexam_questions_hint),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            TextButton(onClick = viewModel::markRestCorrect) { Text(stringResource(R.string.mockexam_questions_rest_correct)) }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                (1..count).forEach { number ->
                    val question = questions[number]
                    QuestionCell(
                        number = number,
                        result = question?.result,
                        hasTopic = question?.topicId != null,
                        onClick = { editingNumber = number },
                    )
                }
            }

            SegmentedToggle(
                options = listOf(
                    SegmentedToggleOption(QuestionListMode.BY_TOPIC, stringResource(R.string.mockexam_questions_by_topic)),
                    SegmentedToggleOption(QuestionListMode.BY_QUESTION, stringResource(R.string.mockexam_questions_by_question)),
                ),
                selected = listMode,
                onSelect = { listMode = it },
                modifier = Modifier.padding(top = 16.dp),
            )
            if (listMode == QuestionListMode.BY_TOPIC) {
                TopicDetails(inRange.toList(), topicNames)
            } else {
                QuestionDetails(count, questions, topicNames, onClick = { editingNumber = it })
            }
        }
    }

    editingNumber?.let { number ->
        QuestionPanel(
            number = number,
            count = count,
            question = questions[number],
            topics = topics,
            onResult = { viewModel.setResult(number, it) },
            onTopic = { viewModel.setTopic(number, it) },
            onCreateTopic = viewModel::createTopic,
            onMove = { editingNumber = it },
            onDismiss = { editingNumber = null },
        )
    }
}

@Composable
private fun resultLabel(result: QuestionResult?): String = stringResource(
    when (result) {
        QuestionResult.CORRECT -> R.string.question_result_correct
        QuestionResult.WRONG -> R.string.question_result_wrong
        QuestionResult.BLANK -> R.string.question_result_blank
        null -> R.string.mockexam_questions_not_marked
    }
)

@Composable
private fun resultColor(result: QuestionResult?): Color = when (result) {
    QuestionResult.WRONG -> MaterialTheme.colorScheme.error
    QuestionResult.CORRECT -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** Per topic (most missed first, untagged last): which question numbers were wrong, blank and correct. */
@Composable
private fun TopicDetails(questions: List<MockExamQuestion>, topicNames: Map<Long, String>) {
    val groups = questions.groupBy { it.topicId?.let { id -> topicNames[id] } }
        .map { (name, list) -> name to list.sortedBy { it.number } }
        .sortedWith(
            compareBy<Pair<String?, List<MockExamQuestion>>> { it.first == null }
                .thenByDescending { (_, list) -> list.count { it.result == QuestionResult.WRONG || it.result == QuestionResult.BLANK } }
        )
    if (groups.isEmpty()) {
        Text(stringResource(R.string.mockexam_questions_none_marked), modifier = Modifier.padding(top = 8.dp))
        return
    }
    groups.forEach { (name, list) ->
        ListItem(
            headlineContent = {
                Text(
                    stringResource(
                        R.string.mockexam_questions_topic_head,
                        name ?: stringResource(R.string.mockexam_questions_no_topic),
                        list.size,
                    ),
                    fontWeight = FontWeight.SemiBold,
                )
            },
            supportingContent = {
                Column {
                    listOf(QuestionResult.WRONG, QuestionResult.BLANK, QuestionResult.CORRECT, null).forEach { result ->
                        val numbers = list.filter { it.result == result }.map { it.number }
                        if (numbers.isNotEmpty()) {
                            Text(
                                stringResource(R.string.mockexam_questions_numbers_line, resultLabel(result), numbers.joinToString(", ")),
                                color = resultColor(result),
                            )
                        }
                    }
                }
            },
        )
        HorizontalDivider()
    }
}

/** One line per question: number, result, topic. */
@Composable
private fun QuestionDetails(count: Int, questions: Map<Int, MockExamQuestion>, topicNames: Map<Long, String>, onClick: (Int) -> Unit) {
    (1..count).forEach { number ->
        val question = questions[number]
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onClick(number) }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.mockexam_questions_number, number),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(end = 12.dp),
            )
            Text(resultLabel(question?.result), color = resultColor(question?.result), modifier = Modifier.padding(end = 12.dp))
            Text(
                question?.topicId?.let { topicNames[it] } ?: stringResource(R.string.mockexam_questions_no_topic),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        HorizontalDivider()
    }
}

/** One question's result and topic, saved as they change; Prev/Next move to the neighbouring question. */
@Composable
private fun QuestionPanel(
    number: Int,
    count: Int,
    question: MockExamQuestion?,
    topics: List<Topic>,
    onResult: (QuestionResult?) -> Unit,
    onTopic: (Long?) -> Unit,
    onCreateTopic: (name: String, onCreated: (Long) -> Unit) -> Unit,
    onMove: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        properties = FormDialogProperties,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mockexam_questions_number_title, number, count)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                SegmentedToggle(
                    options = listOf(
                        SegmentedToggleOption<QuestionResult?>(QuestionResult.CORRECT, stringResource(R.string.question_result_correct)),
                        SegmentedToggleOption(QuestionResult.WRONG, stringResource(R.string.question_result_wrong)),
                        SegmentedToggleOption(QuestionResult.BLANK, stringResource(R.string.question_result_blank)),
                        SegmentedToggleOption(null, stringResource(R.string.mockexam_questions_clear)),
                    ),
                    selected = question?.result,
                    onSelect = onResult,
                    textStyle = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // Keyed so the search box starts empty for each question.
                key(number) {
                    TopicSearchField(
                        topics = topics,
                        selectedId = question?.topicId,
                        onSelect = onTopic,
                        onCreate = onCreateTopic,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = { onMove(number - 1) }, enabled = number > 1) {
                    Text(stringResource(R.string.mockexam_questions_prev))
                }
                TextButton(onClick = { if (number < count) onMove(number + 1) else onDismiss() }) {
                    Text(stringResource(if (number < count) R.string.mockexam_questions_next else R.string.mockexam_questions_done))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.mockexam_questions_close)) }
        },
    )
}

@Composable
private fun QuestionCell(number: Int, result: QuestionResult?, hasTopic: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (result) {
        QuestionResult.CORRECT -> colors.primary to colors.onPrimary
        QuestionResult.WRONG -> colors.error to colors.onError
        QuestionResult.BLANK -> colors.surfaceVariant to colors.onSurfaceVariant
        null -> Color.Transparent to colors.onSurface
    }
    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(if (hasTopic) 2.dp else 1.dp, if (hasTopic) colors.tertiary else colors.outlineVariant),
        modifier = Modifier.size(width = 48.dp, height = 44.dp).clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(number.toString(), style = MaterialTheme.typography.labelLarge)
        }
    }
}
