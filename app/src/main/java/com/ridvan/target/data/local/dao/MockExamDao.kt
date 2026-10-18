package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ridvan.target.data.local.entity.CourseCategory
import com.ridvan.target.data.local.entity.MockExam
import com.ridvan.target.data.local.entity.MockExamPart
import com.ridvan.target.data.local.entity.MockExamQuestion
import com.ridvan.target.data.local.entity.QuestionResult
import com.ridvan.target.data.local.entity.ScoringSystem
import com.ridvan.target.data.local.entity.MockExamTopicResult
import kotlinx.coroutines.flow.Flow

data class MockExamPartWithCourse(
    @Embedded val part: MockExamPart,
    /** Null for a named part (no course); use the part's own name then. */
    val courseName: String?,
    val courseIcon: String?,
    val courseCategory: CourseCategory?,
)

data class MockExamTopicResultWithTopic(
    @Embedded val result: MockExamTopicResult,
    val topicName: String,
)

/** One mock exam with its summed results, for lists. */
data class MockExamSummary(
    @Embedded val mockExam: MockExam,
    val examName: String,
    val sectionName: String?,
    val questionCount: Int,
    val correctCount: Int,
    val wrongCount: Int,
)

/** A finished deneme's part with its deneme's context, for Statistics. */
data class MockExamPartStatRow(
    @Embedded val part: MockExamPart,
    val mockName: String,
    val takenAt: Long,
    val durationMinutes: Int,
    val examId: Long,
    val sectionName: String?,
    val scoringSystem: ScoringSystem?,
)

/** A topic result of a finished deneme with names and context, for Statistics' topics to work on. */
data class MockExamTopicStatRow(
    val topicId: Long,
    val topicName: String,
    val courseName: String?,
    val languageName: String?,
    val partCourseId: Long?,
    val examId: Long,
    val takenAt: Long,
    val questionCount: Int,
    val correctCount: Int,
    val wrongCount: Int,
)

@Dao
interface MockExamDao {
    @Insert
    suspend fun insert(mockExam: MockExam): Long

    @Update
    suspend fun update(mockExam: MockExam)

    @Delete
    suspend fun delete(mockExam: MockExam)

    @Insert
    suspend fun insertParts(parts: List<MockExamPart>)

    @Transaction
    suspend fun insertWithParts(mockExam: MockExam, parts: List<MockExamPart>): Long {
        val id = insert(mockExam)
        insertParts(parts.map { it.copy(mockExamId = id) })
        return id
    }

    @Update
    suspend fun updatePart(part: MockExamPart)

    @Update
    suspend fun updateParts(parts: List<MockExamPart>)

    @Transaction
    suspend fun updateWithParts(mockExam: MockExam, parts: List<MockExamPart>) {
        update(mockExam)
        updateParts(parts)
    }

    @Query("SELECT * FROM mock_exams WHERE id = :id")
    fun getById(id: Long): Flow<MockExam?>

    @Query(
        """
        SELECT mock_exam_parts.*, courses.name AS courseName, courses.icon AS courseIcon, courses.category AS courseCategory
        FROM mock_exam_parts LEFT JOIN courses ON courses.id = mock_exam_parts.courseId
        WHERE mockExamId = :mockExamId ORDER BY orderIndex
        """
    )
    fun getParts(mockExamId: Long): Flow<List<MockExamPartWithCourse>>

    @Query("SELECT * FROM mock_exam_parts WHERE mockExamId = :mockExamId ORDER BY orderIndex")
    suspend fun getPartsOnce(mockExamId: Long): List<MockExamPart>

    /** The latest finished TYT-scored deneme of an exam, whose TYT nets feed AYT scores. */
    @Query(
        """
        SELECT * FROM mock_exams WHERE examId = :examId AND scoringSystem = 'TYT' AND status = 'FINISHED'
        ORDER BY takenAt DESC LIMIT 1
        """
    )
    fun getLatestFinishedTyt(examId: Long): Flow<MockExam?>

