package io.github.catplan.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 组件测试：在真机/平板上运行 Compose UI 自动化，验证导航切换。
 * 手机上走底部导航栏，平板/横屏走 Navigation Rail，本测试对两种形态都成立。
 *
 * 运行：gradlew connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class NavigationUiTest {

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomNav_switchesScreens() {
        composeRule.onNodeWithText("今日待办").assertIsDisplayed()
        composeRule.onNodeWithText("日历").performClick()
        composeRule.onNodeWithText("健康日历").assertIsDisplayed()
        composeRule.onNodeWithText("档案").performClick()
        composeRule.onNodeWithText("猫咪档案").assertIsDisplayed()
        composeRule.onNodeWithText("今日").performClick()
        composeRule.onNodeWithText("今日待办").assertIsDisplayed()
    }

    @Test
    fun todayScreen_showsQuickActions() {
        composeRule.onNodeWithText("记体重").assertIsDisplayed()
        composeRule.onNodeWithText("记喂食").assertIsDisplayed()
        composeRule.onNodeWithText("记用药").assertIsDisplayed()
        composeRule.onNodeWithText("拍日记").assertIsDisplayed()
    }

    @Test
    fun ledgerTab_showsSummary() {
        composeRule.onNodeWithText("账本").performClick()
        composeRule.onNodeWithText("本月总支出").assertIsDisplayed()
    }

    @Test
    fun settingsTab_showsBackupSection() {
        composeRule.onNodeWithText("设置").performClick()
        composeRule.onNodeWithText("备份与恢复").assertIsDisplayed()
    }
}
