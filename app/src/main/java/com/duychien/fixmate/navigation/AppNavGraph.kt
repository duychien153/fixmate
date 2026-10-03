package com.duychien.fixmate.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.duychien.fixmate.feature.settings.SettingsScreen
import com.duychien.fixmate.feature.taskdetail.TaskDetailScreen
import com.duychien.fixmate.feature.taskeditor.TaskEditorScreen
import com.duychien.fixmate.feature.tasklist.TaskListScreen

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Destination.TaskList,
        modifier = modifier,
    ) {
        composable<Destination.TaskList> {
            TaskListScreen(
                onTaskClick = { id -> navController.navigate(Destination.TaskDetail(id)) },
                onCreateTask = { navController.navigate(Destination.TaskEditor()) },
                onOpenSettings = { navController.navigate(Destination.Settings) },
            )
        }

        composable<Destination.TaskDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<Destination.TaskDetail>()
            TaskDetailScreen(
                taskId = route.taskId,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Destination.TaskEditor(id)) },
            )
        }

        composable<Destination.TaskEditor> { backStackEntry ->
            val route = backStackEntry.toRoute<Destination.TaskEditor>()
            TaskEditorScreen(
                taskId = route.taskId,
                onBack = { navController.popBackStack() },
            )
        }

        composable<Destination.Settings> {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
