package com.ridvan.target.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object LoginRoute

@Serializable
object RegisterRoute

@Serializable
object HomeRoute

@Serializable
object ExamListRoute

@Serializable
data class ExamDetailRoute(val examId: Long)

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

@Serializable
object SwitchAccountRoute
