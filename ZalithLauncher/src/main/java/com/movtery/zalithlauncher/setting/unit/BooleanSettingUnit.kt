package com.movtery.zalithlauncher.setting.unit

import com.movtery.zalithlauncher.setting.Settings.Manager

class BooleanSettingUnit(key: String, defaultValue: Boolean) : AbstractSettingUnit<Boolean>(key, defaultValue) {
    override fun getValue() = Manager.getValue(key, defaultValue) { it.toBoolean() }

}