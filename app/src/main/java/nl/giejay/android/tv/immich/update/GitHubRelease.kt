package nl.giejay.android.tv.immich.update

import com.google.gson.annotations.SerializedName

/** Subset of the GitHub "get latest release" response, see [UpdateChecker]. */
data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String? = null,
    val name: String? = null,
    val body: String? = null,
    val assets: List<GitHubAsset>? = null,
    val draft: Boolean? = null,
    val prerelease: Boolean? = null
) {
    /** The version this release describes, taken from its tag (falling back to its name). */
    fun version(): AppVersion? = AppVersion.parse(tagName) ?: AppVersion.parse(name)

    /** The installable APK of this release, if it has one. */
    fun apkAsset(): GitHubAsset? = assets?.firstOrNull { it.name?.endsWith(".apk", ignoreCase = true) == true }

    fun toUpdateInfo(): UpdateInfo? {
        val version = version() ?: return null
        val asset = apkAsset() ?: return null
        val url = asset.downloadUrl ?: return null
        return UpdateInfo(
            version = version,
            releaseName = name?.ifBlank { null } ?: "Immich TV $version",
            notes = body.orEmpty(),
            downloadUrl = url,
            sizeBytes = asset.size ?: 0L
        )
    }
}

data class GitHubAsset(
    val name: String? = null,
    @SerializedName("browser_download_url") val downloadUrl: String? = null,
    val size: Long? = null
)

data class UpdateInfo(
    val version: AppVersion,
    val releaseName: String,
    val notes: String,
    val downloadUrl: String,
    val sizeBytes: Long
) {
    /** True when this release is newer than [current]. */
    fun isNewerThan(current: AppVersion?): Boolean = current == null || version > current
}
