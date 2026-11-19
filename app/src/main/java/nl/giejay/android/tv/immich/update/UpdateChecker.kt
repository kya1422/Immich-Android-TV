package nl.giejay.android.tv.immich.update

import arrow.core.Either
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import nl.giejay.android.tv.immich.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException

/**
 * Looks up the latest release of this fork on GitHub. Releases are created by
 * `.github/workflows/release.yml`, which attaches the signed `ImmichTV-<version>.apk`.
 */
object UpdateChecker {
    /** Change this if you publish the APK somewhere else for your own fork. */
    const val GITHUB_REPO = "kya1422/Immich-Android-TV"

    private const val LATEST_RELEASE_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"

    /** Pure JSON handling, unit tested. */
    fun parseLatestRelease(json: String): Either<String, UpdateInfo> {
        val release = try {
            Gson().fromJson(json, GitHubRelease::class.java)
        } catch (e: JsonSyntaxException) {
            Timber.w(e, "Could not parse the GitHub release response")
            return Either.Left("Could not read the release information")
        } ?: return Either.Left("Empty release information")
        return release.toUpdateInfo()?.let { Either.Right(it) }
            ?: Either.Left("The latest release has no installable APK")
    }

    /** Blocking network call, run it off the main thread. */
    fun fetchLatest(client: OkHttpClient = OkHttpClient()): Either<String, UpdateInfo> {
        val request = Request.Builder()
            .url(LATEST_RELEASE_URL)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "ImmichTV/${BuildConfig.VERSION_NAME}")
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                when {
                    response.code == 404 -> Either.Left("No release found for $GITHUB_REPO")
                    !response.isSuccessful -> Either.Left("GitHub returned status ${response.code}")
                    body.isNullOrBlank() -> Either.Left("Empty response from GitHub")
                    else -> parseLatestRelease(body)
                }
            }
        } catch (e: IOException) {
            Timber.w(e, "Could not reach GitHub for an update check")
            Either.Left("Could not reach GitHub: ${e.message.orEmpty()}")
        }
    }
}
