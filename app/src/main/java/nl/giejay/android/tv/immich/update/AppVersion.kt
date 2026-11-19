package nl.giejay.android.tv.immich.update

/**
 * A `major.minor.patch` version as used by the app's `versionName` and the GitHub release tags.
 *
 * [parse] is lenient on purpose: tags may be `v7.1.0`, release names may be `Immich TV 7.1.0`,
 * and a version may be shortened to `7.1`.
 */
data class AppVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<AppVersion> {

    override fun compareTo(other: AppVersion): Int = compareValuesBy(
        this, other, { it.major }, { it.minor }, { it.patch }
    )

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        private val PATTERN = Regex("""(\d+)\.(\d+)(?:\.(\d+))?""")

        fun parse(raw: String?): AppVersion? {
            val match = PATTERN.find(raw ?: return null) ?: return null
            return AppVersion(
                major = match.groupValues[1].toIntOrNull() ?: return null,
                minor = match.groupValues[2].toIntOrNull() ?: return null,
                patch = match.groupValues[3].toIntOrNull() ?: 0
            )
        }
    }
}