    /** Parts of the latest mock exam for this exam (and section, null = none), to prefill the next one. */
    @Query(
        """
        SELECT * FROM mock_exam_parts WHERE mockExamId =
            (SELECT id FROM mock_exams WHERE examId = :examId AND sectionId IS :sectionId ORDER BY takenAt DESC LIMIT 1)
        ORDER BY orderIndex
        """
    )
    suspend fun getLatestParts(examId: Long, sectionId: Long?): List<MockExamPart>

    /** Planned mock exams with a start time, for the "time to start?" prompt and reminders. */
    @Query(
        """
        SELECT mock_exams.* FROM mock_exams JOIN exams ON exams.id = mock_exams.examId
        WHERE exams.userId = :userId AND mock_exams.status = 'PLANNED' AND mock_exams.scheduledAt IS NOT NULL
        ORDER BY mock_exams.scheduledAt
        """
    )
    fun getScheduledForUser(userId: Long): Flow<List<MockExam>>

    @Query("SELECT * FROM mock_exams WHERE examId = :examId AND sectionId IS :sectionId ORDER BY takenAt DESC LIMIT 1")
    suspend fun getLatest(examId: Long, sectionId: Long?): MockExam?

    @Query(
        """
        SELECT mock_exams.*, exams.name AS examName, sections.name AS sectionName,
               COALESCE(SUM(p.questionCount), 0) AS questionCount,
               COALESCE(SUM(p.correctCount), 0) AS correctCount,
               COALESCE(SUM(p.wrongCount), 0) AS wrongCount
        FROM mock_exams
        JOIN exams ON exams.id = mock_exams.examId
        LEFT JOIN sections ON sections.id = mock_exams.sectionId
        LEFT JOIN mock_exam_parts p ON p.mockExamId = mock_exams.id
        WHERE mock_exams.examId = :examId
        GROUP BY mock_exams.id ORDER BY mock_exams.takenAt DESC
        """
    )
    fun getSummariesByExam(examId: Long): Flow<List<MockExamSummary>>

    @Query(
        """
        SELECT mock_exams.*, exams.name AS examName, sections.name AS sectionName,
               COALESCE(SUM(p.questionCount), 0) AS questionCount,
               COALESCE(SUM(p.correctCount), 0) AS correctCount,
               COALESCE(SUM(p.wrongCount), 0) AS wrongCount
        FROM mock_exams
        JOIN exams ON exams.id = mock_exams.examId
        LEFT JOIN sections ON sections.id = mock_exams.sectionId
        LEFT JOIN mock_exam_parts p ON p.mockExamId = mock_exams.id
        WHERE exams.userId = :userId AND mock_exams.finishedAt IS NOT NULL
        GROUP BY mock_exams.id ORDER BY mock_exams.takenAt ASC
        """
    )
    fun getSummariesForUser(userId: Long): Flow<List<MockExamSummary>>

    @Query(
        """
        SELECT mock_exam_topic_results.*, topics.name AS topicName
        FROM mock_exam_topic_results JOIN topics ON topics.id = mock_exam_topic_results.topicId
        WHERE mockExamPartId IN (SELECT id FROM mock_exam_parts WHERE mockExamId = :mockExamId)
        ORDER BY topics.name
        """
    )
    fun getTopicResults(mockExamId: Long): Flow<List<MockExamTopicResultWithTopic>>

    @Insert
    suspend fun insertTopicResult(result: MockExamTopicResult)

    @Update
    suspend fun updateTopicResult(result: MockExamTopicResult)

    @Delete
    suspend fun deleteTopicResult(result: MockExamTopicResult)

    @Query(
        """
        SELECT mock_exam_parts.*, courses.name AS courseName, courses.icon AS courseIcon, courses.category AS courseCategory
        FROM mock_exam_parts LEFT JOIN courses ON courses.id = mock_exam_parts.courseId
        WHERE mock_exam_parts.id = :partId
        """
    )
    fun getPartWithCourse(partId: Long): Flow<MockExamPartWithCourse?>

    @Query("SELECT * FROM mock_exam_parts WHERE id = :partId")
    suspend fun getPartOnce(partId: Long): MockExamPart?

