package com.movtery.zalithlauncher.feature.unpack

import com.movtery.zalithlauncher.R

enum class Components(val component: String, val displayName: String, val summary: Int?, val privateDirectory: Boolean) {
    OTHER_LOGIN("other_login", "authlib-injector", R.string.splash_screen_authlib_injector, false),
    CACIOCAVALLO("caciocavallo", "caciocavallo", R.string.splash_screen_cacio, false),
    CACIOCAVALLO17("caciocavallo17", "caciocavallo 17", R.string.splash_screen_cacio, false),

    // LWJGL components must live under the app-private files directory so libjvm can dlopen them
    // through the classloader namespace without hitting the external-storage restriction.
    LWJGL3("lwjgl3", "LWJGL 3", R.string.splash_screen_lwjgl, true),
    LWJGL342("lwjgl3.4.2", "LWJGL 3.4.2", R.string.splash_screen_lwjgl, true),

    // Launcher support components (like MioLibPatcher.jar) are expected from DIR_DATA/components,
    // not from context.filesDir/components.
    COMPONENTS("components", "Launcher Components", R.string.splash_screen_components, false)
}
