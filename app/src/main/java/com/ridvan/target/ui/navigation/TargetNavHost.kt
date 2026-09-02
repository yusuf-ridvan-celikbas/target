package com.ridvan.target.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ridvan.target.ui.examdetail.ExamDetailScreen
import com.ridvan.target.ui.examlist.ExamListScreen
import com.ridvan.target.ui.sectiondetail.SectionDetailScreen

@Composable
fun TargetNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ExamListRoute) {
        composable<ExamListRoute> {
            ExamListScreen(
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
    }
}
