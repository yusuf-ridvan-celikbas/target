package com.ridvan.target.ui.mockexam

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ridvan.target.R
import com.ridvan.target.data.local.dao.ExamCourseWithCourse
import com.ridvan.target.ui.common.CourseIconAvatar
import com.ridvan.target.ui.common.FormDialogProperties
import com.ridvan.target.ui.common.courseDisplayName

/** What the new-deneme form starts from: the last deneme's limit and counts, if any. */
data class MockExamDraft(
    val name: String,
    val publisher: String,
    val timeLimitMinutes: Int?,
    val questionCounts: Map<Long, Int>,
)

/**
 * New combined mock exam: name, publisher, time limit and a question count per course of the exam's pool
 * (0 = course not in this deneme), plus an optional planned start. [onSave] plans it (started later from its
 * page); [onAlreadyTaken] saves one taken elsewhere, straight to entering results.
 */
@Composable
fun NewMockExamDialog(
    courses: List<ExamCourseWithCourse>,
    draft: MockExamDraft,
    onSave: (name: String, publisher: String, timeLimitMinutes: Int, counts: Map<Long, Int>, scheduledAt: Long?) -> Unit,
    onAlreadyTaken: (name: String, publisher: String, timeLimitMinutes: Int, counts: Map<Long, Int>) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(draft.name) }
    var publisher by remember { mutableStateOf(draft.publisher) }
    var scheduledAt by remember { mutableStateOf<Long?>(null) }
    var limitText by remember { mutableStateOf(draft.timeLimitMinutes?.toString().orEmpty()) }
    val countTexts = remember {
        mutableStateMapOf<Long, String>().apply {
            courses.forEach { put(it.examCourse.courseId, draft.questionCounts[it.examCourse.courseId]?.toString().orEmpty()) }
        }
    }
    val counts = countTexts.mapValues { it.value.toIntOrNull() ?: 0 }.filterValues { it > 0 }
    val limit = limitText.toIntOrNull() ?: 0
    val valid = name.isNotBlank() && limit > 0 && counts.isNotEmpty()

    AlertDialog(
        properties = FormDialogProperties,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mockexam_new_title)) },
        text = {
            Column(modifier = Modifier.imePadding().verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.common_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = publisher,
                    onValueChange = { publisher = it },
                    label = { Text(stringResource(R.string.label_publisher_optional)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { input -> if (input.length <= 3 && input.all(Char::isDigit)) limitText = input },
                    label = { Text(stringResource(R.string.mockexam_time_limit_minutes)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                StartTimeField(
                    value = scheduledAt,
                    onValueChange = { scheduledAt = it },
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    stringResource(R.string.mockexam_question_counts),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 16.dp),
                )
                if (courses.isEmpty()) {
                    Text(stringResource(R.string.mockexam_no_courses), modifier = Modifier.padding(top = 4.dp))
                }
                courses.forEach { course ->
                    val courseId = course.examCourse.courseId
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CourseIconAvatar(course.courseIcon)
                        Text(
                            courseDisplayName(course.courseName),
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                        )
                        OutlinedTextField(
                            value = countTexts[courseId].orEmpty(),
                            onValueChange = { input -> if (input.length <= 3 && input.all(Char::isDigit)) countTexts[courseId] = input },
                            placeholder = { Text("0") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(88.dp),
                        )
                    }
                }
                Text(
                    stringResource(R.string.mockexam_question_counts_hint),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, publisher, limit, counts, scheduledAt) }, enabled = valid) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
                TextButton(onClick = { onAlreadyTaken(name, publisher, limit, counts) }, enabled = valid) {
                    Text(stringResource(R.string.mockexam_already_taken))
                }
            }
        },
    )
}
