package io.github.omrcat.catplan

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
 * 组件测试：在真机/模拟器上运行 Compose UI 自动化，验证导航切换。
 * 断言文案一律取自字符串资源（随系统语言变化）。
 * 手机上走底部导航栏，平板/横屏走 Navigation Rail，本测试对两种形态都成立。
 */
@RunWith(AndroidJUnit4::class)
class NavigationUiTest {

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun str(resId: Int): String = composeRule.activity.getString(resId)

    @Test
    fun bottomNav_switchesScreens() {
        composeRule.onNodeWithText(str(R.string.today_section_tasks)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.tab_calendar)).performClick()
        composeRule.onNodeWithText(str(R.string.calendar_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.tab_cats)).performClick()
        composeRule.onNodeWithText(str(R.string.cats_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.tab_today)).performClick()
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
        composeRule.onNodeWithText(str(R.string.tab_ledger)).performClick()
        composeRule.onNodeWithText(str(R.string.ledger_total)).assertIsDisplayed()
    }

    @Test
    fun settingsTab_showsBackupSection() {
        composeRule.onNodeWithText(str(R.string.tab_settings)).performClick()
        composeRule.onNodeWithText(str(R.string.settings_backup_section)).assertIsDisplayed()
    }
}
