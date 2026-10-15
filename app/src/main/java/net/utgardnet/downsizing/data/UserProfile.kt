package net.utgardnet.downsizing.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

data class UserProfile(val country: String, val name: String, val dateOfBirth: LocalDate, val heightCm: Double)

data class SetupDetails(val profile: UserProfile, val weightKg: Double, val goalKg: Double) {
    fun isValid(today: LocalDate = LocalDate.now()): Boolean =
        profile.country == "NO" && profile.name.trim().length in 1..120 &&
            validBirthDate(profile.dateOfBirth, today) && profile.heightCm in 30.0..300.0 &&
            weightKg in 10.0..600.0 && goalKg in 10.0..600.0
}

fun validBirthDate(date: LocalDate, today: LocalDate = LocalDate.now()): Boolean =
    !date.isAfter(today) && !date.isBefore(today.minusYears(130))

fun parseBirthDate(value: String): LocalDate? = try {
    LocalDate.parse(value.trim(), DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT))
} catch (_: java.time.DateTimeException) { null }

fun metricNumber(value: String): Double? = value.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
