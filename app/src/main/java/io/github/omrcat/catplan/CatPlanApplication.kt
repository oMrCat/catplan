package io.github.omrcat.catplan

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import io.github.omrcat.catplan.worker.ReminderWorker
import java.util.concurrent.TimeUnit

/**
 * 应用入口：创建通知渠道 + 注册周期性的护理提醒巡检。
 * WorkManager 自带持久化，设备重启后任务会自动恢复，无需额外的开机广播。
 */
class CatPlanApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        scheduleReminderCheck()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID_CARE,
            getString(R.string.channel_care_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.channel_care_desc)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** 每 12 小时巡检一次到期任务；与纯本地策略一致，不依赖任何推送服务 */
    private fun scheduleReminderCheck() {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(12, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            REMINDER_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    companion object {
        const val CHANNEL_ID_CARE = "care_reminders"
        const val REMINDER_WORK_NAME = "care-reminder-check"
    }
}
