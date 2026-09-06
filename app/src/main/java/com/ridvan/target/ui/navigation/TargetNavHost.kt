package com.ridvan.target.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.ridvan.target.TargetApplication
import com.ridvan.target.ui.auth.LoginScreen
import com.ridvan.target.ui.auth.RegisterScreen
import com.ridvan.target.ui.courselist.CourseListByTypeScreen
import com.ridvan.target.ui.courselist.CourseListScreen
import com.ridvan.target.ui.coursedetail.CourseDetailScreen
import com.ridvan.target.ui.examdetail.ExamDetailScreen
import com.ridvan.target.ui.examlist.ExamListScreen
import com.ridvan.target.ui.home.HomeScreen
import com.ridvan.target.ui.languagedetail.LanguageDetailScreen
import com.ridvan.target.ui.languagelist.LanguageExamCoursesScreen
import com.ridvan.target.ui.languagelist.LanguageListScreen
import com.ridvan.target.ui.sectiondetail.SectionDetailScreen
import com.ridvan.target.ui.settings.SettingsScreen
import com.ridvan.target.ui.shell.ShellNavigation
import com.ridvan.target.ui.studyresource.CourseStudyResourceScreen
import com.ridvan.target.ui.studyresource.LanguageStudyResourceScreen
import com.ridvan.target.ui.studyresource.StudyResourceHomeScreen
import com.ridvan.target.ui.studyresourcedetail.StudyResourceDetailScreen
import com.ridvan.target.ui.switchaccount.SwitchAccountScreen
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

    val shellNavigation = ShellNavigation(
        onNavigateHome = { navController.navigateToShellDestination(HomeRoute) },
        onNavigateExams = { navController.navigateToShellDestination(ExamListRoute) },
        onNavigateCourses = { navController.navigateToShellDestination(CourseListRoute) },
        onNavigateStudyResources = { navController.navigateToShellDestination(StudyResourceHomeRoute) },
        onNavigateLanguages = { navController.navigateToShellDestination(LanguageListRoute) },
        onNavigateUser = { navController.navigateToShellDestination(UserEditRoute) },
        onNavigateSettings = { navController.navigateToShellDestination(SettingsRoute) },
        onLogOut = signOut,
        onSwitchAccount = { navController.navigate(SwitchAccountRoute) },
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
                onRegisterSuccess = { navController.navigate(HomeRoute) { popUpTo(0) } },
                onNavigateToLogin = { navController.navigate(LoginRoute) },
            )
        }
        composable<HomeRoute> {
            HomeScreen(shellNavigation = shellNavigation)
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
            )
        }
        composable<StudyResourceHomeRoute> {
            StudyResourceHomeScreen(
                shellNavigation = shellNavigation,
                onCourseTypeClick = { examType -> navController.navigate(StudyResourceCourseListByTypeRoute(examType.id)) },
                onLanguageTypeClick = { navController.navigate(StudyResourceLanguageExamCoursesRoute) },
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
            )
        }
        composable<StudyResourceLanguageExamCoursesRoute> {
            LanguageExamCoursesScreen(
                onLanguageClick = { languageId -> navController.navigate(LanguageStudyResourceRoute(languageId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<CourseDetailRoute> { backStackEntry ->
            val courseId = backStackEntry.toRoute<CourseDetailRoute>().courseId
            CourseDetailScreen(
                onStudyResourcesClick = { navController.navigate(CourseStudyResourceRoute(courseId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<CourseStudyResourceRoute> {
            CourseStudyResourceScreen(
                onResourceClick = { resourceId -> navController.navigate(StudyResourceDetailRoute(resourceId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<LanguageDetailRoute> { backStackEntry ->
            val languageId = backStackEntry.toRoute<LanguageDetailRoute>().languageId
            LanguageDetailScreen(
                onStudyResourcesClick = { navController.navigate(LanguageStudyResourceRoute(languageId)) },
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
            StudyResourceDetailScreen(onBack = { navController.popBackStack() })
        }
        composable<UserEditRoute> {
            UserEditScreen(
                shellNavigation = shellNavigation,
                onSaved = { navController.navigateToShellDestination(HomeRoute) },
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(shellNavigation = shellNavigation)
        }
        composable<SwitchAccountRoute> {
            SwitchAccountScreen(
                onSwitched = { navController.navigate(HomeRoute) { popUpTo(0) } },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
