package io.github.omrcat.catplan.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.omrcat.catplan.CatPlanApplication
import io.github.omrcat.catplan.MainActivity
import io.github.omrcat.catplan.R
import io.github.omrcat.catplan.data.local.CatPlanDatabase
import io.github.omrcat.catplan.domain.CatBirthday
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
        val today = LocalDate.now()
        val until = today.plusDays(3).toEpochDay()
        val dueTasks = db.careTaskDao().getDueWithin(until)
        val cats = db.catDao().getAll()

        // 生日提醒：3 天内的下一个生日
        val birthdayLines = cats.mapNotNull { cat ->
            val days = CatBirthday.daysUntil(LocalDate.ofEpochDay(cat.birthdayEpochDay), today)
            when {
                days == 0 -> "🎂 ${cat.name} 的生日就是今天！"
                days in 1..3 -> "🎂 ${cat.name} 的生日还有 $days 天"
                else -> null
            }
        }
        if (dueTasks.isEmpty() && birthdayLines.isEmpty()) return Result.success()

        val taskLines = dueTasks
            .groupBy { it.catId }
            .map { (catId, tasks) ->
                val name = cats.find { it.id == catId }?.name ?: "猫咪"
                "$name：" + tasks.joinToString("、") { it.title }
            }
        val lines = (taskLines + birthdayLines).joinToString("\n")

        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_tab", "today")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = if (dueTasks.isEmpty()) {
            applicationContext.getString(R.string.reminder_birthday_title)
        } else {
            applicationContext.getString(R.string.reminder_title, dueTasks.size)
        }
        val notification = NotificationCompat.Builder(applicationContext, CatPlanApplication.CHANNEL_ID_CARE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
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
