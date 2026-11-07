# 参与贡献 CatPlan 🐱

感谢你愿意帮这只「开源猫」梳毛！请先读完这份指南。

## 环境

- Android Studio Ladybug+ / JDK 17（Temurin）
- 本地 JDK 17 + Android SDK（`sdkmanager` 可自动补齐 platform/build-tools）
- 无需任何账号或密钥：无 `keystore.properties` 时 release 构建自动退回 debug 签名

## 分支与提交

- 从 `main` 拉功能分支：`feat/xxx`、`fix/xxx`、`test/xxx`
- 提交信息用约定式前缀：`feat:` / `fix:` / `test:` / `docs:` / `refactor:` / `chore:`
- 单个 commit 只做一件事

## 测试要求（发版纪律）

**任何功能 commit 必须附带对应的测试，且以下命令全绿：**

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

- 排期/统计/日期等纯逻辑 → `src/test/`（JUnit，可加 Robolectric）
- 通知、DB 链路、导航 → `src/androidTest/`（`work-testing` / Compose UI Test）
- **Release 纪律**：打 tag 前必须完成该 commit 的全功能真机 E2E（参考 v0.2.0 的教训）

## 数据安全红线

- 备份/恢复（`BackupCodec`）涉及用户数据安全：改动必须带往返测试
- 可空字段必须带 `isNull` 守卫（org.json 的 `optString` 会把 JSON null 变成 `"null"` 字符串）
- 时间戳字段（`createdAtEpochMs`）必须参与备份，否则往返不等

## 隐私红线

- 不引入任何统计/广告/追踪 SDK
- 不新增网络用途（当前唯一联网行为 = 用户主动触发的 WebDAV 同步）
- 新表字段要进备份编解码（`BackupCodec`），并同步更新 schema

## 提交 PR

1. 确认 CI 全绿（构建 + 单测 + lint + release 构建）
2. PR 描述写清楚：做了什么、为什么、怎么验证的
3. 截图/录屏欢迎（UI 变更必附）

## 联系

Issue 里 @oMrCat，或者直接在 Issue 里贴猫照贿赂维护者 🐾
