package com.example.bymevpn.components

internal fun formatBitrate(bps: Long, isRussian: Boolean): String {
    if (bps <= 0L) return if (isRussian) "0.0 Кбит/с" else "0.0 Kbps"
    val kbit = (bps * 8) / 1024.0
    val mbit = kbit / 1024.0
    val unitM = if (isRussian) "Мбит/с" else "Mbps"
    val unitK = if (isRussian) "Кбит/с" else "Kbps"
    return if (mbit >= 1.0) {
        String.format(java.util.Locale.US, "%.1f %s", mbit, unitM)
    } else if (kbit < 1.0) {
        String.format(java.util.Locale.US, "%.1f %s", kbit, unitK)
    } else {
        String.format(java.util.Locale.US, "%.0f %s", kbit, unitK)
    }
}

internal fun formatVolume(totalBytes: Long, isConnected: Boolean, isRussian: Boolean): String {
    val unitKb = if (isRussian) "КБ" else "KB"
    val unitMb = if (isRussian) "МБ" else "MB"
    val unitGb = if (isRussian) "ГБ" else "GB"
    if (!isConnected || totalBytes <= 0L) return "0 $unitMb"
    if (totalBytes < 1024L * 1024L) {
        val kb = (totalBytes / 1024.0).coerceAtLeast(1.0)
        return String.format(java.util.Locale.US, "%.0f %s", kb, unitKb)
    }
    val mb = totalBytes.toDouble() / (1024.0 * 1024.0)
    return if (mb < 1000.0) {
        String.format(java.util.Locale.US, "%.1f %s", mb, unitMb)
    } else {
        val gb = mb / 1024.0
        String.format(java.util.Locale.US, "%.2f %s", gb, unitGb)
    }
}
