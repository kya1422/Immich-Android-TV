package nl.giejay.android.tv.immich.api

import com.google.common.truth.Truth.assertThat
import nl.giejay.android.tv.immich.api.util.ApiHeaders
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Test

class ApiClientFactoryHeadersTest {

    private val apiKey = ApiHeaders.apiKey("key")
    private val serviceToken = ApiHeaders.serviceToken("client-id.access", "secret-value")

    @Test
    fun `service token is added for the configured server host`() {
        val headers = ApiClientFactory.headersFor("immich.example.com", apiKey, serviceToken, "immich.example.com")

        assertThat(headers).containsKey(ApiHeaders.CF_ACCESS_CLIENT_ID_HEADER)
        assertThat(headers).containsKey(ApiHeaders.CF_ACCESS_CLIENT_SECRET_HEADER)
    }

    @Test
    fun `service token is not added for another host`() {
        val headers = ApiClientFactory.headersFor("other.example.com", apiKey, serviceToken, "immich.example.com")

        assertThat(headers).containsExactly(ApiHeaders.API_KEY_HEADER, "key")
    }

    @Test
    fun `host comparison ignores case and missing host`() {
        assertThat(ApiClientFactory.headersFor("IMMICH.example.com", apiKey, serviceToken, "immich.EXAMPLE.com"))
            .containsKey(ApiHeaders.CF_ACCESS_CLIENT_ID_HEADER)
        assertThat(ApiClientFactory.headersFor(null, apiKey, serviceToken, "immich.example.com"))
            .containsExactly(ApiHeaders.API_KEY_HEADER, "key")
        assertThat(ApiClientFactory.headersFor("immich.example.com", apiKey, serviceToken, null))
            .containsExactly(ApiHeaders.API_KEY_HEADER, "key")
    }

    @Test
    fun `cloudflare access login page is recognised`() {
        assertThat(
            ApiClientFactory.isCloudflareAccessLoginUrl(
                "https://my-team.cloudflareaccess.com/cdn-cgi/access/login/immich.example.com".toHttpUrl()
            )
        ).isTrue()
        assertThat(
            ApiClientFactory.isCloudflareAccessLoginUrl("https://immich.example.com/api/server/ping".toHttpUrl())
        ).isFalse()
        assertThat(ApiClientFactory.isCloudflareAccessLoginUrl(null)).isFalse()
    }
}
