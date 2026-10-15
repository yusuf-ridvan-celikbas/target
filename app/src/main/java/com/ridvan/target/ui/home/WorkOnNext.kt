package com.ridvan.target.ui.home

import com.ridvan.target.data.local.dao.WorkOnNextRow

const val WORK_ON_NEXT_MAX = 3
const val WORK_ON_NEXT_MIN_ANSWERED = 10
/** Accuracy at or above this isn't "weak" and never gets a slot on accuracy alone. */
const val WORK_ON_NEXT_WEAK_BELOW_PERCENT = 85

data class WorkOnNextItem(
    val topicId: Long,
    /** Set when the topic sits in exactly one Question Bank, so Home can open its progress directly. */
    val studyResourceTopicId: Long?,
    val topicName: String,
    val ownerName: String,
    val isCourse: Boolean,
    /** Accuracy percent when this slot was won on accuracy, else null. */
    val accuracyPercent: Int?,
    val questionsLeft: Int,
)

/**
 * Up to [WORK_ON_NEXT_MAX] topics: weakest accuracy first (topics with at least
 * [WORK_ON_NEXT_MIN_ANSWERED] answered and accuracy below [WORK_ON_NEXT_WEAK_BELOW_PERCENT]),
 * then the topics with the most questions left against their targets.
 */
fun rankWorkOnNext(rows: List<WorkOnNextRow>): List<WorkOnNextItem> {
    val topics = rows.groupBy { it.topicId }.map { (topicId, group) ->
        val first = group.first()
        val correct = group.sumOf { it.correct }
        val wrong = group.sumOf { it.wrong }
        val answered = correct + wrong
        WorkOnNextItem(
            topicId = topicId,
            studyResourceTopicId = group.singleOrNull()?.studyResourceTopicId,
            topicName = first.topicName,
            ownerName = first.courseName ?: first.languageName ?: "",
            isCourse = first.courseName != null,
            accuracyPercent = if (answered >= WORK_ON_NEXT_MIN_ANSWERED) correct * 100 / answered else null,
            questionsLeft = maxOf(0, group.sumOf { it.targetQuestions } - group.sumOf { it.loggedQuestions }),
        )
    }
    val weak = topics
        .filter { it.accuracyPercent != null && it.accuracyPercent < WORK_ON_NEXT_WEAK_BELOW_PERCENT }
        .sortedWith(compareBy<WorkOnNextItem> { it.accuracyPercent }.thenByDescending { it.questionsLeft }.thenBy { it.topicName })
        .take(WORK_ON_NEXT_MAX)
    val weakIds = weak.map { it.topicId }.toSet()
    val rest = topics
        .filter { it.topicId !in weakIds && it.questionsLeft > 0 }
        .sortedWith(compareByDescending<WorkOnNextItem> { it.questionsLeft }.thenBy { it.topicName })
        .map { it.copy(accuracyPercent = null) }
    return (weak + rest).take(WORK_ON_NEXT_MAX)
}
