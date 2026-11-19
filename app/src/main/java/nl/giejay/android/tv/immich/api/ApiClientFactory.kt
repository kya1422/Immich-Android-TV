package nl.giejay.android.tv.immich.api

import nl.giejay.android.tv.immich.api.interceptor.ResponseLoggingInterceptor
import nl.giejay.android.tv.immich.api.util.UnsafeOkHttpClient
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.IOException

/** Thrown when Cloudflare Access answers with its login page instead of the Immich server. */
class CloudflareAccessBlockedException :
    IOException("Cloudflare Access blocked this request, a service token is missing or invalid")

private const val CLOUDFLARE_ACCESS_HOST_SUFFIX = "cloudflareaccess.com"

object ApiClientFactory {

    /**
     * [headers] are added to every request, e.g. the Immich API key.
     *
     * [hostScopedHeaders] are added only when the request goes to [hostScopedHost] - used for the
     * Cloudflare Access service token so a redirect (or a URL pointing somewhere else entirely)
     * can't hand the token to another host.
     */
    fun getClient(
        disableSsl: Boolean,
        headers: Map<String, String>,
        debugMode: Boolean,
        hostScopedHeaders: Map<String, String> = emptyMap(),
        hostScopedHost: String? = null
    ): OkHttpClient {
        val headerInterceptor = interceptor(headers, hostScopedHeaders, hostScopedHost)
        val builder = if (disableSsl)
            UnsafeOkHttpClient.unsafeOkHttpClient()
        else OkHttpClient.Builder()
        builder.addInterceptor(headerInterceptor)
        return if(debugMode){
            builder.addInterceptor(ResponseLoggingInterceptor()).build()
        } else
            builder.build()
    }

    /** True for the login page Cloudflare Access redirects unauthenticated requests to. */
    internal fun isCloudflareAccessLoginUrl(url: HttpUrl?): Boolean =
        url?.host?.endsWith(CLOUDFLARE_ACCESS_HOST_SUFFIX, ignoreCase = true) == true

    internal fun headersFor(
        requestUrlHost: String?,
        headers: Map<String, String>,
        hostScopedHeaders: Map<String, String>,
        hostScopedHost: String?
    ): Map<String, String> {
        val scopedHostMatches = hostScopedHost != null && requestUrlHost.equals(hostScopedHost, ignoreCase = true)
        return if (scopedHostMatches) headers + hostScopedHeaders else headers
    }

    private fun interceptor(
        headers: Map<String, String>,
        hostScopedHeaders: Map<String, String>,
        hostScopedHost: String?
    ): Interceptor = Interceptor { chain ->
        try {
            val request = chain.request()
            val requestHeaders = headersFor(request.url.host, headers, hostScopedHeaders, hostScopedHost)
            val newRequest = request.newBuilder().apply {
                requestHeaders.forEach { (name, value) -> addHeader(name, value) }
            }.build()
            val response = chain.proceed(newRequest)
            // OkHttp follows the Access redirect, so without a (valid) service token the app would
            // otherwise try to parse the Access login page as JSON
            if (isCloudflareAccessLoginUrl(response.request.url)) {
                response.close()
                throw CloudflareAccessBlockedException()
            }
            response
        } catch (e: Exception) {
            Timber.e(e, "Error adding request headers, invalid format")
            chain.proceed(chain.request()) // Proceed with the original request if there's an error
        }
    }
}
