package net.utgardnet.downsizing

import net.utgardnet.downsizing.data.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ProfileValidationTest {
    @Test fun datesAreStrictAndCannotBeInTheFuture() {
        assertNull(parseBirthDate("31.02.2000"))
        assertNull(parseBirthDate("24.06.90"))
        assertEquals(LocalDate.of(2000, 2, 29), parseBirthDate("29.02.2000"))
        assertFalse(validBirthDate(LocalDate.of(2027, 1, 1), LocalDate.of(2026, 10, 15)))
        assertFalse(validBirthDate(LocalDate.of(1800, 1, 1)))
    }
    @Test fun metricValuesAcceptNorwegianDecimalsAndRejectNonFiniteValues() {
        assertEquals(82.5, metricNumber("82,5")!!, 0.0)
        assertEquals(172.5, metricNumber("172.5")!!, 0.0)
        assertNull(metricNumber("NaN"))
        assertNull(metricNumber("Infinity"))
        assertNull(metricNumber("abc"))
    }
}
