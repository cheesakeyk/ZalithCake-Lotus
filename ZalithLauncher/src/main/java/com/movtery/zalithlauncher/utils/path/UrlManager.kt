package com.movtery.zalithlauncher.utils.path

import com.movtery.zalithlauncher.BuildConfig
import com.movtery.zalithlauncher.InfoDistributor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.util.concurrent.TimeUnit

class UrlManager {
    companion object {
        private const val URL_USER_AGENT: String =
            "${InfoDistributor.LAUNCHER_NAME}/${BuildConfig.VERSION_NAME}"

        @JvmField
        val TIME_OUT = Pair(8000, TimeUnit.MILLISECONDS)

        // Old contents API, keep this only if notice/sponsor still use it
        const val URL_GITHUB_HOME: String =
            "https://api.github.com/repos/ZalithLauncher/Zalith-Info/contents/"

        // Your actual project page used by About screen
        const val URL_HOME: String =
            "https://github.com/cheesakeyk/ZalithCake-Lotus"

        // Your GitHub Releases API used by the updater
        const val URL_GITHUB_RELEASES: String =
            "https://api.github.com/repos/cheesakeyk/ZalithCake-Lotus/releases"

        const val URL_MCMOD: String = "https://www.mcmod.cn/"
        const val URL_MINECRAFT: String = "https://www.minecraft.net/"
        const val URL_MINECRAFT_VERSION_REPOS: String =
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
        const val URL_SUPPORT: String = "https://afdian.com/a/MovTery"
        const val URL_FCL_RENDERER_PLUGIN: String =
            "https://github.com/ShirosakiMio/FCLRendererPlugin/releases/tag/Renderer"
        const val URL_FCL_DRIVER_PLUGIN: String =
            "https://github.com/FCL-Team/FCLDriverPlugin/releases/tag/Turnip"

        @JvmStatic
        fun createConnection(url: URL): URLConnection {
            val connection = url.openConnection()
            connection.setRequestProperty("User-Agent", URL_USER_AGENT)
            connection.setConnectTimeout(TIME_OUT.first)
            connection.setReadTimeout(TIME_OUT.first)
            return connection
        }

        @JvmStatic
        @Throws(IOException::class)
        fun createHttpConnection(url: URL): HttpURLConnection {
            return createConnection(url) as HttpURLConnection
        }

        @JvmStatic
        fun createRequestBuilder(url: String): Request.Builder {
            return createRequestBuilder(url, null)
        }

        @JvmStatic
        fun createRequestBuilder(url: String, body: RequestBody?): Request.Builder {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", URL_USER_AGENT)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")

            body?.let { request.post(it) }
            return request
        }

        @JvmStatic
        fun createOkHttpClient(): OkHttpClient = createOkHttpClientBuilder().build()

        @JvmStatic
        fun createOkHttpClientBuilder(action: (OkHttpClient.Builder) -> Unit = { }): OkHttpClient.Builder {
            return OkHttpClient.Builder()
                .callTimeout(TIME_OUT.first.toLong(), TIME_OUT.second)
                .apply(action)
        }
    }
}
