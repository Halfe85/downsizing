package net.utgardnet.downsizing

import android.app.Activity
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView

class PrivacyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = TextView(this).apply {
            textSize = 17f
            setPadding(36, 48, 36, 48)
            text = "Downsizing privacy\n\nWeight and food logs are saved on this phone. " +
                "Health Connect access is optional and only used to display steps, " +
                "heart rate, exercise, and energy expenditure in the app. " +
                "We do not pass Health Connect or nutrition information to advertisers.\n\n" +
                "If enabled, Google AdMob separately processes device and network data " +
                "for advertisements under Google's policies. You can use Downsizing " +
                "without granting Health Connect permissions."
        }
        setContentView(ScrollView(this).apply { addView(text) })
    }
}
