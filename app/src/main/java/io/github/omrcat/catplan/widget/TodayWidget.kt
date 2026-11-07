package io.github.omrcat.catplan.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.action.Action
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.omrcat.catplan.MainActivity
import io.github.omrcat.catplan.R
import io.github.omrcat.catplan.data.local.CareTaskEntity
import io.github.omrcat.catplan.data.local.CatPlanDatabase
import io.github.omrcat.catplan.data.local.DailyEntity
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * 桌面小组件：今日待办数量 + 喂食/铲屎打卡状态。
 * 打卡按钮直接写库（无需打开 App），点卡片进今日页。
 */
class TodayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = CatPlanDatabase.get(context)
        val today = LocalDate.now().toEpochDay()
        val tasks = db.careTaskDao()
            .observeDueWithin(today + 7)
            .first()
            .filter { it.type != CareTaskEntity.TYPE_BIRTHDAY }
            .sortedBy { it.dueDateEpochDay }
        val dailies = db.dailyDao().observeSince(today).first()
        val fed = dailies.any { it.type == DailyEntity.TYPE_FEED }
        val litter = dailies.any { it.type == DailyEntity.TYPE_LITTER }

        provideContent {
            GlanceTheme {
                Content(
                    taskCount = tasks.size,
                    nextTask = tasks.firstOrNull()?.title ?: context.getString(R.string.widget_no_tasks),
                    fed = fed,
                    litter = litter,
                )
            }
        }
    }

    @Composable
    private fun Content(taskCount: Int, nextTask: String, fed: Boolean, litter: Boolean) {
        GlanceTheme {
            Column(
                modifier = GlanceModifier.fillMaxSize().padding(12.dp)
                    .clickable(actionStartActivity<MainActivity>()),
            ) {
                Text(
                    text = "🐱 猫计划 · 今日",
                    style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurface),
                )
                Spacer(modifier = GlanceModifier.padding(2.dp))
                Text(
                    text = if (taskCount == 0) nextTask else "$taskCount 件待办 · $nextTask",
                    style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant),
                    modifier = GlanceModifier.padding(bottom = 8.dp),
                )
                Row(modifier = GlanceModifier.fillMaxSize()) {
                    PunchChip(
                        label = if (fed) "🍚 已喂" else "🍚 喂食",
                        done = fed,
                        action = actionRunCallback<FeedAction>(),
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    Spacer(modifier = GlanceModifier.padding(4.dp))
                    PunchChip(
                        label = if (litter) "🧹 已铲" else "🧹 铲屎",
                        done = litter,
                        action = actionRunCallback<LitterAction>(),
                        modifier = GlanceModifier.defaultWeight(),
                    )
                }
            }
        }
    }

    @Composable
    private fun PunchChip(
        label: String,
        done: Boolean,
        action: Action,
        modifier: GlanceModifier,
    ) {
        Box(
            modifier = modifier
                .background(
                    if (done) GlanceTheme.colors.secondaryContainer
                    else GlanceTheme.colors.primaryContainer,
                )
                .padding(8.dp)
                .clickable(action),
        ) {
            Text(
                text = label,
                style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onPrimaryContainer),
            )
        }
    }
}

/** 喂食打卡（小组件按钮） */
class FeedAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        punch(context, DailyEntity.TYPE_FEED, glanceId)
    }
}

/** 铲屎打卡（小组件按钮） */
class LitterAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        punch(context, DailyEntity.TYPE_LITTER, glanceId)
    }
}

private suspend fun punch(context: Context, type: String, glanceId: GlanceId) {
    CatPlanDatabase.get(context).dailyDao().insert(
        DailyEntity(type = type, catId = null, dateEpochDay = LocalDate.now().toEpochDay()),
    )
    TodayWidget().update(context, glanceId)
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}
