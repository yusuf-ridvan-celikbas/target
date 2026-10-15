package com.ridvan.target.ui.navigation

import com.ridvan.target.ui.reading.BookDetailScreen
import com.ridvan.target.ui.reading.ReadingScreen
import com.ridvan.target.ui.reading.LibraryScreen
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.ridvan.target.R
import com.ridvan.target.TargetApplication
import com.ridvan.target.ui.auth.LoginScreen
import com.ridvan.target.ui.auth.RegisterScreen
import com.ridvan.target.ui.courselist.CourseListByTypeScreen
import com.ridvan.target.ui.courselist.CourseListScreen
import com.ridvan.target.ui.coursedetail.CourseDetailScreen
import com.ridvan.target.ui.examdetail.ExamDetailScreen
import com.ridvan.target.ui.examlist.ExamListScreen
import com.ridvan.target.ui.focustimer.FocusHistoryHomeScreen
import com.ridvan.target.ui.focustimer.FocusHistoryScreen
import com.ridvan.target.ui.focustimer.FocusSessionDetailScreen
import com.ridvan.target.ui.focustimer.FocusWorkHistoryScreen
import com.ridvan.target.ui.focustimer.FocusPresetListScreen
import com.ridvan.target.ui.focustimer.FocusTimerScreen
import com.ridvan.target.ui.focustimer.FlowTimerScreen
import com.ridvan.target.ui.help.HelpScreen
import com.ridvan.target.ui.home.HomeScreen
import com.ridvan.target.ui.languagedetail.LanguageDetailScreen
import com.ridvan.target.ui.languagelist.LanguageExamCoursesScreen
import com.ridvan.target.ui.languagelist.LanguageListScreen
import com.ridvan.target.ui.myaccount.MyAccountScreen
import com.ridvan.target.ui.planner.PlannerHomeScreen
import com.ridvan.target.ui.practiceexam.PracticeExamEntryDetailScreen
import com.ridvan.target.ui.sectiondetail.SectionDetailScreen
import com.ridvan.target.ui.settings.SettingsScreen
import com.ridvan.target.ui.hub.FocusHubScreen
import com.ridvan.target.ui.hub.PlanHubScreen
import com.ridvan.target.ui.routines.RoutineDetailScreen
import com.ridvan.target.ui.routines.RoutineListScreen
import com.ridvan.target.ui.trips.TripDetailScreen
import com.ridvan.target.ui.trips.TripListScreen
import com.ridvan.target.ui.hub.StudyHubScreen
import com.ridvan.target.ui.shell.ShellNavigation
import com.ridvan.target.ui.shell.ShellTab
import com.ridvan.target.ui.studyresource.CourseStudyResourceScreen
import com.ridvan.target.ui.studyresource.LanguageStudyResourceScreen
import com.ridvan.target.ui.studyresource.StudyResourceHomeScreen
import com.ridvan.target.ui.studyresourcedetail.StudyResourceDetailScreen
import com.ridvan.target.ui.statistics.StatisticsScreen
import com.ridvan.target.ui.switchaccount.SwitchAccountScreen
import com.ridvan.target.ui.topicdetail.TopicDetailScreen
import com.ridvan.target.ui.topicprogress.TopicProgressScreen
import com.ridvan.target.ui.topiclist.LanguageTopicListScreen
import com.ridvan.target.ui.topiclist.TopicHomeScreen
import com.ridvan.target.ui.topiclist.TopicListScreen
import com.ridvan.target.ui.user.UserEditScreen

