package io.github.catplan.app.data

import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

/**
 * 极简 WebDAV 客户端（v0.2 基础版）：只做 PUT 上传 / GET 下载。
 * 用户填「完整文件 URL」（坚果云/Nextcloud/群晖的 WebDAV 地址），Basic 认证。
 */
object WebDavClient {

    fun upload(url: String, username: String?, password: String?, content: String): Result<Unit> = runCatching {
        val conn = open(url, username, password).apply {
            requestMethod = "PUT"
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        conn.outputStream.use { it.write(content.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        conn.disconnect()
        check(code in 200..299) { "服务器返回 HTTP $code" }
    }

    fun download(url: String, username: String?, password: String?): Result<String> = runCatching {
        val conn = open(url, username, password).apply { requestMethod = "GET" }
        val body = conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
        val code = conn.responseCode
        conn.disconnect()
        check(code in 200..299) { "服务器返回 HTTP $code" }
        body
    }

    private fun open(url: String, username: String?, password: String?): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        if (!username.isNullOrBlank()) {
            val raw = "$username:${password ?: ""}"
            conn.setRequestProperty(
                "Authorization",
                "Basic " + Base64.getEncoder().encodeToString(raw.toByteArray(Charsets.UTF_8)),
            )
        }
        return conn
    }
}
