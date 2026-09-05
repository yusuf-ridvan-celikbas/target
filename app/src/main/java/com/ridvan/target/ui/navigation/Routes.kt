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
data class CourseStudySourceRoute(val courseId: Long)

@Serializable
object LanguageListRoute

@Serializable
data class LanguageStudySourceRoute(val languageId: Long)

@Serializable
object StudySourceHomeRoute

@Serializable
object UserEditRoute

@Serializable
object SettingsRoute
