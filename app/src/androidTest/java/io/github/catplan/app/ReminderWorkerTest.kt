package io.github.catplan.app

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import io.github.catplan.app.data.local.CareTaskEntity
import io.github.catplan.app.data.local.CatEntity
import io.github.catplan.app.data.local.CatPlanDatabase
import io.github.catplan.app.worker.ReminderWorker
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * 提醒 Worker 的端到端测试：真实 DB + 真实 NotificationManager。
 * 这是「不错过疫苗」这个核心卖点的自动化证据。
 */
@RunWith(AndroidJUnit4::class)
class ReminderWorkerTest {

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.POST_NOTIFICATIONS)

    private lateinit var context: Context
    private lateinit var db: CatPlanDatabase
    private var insertedCatId: Long = 0

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        runBlocking {
            NotificationManagerCompatHolder.from(context).cancelAll()
            db = CatPlanDatabase.get(context)
            insertedCatId = db.catDao().insert(
                CatEntity(name = "Worker测试猫", birthdayEpochDay = 19000),
            )
            db.careTaskDao().insert(
                CareTaskEntity(
                    catId = insertedCatId,
                    type = CareTaskEntity.TYPE_RABIES,
                    title = "狂犬疫苗 · 年度",
                    dueDateEpochDay = LocalDate.now().plusDays(1).toEpochDay(),
                    intervalDays = 365,
                ),
            )
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            NotificationManagerCompatHolder.from(context).cancelAll()
            db.catDao().getById(insertedCatId)?.let { db.catDao().delete(it) }
        }
    }

    @Test
    fun workerPostsNotificationForDueTask() {
        runBlocking {
            val worker = TestListenableWorkerBuilder.from(context, ReminderWorker::class.java).build()
            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            val active = NotificationManagerCompatHolder.from(context).activeNotifications
            assertTrue(
                "应存在护理提醒渠道的活跃通知",
                active.any { it.notification.channelId == CatPlanApplication.CHANNEL_ID_CARE },
            )
        }
    }
}

/** 小包装，避免直接依赖 NotificationManagerCompat 的静态初始化顺序 */
private object NotificationManagerCompatHolder {
    fun from(context: Context) = androidx.core.app.NotificationManagerCompat.from(context)
}
