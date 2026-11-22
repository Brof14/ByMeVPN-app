package com.example.bymevpn

import com.example.bymevpn.components.formatBitrate
import com.example.bymevpn.components.formatVolume
import org.junit.Assert.assertEquals
import org.junit.Test

class LiveSpeedWaveTelemetryTest {

    @Test
    fun zeroBpsFormatsCorrectlyInRussianAndEnglish() {
        assertEquals("0.0 Кбит/с", formatBitrate(0L, isRussian = true))
        assertEquals("0.0 Kbps", formatBitrate(0L, isRussian = false))
        assertEquals("0.0 Кбит/с", formatBitrate(-10L, isRussian = true))
    }

    @Test
    fun subMegabitFormatsInKilobits() {
        // 12.5 KB/s = 100 kbit/s
        val bps = (100 * 1024) / 8L
        val formattedRu = formatBitrate(bps, isRussian = true)
        val formattedEn = formatBitrate(bps, isRussian = false)

        assertEquals("100 Кбит/с", formattedRu)
        assertEquals("100 Kbps", formattedEn)
    }

    @Test
    fun megabitSpeedFormatsWithDecimalPrecision() {
        // 2.5 Mbit/s
        val bps = ((2.5 * 1024 * 1024) / 8).toLong()
        val formattedRu = formatBitrate(bps, isRussian = true)
        val formattedEn = formatBitrate(bps, isRussian = false)

        assertEquals("2.5 Мбит/с", formattedRu)
        assertEquals("2.5 Mbps", formattedEn)
    }

    @Test
    fun zeroOrDisconnectedVolumeFormatsSafely() {
        assertEquals("0 МБ", formatVolume(0L, isConnected = false, isRussian = true))
        assertEquals("0 MB", formatVolume(0L, isConnected = true, isRussian = false))
        assertEquals("0 MB", formatVolume(100_000_000L, isConnected = false, isRussian = false))
    }

    @Test
    fun megabyteVolumeFormatsCorrectly() {
        val bytes = 150L * 1024L * 1024L // 150 MB
        val formattedRu = formatVolume(bytes, isConnected = true, isRussian = true)
        val formattedEn = formatVolume(bytes, isConnected = true, isRussian = false)

        assertEquals("150.0 МБ", formattedRu)
        assertEquals("150.0 MB", formattedEn)
    }

    @Test
    fun gigabyteVolumeTransitionsAt1000Mb() {
        val bytes = (2.45 * 1024.0 * 1024.0 * 1024.0).toLong() // ~2.45 GB
        val formattedRu = formatVolume(bytes, isConnected = true, isRussian = true)
        val formattedEn = formatVolume(bytes, isConnected = true, isRussian = false)

        assertEquals("2.45 ГБ", formattedRu)
        assertEquals("2.45 GB", formattedEn)
    }
}
