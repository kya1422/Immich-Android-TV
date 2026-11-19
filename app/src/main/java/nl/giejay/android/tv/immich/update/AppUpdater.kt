package nl.giejay.android.tv.immich.update

import android.app.Activity
import android.app.AlertDialog
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.giejay.android.tv.immich.BuildConfig
import nl.giejay.android.tv.immich.R
import nl.giejay.android.tv.immich.shared.prefs.AUTO_UPDATE_CHECK
import nl.giejay.android.tv.immich.shared.prefs.PreferenceManager
import timber.log.Timber

/**
 * Self-update against the GitHub releases of this fork (see [UpdateChecker]): check, prompt,
 * download and hand over to the package installer.
 *
 * [checkSilently] runs from [nl.giejay.android.tv.immich.MainActivity] at most once a day and only
 * shows a dialog when there is something to install; [checkNow] is the "check for updates" action
 * in the settings and always reports its result.
 */
object AppUpdater {
    private const val LAST_CHECK_KEY = "lastUpdateCheck"
    private const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L
    private const val MAX_NOTES_LENGTH = 1200

    val currentVersion: AppVersion? = AppVersion.parse(BuildConfig.VERSION_NAME)

    /** Called on app start: throttled, silent unless an update exists. */
    fun checkSilently(activity: Activity) {
        if (BuildConfig.DEBUG) return
        if (!PreferenceManager.get(AUTO_UPDATE_CHECK)) return
        if (!isCheckDue()) return
        markChecked()
        check(activity, silent = true)
    }

    /** Manual check from the settings screen. */
    fun checkNow(activity: Activity) {
        markChecked()
        check(activity, silent = false)
    }

    private fun isCheckDue(): Boolean {
        val lastCheck = PreferenceManager.sharedPreference.getLong(LAST_CHECK_KEY, 0L)
        return lastCheck == 0L || System.currentTimeMillis() - lastCheck > CHECK_INTERVAL_MS
    }

    private fun markChecked() {
        PreferenceManager.sharedPreference.edit()
            .putLong(LAST_CHECK_KEY, System.currentTimeMillis())
            .apply()
    }

    private fun check(activity: Activity, silent: Boolean) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            val result = UpdateChecker.fetchLatest()
            withContext(Dispatchers.Main) {
                if (activity.isFinishing || activity.isDestroyed) {
                    return@withContext
                }
                val current = currentVersion
                result.fold(
                    { error ->
                        Timber.w("Update check failed: $error")
                        if (!silent) showToast(activity, activity.getString(R.string.update_check_failed, error))
                    },
                    { update ->
                        if (update.isNewerThan(current)) {
                            promptInstall(activity, update)
                        } else if (!silent) {
                            showToast(activity, activity.getString(R.string.update_none_available, BuildConfig.VERSION_NAME))
                        }
                    }
                )
            }
            scope.cancel()
        }
    }

    private fun promptInstall(activity: Activity, update: UpdateInfo) {
        AlertDialog.Builder(activity)
            .setTitle(
                activity.getString(
                    R.string.update_available_title,
                    update.version.toString(),
                    BuildConfig.VERSION_NAME
                )
            )
            .setMessage(notesFor(activity, update))
            .setPositiveButton(R.string.update_install_now) { _, _ -> downloadAndInstall(activity, update) }
            .setNeutralButton(R.string.update_later, null)
            .show()
    }

    private fun notesFor(activity: Activity, update: UpdateInfo): String {
        val notes = update.notes.trim().ifBlank { activity.getString(R.string.update_no_notes) }
        return if (notes.length <= MAX_NOTES_LENGTH) notes else notes.take(MAX_NOTES_LENGTH) + "…"
    }

    private fun downloadAndInstall(activity: Activity, update: UpdateInfo) {
        val progress = AlertDialog.Builder(activity)
            .setTitle(R.string.update_downloading_title)
            .setMessage(activity.getString(R.string.update_downloading_message, 0))
            .setCancelable(false)
            .show()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            val apk = UpdateInstaller.download(update, UpdateInstaller.apkFile(activity, update)) { percent ->
                if (percent < 0) return@download
                activity.runOnUiThread {
                    if (progress.isShowing) {
                        progress.setMessage(activity.getString(R.string.update_downloading_message, percent))
                    }
                }
            }
            withContext(Dispatchers.Main) {
                progress.dismiss()
                if (apk == null) {
                    promptDownloadFailed(activity)
                } else {
                    UpdateInstaller.install(activity, apk)
                }
            }
            scope.cancel()
        }
    }

    private fun promptDownloadFailed(activity: Activity) {
        AlertDialog.Builder(activity)
            .setTitle(R.string.update_download_failed_title)
            .setMessage(R.string.update_download_failed_message)
            .setPositiveButton(R.string.update_open_release_page) { _, _ ->
                UpdateInstaller.openReleasePage(activity)
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun showToast(activity: Activity, message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
    }
}
