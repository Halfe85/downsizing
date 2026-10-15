package net.utgardnet.downsizing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.utgardnet.downsizing.ui.DownsizingTheme

class PrivacyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DownsizingTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Downsizing privacy", style = MaterialTheme.typography.headlineMedium)
                        Text("Your name, date of birth, country, measurements, goals and personal logs are saved on this phone. They are never used for advertising targeting.")
                        Text("Health Connect access is optional and used only to display steps, heart rate, exercise and energy expenditure. We do not pass this information to advertisers.")
                        Text("If enabled, Google AdMob separately processes device and network data for advertisements under Google's policies. You can use Downsizing without granting Health Connect permissions.")
                        Text("Reference food information is stored separately from your profile and meal history.")
                        TextButton(onClick = { finish() }) { Text("Close") }
                    }
                }
            }
        }
    }
}
