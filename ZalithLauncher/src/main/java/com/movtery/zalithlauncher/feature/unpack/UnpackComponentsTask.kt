package com.movtery.zalithlauncher.feature.unpack

import android.content.Context
import android.content.res.AssetManager
import com.movtery.zalithlauncher.feature.log.Logging.i
import com.movtery.zalithlauncher.utils.path.PathManager
import net.kdt.pojavlaunch.Tools
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class UnpackComponentsTask(val context: Context, val component: Components) : AbstractUnpackTask() {
    private lateinit var am: AssetManager
    private lateinit var rootDir: String
    private lateinit var versionFile: File
    private lateinit var input: InputStream
    private var isCheckFailed: Boolean = false

    init {
        runCatching {
            am = context.assets
            rootDir = when (component) {
                // Launcher support jars are still consumed from DIR_DATA/components.
                Components.COMPONENTS -> PathManager.DIR_DATA

                // LWJGL packs are private and should live under context.filesDir.
                Components.LWJGL3, Components.LWJGL342 -> PathManager.DIR_FILE.absolutePath

                // Everything else keeps its existing location.
                else -> if (component.privateDirectory) {
                    PathManager.DIR_FILE.absolutePath
                } else {
                    PathManager.DIR_GAME_HOME
                }
            }

            versionFile = File("$rootDir/${component.component}/version")
            input = am.open("components/${component.component}/version")
        }.getOrElse {
            isCheckFailed = true
        }
    }

    fun isCheckFailed() = isCheckFailed

    override fun isNeedUnpack(): Boolean {
        if (isCheckFailed) return false

        if (!versionFile.exists()) {
            requestEmptyParentDir(versionFile)
            i("Unpack Components", "${component.component}: Pack was installed manually, or does not exist...")
            return true
        } else {
            val fis = FileInputStream(versionFile)
            val release1 = Tools.read(input)
            val release2 = Tools.read(fis)
            if (release1 != release2) {
                requestEmptyParentDir(versionFile)
                return true
            } else {
                i("UnpackPrep", "${component.component}: Pack is up-to-date with the launcher, continuing...")
                return false
            }
        }
    }

    override fun run() {
        listener?.onTaskStart()
        copyAssetDirectoryRecursively(
            "components/${component.component}",
            "$rootDir/${component.component}"
        )
        listener?.onTaskEnd()
    }

    private fun copyAssetDirectoryRecursively(assetPath: String, outputPath: String) {
        val entries = am.list(assetPath) ?: return

        if (entries.isEmpty()) {
            val outFile = File(outputPath)
            outFile.parentFile?.mkdirs()
            Tools.copyAssetFile(context, assetPath, outFile.parent ?: return, outFile.name, true)
            return
        }

        File(outputPath).mkdirs()
        for (entry in entries) {
            val childAssetPath = "$assetPath/$entry"
            val childOutputPath = "$outputPath/$entry"
            copyAssetDirectoryRecursively(childAssetPath, childOutputPath)
        }
    }

    private fun requestEmptyParentDir(file: File) {
        file.parentFile!!.apply {
            if (exists() && isDirectory) {
                FileUtils.deleteDirectory(this)
            }
            mkdirs()
        }
    }
}
