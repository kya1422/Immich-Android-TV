package nl.giejay.android.tv.immich.shared

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import nl.giejay.android.tv.immich.api.ApiClientFactory
import nl.giejay.android.tv.immich.api.util.ApiHeaders
import nl.giejay.android.tv.immich.shared.prefs.API_KEY
import nl.giejay.android.tv.immich.shared.prefs.CF_ACCESS_CLIENT_ID
import nl.giejay.android.tv.immich.shared.prefs.CF_ACCESS_CLIENT_SECRET
import nl.giejay.android.tv.immich.shared.prefs.DEBUG_MODE
import nl.giejay.android.tv.immich.shared.prefs.DISABLE_SSL_VERIFICATION
import nl.giejay.android.tv.immich.shared.prefs.LiveSharedPreferences
import nl.giejay.android.tv.immich.shared.prefs.PreferenceManager
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.InputStream


@GlideModule
class CustomGlideModule : AppGlideModule() {
    private var factory: OkHttpUrlLoader.Factory = OkHttpUrlLoader.Factory()
    private var registry: Registry? = null
    private val prefs: LiveSharedPreferences =
        LiveSharedPreferences(PreferenceManager.sharedPreference)

    init {
        // the Cloudflare Access token is part of the request headers, so a changed token needs a
        // new client as well
        prefs.listenMultiple(
            listOf(
                API_KEY.key(),
                DEBUG_MODE.key(),
                DISABLE_SSL_VERIFICATION.key(),
                CF_ACCESS_CLIENT_ID.key(),
                CF_ACCESS_CLIENT_SECRET.key()
            )
        ).observeForever {
            // listenMultiple's raw values are still encrypted, so read the headers via prefs again
            reloadFactory(it[DISABLE_SSL_VERIFICATION.key()] == true)
        }
    }

    private fun reloadFactory(disableSsl: Boolean) {
        factory = if (PreferenceManager.isLoggedId()) {
            OkHttpUrlLoader.Factory(
                ApiClientFactory.getClient(
                    disableSsl,
                    ApiHeaders.apiKey(PreferenceManager.get(API_KEY)),
                    // never enable debug mode of responses in glide, too much data
                    false,
                    ApiHeaders.serviceTokenFromPrefs(),
                    PreferenceManager.hostName.toHttpUrlOrNull()?.host
                )
            )
        } else {
            OkHttpUrlLoader.Factory()
        }
        this.registry?.replace(
            GlideUrl::class.java,
            InputStream::class.java, factory
        )
    }

    override fun applyOptions(context: Context, builder: GlideBuilder) {
    }

    override fun registerComponents(
        context: Context, glide: Glide, registry: Registry
    ) {
        this.registry = registry
        // override default loader with one that attaches headers
        registry.replace(
            GlideUrl::class.java,
            InputStream::class.java, factory
        )
    }
}