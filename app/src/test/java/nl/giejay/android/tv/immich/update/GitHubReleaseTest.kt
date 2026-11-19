package nl.giejay.android.tv.immich.update

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GitHubReleaseTest {

    private val releaseJson = """
        {
          "tag_name": "v7.1.0",
          "name": "Immich TV 7.1.0",
          "body": "New stuff",
          "draft": false,
          "prerelease": false,
          "assets": [
            {"name": "checksums.txt", "browser_download_url": "https://example.com/checksums.txt", "size": 12},
            {"name": "ImmichTV-7.1.0.apk", "browser_download_url": "https://example.com/ImmichTV-7.1.0.apk", "size": 44000000}
          ]
        }
    """.trimIndent()

    @Test
    fun `picks the apk asset and the tagged version`() {
        val result = UpdateChecker.parseLatestRelease(releaseJson)

        assertThat(result.isRight()).isTrue()
        val update = result.getOrNull()!!
        assertThat(update.version).isEqualTo(AppVersion(7, 1, 0))
        assertThat(update.downloadUrl).isEqualTo("https://example.com/ImmichTV-7.1.0.apk")
        assertThat(update.sizeBytes).isEqualTo(44_000_000L)
        assertThat(update.notes).isEqualTo("New stuff")
    }

    @Test
    fun `fails when there is no apk in the release`() {
        val json = """{"tag_name":"7.1.0","assets":[{"name":"notes.txt","browser_download_url":"https://example.com/notes.txt"}]}"""

        val result = UpdateChecker.parseLatestRelease(json)

        assertThat(result.isLeft()).isTrue()
    }

    @Test
    fun `fails on a broken response`() {
        assertThat(UpdateChecker.parseLatestRelease("not json").isLeft()).isTrue()
    }

    @Test
    fun `only newer versions count as an update`() {
        val current = AppVersion(7, 0, 0)
        val update = UpdateInfo(AppVersion(7, 1, 0), "Immich TV 7.1.0", "", "https://example.com/a.apk", 1)

        assertThat(update.isNewerThan(current)).isTrue()
        assertThat(update.isNewerThan(AppVersion(7, 1, 0))).isFalse()
        assertThat(update.isNewerThan(AppVersion(7, 2, 0))).isFalse()
    }

    @Test
    fun `falls back to the release name when the tag has no version`() {
        val json = """{"tag_name":"nightly","name":"Immich TV 8.0.0","assets":[{"name":"a.apk","browser_download_url":"https://example.com/a.apk"}]}"""

        assertThat(UpdateChecker.parseLatestRelease(json).getOrNull()?.version).isEqualTo(AppVersion(8, 0, 0))
    }
}
