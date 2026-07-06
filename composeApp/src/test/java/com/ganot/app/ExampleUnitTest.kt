package com.ganot.app

import org.junit.Assert.*
import org.junit.Test
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull

class ExampleUnitTest {
  @Test
  fun testIsGdImport() {
    val client = OkHttpClient()
    val code = "6S2OB27"
    try {
        val request = Request.Builder()
            .url("https://is.gd/$code")
            .get()
            .build()
            
        client.newCall(request).execute().use { response ->
            val body = response.body?.string()?.trim() ?: ""
            println("--- IS.GD DIRECT REDIRECT IMPORT RESULT: Code=${response.code}, Body=$body")
        }
    } catch (e: Exception) {
        println("--- IS.GD DIRECT REDIRECT IMPORT ERROR: ${e.message}")
    }
  }
}






















