package com.shinodroid.ssldemo

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import kotlin.concurrent.thread

private const val TAG = "SSLPinTest"
private const val ROOT_TAG = "RootCheck"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BypassTestScreen()
                }
            }
        }
    }
}

@Composable
fun BypassTestScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        SslPinTestSection()
        Divider()
        RootCheckSection()
    }
}

// ---------------------------------------------------------------------
// SSL Pinning section (unchanged from your original)
// ---------------------------------------------------------------------

@Composable
fun SslPinTestSection() {
    var result by remember { mutableStateOf("Tap the button to run the request") }
    var isLoading by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "SSL Pinning Bypass Test",
            style = MaterialTheme.typography.titleLarge
        )

        Button(
            onClick = {
                isLoading = true
                result = "Running request..."
                Log.i(TAG, "=== Starting pinned request ===")
                runPinnedRequest { message ->
                    result = message
                    isLoading = false
                }
            },
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Running..." else "Run Pinned Request")
        }

        Text(text = result)
    }
}

private fun runPinnedRequest(onResult: (String) -> Unit) {
    val certificatePinner = CertificatePinner.Builder()
        .add("google.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
        .build()

    val client = OkHttpClient.Builder()
        .certificatePinner(certificatePinner)
        .build()

    val request = Request.Builder()
        .url("https://google.com")
        .build()

    thread {
        try {
            client.newCall(request).execute().use { response ->
                val msg = "SUCCESS: HTTP ${response.code}"
                Log.i(TAG, "=== RESULT: $msg ===")
                onResult(msg)
            }
        } catch (e: IOException) {
            Log.e(TAG, "=== RESULT: FAILED ===", e)

            var cause: Throwable? = e
            var depth = 0
            while (cause != null) {
                Log.e(TAG, "CAUSE[$depth]: ${cause.javaClass.name}: ${cause.message}")
                cause.stackTrace.forEach { frame ->
                    Log.e(TAG, "    at $frame")
                }
                cause = cause.cause
                depth++
            }

            val msg = "FAILED: ${e.javaClass.simpleName}\n${e.message}\n(check logcat tag \"$TAG\" for full trace)"
            onResult(msg)
        }
    }
}

// ---------------------------------------------------------------------
// Root Detection section (wraps RootDetector.kt from the previous file)
// ---------------------------------------------------------------------

@Composable
fun RootCheckSection() {
    val context = LocalContext.current
    var result by remember { mutableStateOf("Tap the button to run root detection checks") }
    var isLoading by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Root Detection Bypass Test",
            style = MaterialTheme.typography.titleLarge
        )

        Button(
            onClick = {
                isLoading = true
                result = "Running checks..."
                Log.i(ROOT_TAG, "=== Starting root checks ===")
                thread {
                    val report = RootDetector.checkAll(context.applicationContext)

                    val builder = StringBuilder()
                    builder.append(
                        if (report.isLikelyRooted) "OVERALL: LIKELY ROOTED\n\n"
                        else "OVERALL: NOT ROOTED (no signals triggered)\n\n"
                    )

                    for (r in report.results) {
                        val status = if (r.positive) "ROOTED" else "NOT ROOTED"
                        Log.i(ROOT_TAG, "${r.name} -> $status (${r.detail})")
                        builder.append("• ${r.name}: $status\n")
                        builder.append("   ${r.detail}\n\n")
                    }

                    val text = builder.toString()
                    Log.i(ROOT_TAG, "=== Root check complete ===")

                    // hop back onto the composition
                    (context as? ComponentActivity)?.runOnUiThread {
                        result = text
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Running..." else "Check Root Status")
        }

        Text(
            text = result,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Normal
        )
    }
}