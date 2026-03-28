package com.movtery.zalithlauncher.feature.version

import android.os.Parcel
import android.os.Parcelable
import com.movtery.zalithlauncher.feature.customprofilepath.ProfilePathHome
import com.movtery.zalithlauncher.feature.mod.parser.ModChecker
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.utils.ZHTools
import com.movtery.zalithlauncher.utils.path.PathManager
import net.kdt.pojavlaunch.Tools
import java.io.File

/**
 * Represents a Minecraft version, identified by its version name.
 *
 * @param versionsFolder The parent versions directory that contains this version.
 * @param versionPath The full path to this version folder.
 * @param versionConfig The per-version configuration.
 * @param isValid Whether this version is considered valid.
 */
class Version(
    private val versionsFolder: String,
    private val versionPath: String,
    private val versionConfig: VersionConfig,
    private val isValid: Boolean
) : Parcelable {

    /**
     * Controls whether the current account should be treated as an offline account when launching.
     */
    var offlineAccountLogin: Boolean = false

    /**
     * Result of the mod check, if available.
     */
    var modCheckResult: ModChecker.ModCheckResult? = null

    /**
     * @return The parent versions directory for this version.
     */
    fun getVersionsFolder(): String = versionsFolder

    /**
     * @return The version folder.
     */
    fun getVersionPath(): File = File(versionPath)

    /**
     * @return The version name.
     */
    fun getVersionName(): String = getVersionPath().name

    /**
     * @return The version configuration.
     */
    fun getVersionConfig(): VersionConfig = versionConfig

    /**
     * @return Whether this version is valid, meaning the version folder still exists
     * and it was recognized as a valid version when scanned.
     */
    fun isValid(): Boolean = isValid && getVersionPath().exists()

    /**
     * @return Whether version isolation is enabled.
     */
    fun isIsolation(): Boolean = versionConfig.isIsolation()

    /**
     * @return The game directory for this version.
     *
     * If version isolation is enabled, the version folder is used.
     * If isolation is disabled and a custom path is set, that path is used.
     * Otherwise, the default game directory is returned.
     */
    fun getGameDir(): File {
        return when {
            versionConfig.isIsolation() -> versionConfig.getVersionPath()
            versionConfig.getCustomPath().isNotEmpty() -> File(versionConfig.getCustomPath())
            else -> File(ProfilePathHome.getGameHome())
        }
    }

    private fun String.getValueOrDefault(default: String): String {
        return takeIf { it.isNotEmpty() } ?: default
    }

    fun getRenderer(): String {
        return versionConfig.getRenderer().getValueOrDefault(AllSettings.renderer.getValue())
    }

    fun getDriver(): String {
        return versionConfig.getDriver().getValueOrDefault(AllSettings.driver.getValue())
    }

    fun getJavaDir(): String {
        return versionConfig.getJavaDir().getValueOrDefault(AllSettings.defaultRuntime.getValue())
    }

    fun getJavaArgs(): String {
        return versionConfig.getJavaArgs().getValueOrDefault(AllSettings.javaArgs.getValue())
    }

    fun getControl(): String {
        val configControl = versionConfig.getControl().removeSuffix("./")
        return if (configControl.isNotEmpty()) {
            File(PathManager.DIR_CTRLMAP_PATH, configControl).absolutePath
        } else {
            File(AllSettings.defaultCtrl.getValue()).absolutePath
        }
    }

    fun getCustomInfo(): String {
        return versionConfig
            .getCustomInfo()
            .getValueOrDefault(AllSettings.versionCustomInfo.getValue())
            .replace("[zl_version]", ZHTools.getVersionName())
    }

    fun getVersionInfo(): VersionInfo? {
        return runCatching {
            val infoFile = File(VersionsManager.getZalithVersionPath(this), "VersionInfo.json")
            Tools.GLOBAL_GSON.fromJson(Tools.read(infoFile), VersionInfo::class.java)
        }.getOrNull()
    }

    private fun Boolean.toIntValue(): Int = if (this) 1 else 0

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeStringList(listOf(versionsFolder, versionPath))
        dest.writeParcelable(versionConfig, flags)
        dest.writeInt(isValid.toIntValue())
        dest.writeInt(offlineAccountLogin.toIntValue())
        dest.writeParcelable(modCheckResult, flags)
    }

    companion object CREATOR : Parcelable.Creator<Version> {
        private fun Int.toBooleanValue(): Boolean = this != 0

        override fun createFromParcel(parcel: Parcel): Version {
            val stringList = ArrayList<String>()
            parcel.readStringList(stringList)

            val versionConfig =
                parcel.readParcelable<VersionConfig>(VersionConfig::class.java.classLoader)!!
            val isValid = parcel.readInt().toBooleanValue()
            val offlineAccount = parcel.readInt().toBooleanValue()
            val modCheckResult =
                parcel.readParcelable<ModChecker.ModCheckResult>(
                    ModChecker.ModCheckResult::class.java.classLoader
                )

            return Version(
                stringList[0],
                stringList[1],
                versionConfig,
                isValid
            ).apply {
                offlineAccountLogin = offlineAccount
                this.modCheckResult = modCheckResult
            }
        }

        override fun newArray(size: Int): Array<Version?> {
            return arrayOfNulls(size)
        }
    }
}
