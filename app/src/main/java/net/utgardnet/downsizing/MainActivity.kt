package net.utgardnet.downsizing

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.utgardnet.downsizing.ads.AdMobBanner
import net.utgardnet.downsizing.ads.GoogleAds
import net.utgardnet.downsizing.data.*
import net.utgardnet.downsizing.health.HealthReader
import net.utgardnet.downsizing.health.HealthSummary
import net.utgardnet.downsizing.reminders.Reminders
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    private lateinit var store: LocalStore
    private lateinit var reader: HealthReader
    private var day by mutableStateOf(DaySnapshot(emptyList(), emptyList(), emptyList(), emptyList()))
    private var health by mutableStateOf(HealthSummary())
    private var adsReady by mutableStateOf(false)
    private var goalKg by mutableFloatStateOf(0f)
    private var dailyBurn by mutableIntStateOf(0)
    private var remindersOn by mutableStateOf(false)
    private var errorText by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = LocalStore(applicationContext)
        reader = HealthReader(applicationContext)
        goalKg = store.goalWeight
        dailyBurn = store.dailyBurnTarget
        remindersOn = store.remindersEnabled
        lifecycleScope.launch {
            refresh()
            refreshHealth()
        }
        GoogleAds.requestConsentAndStart(this) { adsReady = true }
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) { DownsizingApp() }
        }
    }

    private suspend fun refresh() {
        day = withContext(Dispatchers.IO) { store.snapshot() }
    }

    private suspend fun refreshHealth() {
        health = reader.readToday()
    }

    private fun write(action: () -> Unit) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { action() }
                errorText = null
                refresh()
            } catch (e: Exception) {
                errorText = "Unable to save. Check the entered values."
            }
        }
    }

    @Composable
    private fun DownsizingApp() {
        var tab by rememberSaveable { mutableIntStateOf(0) }
        val tabs = listOf("Today", "Food", "Move", "Weight", "Settings")
        val permissionLauncher = rememberLauncherForActivityResult(
            HealthReader.permissionContract()
        ) { lifecycleScope.launch { refreshHealth() } }
        val notificationLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                remindersOn = true
                store.remindersEnabled = true
                Reminders.setEnabled(this, true)
            }
        }
        Scaffold(
            topBar = {
                Surface(tonalElevation = 3.dp) {
                    Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Downsizing", style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold)
                            Text("A healthier routine, one day at a time",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            bottomBar = {
                Column {
                    if (adsReady) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { AdMobBanner() }
                    }
                    NavigationBar {
                        tabs.forEachIndexed { index, name ->
                            NavigationBarItem(
                                selected = tab == index,
                                onClick = { tab = index },
                                icon = { Text(listOf("◉", "▣", "➜", "▴", "⚙")[index]) },
                                label = { Text(name) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                errorText?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                when (tab) {
                    0 -> TodayPanel(day, health, dailyBurn, goalKg,
                        onConnect = { if (reader.available()) permissionLauncher.launch(HealthReader.permissions) },
                        onRefresh = { lifecycleScope.launch { refreshHealth(); refresh() } })
                    1 -> FoodPanel(day,
                        onAdd = { name, kcal -> write { store.addFood(name, kcal) } },
                        onDelete = { id -> write { store.deleteEntry("foods", id) } })
                    2 -> MovementPanel(day, health,
                        onAdd = { name, mins, kcal -> write { store.addActivity(name, mins, kcal) } },
                        onRoutine = { name -> write { store.addRoutine(name) } },
                        onChecked = { id, checked -> write { store.setRoutineDone(id, checked) } },
                        onDeleteActivity = { id -> write { store.deleteEntry("activities", id) } })
                    3 -> WeightPanel(day, goalKg,
                        onAdd = { kg -> write { store.addWeight(kg) } },
                        onGoal = { kg -> goalKg = kg; store.goalWeight = kg },
                        onDelete = { id -> write { store.deleteEntry("weights", id) } })
                    else -> SettingsPanel(health, dailyBurn, remindersOn,
                        onDailyBurn = { value -> dailyBurn = value; store.dailyBurnTarget = value },
                        onConnect = { if (reader.available()) permissionLauncher.launch(HealthReader.permissions) },
                        onReminder = { enable ->
                            if (enable && Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else {
                                remindersOn = enable
                                store.remindersEnabled = enable
                                Reminders.setEnabled(this, enable)
                            }
                        },
                        onPrivacy = { GoogleAds.showPrivacyOptions(this) })
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        })
    }
}

@Composable
private fun ValueRow(name: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(name)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TodayPanel(
    data: DaySnapshot, health: HealthSummary, dailyBurn: Int, goalKg: Float,
    onConnect: () -> Unit, onRefresh: () -> Unit
) {
    Text("Today's overview", style = MaterialTheme.typography.titleLarge)
    Section("Energy balance") {
        val intakeAvailable = data.foods.isNotEmpty()
        val outgoing = health.totalBurnKcal ?: dailyBurn.takeIf { it > 0 }?.let {
            (it + data.manualCaloriesOut).toDouble()
        }
        ValueRow("Food logged", if (intakeAvailable) data.caloriesIn.toString() + " kcal" else "No meals yet")
        ValueRow("Total daily expenditure", outgoing?.toInt()?.toString()?.plus(" kcal") ?: "Not available")
        if (outgoing != null && intakeAvailable) {
            val balance = data.caloriesIn - outgoing.toInt()
            ValueRow("Estimated net balance", (if (balance > 0) "+" else "") + balance + " kcal")
            Text("A daily estimate, not a measure of actual fat gain or loss.",
                style = MaterialTheme.typography.bodySmall)
        } else {
            Text("Add food and connect Health Connect or set a daily energy estimate to compare intake.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
    Section("Movement and health") {
        ValueRow("Steps", health.steps?.toString() ?: "—")
        ValueRow("Latest recorded pulse", health.heartRateBpm?.toString()?.plus(" bpm") ?: "—")
        ValueRow("Today's exercise sessions", health.workouts?.toString() ?: "—")
        Text(health.status, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onConnect) { Text("Connect") }
            OutlinedButton(onClick = onRefresh) { Text("Refresh") }
        }
    }
    Section("Weight trend") {
        val latest = data.weights.firstOrNull()
        ValueRow("Latest weight", latest?.kg?.let { String.format("%.1f kg", it) } ?: "No entry")
        ValueRow("Target weight", if (goalKg > 0) String.format("%.1f kg", goalKg) else "Not set")
        if (data.weights.size >= 2) {
            val recent = data.weights.take(14)
            val change = recent.first().kg - recent.last().kg
            ValueRow("Change over last " + recent.size + " entries",
                (if (change > 0) "+" else "") + String.format("%.1f kg", change))
        }
    }
    Section("Daily routines") {
        if (data.routines.isEmpty()) Text("No routines added yet.")
        else {
            ValueRow("Completed", data.routines.count { it.done }.toString() + "/" + data.routines.size)
        }
    }
}

@Composable
private fun FoodPanel(
    data: DaySnapshot, onAdd: (String, Int) -> Unit, onDelete: (Long) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var kcal by rememberSaveable { mutableStateOf("") }
    Text("Food and calories", style = MaterialTheme.typography.titleLarge)
    Section("Log a meal") {
        OutlinedTextField(name, { name = it }, label = { Text("Food or meal name") },
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(kcal, { kcal = it }, label = { Text("Calories (kcal)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        Button(onClick = {
            kcal.toIntOrNull()?.takeIf { it in 0..10000 }?.let {
                if (name.isNotBlank()) { onAdd(name, it); name = ""; kcal = "" }
            }
        }, enabled = name.isNotBlank() && kcal.toIntOrNull() in 0..10000) { Text("Add food") }
    }
    Section("Today's food • " + data.caloriesIn + " kcal") {
        if (data.foods.isEmpty()) Text("Nothing logged. Later we will add the searchable Norwegian food catalog.")
        data.foods.forEach { entry ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(entry.name, fontWeight = FontWeight.Medium)
                    Text(entry.kcal.toString() + " kcal", style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = { onDelete(entry.id) }) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun MovementPanel(
    data: DaySnapshot, health: HealthSummary,
    onAdd: (String, Int, Int) -> Unit, onRoutine: (String) -> Unit,
    onChecked: (Long, Boolean) -> Unit, onDeleteActivity: (Long) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableStateOf("") }
    var calories by rememberSaveable { mutableStateOf("") }
    var routine by rememberSaveable { mutableStateOf("") }
    Text("Movement and routines", style = MaterialTheme.typography.titleLarge)
    Section("Activity") {
        ValueRow("Steps from Health Connect", health.steps?.toString() ?: "Not connected")
        Text("Workout calories are estimates. If Health Connect already includes your exercise, " +
            "Downsizing will not add manually logged activity on top of that total.",
            style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(name, { name = it }, label = { Text("Activity name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(minutes, { minutes = it }, label = { Text("Duration (minutes)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(calories, { calories = it }, label = { Text("Estimated kcal burned") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        val mins = minutes.toIntOrNull()
        val kcals = calories.toIntOrNull()
        Button(onClick = {
            if (mins != null && kcals != null && name.isNotBlank()) {
                onAdd(name, mins, kcals); name = ""; minutes = ""; calories = ""
            }
        }, enabled = name.isNotBlank() && mins != null && mins in 1..1440 &&
            kcals != null && kcals in 0..20000) { Text("Save activity") }
        data.activities.forEach {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(it.name + " • " + it.minutes + " min • " + it.kcal + " kcal", Modifier.weight(1f))
                TextButton(onClick = { onDeleteActivity(it.id) }) { Text("Delete") }
            }
        }
    }
    Section("Today's routine checklist") {
        OutlinedTextField(routine, { routine = it }, label = { Text("Routine (e.g. evening walk)") },
            modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            if (routine.isNotBlank()) { onRoutine(routine); routine = "" }
        }, enabled = routine.isNotBlank()) { Text("Add routine") }
        data.routines.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = item.done, onCheckedChange = { onChecked(item.id, it) })
                Text(item.name)
            }
        }
    }
}

@Composable
private fun WeightPanel(
    data: DaySnapshot, goalKg: Float, onAdd: (Double) -> Unit,
    onGoal: (Float) -> Unit, onDelete: (Long) -> Unit
) {
    var input by rememberSaveable { mutableStateOf("") }
    var target by rememberSaveable { mutableStateOf("") }
    Text("Weight tracking", style = MaterialTheme.typography.titleLarge)
    Section("Record weight") {
        OutlinedTextField(input, { input = it }, label = { Text("Weight in kg") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
        val kg = input.replace(',', '.').toDoubleOrNull()
        Button(onClick = { if (kg != null) { onAdd(kg); input = "" } },
            enabled = kg != null && kg in 10.0..600.0) { Text("Save weight") }
    }
    Section("Target weight") {
        Text(if (goalKg > 0) "Current goal: " + String.format("%.1f kg", goalKg) else "No target set")
        OutlinedTextField(target, { target = it }, label = { Text("Goal in kg") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
        val value = target.replace(',', '.').toFloatOrNull()
        Button(onClick = { if (value != null) { onGoal(value); target = "" } },
            enabled = value != null && value in 10f..600f) { Text("Save target") }
    }
    Section("Weight history") {
        if (data.weights.isEmpty()) Text("Your recorded weights will appear here.")
        data.weights.forEach { entry ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(entry.day + " • " + String.format("%.1f kg", entry.kg), Modifier.weight(1f))
                TextButton(onClick = { onDelete(entry.id) }) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun SettingsPanel(
    health: HealthSummary, dailyBurn: Int, remindersOn: Boolean,
    onDailyBurn: (Int) -> Unit, onConnect: () -> Unit,
    onReminder: (Boolean) -> Unit, onPrivacy: () -> Unit
) {
    var estimate by rememberSaveable { mutableStateOf("") }
    Text("Settings and privacy", style = MaterialTheme.typography.titleLarge)
    Section("Energy estimates") {
        Text("Daily estimated total calories burned: " +
            if (dailyBurn > 0) dailyBurn.toString() + " kcal" else "not set")
        Text("Used only when Health Connect doesn't supply total calories burned. " +
            "Consult a qualified professional for individual energy requirements.",
            style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(estimate, { estimate = it }, label = { Text("Estimated daily kcal burned") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        val value = estimate.toIntOrNull()
        Button(onClick = { if (value != null) { onDailyBurn(value); estimate = "" } },
            enabled = value != null && value in 100..10000) { Text("Save estimate") }
    }
    Section("Watch and phone health data") {
        Text(health.status)
        Text("Connect compatible apps like Samsung Health through Android Health Connect. " +
            "Read permissions are optional, and nothing from Health Connect is sent to AdMob.",
            style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = onConnect) { Text("Connect Health Connect") }
    }
    Section("Notifications") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Daily check-in reminder", Modifier.weight(1f))
            Switch(checked = remindersOn, onCheckedChange = onReminder)
        }
        Text("Local notification, typically once daily. Android chooses the precise delivery time.",
            style = MaterialTheme.typography.bodySmall)
    }
    Section("Advertising and data") {
        Text("Your food, weight, activity, routines, and goals remain in local app storage. " +
            "AdMob separately processes advertising-related data and may use device identifiers. " +
            "Health information is never passed as ad targeting data.",
            style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = onPrivacy) { Text("Ad privacy choices") }
    }
}
