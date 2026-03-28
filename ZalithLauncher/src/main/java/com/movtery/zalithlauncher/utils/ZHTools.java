package com.movtery.zalithlauncher.utils;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.movtery.zalithlauncher.BuildConfig;
import com.movtery.zalithlauncher.InfoDistributor;
import com.movtery.zalithlauncher.R;
import com.movtery.zalithlauncher.context.ContextExecutor;
import com.movtery.zalithlauncher.feature.log.Logging;
import com.movtery.zalithlauncher.setting.AllSettings;
import com.movtery.zalithlauncher.task.Task;
import com.movtery.zalithlauncher.task.TaskExecutors;
import com.movtery.zalithlauncher.ui.dialog.TipDialog;
import com.movtery.zalithlauncher.ui.fragment.FragmentWithAnim;
import com.movtery.zalithlauncher.utils.file.FileTools;
import com.movtery.zalithlauncher.utils.path.PathManager;

import net.kdt.pojavlaunch.Tools;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipOutputStream;

public final class ZHTools {
    private static final String TAG_ZIP_LOG = "Zip Log";
    private static final String TAG_VENDOR_CHECK = "CheckVendor";
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private ZHTools() {
    }

    public static void onBackPressed(FragmentActivity activity) {
        activity.getOnBackPressedDispatcher().onBackPressed();
    }

    public static boolean isEnglish(Context context) {
        LocaleList locales = context.getResources().getConfiguration().getLocales();
        return !locales.isEmpty() && "en".equals(locales.get(0).getLanguage());
    }

    public static boolean isChinese(Context context) {
        Locale locale = context.getResources().getConfiguration().getLocales().get(0);
        return Locale.SIMPLIFIED_CHINESE.equals(locale);
    }

    public static void setTooltipText(ImageView... views) {
        for (ImageView view : views) {
            setTooltipText(view, view.getContentDescription());
        }
    }

    public static void setTooltipText(View view, CharSequence tooltip) {
        TooltipCompat.setTooltipText(view, tooltip);
    }

    public static synchronized Drawable customMouse(Context context) {
        File mouseFile = getCustomMouse();
        if (mouseFile != null && mouseFile.exists()) {
            return Drawable.createFromPath(mouseFile.getAbsolutePath());
        }
        return ResourcesCompat.getDrawable(
                context.getResources(),
                R.drawable.ic_mouse_pointer,
                context.getTheme()
        );
    }

    public static File getCustomMouse() {
        String customMouse = AllSettings.getCustomMouse().getValue();
        if (customMouse == null || customMouse.isEmpty()) {
            return null;
        }
        return new File(PathManager.DIR_CUSTOM_MOUSE, customMouse);
    }

    public static void dialogForceClose(Context context) {
        new TipDialog.Builder(context)
                .setTitle(R.string.option_force_close)
                .setMessage(R.string.force_exit_confirm)
                .setConfirmClickListener(checked -> {
                    try {
                        killProcess();
                    } catch (Throwable throwable) {
                        Logging.w(
                                InfoDistributor.LAUNCHER_NAME,
                                "Could not force close the process.",
                                throwable
                        );
                    }
                })
                .showDialog();
    }

    /**
     * Shows a confirmation dialog before opening a link in the browser.
     *
     * @param link the link to open
     */
    public static void openLink(Context context, String link) {
        openLink(context, link, null);
    }

    /**
     * Shows a confirmation dialog before opening a link or URI in another app.
     *
     * @param link the link to open
     * @param dataType optional MIME type for the intent
     */
    public static void openLink(Context context, String link, String dataType) {
        new TipDialog.Builder(context)
                .setTitle(R.string.open_link)
                .setMessage(link)
                .setConfirmClickListener(checked -> {
                    Uri uri = Uri.parse(link);
                    Intent intent;
                    if (dataType != null) {
                        intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(uri, dataType);
                    } else {
                        intent = new Intent(Intent.ACTION_VIEW, uri);
                    }
                    context.startActivity(intent);
                })
                .showDialog();
    }

    public static void swapFragmentWithAnim(
            Fragment fragment,
            Class<? extends Fragment> fragmentClass,
            @Nullable String fragmentTag,
            @Nullable Bundle bundle
    ) {
        if (fragment instanceof FragmentWithAnim) {
            ((FragmentWithAnim) fragment).slideOut();
        }

        getFragmentTransaction(fragment)
                .replace(R.id.container_fragment, fragmentClass, bundle, fragmentTag)
                .addToBackStack(fragmentClass.getName())
                .commit();
    }

