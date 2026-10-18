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
}
