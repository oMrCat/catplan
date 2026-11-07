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
        // 多条任务可能把打卡区挤出首屏；断言节点存在（可见性由人工 E2E 截图验证）
        composeRule.onNodeWithText("记体重").assertExists()
        // 喂食/铲屎 chip 文案随当天打卡状态变化（记喂食↔已喂 ✓），断言状态无关的区块与固定项
        composeRule.onNodeWithText("日常打卡").assertExists()
        composeRule.onNodeWithText("拍日记").assertExists()
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
