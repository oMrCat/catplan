package io.github.catplan.app.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.catplan.app.CatPlanApplication
import io.github.catplan.app.R
import io.github.catplan.app.data.local.CatPlanDatabase
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * 护理提醒巡检：把未来 3 天内到期（或已逾期）的任务按猫合并成一条通知。
 * 由 CatPlanApplication 每 12 小时调度一次，完全本地运行。
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = CatPlanDatabase.get(applicationContext)
        val until = LocalDate.now().plusDays(3).toEpochDay()
        val dueTasks = db.careTaskDao().getDueWithin(until)
        if (dueTasks.isEmpty()) return Result.success()

        val cats = db.catDao().observeAll().first().associateBy { it.id }
        val lines = dueTasks
            .groupBy { it.catId }
            .map { (catId, tasks) ->
                val name = cats[catId]?.name ?: "猫咪"
                "$name：" + tasks.joinToString("、") { it.title }
            }
            .joinToString("\n")

        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(applicationContext, CatPlanApplication.CHANNEL_ID_CARE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(applicationContext.getString(R.string.reminder_title, dueTasks.size))
            .setContentText(lines)
            .setStyle(NotificationCompat.BigTextStyle().bigText(lines))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        val manager = NotificationManagerCompat.from(applicationContext)
        if (manager.areNotificationsEnabled()) {
            try {
                manager.notify(NOTIFICATION_ID, notification)
            } catch (_: SecurityException) {
                // 通知权限未授予时静默跳过；MainActivity 启动时会再请求一次
            }
        }
        return Result.success()
    }

    private companion object {
        const val NOTIFICATION_ID = 1001
    }
}
