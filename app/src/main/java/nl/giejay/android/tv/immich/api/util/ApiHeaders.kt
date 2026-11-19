package nl.giejay.android.tv.immich.api.util

import nl.giejay.android.tv.immich.shared.prefs.API_KEY
import nl.giejay.android.tv.immich.shared.prefs.CF_ACCESS_CLIENT_ID
import nl.giejay.android.tv.immich.shared.prefs.CF_ACCESS_CLIENT_SECRET
import nl.giejay.android.tv.immich.shared.prefs.PreferenceManager

/**
 * The HTTP headers every request to the Immich server has to carry:
 * the Immich API key and, when the server sits behind Cloudflare Access, a Cloudflare
 * Access service token (`CF-Access-Client-Id` / `CF-Access-Client-Secret`).
 *
 * A service token is only sent when both its id and secret are configured, otherwise
 * Cloudflare rejects the request with an incomplete-token error and the app could never
 * talk to a server without Access again.
 */
object ApiHeaders {
    const val API_KEY_HEADER = "x-api-key"
    const val CF_ACCESS_CLIENT_ID_HEADER = "CF-Access-Client-Id"
    const val CF_ACCESS_CLIENT_SECRET_HEADER = "CF-Access-Client-Secret"

    /** Headers for the currently configured server. Reads SharedPreferences, so no unit test. */
    fun fromPrefs(): Map<String, String> = build(
        PreferenceManager.get(API_KEY),
        PreferenceManager.get(CF_ACCESS_CLIENT_ID),
        PreferenceManager.get(CF_ACCESS_CLIENT_SECRET)
    )

    /**
     * Everything needed for the configured server: API key plus service token. Only use this where
     * every request goes to the Immich server itself; the OkHttp client uses [apiKey]/[serviceToken]
     * separately so a redirect to another host can't collect the service token.
     */
    fun build(apiKey: String, serviceTokenId: String, serviceTokenSecret: String): Map<String, String> =
        apiKey(apiKey) + serviceToken(serviceTokenId, serviceTokenSecret)

    /** Pure, unit tested. */
    fun apiKey(apiKey: String): Map<String, String> = mapOf(API_KEY_HEADER to apiKey.trim())

    /** Pure, unit tested; blank values disable the Cloudflare Access headers. */
    fun serviceToken(serviceTokenId: String, serviceTokenSecret: String): Map<String, String> {
        val clientId = serviceTokenId.trim()
        val clientSecret = serviceTokenSecret.trim()
        return if (clientId.isEmpty() || clientSecret.isEmpty()) {
            emptyMap()
        } else {
            linkedMapOf(
                CF_ACCESS_CLIENT_ID_HEADER to clientId,
                CF_ACCESS_CLIENT_SECRET_HEADER to clientSecret
            )
        }
    }

    fun serviceTokenFromPrefs(): Map<String, String> = serviceToken(
        PreferenceManager.get(CF_ACCESS_CLIENT_ID),
        PreferenceManager.get(CF_ACCESS_CLIENT_SECRET)
    )
}
