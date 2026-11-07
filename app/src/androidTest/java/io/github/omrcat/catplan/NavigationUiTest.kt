package io.github.omrcat.catplan

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 组件测试：在真机/模拟器上运行 Compose UI 自动化，验证导航切换。
 * 断言文案一律取自字符串资源（随系统语言变化）。
 * 底部导航用 testTag 定位（页面标题可能与 tab 文本同名，文本匹配会撞多节点）。
 */
@RunWith(AndroidJUnit4::class)
class NavigationUiTest {

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun str(resId: Int): String = composeRule.activity.getString(resId)

    private fun tapTab(route: String) {
        composeRule.onNodeWithTag("tab_$route").performClick()
    }

    @Test
    fun bottomNav_switchesScreens() {
        composeRule.onNodeWithText(str(R.string.today_section_tasks)).assertIsDisplayed()
        tapTab("calendar")
        composeRule.onNodeWithText(str(R.string.calendar_title)).assertIsDisplayed()
        tapTab("cats")
        composeRule.onNodeWithText(str(R.string.cats_add)).assertIsDisplayed()
        tapTab("today")
        composeRule.onNodeWithText(str(R.string.today_section_tasks)).assertIsDisplayed()
    }

    @Test
    fun todayScreen_showsQuickActions() {
        // 多条任务可能把打卡区挤出首屏；断言节点存在（可见性由人工 E2E 截图验证）
        composeRule.onNodeWithText(str(R.string.daily_weight)).assertExists()
        composeRule.onNodeWithText(str(R.string.today_section_daily)).assertExists()
        composeRule.onNodeWithText(str(R.string.daily_diary)).assertExists()
    }

    @Test
    fun ledgerTab_showsSummary() {
        tapTab("ledger")
        composeRule.onNodeWithText(str(R.string.ledger_total)).assertIsDisplayed()
    }

    @Test
    fun settingsTab_showsBackupSection() {
        tapTab("settings")
        composeRule.onNodeWithText(str(R.string.settings_backup_section)).assertIsDisplayed()
    }
}