    public static void addFragment(
            Fragment fragment,
            Class<? extends Fragment> fragmentClass,
            @Nullable String fragmentTag,
            @Nullable Bundle bundle
    ) {
        getFragmentTransaction(fragment)
                .addToBackStack(fragmentClass.getName())
                .add(R.id.container_fragment, fragmentClass, bundle, fragmentTag)
                .hide(fragment)
                .commit();
    }

    private static FragmentTransaction getFragmentTransaction(Fragment fragment) {
        FragmentTransaction transaction =
                fragment.requireActivity().getSupportFragmentManager().beginTransaction();

        if (AllSettings.getAnimation().getValue()) {
            transaction.setCustomAnimations(
                    R.anim.cut_into,
                    R.anim.cut_out,
                    R.anim.cut_into,
                    R.anim.cut_out
            );
        }

        return transaction.setReorderingAllowed(true);
    }

    public static void killProcess() {
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    public static int getVersionCode() {
        return BuildConfig.VERSION_CODE;
    }

    public static String getVersionName() {
        return BuildConfig.VERSION_NAME;
    }

    public static String getPackageName() {
        return BuildConfig.APPLICATION_ID;
    }

    /**
     * Returns the last update time of the app package.
     */
    public static String getLastUpdateTime(Context context) {
        try {
            PackageInfo packageInfo =
                    context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            Date date = new Date(packageInfo.lastUpdateTime);
            return new SimpleDateFormat(DATE_TIME_FORMAT, Locale.getDefault()).format(date);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isPreRelease() {
        return "PRE_RELEASE".equals(InfoDistributor.BUILD_TYPE);
    }

    public static boolean isRelease() {
        return "RELEASE".equals(InfoDistributor.BUILD_TYPE);
    }

    public static boolean isDebug() {
        return "DEBUG".equals(InfoDistributor.BUILD_TYPE);
    }

    /**
     * Returns the localized launcher build type string.
     */
    public static String getVersionStatus(Context context) {
        if (isPreRelease()) {
            return context.getString(R.string.generic_pre_release);
        }
        if (isRelease()) {
            return context.getString(R.string.generic_release);
        }
        return context.getString(R.string.generic_debug);
    }

    public static Date getDate(String dateString) {
        return Date.from(OffsetDateTime.parse(dateString).toInstant());
    }

    public static boolean checkDate(int month, int day) {
        LocalDate currentDate = LocalDate.now();
        return currentDate.getMonthValue() == month
                && currentDate.getDayOfMonth() == day;
    }

    public static boolean areaChecks(String area) {
        return getSystemLanguageName().equals(area);
    }

    public static String getSystemLanguageName() {
        return Locale.getDefault().getLanguage();
    }

    public static String getSystemLanguage() {
        Locale locale = Locale.getDefault();
        return locale.getLanguage() + "_" + locale.getCountry().toLowerCase();
    }

    public static boolean checkForNotificationPermission() {
        return Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
                ContextExecutor.getApplication(),
                Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_DENIED;
    }

    public static AlertDialog createTaskRunningDialog(Context context) {
        return createTaskRunningDialog(context, null);
    }

    public static AlertDialog createTaskRunningDialog(Context context, String message) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.view_task_running, null);
        TextView textView = dialogView.findViewById(R.id.text_view);

        if (textView != null && message != null) {
            textView.setText(message);
        }

        return new AlertDialog.Builder(context, R.style.CustomAlertDialogTheme)
                .setView(dialogView)
                .setCancelable(false)
                .create();
    }

    public static AlertDialog showTaskRunningDialog(Context context) {
        AlertDialog dialog = createTaskRunningDialog(context);
        dialog.show();
        return dialog;
    }

    public static AlertDialog showTaskRunningDialog(Context context, String message) {
        AlertDialog dialog = createTaskRunningDialog(context, message);
        dialog.show();
        return dialog;
    }

    public static boolean isAdrenoGPU() {
        EGLDisplay eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            Logging.e(TAG_VENDOR_CHECK, "Failed to get EGL display");
            return false;
        }

        if (!EGL14.eglInitialize(eglDisplay, null, 0, null, 0)) {
            Logging.e(TAG_VENDOR_CHECK, "Failed to initialize EGL");
            return false;
        }

        int[] eglAttributes = {
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_NONE
        };

        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!EGL14.eglChooseConfig(eglDisplay, eglAttributes, 0, configs, 0, 1, numConfigs, 0)
                || numConfigs[0] == 0) {
            EGL14.eglTerminate(eglDisplay);
            Logging.e(TAG_VENDOR_CHECK, "Failed to choose an EGL config");
            return false;
        }

