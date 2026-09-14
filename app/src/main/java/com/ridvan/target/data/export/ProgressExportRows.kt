package com.ridvan.target.data.export

import com.ridvan.target.data.local.dao.PracticeLogDao
import com.ridvan.target.data.local.dao.TopicDao
import kotlinx.coroutines.flow.first

/**
 * Builds one CSV row per (course, topic) across [courses] — target test/question counts (cross
 * publisher, from [TopicDao.getTopicTotalsByCourseId]) alongside cumulative solved/correct/wrong
 * progress (from [PracticeLogDao.getProgressByTopicIds]), for the multi-course progress export
 * (Section/Exam detail screens). Topics with no logged sessions still get a row, all zeros.
 */
suspend fun buildProgressExportRows(
    courses: List<Pair<Long, String>>,
    topicDao: TopicDao,
    practiceLogDao: PracticeLogDao,
): List<List<String>> {
    val topicsByCourse = courses.map { (courseId, courseName) ->
        courseName to topicDao.getTopicTotalsByCourseId(courseId).first()
    }
    val allTopicIds = topicsByCourse.flatMap { (_, topics) -> topics.map { it.topic.id } }
    val progressByTopicId = practiceLogDao.getProgressByTopicIds(allTopicIds).first().associateBy { it.topicId }

    return topicsByCourse.flatMap { (courseName, topics) ->
        topics.map { topicWithTotals ->
            val progress = progressByTopicId[topicWithTotals.topic.id]
            val solved = progress?.totalSolved ?: 0
            val unsolved = progress?.totalUnsolved ?: 0
            val testsSolved = progress?.totalTestsSolved ?: 0
            val durationMinutes = progress?.totalDurationMinutes ?: 0
            val totalQuestions = solved + unsolved
            val remainingTests = (topicWithTotals.totalTestCount - testsSolved).coerceAtLeast(0)
            val remainingQuestions = (topicWithTotals.totalQuestionCount - totalQuestions).coerceAtLeast(0)
            val net = solved - unsolved / 4.0
            val accuracy = if (totalQuestions == 0) 0 else solved * 100 / totalQuestions
            listOf(
                courseName,
                topicWithTotals.topic.name,
                topicWithTotals.totalTestCount.toString(),
                topicWithTotals.totalQuestionCount.toString(),
                testsSolved.toString(),
                solved.toString(),
                unsolved.toString(),
                remainingTests.toString(),
                remainingQuestions.toString(),
                "%.2f".format(net),
                accuracy.toString(),
                durationMinutes.toString(),
            )
        }
    }
}
