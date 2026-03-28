package com.movtery.zalithlauncher.feature.download.install

import com.kdt.mcgui.ProgressLayout
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.event.value.DownloadProgressKeyEvent
import com.movtery.zalithlauncher.feature.download.item.ModLoaderWrapper
import com.movtery.zalithlauncher.feature.download.item.VersionItem
import com.movtery.zalithlauncher.feature.log.Logging
import com.movtery.zalithlauncher.feature.version.VersionsManager
import com.movtery.zalithlauncher.task.Task
import com.movtery.zalithlauncher.utils.path.PathManager
import net.kdt.pojavlaunch.progresskeeper.DownloaderProgressWrapper
import net.kdt.pojavlaunch.utils.DownloadUtils
import org.apache.commons.io.FileUtils
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.io.IOException

class InstallHelper {
    companion object {
        @Throws(Throwable::class)
        fun downloadFile(version: VersionItem, targetFile: File, progressKey: String) {
            downloadFile(version, targetFile, progressKey, null)
        }

        @Throws(Throwable::class)
        fun downloadFile(
            version: VersionItem,
            targetFile: File,
            progressKey: String,
            listener: OnFileDownloadedListener?
        ) {
            Task.runTask {
                EventBus.getDefault().post(DownloadProgressKeyEvent(progressKey, true))
                try {
                    val downloadBuffer = ByteArray(8192)
                    DownloadUtils.ensureSha1<Void?>(targetFile, version.fileHash) {
                        Logging.i("InstallHelper", "Download Url: ${version.fileUrl}")
                        DownloadUtils.downloadFileMonitored(
                            version.fileUrl,
                            targetFile,
                            downloadBuffer,
                            DownloaderProgressWrapper(
                                R.string.download_install_download_file,
                                progressKey
                            )
                        )
                        null
                    }
                } finally {
                    listener?.onEnded(targetFile)
                    ProgressLayout.clearProgress(progressKey)
                    EventBus.getDefault().post(DownloadProgressKeyEvent(progressKey, false))
                }
            }.execute()
        }

        @Throws(IOException::class)
        fun installModPack(
            version: VersionItem,
            customName: String,
            installFunction: ModPackInstallFunction
        ): ModLoaderWrapper? {
            val modpackFile = File(
                PathManager.DIR_CACHE,
                "$customName.cf"
            ) // Temporary cached modpack file

            val targetVersionPath = VersionsManager.getVersionPath(customName)
            val modLoaderInfo: ModLoaderWrapper?

            try {
                val downloadBuffer = ByteArray(8192)
                DownloadUtils.ensureSha1<Void?>(
                    modpackFile,
                    version.fileHash
                ) {
                    Logging.i("InstallHelper", "Download Url: ${version.fileUrl}")
                    DownloadUtils.downloadFileMonitored(
                        version.fileUrl,
                        modpackFile,
                        downloadBuffer,
                        DownloaderProgressWrapper(
                            R.string.modpack_download_downloading_metadata,
                            ProgressLayout.INSTALL_RESOURCE
                        )
                    )
                    null
                }

                // Install the modpack into the selected version directory.
                modLoaderInfo = installFunction.install(modpackFile, targetVersionPath)

                // Make the installed modpack the current version and refresh the in-memory
                // version list immediately so the UI updates without needing a manual refresh.
                VersionsManager.saveCurrentVersion(customName)
                VersionsManager.refresh("InstallHelper:installModPack", true)
            } finally {
                FileUtils.deleteQuietly(modpackFile)
                ProgressLayout.clearProgress(ProgressLayout.INSTALL_RESOURCE)
            }

            if (modLoaderInfo == null) {
                Logging.i(
                    "InstallHelper",
                    "Modpack installed with no additional ModLoader step required for version $customName"
                )
                return null
            }

            Logging.i("InstallHelper", "ModLoader is ${modLoaderInfo.nameById}")
            return modLoaderInfo
        }
    }
}
