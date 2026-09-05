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
import com.ridvan.target.ui.studysource.CourseStudySourceScreen
import com.ridvan.target.ui.studysource.LanguageStudySourceScreen
import com.ridvan.target.ui.studysource.StudySourceHomeScreen
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

    val shellNavigation = ShellNavigation(
        onNavigateHome = { navController.navigateToShellDestination(HomeRoute) },
        onNavigateExams = { navController.navigateToShellDestination(ExamListRoute) },
        onNavigateCourses = { navController.navigateToShellDestination(CourseListRoute) },
        onNavigateStudySources = { navController.navigateToShellDestination(StudySourceHomeRoute) },
        onNavigateLanguages = { navController.navigateToShellDestination(LanguageListRoute) },
        onNavigateUser = { navController.navigateToShellDestination(UserEditRoute) },
        onNavigateSettings = { navController.navigateToShellDestination(SettingsRoute) },
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
                onBack = { navController.popBackStack() },
            )
        }
        composable<SectionDetailRoute> {
            SectionDetailScreen(onBack = { navController.popBackStack() })
        }
        composable<CourseListRoute> {
            CourseListScreen(
                shellNavigation = shellNavigation,
                onCourseTypeClick = { examType -> navController.navigate(CourseListByTypeRoute(examType.id)) },
                onLanguageTypeClick = { navController.navigate(LanguageExamCoursesRoute) },
            )
        }
        composable<StudySourceHomeRoute> {
            StudySourceHomeScreen(
                shellNavigation = shellNavigation,
                onCourseTypeClick = { examType -> navController.navigate(CourseListByTypeRoute(examType.id)) },
                onLanguageTypeClick = { navController.navigate(LanguageExamCoursesRoute) },
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
        composable<CourseDetailRoute> { backStackEntry ->
            val courseId = backStackEntry.toRoute<CourseDetailRoute>().courseId
            CourseDetailScreen(
                onStudySourcesClick = { navController.navigate(CourseStudySourceRoute(courseId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<CourseStudySourceRoute> {
            CourseStudySourceScreen(onBack = { navController.popBackStack() })
        }
        composable<LanguageDetailRoute> { backStackEntry ->
            val languageId = backStackEntry.toRoute<LanguageDetailRoute>().languageId
            LanguageDetailScreen(
                onStudySourcesClick = { navController.navigate(LanguageStudySourceRoute(languageId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<LanguageListRoute> {
            LanguageListScreen(
                shellNavigation = shellNavigation,
                onLanguageClick = { languageId -> navController.navigate(LanguageDetailRoute(languageId)) },
            )
        }
        composable<LanguageStudySourceRoute> {
            LanguageStudySourceScreen(onBack = { navController.popBackStack() })
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
    }
}
