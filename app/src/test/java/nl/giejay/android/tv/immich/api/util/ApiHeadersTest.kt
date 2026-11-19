package nl.giejay.android.tv.immich.api.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ApiHeadersTest {

    @Test
    fun `api key is always sent without a service token`() {
        val headers = ApiHeaders.build(" key ", "", "")

        assertThat(headers).containsExactly(ApiHeaders.API_KEY_HEADER, "key")
    }

    @Test
    fun `service token adds the cloudflare access headers`() {
        val headers = ApiHeaders.build("key", "client-id.access", "secret-value")

        assertThat(headers).containsExactly(
            ApiHeaders.API_KEY_HEADER, "key",
            ApiHeaders.CF_ACCESS_CLIENT_ID_HEADER, "client-id.access",
            ApiHeaders.CF_ACCESS_CLIENT_SECRET_HEADER, "secret-value"
        )
    }

    @Test
    fun `half a service token is ignored`() {
        assertThat(ApiHeaders.build("key", "client-id.access", "  "))
            .containsExactly(ApiHeaders.API_KEY_HEADER, "key")
        assertThat(ApiHeaders.build("key", "", "secret-value"))
            .containsExactly(ApiHeaders.API_KEY_HEADER, "key")
    }

    @Test
    fun `values are trimmed`() {
        val headers = ApiHeaders.build("key", " id ", " secret ")

        assertThat(headers[ApiHeaders.CF_ACCESS_CLIENT_ID_HEADER]).isEqualTo("id")
        assertThat(headers[ApiHeaders.CF_ACCESS_CLIENT_SECRET_HEADER]).isEqualTo("secret")
    }

    @Test
    fun `service token map is empty without a complete token`() {
        assertThat(ApiHeaders.serviceToken("", "")).isEmpty()
        assertThat(ApiHeaders.serviceToken("id", "")).isEmpty()
        assertThat(ApiHeaders.serviceToken("id", "secret")).hasSize(2)
    }
}
