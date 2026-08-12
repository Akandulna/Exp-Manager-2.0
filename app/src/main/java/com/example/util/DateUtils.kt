package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {

    private val datePatterns = listOf(
        "dd MMM, yyyy hh:mm a",
        "dd MMM yyyy hh:mm a",
        "dd MMM ''yy hh:mm a",
        "dd MMM 'yy hh:mm a",
        "dd MMM, yyyy HH:mm",
        "dd MMM yyyy HH:mm",
        "dd MMM ''yy HH:mm",
        "dd/MM/yyyy hh:mm a",
        "yyyy-MM-dd hh:mm a",
        "dd MMM, yyyy",
        "dd MMM yyyy",
        "dd MMM ''yy",
        "dd MMM 'yy",
        "dd/MM/yyyy",
        "yyyy-MM-dd"
    )

    fun parseToMillis(dateStr: String, timeStr: String = ""): Long {
        val cleanDate = dateStr.trim()
        val cleanTime = timeStr.trim()
        val combined = if (cleanTime.isNotBlank()) "$cleanDate $cleanTime" else cleanDate

        for (pattern in datePatterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                val parsed = sdf.parse(combined)
                if (parsed != null) {
                    return parsed.time
                }
            } catch (_: Exception) {
            }
        }

        // Secondary fallback: sanitize apostrophe or punctuation variations
        val sanitized = combined.replace("'", "").replace("’", "")
        val fallbackPatterns = listOf(
            "dd MMM yy hh:mm a",
            "dd MMM yy HH:mm",
            "dd MMM yy",
            "dd MMM, yyyy",
            "dd MMM yyyy"
        )
        for (pattern in fallbackPatterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                val parsed = sdf.parse(sanitized)
                if (parsed != null) {
                    return parsed.time
                }
            } catch (_: Exception) {
            }
        }

        return System.currentTimeMillis()
    }
}
