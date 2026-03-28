package com.movtery.zalithlauncher.feature.version

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.google.gson.JsonParser
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.feature.log.Logging
import com.movtery.zalithlauncher.feature.version.VersionsManager.getZalithVersionPath
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.utils.stringutils.StringUtils.getStringNotNull
import net.kdt.pojavlaunch.Tools
import java.io.File
import java.io.FileWriter

class VersionConfig(private var versionPath: File) : Parcelable {
    private var isolationType: IsolationType = IsolationType.FOLLOW_GLOBAL
    private var javaDir: String = ""
    private var javaArgs: String = ""
    private var renderer: String = ""
    private var driver: String = ""
    private var control: String = ""
    private var customPath: String = ""
    private var customInfo: String = ""

    constructor(
        filePath: File,
        isolationType: IsolationType = IsolationType.FOLLOW_GLOBAL,
        javaDir: String = "",
        javaArgs: String = "",
        renderer: String = "",
        driver: String = "",
        control: String = "",
        customPath: String = "",
        customInfo: String = ""
    ) : this(filePath) {
        this.isolationType = isolationType
        this.javaDir = javaDir
        this.javaArgs = javaArgs
        this.renderer = renderer
        this.driver = driver
        this.control = control
        this.customPath = customPath
        this.customInfo = customInfo
    }

    fun copy(): VersionConfig = VersionConfig(versionPath,
        getIsolationTypeNotNull(isolationType),
        getStringNotNull(javaDir),
        getStringNotNull(javaArgs),
        getStringNotNull(renderer),
        getStringNotNull(driver),
        getStringNotNull(control),
        getStringNotNull(customPath),
        getStringNotNull(customInfo)
    )

    fun save() {
        runCatching {
            saveWithThrowable()
        }.onFailure { e ->
            Logging.e("Save Version Config", "$this\n${Tools.printToString(e)}")
        }
    }

    @Throws(Throwable::class)
    fun saveWithThrowable() {
        Logging.i("Save Version Config", "Trying to save: $this")

        val zalithVersionPath = getZalithVersionPath(versionPath)
        if (!zalithVersionPath.exists() && !zalithVersionPath.mkdirs()) {
            throw IllegalStateException("Failed to create version config directory: ${zalithVersionPath.absolutePath}")
        }

        val configFile = File(zalithVersionPath, "VersionConfig.json")

        FileWriter(configFile, false).use {
            it.write(Tools.GLOBAL_GSON.toJson(this))
        }

        Logging.i("Save Version Config", "Saved: $this")
    }

    fun getVersionPath() = versionPath

    fun setVersionPath(versionPath: File) {
        this.versionPath = versionPath
    }

    fun isIsolation(): Boolean = when(getIsolationTypeNotNull(isolationType)) {
        IsolationType.FOLLOW_GLOBAL -> AllSettings.versionIsolation.getValue()
        IsolationType.ENABLE -> true
        IsolationType.DISABLE -> false
    }

    fun getIsolationType() = getIsolationTypeNotNull(isolationType)

    fun setIsolationType(isolationType: IsolationType) { this.isolationType = isolationType }

    fun getJavaDir(): String = getStringNotNull(javaDir)

    fun setJavaDir(dir: String) { this.javaDir = dir }

    fun getJavaArgs(): String = getStringNotNull(javaArgs)

    fun setJavaArgs(args: String) { this.javaArgs = args }

    fun getRenderer(): String = getStringNotNull(renderer)

    fun setRenderer(renderer: String) { this.renderer = renderer }

    fun getDriver(): String = getStringNotNull(driver)

    fun setDriver(driver: String) { this.driver = driver }

    fun getControl(): String = getStringNotNull(control)

    fun setControl(control: String) { this.control = control }

    fun getCustomPath(): String = getStringNotNull(customPath)

    fun setCustomPath(customPath: String) { this.customPath = customPath }

    fun getCustomInfo(): String = getStringNotNull(customInfo)

    fun setCustomInfo(customInfo: String) { this.customInfo = customInfo }

    fun checkDifferent(otherConfig: VersionConfig): Boolean {
        return !(this.getIsolationType() == otherConfig.getIsolationType() &&
                this.getJavaDir() == otherConfig.getJavaDir() &&
                this.getJavaArgs() == otherConfig.getJavaArgs() &&
                this.getRenderer() == otherConfig.getRenderer() &&
                this.getDriver() == otherConfig.getDriver() &&
                this.getControl() == otherConfig.getControl() &&
                this.getCustomPath() == otherConfig.getCustomPath() &&
                this.getCustomInfo() == otherConfig.getCustomInfo())
    }