        int[] contextAttributes = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };

        EGLContext context = EGL14.eglCreateContext(
                eglDisplay,
                configs[0],
                EGL14.EGL_NO_CONTEXT,
                contextAttributes,
                0
        );

        if (context == EGL14.EGL_NO_CONTEXT) {
            EGL14.eglTerminate(eglDisplay);
            Logging.e(TAG_VENDOR_CHECK, "Failed to create EGL context");
            return false;
        }

        if (!EGL14.eglMakeCurrent(
                eglDisplay,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                context
        )) {
            EGL14.eglDestroyContext(eglDisplay, context);
            EGL14.eglTerminate(eglDisplay);
            Logging.e(TAG_VENDOR_CHECK, "Failed to make EGL context current");
            return false;
        }

        String vendor = GLES20.glGetString(GLES20.GL_VENDOR);
        String renderer = GLES20.glGetString(GLES20.GL_RENDERER);

        boolean isAdreno =
                vendor != null
                        && renderer != null
                        && "Qualcomm".equalsIgnoreCase(vendor)
                        && renderer.toLowerCase().contains("adreno");

        EGL14.eglMakeCurrent(
                eglDisplay,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT
        );
        EGL14.eglDestroyContext(eglDisplay, context);
        EGL14.eglTerminate(eglDisplay);

        Logging.d(TAG_VENDOR_CHECK, "Running on Adreno GPU: " + isAdreno);
        return isAdreno;
    }

    public static synchronized void shareLogs(Context context) {
        AlertDialog dialog = createTaskRunningDialog(context);

        Task.runTask(() -> {
                    File zipFile = new File(PathManager.DIR_APP_CACHE, "logs.zip");

                    try (FileOutputStream fos = new FileOutputStream(zipFile);
                         ZipOutputStream zos = new ZipOutputStream(fos)) {

                        File logsFolder = new File(PathManager.DIR_LAUNCHER_LOG);
                        if (logsFolder.exists() && logsFolder.isDirectory()) {
                            FileTools.zipDirectory(
                                    logsFolder,
                                    "launcher_logs/",
                                    file -> {
                                        String fileName = file.getName();
                                        return fileName.equals("latestcrash.txt")
                                                || (fileName.startsWith("log") && fileName.endsWith(".txt"));
                                    },
                                    zos
                            );
                        } else {
                            Log.d(TAG_ZIP_LOG, "Launcher log folder does not exist or is unavailable");
                        }

                        File latestLogFile = new File(PathManager.DIR_GAME_HOME, "/latestlog.txt");
                        if (latestLogFile.exists() && latestLogFile.isFile()) {
                            FileTools.zipFile(latestLogFile, latestLogFile.getName(), zos);
                        } else {
                            Log.d(TAG_ZIP_LOG, "Game log file does not exist");
                        }
                    }

                    return zipFile;
                })
                .beforeStart(TaskExecutors.getAndroidUI(), dialog::show)
                .ended(TaskExecutors.getAndroidUI(), zipFile -> {
                    if (zipFile != null) {
                        FileTools.shareFile(context, zipFile);
                    }
                })
                .onThrowable(t -> Logging.e(TAG_ZIP_LOG, Tools.printToString(t)))
                .finallyTask(TaskExecutors.getAndroidUI(), dialog::dismiss)
                .execute();
    }

    public static boolean isDarkMode(Context context) {
        Configuration configuration = context.getResources().getConfiguration();
        return (configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    public static void getWebViewAfterProcessing(WebView view) {
        view.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView webView, String url) {
                super.onPageFinished(webView, url);

                boolean darkMode = isDarkMode(webView.getContext());
                String backgroundColor = darkMode ? "#333333" : "#CFCFCF";
                String textColor = darkMode ? "#ffffff" : "#0E0E0E";

                String css = "body { background-color: " + backgroundColor + "; color: " + textColor + "; }"
                        + "a, a:link, a:visited, a:hover, a:active {"
                        + "  color: " + textColor + ";"
                        + "  text-decoration: none;"
                        + "  pointer-events: none;"
                        + "}";

                String escapedCss = css.replace("'", "\\'");
                String js =
                        "var parent = document.getElementsByTagName('head').item(0);"
                                + "var style = document.createElement('style');"
                                + "style.type = 'text/css';"
                                + "if (style.styleSheet){"
                                + "  style.styleSheet.cssText = '" + escapedCss + "';"
                                + "} else {"
                                + "  style.appendChild(document.createTextNode('" + escapedCss + "'));"
                                + "}"
                                + "parent.appendChild(style);";

                webView.evaluateJavascript(js, null);
            }
        });
    }

    public static long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }
}