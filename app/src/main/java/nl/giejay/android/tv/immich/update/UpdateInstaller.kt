package nl.giejay.android.tv.immich.update

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import nl.giejay.android.tv.immich.R
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.IOException

/**
 * Downloads a release APK and hands it to the system package installer.
 *
 * The APK is signed with the fork's fixed release key (see `keystore/keystore.properties`), so the
 * installer accepts it as an update of the installed app - as long as the currently installed build
 * was signed with the same key.
 */
object UpdateInstaller {

    const val APK_MIME_TYPE = "application/vnd.android.package-archive"

    fun updateDir(context: Context): File {
        val base = context.getExternalFilesDir(null) ?: context.cacheDir
        return File(base, "updates").apply { mkdirs() }
    }

    fun apkFile(context: Context, update: UpdateInfo): File =
        File(updateDir(context), "ImmichTV-${update.version}.apk")

    /**
     * Downloads [update] into [target], calling [onProgress] with 0..100 (or -1 when the size is
     * unknown). Blocking, run it off the main thread; returns the file or null on failure.
     */
    fun download(
        update: UpdateInfo,
        target: File,
        client: OkHttpClient = OkHttpClient(),
        onProgress: (Int) -> Unit = {}
    ): File? {
        val request = Request.Builder()
            .url(update.downloadUrl)
            .header("Accept", "application/octet-stream")
            .header("User-Agent", "ImmichTV")
            .build()
        val partial = File(target.parentFile, "${target.name}.part")
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.w("Download failed with status ${response.code}")
                    return null
                }
                val body = response.body ?: return null
                val total = body.contentLength().takeIf { it > 0 } ?: update.sizeBytes
                partial.parentFile?.mkdirs()
                body.byteStream().use { input ->
                    partial.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var copied = 0L
                        var lastPercent = -1
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            copied += read
                            val percent = if (total > 0) ((copied * 100) / total).toInt() else -1
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent)
                            }
                        }
                    }
                }
            }
            if (target.exists()) target.delete()
            if (!partial.renameTo(target)) {
                partial.copyTo(target, overwrite = true)
                partial.delete()
            }
            target
        } catch (e: IOException) {
            Timber.w(e, "Could not download the update")
            partial.delete()
            null
        }
    }

    /**
     * Opens the package installer for [apk]. Android 8+ additionally needs the user to allow
     * "install unknown apps" for this app, so ask for that first when it is missing.
     */
    fun install(activity: Activity, apk: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !activity.packageManager.canRequestPackageInstalls()
        ) {
            AlertDialog.Builder(activity)
                .setTitle(R.string.update_install_permission_title)
                .setMessage(R.string.update_install_permission_message)
                .setPositiveButton(R.string.update_open_settings) { _, _ ->
                    openInstallPermissionSettings(activity)
                }
                .setNegativeButton(R.string.close, null)
                .show()
            return
        }
        try {
            val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", apk)
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, APK_MIME_TYPE)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "Could not start the package installer")
            openReleasePage(activity)
        }
    }

    private fun openInstallPermissionSettings(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
            .setData(Uri.parse("package:${activity.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            activity.startActivity(intent)
        } catch (e: Exception) {
            Timber.w(e, "Could not open the unknown-sources settings")
        }
    }

    fun openReleasePage(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW)
            .setData(Uri.parse("https://github.com/${UpdateChecker.GITHUB_REPO}/releases/latest"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Timber.w(e, "No browser available to open the release page")
        }
    }
}
