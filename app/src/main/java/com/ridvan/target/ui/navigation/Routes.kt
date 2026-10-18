package com.ridvan.target.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object LoginRoute

@Serializable
object RegisterRoute

@Serializable
object ResetPasswordRoute

@Serializable
object HomeRoute

/** Bottom-bar tab roots that list their pages (the other three tabs open a real page directly). */
@Serializable
object StudyHubRoute

@Serializable
object FocusHubRoute

@Serializable
object ExamListRoute

@Serializable
data class ExamDetailRoute(val examId: Long, val openNewMockExam: Boolean = false)

@Serializable
data class SectionDetailRoute(val sectionId: Long)

@Serializable
object CourseListRoute

@Serializable
data class CourseListByTypeRoute(val examTypeId: Long)

@Serializable
object LanguageExamCoursesRoute

@Serializable
data class CourseDetailRoute(val courseId: Long)

@Serializable
data class LanguageDetailRoute(val languageId: Long)

@Serializable
data class CourseStudyResourceRoute(val courseId: Long)

@Serializable
object LanguageListRoute

@Serializable
data class LanguageStudyResourceRoute(val languageId: Long)

@Serializable
data class StudyResourceDetailRoute(val studyResourceId: Long)

@Serializable
object StudyResourceHomeRoute

@Serializable
data class StudyResourceCourseListByTypeRoute(val examTypeId: Long)

@Serializable
object StudyResourceLanguageExamCoursesRoute

@Serializable
object UserEditRoute

@Serializable
object SettingsRoute

/** Which parts of the app the account uses; [onboarding] is the screen shown right after Register. */
@Serializable
data class ModulesRoute(val onboarding: Boolean = false)

@Serializable
object MyAccountRoute

@Serializable
object SwitchAccountRoute

@Serializable
data class TopicListRoute(val courseId: Long)

@Serializable
data class TopicDetailRoute(val topicId: Long)

@Serializable
object TopicHomeRoute

@Serializable
data class TopicCourseListByTypeRoute(val examTypeId: Long)

@Serializable
object TopicLanguageExamCoursesRoute

@Serializable
data class LanguageTopicListRoute(val languageId: Long)

@Serializable
object HelpRoute

@Serializable
data class TopicProgressRoute(val studyResourceTopicId: Long)

@Serializable
object StatisticsRoute

@Serializable
data class PracticeExamEntryDetailRoute(val entryId: Long)

@Serializable
object PlannerRoute

@Serializable
object FocusTimerRoute

@Serializable
object FlowTimerRoute

@Serializable
object FocusPresetListRoute

@Serializable
object FocusHistoryRoute

@Serializable
object FocusHistoryHomeRoute

@Serializable
data class FocusSessionDetailRoute(val sessionId: Long)

/** Focus Timer history filtered to one course — reached from CourseDetailScreen's Work History button. */
@Serializable
data class CourseWorkHistoryRoute(val courseId: Long)

/** Focus Timer history filtered to one language — reached from LanguageDetailScreen's Work History button. */
@Serializable
data class LanguageWorkHistoryRoute(val languageId: Long)

/** App Settings opened from Focus Timer's settings icon — same screen as SettingsRoute, scrolled to Focus Timer Preferences. */
@Serializable
object FocusTimerSettingsRoute

@Serializable
object ReadingRoute

@Serializable
data class BookDetailRoute(val bookId: Long)

/** Every book on wooden shelves — a drill-down from the Reading page. */
@Serializable
object LibraryRoute
