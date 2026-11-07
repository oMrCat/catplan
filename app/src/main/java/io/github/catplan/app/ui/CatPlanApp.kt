package io.github.catplan.app.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.catplan.app.R
import io.github.catplan.app.ui.screens.CalendarScreen
import io.github.catplan.app.ui.screens.CatDetailScreen
import io.github.catplan.app.ui.screens.CatsScreen
import io.github.catplan.app.ui.screens.LedgerScreen
import io.github.catplan.app.ui.screens.SettingsScreen
import io.github.catplan.app.ui.screens.TodayScreen

private data class TabItem(val route: String, val labelRes: Int, val icon: ImageVector)

private val TABS = listOf(
    TabItem(route = "today", labelRes = R.string.tab_today, icon = Icons.Filled.Home),
    TabItem(route = "calendar", labelRes = R.string.tab_calendar, icon = Icons.Filled.CalendarMonth),
    TabItem(route = "cats", labelRes = R.string.tab_cats, icon = Icons.Filled.Pets),
    TabItem(route = "ledger", labelRes = R.string.tab_ledger, icon = Icons.Filled.AccountBalanceWallet),
    TabItem(route = "settings", labelRes = R.string.tab_settings, icon = Icons.Filled.Settings),
)

/**
 * 应用骨架：手机用底部导航栏；平板 / 横屏自动切换为侧边 Navigation Rail。
 * v0.2 计划在宽屏下进一步提供「左侧猫列表 + 右侧详情」的双栏布局。
 */
@Composable
fun CatPlanApp(widthSizeClass: WindowWidthSizeClass, initialTab: String = "today") {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val isSelected: (TabItem) -> Boolean = { tab ->
        currentDestination?.hierarchy?.any { it.route == tab.route } == true
    }
    val onTabClick: (TabItem) -> Unit = { tab ->
        navController.navigate(tab.route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val navHost: @Composable (Modifier) -> Unit = { modifier ->
        NavHost(navController = navController, startDestination = initialTab, modifier = modifier) {
            composable("today") { TodayScreen() }
            composable("calendar") { CalendarScreen() }
            composable("cats") {
                CatsScreen(onOpenCat = { id -> navController.navigate("cat/$id") })
            }
            composable("ledger") { LedgerScreen() }
            composable("settings") { SettingsScreen() }
            composable(
                route = "cat/{catId}",
                arguments = listOf(navArgument("catId") { type = NavType.LongType }),
            ) { entry ->
                CatDetailScreen(catId = entry.arguments?.getLong("catId") ?: 0L)
            }
        }
    }

    if (widthSizeClass != WindowWidthSizeClass.Compact) {
        // 平板 / 横屏：Navigation Rail
        Scaffold { innerPadding ->
            Row(Modifier.fillMaxSize().padding(innerPadding)) {
                NavigationRail {
                    TABS.forEach { tab ->
                        NavigationRailItem(
                            selected = isSelected(tab),
                            onClick = { onTabClick(tab) },
                            icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
                navHost(Modifier.fillMaxSize())
            }
        }
    } else {
        // 手机：底部导航栏
        Scaffold(
            bottomBar = {
                NavigationBar {
                    TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = isSelected(tab),
                            onClick = { onTabClick(tab) },
                            icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            },
        ) { innerPadding ->
            navHost(Modifier.fillMaxSize().padding(innerPadding))
        }
    }
}