private fun NavHostController.navigateToShellDestination(route: Any) {
    navigate(route) {
        popUpTo<HomeRoute> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun TargetNavHost() {
    val navController = rememberNavController()
    val application = LocalContext.current.applicationContext as TargetApplication
    val startDestination = if (application.preferences.currentUserId != null) HomeRoute else LoginRoute

    val signOut: () -> Unit = {
        application.preferences.currentUserId = null
        navController.navigate(LoginRoute) { popUpTo(0) }
    }

    // Bottom-bar tabs switch with navigateToShellDestination (each tab keeps its own back stack);
    // pages inside a tab, and the profile menu's pages, are pushed on top of the current tab.
    val push: (Any) -> Unit = { route -> navController.navigate(route) { launchSingleTop = true } }
    val tabRoutes = mapOf<ShellTab, Any>(
        ShellTab.HOME to HomeRoute,
        ShellTab.STUDY to StudyHubRoute,
        ShellTab.PLAN to PlanHubRoute,
        ShellTab.FOCUS to FocusHubRoute,
        ShellTab.STATS to StatisticsRoute,
    )
    val shellNavigation = ShellNavigation(
        onNavigateHome = { navController.navigateToShellDestination(HomeRoute) },
        onNavigateStudyHub = { navController.navigateToShellDestination(StudyHubRoute) },
        onNavigateFocusHub = { navController.navigateToShellDestination(FocusHubRoute) },
        onNavigatePlanHub = { navController.navigateToShellDestination(PlanHubRoute) },
        onNavigateTrips = { push(TripListRoute) },
        onNavigateRoutines = { push(RoutineListRoute) },
        onNavigateExams = { push(ExamListRoute) },
        onNavigateCourses = { push(CourseListRoute) },
        onNavigateStudyResources = { push(StudyResourceHomeRoute) },
        onNavigateTopics = { push(TopicHomeRoute) },
        onNavigateStatistics = { navController.navigateToShellDestination(StatisticsRoute) },
        onNavigateLanguages = { push(LanguageListRoute) },
        onNavigatePlanner = { push(PlannerRoute) },
        onNavigateFocusTimer = { push(FocusTimerRoute) },
        onNavigateFlowTimer = { push(FlowTimerRoute) },
        onNavigateStudyHistory = { push(FocusHistoryHomeRoute) },
        onNavigateReading = { push(ReadingRoute) },
        onNavigateHelp = { push(HelpRoute) },
        onNavigateMyAccount = { push(MyAccountRoute) },
        onNavigateSettings = { push(SettingsRoute) },
        onNavigateModules = { push(ModulesRoute()) },
        onNavigateProfile = { navController.navigate(UserEditRoute) },
        onLogOut = signOut,
        onSwitchAccount = { navController.navigate(SwitchAccountRoute) },
        onReselectTab = { tab ->
            val root = tabRoutes.getValue(tab)
            // Back to the tab's own first page; if it isn't on the stack (e.g. Reading opened
            // straight from Home), open the tab fresh instead.
            if (!navController.popBackStack(root, inclusive = false)) navController.navigateToShellDestination(root)
        },
        onBack = { navController.popBackStack() },
    )

    NavHost(navController = navController, startDestination = startDestination) {
        composable<LoginRoute> {
            LoginScreen(
                onLoginSuccess = { navController.navigate(HomeRoute) { popUpTo(0) } },
                onNavigateToRegister = { navController.navigate(RegisterRoute) },
            )
        }
        composable<RegisterRoute> {
            RegisterScreen(
                onRegisterSuccess = { navController.navigate(ModulesRoute(onboarding = true)) { popUpTo(0) } },
                onNavigateToLogin = { navController.navigate(LoginRoute) },
            )
        }
        composable<HomeRoute> {
            HomeScreen(
                shellNavigation = shellNavigation,
                onExamClick = { examId -> navController.navigate(ExamDetailRoute(examId)) },
                onSectionClick = { sectionId -> navController.navigate(SectionDetailRoute(sectionId)) },
                onFocusSessionClick = { sessionId -> navController.navigate(FocusSessionDetailRoute(sessionId)) },
                onPracticeSessionClick = { id -> navController.navigate(TopicProgressRoute(id)) },
                onBookClick = { bookId -> navController.navigate(BookDetailRoute(bookId)) },
                onOpenReading = shellNavigation.onNavigateReading,
                onTopicClick = { topicId -> navController.navigate(TopicDetailRoute(topicId)) },
                onRoutineClick = { routineId -> navController.navigate(RoutineDetailRoute(routineId)) },
            )
        }
        composable<StudyHubRoute> {
            StudyHubScreen(shellNavigation = shellNavigation)
        }
        composable<FocusHubRoute> {
            FocusHubScreen(shellNavigation = shellNavigation)
        }
        composable<PlanHubRoute> {
            PlanHubScreen(shellNavigation = shellNavigation)
        }
        composable<TripListRoute> {
            TripListScreen(
                shellNavigation = shellNavigation,
                onTripClick = { tripId -> navController.navigate(TripDetailRoute(tripId)) },
            )
        }
        composable<TripDetailRoute> {
            TripDetailScreen(onBack = { navController.popBackStack() })
        }
        composable<RoutineListRoute> {
            RoutineListScreen(
                shellNavigation = shellNavigation,
                onRoutineClick = { routineId -> navController.navigate(RoutineDetailRoute(routineId)) },
            )
        }
        composable<RoutineDetailRoute> {
            RoutineDetailScreen(onBack = { navController.popBackStack() })
        }
        composable<ExamListRoute> {
            ExamListScreen(
                shellNavigation = shellNavigation,
                onExamClick = { examId -> navController.navigate(ExamDetailRoute(examId)) },
            )
        }
        composable<ExamDetailRoute> {
            ExamDetailScreen(
                onSectionClick = { sectionId -> navController.navigate(SectionDetailRoute(sectionId)) },
                onCourseClick = { courseId -> navController.navigate(CourseDetailRoute(courseId)) },
                onLanguageClick = { languageId -> navController.navigate(LanguageDetailRoute(languageId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<SectionDetailRoute> {
            SectionDetailScreen(
                onCourseClick = { courseId -> navController.navigate(CourseDetailRoute(courseId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<CourseListRoute> {
            CourseListScreen(
                shellNavigation = shellNavigation,
                onCourseTypeClick = { examType -> navController.navigate(CourseListByTypeRoute(examType.id)) },
                onLanguageTypeClick = { navController.navigate(LanguageExamCoursesRoute) },
                onCourseShortcutClick = { courseId -> navController.navigate(CourseDetailRoute(courseId)) },
                onLanguageShortcutClick = { languageId -> navController.navigate(LanguageDetailRoute(languageId)) },
            )
        }
        composable<StudyResourceHomeRoute> {
            StudyResourceHomeScreen(
                shellNavigation = shellNavigation,
                onCourseTypeClick = { examType -> navController.navigate(StudyResourceCourseListByTypeRoute(examType.id)) },
                onLanguageTypeClick = { navController.navigate(StudyResourceLanguageExamCoursesRoute) },
                onCourseShortcutClick = { courseId -> navController.navigate(CourseStudyResourceRoute(courseId)) },
                onLanguageShortcutClick = { languageId -> navController.navigate(LanguageStudyResourceRoute(languageId)) },
            )
        }
        composable<CourseListByTypeRoute> {
            CourseListByTypeScreen(
                onCourseClick = { courseId -> navController.navigate(CourseDetailRoute(courseId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<LanguageExamCoursesRoute> {
            LanguageExamCoursesScreen(
                onLanguageClick = { languageId -> navController.navigate(LanguageDetailRoute(languageId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<StudyResourceCourseListByTypeRoute> {
            CourseListByTypeScreen(
                onCourseClick = { courseId -> navController.navigate(CourseStudyResourceRoute(courseId)) },
                onBack = { navController.popBackStack() },
                titleFormatRes = R.string.course_type_bucket_title_resources,
            )
        }
        composable<StudyResourceLanguageExamCoursesRoute> {
            LanguageExamCoursesScreen(
                onLanguageClick = { languageId -> navController.navigate(LanguageStudyResourceRoute(languageId)) },
                onBack = { navController.popBackStack() },
                titleRes = R.string.label_language_exam_resources,
            )
        }
        composable<TopicHomeRoute> {
            TopicHomeScreen(
                shellNavigation = shellNavigation,
                onCourseTypeClick = { examType -> navController.navigate(TopicCourseListByTypeRoute(examType.id)) },
                onLanguageTypeClick = { navController.navigate(TopicLanguageExamCoursesRoute) },
                onCourseShortcutClick = { courseId -> navController.navigate(TopicListRoute(courseId)) },
                onLanguageShortcutClick = { languageId -> navController.navigate(LanguageTopicListRoute(languageId)) },
            )
        }
        composable<TopicCourseListByTypeRoute> {
            CourseListByTypeScreen(
                onCourseClick = { courseId -> navController.navigate(TopicListRoute(courseId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<TopicLanguageExamCoursesRoute> {
            LanguageExamCoursesScreen(
                onLanguageClick = { languageId -> navController.navigate(LanguageTopicListRoute(languageId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<LanguageTopicListRoute> {
            LanguageTopicListScreen(
                onTopicClick = { topicId -> navController.navigate(TopicDetailRoute(topicId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<CourseDetailRoute> { backStackEntry ->
            val courseId = backStackEntry.toRoute<CourseDetailRoute>().courseId
            CourseDetailScreen(
                onStudyResourcesClick = { navController.navigate(CourseStudyResourceRoute(courseId)) },
                onTopicsClick = { navController.navigate(TopicListRoute(courseId)) },
                onWorkHistoryClick = { navController.navigate(CourseWorkHistoryRoute(courseId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<CourseStudyResourceRoute> {
            CourseStudyResourceScreen(
                onResourceClick = { resourceId -> navController.navigate(StudyResourceDetailRoute(resourceId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<TopicListRoute> {
            TopicListScreen(
                onTopicClick = { topicId -> navController.navigate(TopicDetailRoute(topicId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<TopicDetailRoute> {
            TopicDetailScreen(
                onResourceClick = { resourceId -> navController.navigate(StudyResourceDetailRoute(resourceId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<LanguageDetailRoute> { backStackEntry ->
            val languageId = backStackEntry.toRoute<LanguageDetailRoute>().languageId
            LanguageDetailScreen(
                onStudyResourcesClick = { navController.navigate(LanguageStudyResourceRoute(languageId)) },
                onTopicsClick = { navController.navigate(LanguageTopicListRoute(languageId)) },
                onWorkHistoryClick = { navController.navigate(LanguageWorkHistoryRoute(languageId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<LanguageListRoute> {
            LanguageListScreen(
                shellNavigation = shellNavigation,
                onLanguageClick = { languageId -> navController.navigate(LanguageDetailRoute(languageId)) },
            )
        }
        composable<LanguageStudyResourceRoute> {
            LanguageStudyResourceScreen(
                onResourceClick = { resourceId -> navController.navigate(StudyResourceDetailRoute(resourceId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<StudyResourceDetailRoute> {
            StudyResourceDetailScreen(
                onBack = { navController.popBackStack() },
                onDuplicated = { newStudyResourceId -> navController.navigate(StudyResourceDetailRoute(newStudyResourceId)) },
                onOpenTopicProgress = { studyResourceTopicId -> navController.navigate(TopicProgressRoute(studyResourceTopicId)) },
                onOpenPracticeExamEntry = { entryId -> navController.navigate(PracticeExamEntryDetailRoute(entryId)) },
            )
        }
        composable<TopicProgressRoute> {
            TopicProgressScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable<PracticeExamEntryDetailRoute> {
            PracticeExamEntryDetailScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable<StatisticsRoute> {
            StatisticsScreen(
                onBookClick = { bookId -> navController.navigate(BookDetailRoute(bookId)) },
                shellNavigation = shellNavigation,
                onTopicClick = { topicId -> navController.navigate(TopicDetailRoute(topicId)) },
            )
        }
        composable<UserEditRoute> {
            UserEditScreen(
                shellNavigation = shellNavigation,
                onSaved = { navController.navigateToShellDestination(HomeRoute) },
            )
        }
        composable<ModulesRoute> { entry ->
            val onboarding = entry.toRoute<ModulesRoute>().onboarding
            com.ridvan.target.ui.modules.ModulesScreen(
                onboarding = onboarding,
                onDone = { if (onboarding) navController.navigate(HomeRoute) { popUpTo(0) } else navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(shellNavigation = shellNavigation)
        }
        composable<MyAccountRoute> {
            MyAccountScreen(shellNavigation = shellNavigation)
        }
        composable<HelpRoute> {
            HelpScreen(shellNavigation = shellNavigation)
        }
        composable<SwitchAccountRoute> {
            SwitchAccountScreen(
                onSwitched = { navController.navigate(HomeRoute) { popUpTo(0) } },
                onBack = { navController.popBackStack() },
            )
        }
        composable<PlannerRoute> {
            PlannerHomeScreen(
                shellNavigation = shellNavigation,
                onExamClick = { examId -> navController.navigate(ExamDetailRoute(examId)) },
                onSectionClick = { sectionId -> navController.navigate(SectionDetailRoute(sectionId)) },
                onCourseClick = { courseId -> navController.navigate(CourseDetailRoute(courseId)) },
                onTopicClick = { topicId -> navController.navigate(TopicDetailRoute(topicId)) },
                onFocusSessionClick = { sessionId -> navController.navigate(FocusSessionDetailRoute(sessionId)) },
                onPracticeSessionClick = { id -> navController.navigate(TopicProgressRoute(id)) },
                onTripClick = { tripId -> navController.navigate(TripDetailRoute(tripId)) },
                onRoutineClick = { routineId, dateMillis -> navController.navigate(RoutineDetailRoute(routineId, dateMillis)) },
            )
        }
        composable<FocusTimerRoute> {
            FocusTimerScreen(
                shellNavigation = shellNavigation,
                onManagePresets = { navController.navigate(FocusPresetListRoute) },
                onOpenHistory = { navController.navigate(FocusHistoryRoute) },
                onOpenSettings = { navController.navigate(FocusTimerSettingsRoute) },
                onSessionClick = { sessionId -> navController.navigate(FocusSessionDetailRoute(sessionId)) },
                onCourseClick = { courseId -> navController.navigate(CourseDetailRoute(courseId)) },
                onLanguageClick = { languageId -> navController.navigate(LanguageDetailRoute(languageId)) },
                onTopicClick = { topicId -> navController.navigate(TopicDetailRoute(topicId)) },
            )
        }
        composable<FlowTimerRoute> {
            FlowTimerScreen(
                shellNavigation = shellNavigation,
                onOpenHistory = { navController.navigate(FocusHistoryRoute) },
                onOpenSettings = { navController.navigate(FocusTimerSettingsRoute) },
                onSessionClick = { sessionId -> navController.navigate(FocusSessionDetailRoute(sessionId)) },
                onCourseClick = { courseId -> navController.navigate(CourseDetailRoute(courseId)) },
                onLanguageClick = { languageId -> navController.navigate(LanguageDetailRoute(languageId)) },
                onTopicClick = { topicId -> navController.navigate(TopicDetailRoute(topicId)) },
            )
        }
        composable<FocusPresetListRoute> {
            FocusPresetListScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable<FocusTimerSettingsRoute> {
            SettingsScreen(shellNavigation = shellNavigation, scrollToFocusTimer = true)
        }
        composable<FocusHistoryHomeRoute> {
            FocusHistoryHomeScreen(
                shellNavigation = shellNavigation,
                onSessionClick = { sessionId -> navController.navigate(FocusSessionDetailRoute(sessionId)) },
            )
        }
        composable<FocusHistoryRoute> {
            FocusHistoryScreen(
                onBack = { navController.popBackStack() },
                onSessionClick = { sessionId -> navController.navigate(FocusSessionDetailRoute(sessionId)) },
            )
        }
        composable<CourseWorkHistoryRoute> {
            FocusWorkHistoryScreen(
                onBack = { navController.popBackStack() },
                onSessionClick = { sessionId -> navController.navigate(FocusSessionDetailRoute(sessionId)) },
            )
        }
        composable<LanguageWorkHistoryRoute> {
            FocusWorkHistoryScreen(
                onBack = { navController.popBackStack() },
                onSessionClick = { sessionId -> navController.navigate(FocusSessionDetailRoute(sessionId)) },
            )
        }
        composable<FocusSessionDetailRoute> {
            FocusSessionDetailScreen(
                onBack = { navController.popBackStack() },
                onCourseClick = { courseId -> navController.navigate(CourseDetailRoute(courseId)) },
                onLanguageClick = { languageId -> navController.navigate(LanguageDetailRoute(languageId)) },
                onTopicClick = { topicId -> navController.navigate(TopicDetailRoute(topicId)) },
                onBookClick = { bookId -> navController.navigate(BookDetailRoute(bookId)) },
            )
        }
        composable<ReadingRoute> {
            ReadingScreen(
                shellNavigation = shellNavigation,
                onBookClick = { bookId -> navController.navigate(BookDetailRoute(bookId)) },
                onOpenLibrary = { navController.navigate(LibraryRoute) },
            )
        }
        composable<LibraryRoute> {
            LibraryScreen(
                onBack = { navController.popBackStack() },
                onBookClick = { bookId -> navController.navigate(BookDetailRoute(bookId)) },
            )
        }
        composable<BookDetailRoute> {
            BookDetailScreen(
                onBack = { navController.popBackStack() },
                onSessionClick = { sessionId -> navController.navigate(FocusSessionDetailRoute(sessionId)) },
            )
        }
    }
}
