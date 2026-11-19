package nl.giejay.android.tv.immich.update

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppVersionTest {

    @Test
    fun `parses plain and prefixed versions`() {
        assertThat(AppVersion.parse("7.0.0")).isEqualTo(AppVersion(7, 0, 0))
        assertThat(AppVersion.parse("v7.1.2")).isEqualTo(AppVersion(7, 1, 2))
        assertThat(AppVersion.parse("Immich TV 7.2.0")).isEqualTo(AppVersion(7, 2, 0))
        assertThat(AppVersion.parse("7.3")).isEqualTo(AppVersion(7, 3, 0))
    }

    @Test
    fun `rejects values without a version`() {
        assertThat(AppVersion.parse(null)).isNull()
        assertThat(AppVersion.parse("")).isNull()
        assertThat(AppVersion.parse("latest")).isNull()
    }

    @Test
    fun `compares numerically, not lexicographically`() {
        assertThat(AppVersion.parse("7.10.0")!! > AppVersion.parse("7.9.9")!!).isTrue()
        assertThat(AppVersion.parse("7.0.1")!! > AppVersion.parse("7.0.0")!!).isTrue()
        assertThat(AppVersion.parse("v7.0.0")!!.compareTo(AppVersion.parse("7.0.0")!!)).isEqualTo(0)
    }
}
