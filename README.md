# 猫计划 CatPlan 🐱

[![CI](https://github.com/oMrCat/catplan/actions/workflows/ci.yml/badge.svg)](https://github.com/oMrCat/catplan/actions/workflows/ci.yml)

> 多猫家庭的一站式猫咪健康管家 —— 让每一次疫苗、驱虫、体检都不错过。

**开源 · 免费 · 无广告 · 无追踪**。所有数据只存在你的手机里（本地优先），应用不申请网络权限，不上传任何数据，也没有任何变现设计 —— 这是一份纯粹的猫奴公益项目。

## 功能

- 🐈 **猫咪档案**：多猫管理（品种 / 生日 / 体重 / 绝育情况）
- ⏰ **护理计划**：按猫龄自动生成疫苗、驱虫、体检计划，并本地推送提醒
- ⚖️ **体重记录**：曲线趋势，异常早发现
- 💰 **多猫记账**：本月总支出、类目占比、按猫分摊（v0.2）
- 👨‍👩‍👧 家庭共享（WebDAV）· 📖 成长相册 —— 计划于 v0.2 后续版本

## 路线图

| 版本 | 内容 |
| --- | --- |
| v0.1 | 猫咪档案 + 护理计划与提醒 + 体重记录（手机优先） |
| v0.2 | 多猫记账 ✅（已完成）+ 数据导出导入 + 成长相册 + 家庭共享（WebDAV 同步） |
| v1.0 | GitHub Release + F-Droid 上架 + 家庭共享（WebDAV 同步） |

## 技术栈

- Kotlin + Jetpack Compose，Material 3 自适应布局：手机用底部导航，平板/横屏自动切到 Navigation Rail
- Room 本地数据库 · WorkManager 本地提醒 · 不依赖任何 Google 服务（为 F-Droid 上架做准备）
- 无自有服务器：家庭共享通过用户自备的 WebDAV（坚果云 / Nextcloud / 群晖）同步

## 构建运行

1. 用 Android Studio（Ladybug 或更新版本）打开本项目；首次同步时若提示缺少 Gradle Wrapper，同意自动生成即可（本地装有 Gradle 的话也可以执行 `gradle wrapper --gradle-version 8.7`）
2. 选择 `app` 配置，Run ▶️ 安装到设备或模拟器（Android 8.0 / API 26 及以上）

> 发布前请把 `app/build.gradle.kts` 里的 `applicationId` 改成你自己的反写域名，例如 `io.github.<你的GitHub用户名>.catplan`，并同步重构包名。

## 项目结构

```
app/src/main/java/io/github/catplan/app/
├── CatPlanApplication.kt     # 通知渠道 + 周期提醒调度
├── MainActivity.kt           # 入口，计算窗口尺寸并选择导航形态
├── ui/
│   ├── CatPlanApp.kt         # 手机底部导航 / 平板 Navigation Rail
│   ├── screens/              # 今日 · 日历 · 档案 三个页面
│   └── theme/                # 品牌暖橘配色，跟随系统深浅色
├── data/local/               # Room：猫咪 / 体重 / 护理任务 三张表
├── domain/
│   └── CareScheduleGenerator.kt  # 核心：按猫龄自动生成护理计划
└── worker/
    └── ReminderWorker.kt     # 每 12 小时巡检到期任务并本地通知
```

## 已知问题与排查

- **MIUI / HyperOS 上 `connectedDebugAndroidTest` 卡住不动**：小米系统默认禁止应用「后台弹出界面」，instrumented test 无法把被测 Activity 拉到前台。解决：开发者选项中开启「USB 调试（安全设置）」（需登录小米账号并插入 SIM 卡），或换非 MIUI 设备运行组件测试。
- **MIUI 上 adb 安装报 `INSTALL_FAILED_USER_RESTRICTED`**：安装时请保持设备亮屏解锁；若仍失败，检查「USB 安装」开关与小米账号登录状态。
- **组件测试显示 "Starting 0 tests"**：请确认 `build.gradle.kts` 配置了 `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"`。

## 参与贡献

Issue 和 PR 都欢迎！提交 PR 前请确认 `./gradlew lint` 通过。如果你也是猫奴，欢迎来认领一个 good first issue 🐾

## 开源协议

[Apache-2.0](LICENSE)
