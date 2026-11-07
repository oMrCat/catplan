package io.github.omrcat.catplan

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import io.github.omrcat.catplan.ui.CatPlanApp
import io.github.omrcat.catplan.ui.theme.CatPlanTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* 拒绝也不影响使用，只是收不到提醒 */ }

    /** 每个进程只请求一次，避免每次回到前台都打扰用户 */
    private var askedNotificationPermission = false

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 通知点击直达：ReminderWorker 传入 open_tab
        val startTab = intent?.getStringExtra("open_tab") ?: "today"
        setContent {
            CatPlanTheme {
                val windowSizeClass = calculateWindowSizeClass(this)
                CatPlanApp(widthSizeClass = windowSizeClass.widthSizeClass, initialTab = startTab)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 必须等首帧渲染完成后再请求权限；在 onCreate 里提前发起会与
        // Activity 的生命周期竞争，触发 ANR（模拟器实测踩坑）
        if (!askedNotificationPermission) {
            askedNotificationPermission = true
            requestNotificationPermissionIfNeeded()
        }
    }

    /** Android 13+ 通知权限需要在运行时请求 */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