    @Query(
        """
        SELECT DISTINCT q.mockExamPartId FROM mock_exam_questions q JOIN mock_exam_parts p ON p.id = q.mockExamPartId
        WHERE p.mockExamId = :mockExamId AND q.result IS NOT NULL
        """
    )
    fun getCountedPartIds(mockExamId: Long): Flow<List<Long>>

    @Query("SELECT * FROM mock_exam_questions WHERE mockExamPartId = :partId ORDER BY number")
    fun getQuestions(partId: Long): Flow<List<MockExamQuestion>>

    @Query("SELECT * FROM mock_exam_questions WHERE mockExamPartId = :partId ORDER BY number")
    suspend fun getQuestionsOnce(partId: Long): List<MockExamQuestion>

    @Query("SELECT * FROM mock_exam_topic_results WHERE mockExamPartId = :partId")
    suspend fun getTopicResultsOnce(partId: Long): List<MockExamTopicResult>

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun upsertQuestions(questions: List<MockExamQuestion>)

    @Query("DELETE FROM mock_exam_questions WHERE mockExamPartId = :partId AND result IS NULL AND topicId IS NULL")
    suspend fun deleteEmptyQuestions(partId: Long)

    @Query("DELETE FROM mock_exam_topic_results WHERE mockExamPartId = :partId")
    suspend fun deleteTopicResultsForPart(partId: Long)

    /**
     * Saves marked questions, then recounts the part from its questions (within its question count):
     * correct/wrong once any question has a result, topic results once any question has a topic.
     */
    @Transaction
    suspend fun saveQuestions(partId: Long, questions: List<MockExamQuestion>) {
        upsertQuestions(questions)
        deleteEmptyQuestions(partId)
        val part = getPartOnce(partId) ?: return
        val marked = getQuestionsOnce(partId).filter { it.number <= part.questionCount }
        if (marked.any { it.result != null }) {
            updatePart(
                part.copy(
                    correctCount = marked.count { it.result == QuestionResult.CORRECT },
                    wrongCount = marked.count { it.result == QuestionResult.WRONG },
                )
            )
        }
        if (marked.any { it.topicId != null }) {
            deleteTopicResultsForPart(partId)
            marked.filter { it.topicId != null }.groupBy { it.topicId!! }.forEach { (topicId, list) ->
                insertTopicResult(
                    MockExamTopicResult(
                        mockExamPartId = partId,
                        topicId = topicId,
                        questionCount = list.size,
                        correctCount = list.count { it.result == QuestionResult.CORRECT },
                        wrongCount = list.count { it.result == QuestionResult.WRONG },
                    )
                )
            }
        }
    }

    @Query(
        """
        SELECT p.*, m.name AS mockName, m.takenAt, m.durationMinutes, m.examId, s.name AS sectionName, m.scoringSystem
        FROM mock_exam_parts p
        JOIN mock_exams m ON m.id = p.mockExamId
        JOIN exams e ON e.id = m.examId
        LEFT JOIN sections s ON s.id = m.sectionId
        WHERE e.userId = :userId AND m.status = 'FINISHED'
        ORDER BY m.takenAt DESC, p.orderIndex
        """
    )
    fun getFinishedPartsForUser(userId: Long): Flow<List<MockExamPartStatRow>>

    @Query(
        """
        SELECT r.topicId, t.name AS topicName, c.name AS courseName, l.name AS languageName, p.courseId AS partCourseId,
               m.examId, m.takenAt, r.questionCount, r.correctCount, r.wrongCount
        FROM mock_exam_topic_results r
        JOIN mock_exam_parts p ON p.id = r.mockExamPartId
        JOIN mock_exams m ON m.id = p.mockExamId
        JOIN exams e ON e.id = m.examId
        JOIN topics t ON t.id = r.topicId
        LEFT JOIN courses c ON c.id = t.courseId
        LEFT JOIN languages l ON l.id = t.languageId
        WHERE e.userId = :userId AND m.status = 'FINISHED'
        """
    )
    fun getFinishedTopicResultsForUser(userId: Long): Flow<List<MockExamTopicStatRow>>
}
