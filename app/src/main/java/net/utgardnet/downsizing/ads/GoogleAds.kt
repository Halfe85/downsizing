package net.utgardnet.downsizing.ads

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import net.utgardnet.downsizing.BuildConfig
import java.util.concurrent.atomic.AtomicBoolean

/** AdMob receives no custom targeting values, food, weight, routines or Health Connect records. */
object GoogleAds {
    private val initialized = AtomicBoolean(false)

    fun requestConsentAndStart(activity: Activity, onReady: () -> Unit) {
        // Release builds without real AdMob IDs never request or show test ads.
        if (!BuildConfig.DEBUG && !BuildConfig.PRODUCTION_ADS_CONFIGURED) return
        val consent = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()
        fun startIfPermitted() {
            if (!consent.canRequestAds()) return
            if (initialized.compareAndSet(false, true)) {
                Thread {
                    // Minimize persistent advertising linkage when using standard AdMob.
                    MobileAds.putPublisherFirstPartyIdEnabled(false)
                    MobileAds.initialize(activity) {
                        activity.runOnUiThread { onReady() }
                    }
                }.start()
            } else {
                activity.runOnUiThread { onReady() }
            }
        }
        consent.requestConsentInfoUpdate(activity, params, {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                startIfPermitted()
            }
        }, {
            // Only use previously valid consent when Google explicitly says ads can be requested.
            startIfPermitted()
        })
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { }
    }
}

@Composable
fun AdMobBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val view = remember {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/9214589741" else BuildConfig.BANNER_AD_UNIT_ID
            loadAd(AdRequest.Builder().build())
        }
    }
    DisposableEffect(view) {
        onDispose { view.destroy() }
    }
    AndroidView(factory = { view }, modifier = modifier.fillMaxWidth())
}
