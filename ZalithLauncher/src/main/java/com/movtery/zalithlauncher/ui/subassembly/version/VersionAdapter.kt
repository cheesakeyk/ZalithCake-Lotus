package com.movtery.zalithlauncher.ui.subassembly.version

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayout
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.databinding.ItemVersionBinding
import com.movtery.zalithlauncher.databinding.ViewVersionManagerBinding
import com.movtery.zalithlauncher.feature.customprofilepath.ProfilePathManager
import com.movtery.zalithlauncher.feature.version.Version
import com.movtery.zalithlauncher.feature.version.VersionsManager
import com.movtery.zalithlauncher.feature.version.utils.VersionIconUtils
import com.movtery.zalithlauncher.task.Task
import com.movtery.zalithlauncher.ui.dialog.TipDialog
import com.movtery.zalithlauncher.ui.fragment.FilesFragment
import com.movtery.zalithlauncher.utils.ZHTools
import com.movtery.zalithlauncher.utils.file.FileDeletionHandler
import net.kdt.pojavlaunch.Tools

class VersionAdapter(
    private val parentFragment: Fragment,
    private val listener: OnVersionItemClickListener
) : RecyclerView.Adapter<VersionAdapter.ViewHolder>() {

    private val versions: MutableList<Version> = ArrayList()

    /**
     * List of all RadioButtons, each tagged with the version path it represents.
     */
    private val radioButtonList: MutableList<RadioButton> = mutableListOf()

    private var currentVersion: String? = null

    private val managerPopupWindow: PopupWindow = PopupWindow().apply {
        isFocusable = true
        isOutsideTouchable = true
    }

    @SuppressLint("NotifyDataSetChanged")
    fun refreshVersions(versions: List<Version>): Int {
        this.versions.clear()
        this.versions.addAll(versions)

        radioButtonList.apply {
            forEach { radioButton -> radioButton.isChecked = false }
            clear()
        }

        currentVersion = resolveCurrentVersionPath(versions)

        // Find the index of the selected version.
        val currentIndex = versions.indexOfFirst {
            it.getVersionPath().absolutePath == currentVersion
        }

        notifyDataSetChanged()
        return currentIndex
    }

    fun closePopupWindow() {
        managerPopupWindow.dismiss()
    }

    private fun resolveCurrentVersionPath(versions: List<Version>): String? {
        val savedCurrentVersionPath = VersionsManager
            .getCurrentVersion()
            ?.getVersionPath()
            ?.absolutePath

        if (savedCurrentVersionPath != null &&
            versions.any { it.getVersionPath().absolutePath == savedCurrentVersionPath }
        ) {
            return savedCurrentVersionPath
        }

        // If the saved current version is missing, automatically select the first valid version.
        // Since the version list is already sorted by VersionsManager, this makes the newest valid
        // version become the active one.
        val fallbackVersion = versions.firstOrNull { it.isValid() } ?: return null
        VersionsManager.saveCurrentVersion(fallbackVersion.getVersionName())
        return fallbackVersion.getVersionPath().absolutePath
    }

    private fun setCurrentVersion(context: Context, version: Version) {
        if (version.isValid()) {
            VersionsManager.saveCurrentVersion(version.getVersionName())
            currentVersion = version.getVersionPath().absolutePath
        } else {
            // Invalid versions cannot be selected. Clicking them will prompt the user to delete them.
            deleteVersion(version, context.getString(R.string.version_manager_delete_tip_invalid))
        }

        radioButtonList.forEach { radioButton ->
            radioButton.isChecked = radioButton.tag.toString() == currentVersion
        }
    }

    /**
     * Ask for confirmation before deleting a version.
     * If the version is invalid, clicking it defaults to deletion.
     */
    private fun deleteVersion(version: Version, deleteMessage: String) {
        val context = parentFragment.requireActivity()

        TipDialog.Builder(context)
            .setTitle(context.getString(R.string.version_manager_delete))
            .setMessage(deleteMessage)
            .setWarning()
            .setCancelable(false)
            .setConfirmClickListener {
                FileDeletionHandler(
                    context,
                    listOf(version.getVersionPath()),
                    Task.runTask {
                        VersionsManager.refresh("VersionAdapter:deleteVersion")
                    }
                ).start()
            }
            .showDialog()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemVersionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(versions[position])
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        radioButtonList.remove(holder.binding.radioButton)
    }

    override fun getItemCount(): Int = versions.size

    inner class ViewHolder(val binding: ItemVersionBinding) : RecyclerView.ViewHolder(binding.root) {
        private val context = binding.root.context

        private fun String.addInfoIfNotBlank(setRed: Boolean = false) {
            takeIf { it.isNotBlank() }?.let { text ->
                binding.versionInfoLayout.addView(createInfoTextView(text, setRed))
            }
        }

        fun bind(version: Version) {
            binding.apply {
                versionInfoLayout.removeAllViews()
                versionName.isSelected = true

                val versionPath = version.getVersionPath().absolutePath
                radioButtonList.add(
                    radioButton.apply {
                        tag = versionPath
                        isChecked = currentVersion == versionPath
                    }
                )

                versionName.text = version.getVersionName()

                if (!version.isValid()) {
                    context.getString(R.string.version_manager_invalid).addInfoIfNotBlank(true)
                }

                if (version.getVersionConfig().isIsolation()) {
                    context.getString(R.string.pedit_isolation_enabled).addInfoIfNotBlank()
                }

                version.getVersionInfo()?.let { versionInfo ->
                    versionInfoLayout.addView(createInfoTextView(versionInfo.minecraftVersion))
                    versionInfo.loaderInfo?.forEach { loaderInfo ->
                        loaderInfo.name.addInfoIfNotBlank()
                        loaderInfo.version.addInfoIfNotBlank()
                    }
                }

                favorite.setOnClickListener {
                    listener.showFavoritesDialog(version.getVersionName())
                }
                favorite.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        if (listener.isVersionFavorited(version.getVersionName())) {
                            R.drawable.ic_favorite
                        } else {
                            R.drawable.ic_favorite_border
                        }
                    )
                )

                operate.setOnClickListener {
                    showPopupWindow(operate, version)
                }

                VersionIconUtils(version).start(versionIcon)

                val onClickListener = View.OnClickListener {
                    setCurrentVersion(context, version)
                }
                radioButton.setOnClickListener(onClickListener)
                root.setOnClickListener(onClickListener)
            }
        }

        private fun createInfoTextView(text: String, setRed: Boolean = false): TextView {
            val textView = TextView(context)
            textView.text = text

            val layoutParams = FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(0, 0, Tools.dpToPx(8f).toInt(), 0)
            textView.layoutParams = layoutParams

            if (setRed) {
                textView.setTextColor(Color.RED)
            }

            return textView
        }

        private fun showPopupWindow(anchorView: View, version: Version) {
            val context = parentFragment.requireActivity()

            val viewBinding = ViewVersionManagerBinding.inflate(LayoutInflater.from(context)).apply {
                val onClickListener = View.OnClickListener { view ->
                    when (view) {
                        gotoView -> swapPath(version.getVersionPath().absolutePath)
                        gamePath -> swapPath(version.getGameDir().absolutePath)
                        rename -> VersionsManager.openRenameDialog(context, version)
                        copy -> VersionsManager.openCopyDialog(context, version)
                        delete -> deleteVersion(
                            version,
                            context.getString(
                                R.string.version_manager_delete_tip,
                                version.getVersionName()
                            )
                        )
                    }
                    managerPopupWindow.dismiss()
                }

                gotoView.setOnClickListener(onClickListener)
                gamePath.setOnClickListener(onClickListener)
                rename.setOnClickListener(onClickListener)
                copy.setOnClickListener(onClickListener)
                delete.setOnClickListener(onClickListener)
            }

            managerPopupWindow.apply {
                viewBinding.root.measure(0, 0)
                contentView = viewBinding.root
                width = viewBinding.root.measuredWidth
                height = viewBinding.root.measuredHeight
                showAsDropDown(anchorView, anchorView.measuredWidth, 0)
            }
        }

        private fun swapPath(path: String) {
            val bundle = Bundle().apply {
                putString(FilesFragment.BUNDLE_LOCK_PATH, ProfilePathManager.getCurrentPath())
                putString(FilesFragment.BUNDLE_LIST_PATH, path)
                putBoolean(FilesFragment.BUNDLE_QUICK_ACCESS_PATHS, false)
            }

            ZHTools.swapFragmentWithAnim(
                parentFragment,
                FilesFragment::class.java,
                FilesFragment.TAG,
                bundle
            )
        }
    }

    interface OnVersionItemClickListener {
        /**
         * The user tapped the favorite button. Check and show the favorites dialog.
         */
        fun showFavoritesDialog(versionName: String)

        /**
         * Checks whether the current version has been favorited.
         */
        fun isVersionFavorited(versionName: String): Boolean
    }
}