    private fun getIsolationTypeNotNull(type: IsolationType?) = type ?: IsolationType.FOLLOW_GLOBAL

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(versionPath.absolutePath)
        dest.writeInt(getIsolationTypeNotNull(isolationType).ordinal)
        dest.writeString(getStringNotNull(javaDir))
        dest.writeString(getStringNotNull(javaArgs))
        dest.writeString(getStringNotNull(renderer))
        dest.writeString(getStringNotNull(driver))
        dest.writeString(getStringNotNull(control))
        dest.writeString(getStringNotNull(customPath))
        dest.writeString(getStringNotNull(customInfo))
    }

    companion object CREATOR : Parcelable.Creator<VersionConfig> {
        override fun createFromParcel(parcel: Parcel): VersionConfig {
            val versionPath = File(parcel.readString().orEmpty())
            val isolationType = IsolationType.entries.getOrNull(parcel.readInt()) ?: IsolationType.FOLLOW_GLOBAL
            val javaDir = parcel.readString().orEmpty()
            val javaArgs = parcel.readString().orEmpty()
            val renderer = parcel.readString().orEmpty()
            val driver = parcel.readString().orEmpty()
            val control = parcel.readString().orEmpty()
            val customPath = parcel.readString().orEmpty()
            val customInfo = parcel.readString().orEmpty()
            return VersionConfig(versionPath, isolationType, javaDir, javaArgs, renderer, driver, control, customPath, customInfo)
        }

        override fun newArray(size: Int): Array<VersionConfig?> {
            return arrayOfNulls(size)
        }

        @JvmStatic
        fun parseConfig(versionPath: File): VersionConfig {
            val zalithVersionPath = getZalithVersionPath(versionPath)
            val oldConfigFile = File(zalithVersionPath, "ZalithVersion.cfg")
            val configFile = File(zalithVersionPath, "VersionConfig.json")

            // Migrate old config format if present
            if (oldConfigFile.exists()) {
                runCatching {
                    Tools.GLOBAL_GSON.fromJson(
                        Tools.read(oldConfigFile),
                        VersionConfig::class.java
                    ).apply {
                        setIsolationType(IsolationType.ENABLE)
                        setVersionPath(versionPath)
                        save()
                    }
                }.onSuccess { migratedConfig ->
                    oldConfigFile.delete()
                    return migratedConfig
                }.onFailure {
                    Logging.e("Refresh Versions", "Failed to migrate old version config.", it)
                    oldConfigFile.delete()
                }
            }

            // Missing config is normal for fresh/manual/non-Zalith versions
            if (!configFile.exists()) {
                val config = VersionConfig(versionPath)
                config.save()
                return config
            }

            return runCatching {
                val configString = Tools.read(configFile)
                val config = Tools.GLOBAL_GSON.fromJson(configString, VersionConfig::class.java)

                runCatching {
                    JsonParser.parseString(configString).asJsonObject.apply {
                        if (has("isolation")) {
                            config.setIsolationType(
                                if (get("isolation").asBoolean) {
                                    IsolationType.ENABLE
                                } else {
                                    IsolationType.DISABLE
                                }
                            )
                        }
                    }
                }.onFailure {
                    Logging.e(
                        "Refresh Versions",
                        "Failed to parse the old isolation field in version config.",
                        it
                    )
                }

                config.setVersionPath(versionPath)
                config
            }.getOrElse { e ->
                Logging.e("Refresh Versions", "Failed to read version config, recreating default.", e)
                val config = VersionConfig(versionPath)
                config.save()
                config
            }
        }

        @JvmStatic
        fun createIsolation(versionPath: File): VersionConfig {
            val config = VersionConfig(versionPath)
            config.setIsolationType(IsolationType.ENABLE)
            return config
        }

        @JvmStatic
        fun getIsolationString(context: Context, type: IsolationType): String = when (type) {
            IsolationType.FOLLOW_GLOBAL -> context.getString(R.string.version_manager_isolation_type_follow_global)
            IsolationType.ENABLE -> context.getString(R.string.generic_open)
            IsolationType.DISABLE -> context.getString(R.string.generic_close)
        }
    }

    enum class IsolationType {
        FOLLOW_GLOBAL, ENABLE, DISABLE
    }
}