package com.ridvan.target.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ridvan.target.data.local.entity.Exam
import com.ridvan.target.data.local.entity.ExamType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditExamDialog(
    examTypes: List<ExamType>,
    initial: Exam? = null,
    onConfirm: (name: String, examTypeId: Long, hasSections: Boolean, examDate: Long?, studyStartDate: Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var selectedTypeId by remember { mutableStateOf(initial?.examTypeId ?: examTypes.firstOrNull()?.id) }
    var hasSections by remember { mutableStateOf(initial?.hasSections ?: false) }
    var examDate by remember { mutableStateOf(initial?.examDate) }
    var studyStartDate by remember { mutableStateOf(initial?.studyStartDate) }

    var showExamDatePicker by remember { mutableStateOf(false) }
    var showStudyStartDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New exam" else "Edit exam") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exam name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                ExamTypeField(
                    examTypes = examTypes,
                    selectedId = selectedTypeId,
                    onSelect = { selectedTypeId = it },
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Has multiple sections", modifier = Modifier.weight(1f))
                    Switch(
                        checked = hasSections,
                        onCheckedChange = { hasSections = it },
                        enabled = initial == null,
                    )
                }

                if (!hasSections) {
                    DateField(
                        label = "Exam date",
                        value = examDate,
                        onClick = { showExamDatePicker = true },
                    )
                }

                DateField(
                    label = "Study start date (optional)",
                    value = studyStartDate,
                    onClick = { showStudyStartDatePicker = true },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val typeId = selectedTypeId ?: return@TextButton
                    onConfirm(name, typeId, hasSections, examDate, studyStartDate)
                },
                enabled = name.isNotBlank() && selectedTypeId != null,
            ) {
                Text(if (initial == null) "Add" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )

    if (showExamDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = examDate)
        DatePickerDialog(
            onDismissRequest = { showExamDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    examDate = state.selectedDateMillis
                    showExamDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showExamDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = state)
        }
    }

    if (showStudyStartDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = studyStartDate)
        DatePickerDialog(
            onDismissRequest = { showStudyStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    studyStartDate = state.selectedDateMillis
                    showStudyStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStudyStartDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun ExamTypeField(examTypes: List<ExamType>, selectedId: Long?, onSelect: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(modifier = Modifier.fillMaxWidth().clickable { expanded = true }) {
            Text("Exam type", style = MaterialTheme.typography.labelSmall)
            Text(
                examTypes.firstOrNull { it.id == selectedId }?.name ?: "Select",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            examTypes.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.name) },
                    onClick = {
                        onSelect(type.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun DateField(label: String, value: Long?, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable(onClick = onClick)) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value?.let { formatDate(it) } ?: "Tap to set", style = MaterialTheme.typography.bodyLarge)
    }
}
