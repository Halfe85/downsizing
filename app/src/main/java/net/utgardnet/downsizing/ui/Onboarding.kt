package net.utgardnet.downsizing.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import net.utgardnet.downsizing.R
import net.utgardnet.downsizing.data.*

@Composable
fun WelcomeBackground() {
    Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(painterResource(R.drawable.ic_health), contentDescription = null, modifier = Modifier.size(108.dp))
            Text("Downsizing", style = MaterialTheme.typography.headlineLarge)
            Text("A healthier routine, one day at a time", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun OnboardingFlow(saving: Boolean, error: String?, onComplete: (SetupDetails) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var countrySelected by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var birthDate by rememberSaveable { mutableStateOf("") }
    var height by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var goal by rememberSaveable { mutableStateOf("") }
    val date = parseBirthDate(birthDate)
    val heightCm = metricNumber(height)
    val weightKg = metricNumber(weight)
    val goalKg = metricNumber(goal)
    val valid = when (step) {
        0 -> countrySelected
        1 -> name.trim().length in 1..120
        2 -> date != null && validBirthDate(date)
        3 -> heightCm != null && heightCm in 30.0..300.0 && weightKg != null && weightKg in 10.0..600.0
        else -> goalKg != null && goalKg in 10.0..600.0
    }
    Dialog(onDismissRequest = {}, properties = DialogProperties(
        dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false
    )) {
        BackHandler(enabled = step > 0 && !saving) { step-- }
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.imePadding().safeDrawingPadding().padding(16.dp)
                .widthIn(max = 520.dp).fillMaxWidth().heightIn(max = 640.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Welcome to Downsizing", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
                Text("Step ${step + 1} of 5", style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(progress = { (step + 1) / 5f }, modifier = Modifier.fillMaxWidth())
                Text(listOf("Choose your country", "What is your name?", "Your date of birth",
                    "Your measurements", "Your weight goal")[step], style = MaterialTheme.typography.headlineSmall)
                when (step) {
                    0 -> {
                        Text("Choose your country to get started.")
                        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
                            Row(Modifier.fillMaxWidth().selectable(selected = countrySelected,
                                role = Role.RadioButton, onClick = { countrySelected = true }).padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Image(painterResource(R.drawable.flag_norway), "Flag of Norway", Modifier.size(44.dp, 32.dp))
                                Text("Norway", Modifier.weight(1f))
                                RadioButton(selected = countrySelected, onClick = null)
                            }
                        }
                        Text("More countries will be added later.", style = MaterialTheme.typography.bodySmall)
                    }
                    1 -> SetupField(name, { name = it.take(120) }, "Name", KeyboardType.Text,
                        "The name you would like to use in the app.")
                    2 -> SetupField(birthDate, { birthDate = it.take(10) }, "Date of birth", KeyboardType.Text,
                        "DD.MM.YYYY — for example 24.06.1990", birthDate.isNotEmpty() && (date == null || !validBirthDate(date)))
                    3 -> {
                        Text("We use metric units: centimetres and kilograms.")
                        SetupField(height, { height = it.take(10) }, "Height (cm)", KeyboardType.Decimal,
                            "Enter a height from 30 to 300 cm.", height.isNotEmpty() && (heightCm == null || heightCm !in 30.0..300.0))
                        SetupField(weight, { weight = it.take(10) }, "Weight (kg)", KeyboardType.Decimal,
                            "Enter a weight from 10 to 600 kg.", weight.isNotEmpty() && (weightKg == null || weightKg !in 10.0..600.0))
                    }
                    4 -> {
                        SetupField(goal, { goal = it.take(10) }, "Weight goal (kg)", KeyboardType.Decimal,
                            "Enter a goal from 10 to 600 kg.", goal.isNotEmpty() && (goalKg == null || goalKg !in 10.0..600.0), enabled = !saving)
                        Text("You can change your goal later.", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text("Your profile stays on this phone. We do not use it for ad targeting.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    if (step > 0) TextButton(onClick = { step-- }, enabled = !saving) { Text("Back") }
                    else Spacer(Modifier.width(1.dp))
                    Button(enabled = valid && !saving, onClick = {
                        if (step < 4) step++
                        else if (date != null && heightCm != null && weightKg != null && goalKg != null) {
                            val details = SetupDetails(UserProfile("NO", name.trim(), date, heightCm), weightKg, goalKg)
                            if (details.isValid()) onComplete(details)
                        }
                    }) { Text(if (saving) "Saving…" else "Next") }
                }
            }
        }
    }
}

@Composable
private fun SetupField(value: String, onChange: (String) -> Unit, label: String,
    keyboard: KeyboardType, help: String, invalid: Boolean = false, enabled: Boolean = true) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) },
        modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard), isError = invalid,
        supportingText = { Text(help) })
}
