package com.example.nubo.utils

// --- Utils for safe logging ---
// Mask long tokens to avoid leaking secrets in logs
private fun String.maskToken(): String {
    if (this.length <= 12) return "***"
    val head = this.take(6)
    val tail = this.takeLast(6)
    return "$head***$tail"
}

// Pretty-print any object via Gson (only for debug)
private val gson by lazy { com.google.gson.GsonBuilder().setPrettyPrinting().create() }
private fun Any?.toPrettyJson(): String = try { gson.toJson(this) } catch (_: Exception) { this.toString() }
