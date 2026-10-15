package net.utgardnet.downsizing.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant

data class HealthSummary(
    val status: String = "Not connected",
    val steps: Long? = null,
    val heartRateBpm: Long? = null,
    val totalBurnKcal: Double? = null,
    val workouts: Int? = null
)

/** Read-only Android Health Connect. Never send these records to the ad component. */
class HealthReader(private val context: Context) {
    companion object {
        val permissions = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class)
        )
        fun permissionContract() = PermissionController.createRequestPermissionResultContract()
    }

    fun available(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    suspend fun readToday(): HealthSummary = withContext(Dispatchers.IO) {
        if (!available()) return@withContext HealthSummary(status = "Health Connect unavailable")
        try {
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            if (!granted.containsAll(permissions)) {
                return@withContext HealthSummary(status = "Connect to Health Connect")
            }
            val now = Instant.now()
            val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
            val range = TimeRangeFilter.between(start, now)
            val aggregate = client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL, TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                    timeRangeFilter = range
                )
            )
            val pulseRecords = client.readRecords(
                ReadRecordsRequest(HeartRateRecord::class, timeRangeFilter = range, pageSize = 500)
            )
            val mostRecentPulse = pulseRecords.records.flatMap { it.samples }
                .maxByOrNull { it.time }?.beatsPerMinute
            val sessions = client.readRecords(
                ReadRecordsRequest(ExerciseSessionRecord::class, timeRangeFilter = range, pageSize = 500)
            )
            HealthSummary(
                status = "Connected",
                steps = aggregate[StepsRecord.COUNT_TOTAL] ?: 0,
                heartRateBpm = mostRecentPulse,
                totalBurnKcal = aggregate[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories,
                workouts = sessions.records.size
            )
        } catch (e: Exception) {
            HealthSummary(status = "Health Connect: permission or read error")
        }
    }
}
