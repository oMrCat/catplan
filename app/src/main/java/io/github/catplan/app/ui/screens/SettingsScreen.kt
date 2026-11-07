package io.github.catplan.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.catplan.app.AppViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 设置（v0.2）：JSON 备份/恢复、WebDAV 家庭同步（基础版）、关于。
 */
@Composable
fun SettingsScreen(vm: AppViewModel = viewModel(), modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrDefault("?")
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri?.let {
            vm.exportTo(context, it) { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            vm.restoreFrom(context, it) { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
        }
    }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("设置", style = MaterialTheme.typography.headlineSmall)

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("备份与恢复", style = MaterialTheme.typography.titleMedium)
                Text(
                    "全部数据导出为一个 JSON 文件；恢复时按「名字 + 生日」合并猫咪，其余记录追加。" +
                        "相册照片不参与备份。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val name = "catplan-backup-" +
                                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".json"
                            exportLauncher.launch(name)
                        },
                    ) { Text("导出备份") }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("*/*")) },
                    ) { Text("导入恢复") }
                }
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("WebDAV 同步（实验性）", style = MaterialTheme.typography.titleMedium)
                Text(
                    "填 WebDAV 的完整文件地址（坚果云 / Nextcloud / 群晖都支持），家人用同一个地址即可共享猫咪数据。" +
                        "账号密码只保存在本机，上传下载仅在点击时发生。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = vm.webDavUrl,
                    onValueChange = { vm.webDavUrl = it },
                    label = { Text("文件 URL（https://…/catplan-backup.json）") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = vm.webDavUsername,
                    onValueChange = { vm.webDavUsername = it },
                    label = { Text("账号") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = vm.webDavPassword,
                    onValueChange = { vm.webDavPassword = it },
                    label = { Text("密码 / 应用密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.uploadToWebDav(::toast) }) { Text("上传备份") }
                    OutlinedButton(onClick = { vm.restoreFromWebDav(::toast) }) { Text("下载恢复") }
                }
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("关于", style = MaterialTheme.typography.titleMedium)
                Text("猫计划 CatPlan v$versionName · Apache-2.0", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "开源地址：github.com/oMrCat/catplan",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { uriHandler.openUri("https://github.com/oMrCat/catplan") },
                )
                Text(
                    "数据全部保存在本机；唯一的联网行为是你主动触发的 WebDAV 同步。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
