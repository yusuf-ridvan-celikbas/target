package com.ridvan.target.ui.mockexam

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.ridvan.target.data.local.entity.QuestionResult
import com.ridvan.target.ui.common.TopicSearchField
import com.ridvan.target.ui.common.SegmentedToggle
import com.ridvan.target.ui.common.SegmentedToggleOption

private enum class QuestionBrushMode { RESULT, TOPIC }

/**
 * Marks each question of a deneme part: pick a brush (a result, or a topic) and tap question numbers.
 * The part's totals and topic results are recounted from the marks.
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

    var mode by remember { mutableStateOf(QuestionBrushMode.RESULT) }
    // null = Clear (result mode) / No topic (topic mode).
    var resultBrush by remember { mutableStateOf<QuestionResult?>(QuestionResult.WRONG) }
    var topicBrush by remember { mutableStateOf<Long?>(null) }

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
            SegmentedToggle(
                options = listOf(
                    SegmentedToggleOption(QuestionBrushMode.RESULT, stringResource(R.string.mockexam_questions_mode_result)),
                    SegmentedToggleOption(QuestionBrushMode.TOPIC, stringResource(R.string.mockexam_questions_mode_topic)),
                ),
                selected = mode,
                onSelect = { mode = it },
                modifier = Modifier.padding(top = 12.dp),
            )
            if (mode == QuestionBrushMode.RESULT) {
                SegmentedToggle(
                    options = listOf(
                        SegmentedToggleOption<QuestionResult?>(QuestionResult.CORRECT, stringResource(R.string.question_result_correct)),
                        SegmentedToggleOption(QuestionResult.WRONG, stringResource(R.string.question_result_wrong)),
                        SegmentedToggleOption(QuestionResult.BLANK, stringResource(R.string.question_result_blank)),
                        SegmentedToggleOption(null, stringResource(R.string.mockexam_questions_clear)),
                    ),
                    selected = resultBrush,
                    onSelect = { resultBrush = it },
                    modifier = Modifier.padding(top = 8.dp),
                )
                TextButton(onClick = viewModel::markRestCorrect) { Text(stringResource(R.string.mockexam_questions_rest_correct)) }
            } else {
                TopicSearchField(
                    topics = topics,
                    selectedId = topicBrush,
                    onSelect = { topicBrush = it },
                    onCreate = viewModel::createTopic,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                (1..count).forEach { number ->
                    val question = questions[number]
                    val highlighted = mode == QuestionBrushMode.TOPIC && topicBrush != null && question?.topicId == topicBrush
                    QuestionCell(
                        number = number,
                        result = question?.result,
                        topicLabel = question?.topicId?.let { topicNames[it] },
                        highlighted = highlighted,
                        onClick = {
                            if (mode == QuestionBrushMode.RESULT) viewModel.setResult(number, resultBrush)
                            else viewModel.setTopic(number, if (question?.topicId == topicBrush) null else topicBrush)
                        },
                    )
                }
            }

            val byTopic = inRange.filter { it.topicId != null }.groupBy { it.topicId!! }
                .mapNotNull { (id, list) -> topicNames[id]?.let { it to list } }
                .sortedByDescending { (_, list) -> list.count { it.result == QuestionResult.WRONG || it.result == QuestionResult.BLANK } }
            if (byTopic.isNotEmpty()) {
                Text(
                    stringResource(R.string.mockexam_questions_by_topic),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )
                byTopic.forEach { (name, list) ->
                    ListItem(
                        headlineContent = { Text(name) },
                        supportingContent = {
                            Text(
                                stringResource(
                                    R.string.mockexam_questions_topic_row,
                                    list.count { it.result == QuestionResult.WRONG },
                                    list.count { it.result == QuestionResult.BLANK },
                                    list.count { it.result == QuestionResult.CORRECT },
                                )
                            )
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun QuestionCell(number: Int, result: QuestionResult?, topicLabel: String?, highlighted: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (result) {
        QuestionResult.CORRECT -> colors.primary to colors.onPrimary
        QuestionResult.WRONG -> colors.error to colors.onError
        QuestionResult.BLANK -> colors.surfaceVariant to colors.onSurfaceVariant
        null -> Color.Transparent to colors.onSurface
    }
    val shape = RoundedCornerShape(8.dp)
    Surface(
        color = container,
        contentColor = content,
        shape = shape,
        border = BorderStroke(if (highlighted) 3.dp else 1.dp, if (highlighted) colors.tertiary else colors.outlineVariant),
        modifier = Modifier.size(width = 52.dp, height = 48.dp).clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(number.toString(), style = MaterialTheme.typography.labelLarge)
                if (topicLabel != null) {
                    Text(topicLabel, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Clip)
                }
            }
        }
    }
}
