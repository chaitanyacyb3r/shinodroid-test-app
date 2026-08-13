package com.shinodroid.ssldemo

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shinodroid.ssldemo.ui.theme.CyberCyan
import com.shinodroid.ssldemo.ui.theme.CyberMagenta
import com.shinodroid.ssldemo.ui.theme.SSLDemoTheme
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
            SSLDemoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BypassTestScreen()
                }
            }
        }
    }
}

@Composable
fun BypassTestScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Subtle grid background effect could be added here
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            HeaderSection()
            CyberSection(title = "SSL PINNING TEST") {
                SslPinTestSection()
            }
            CyberSection(title = "ROOT DETECTION TEST") {
                RootCheckSection()
            }
            FooterSection()
        }
    }
}

@Composable
fun HeaderSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SHINODROID",
            style = MaterialTheme.typography.displaySmall,
            color = CyberCyan,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "SEC-BYPASS UTILITY v1.0",
            style = MaterialTheme.typography.labelSmall,
            color = CyberMagenta
        )
        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = CyberCyan, thickness = 1.dp)
    }
}

@Composable
fun CyberSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, CyberCyan),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "> $title",
                style = MaterialTheme.typography.titleMedium,
                color = CyberCyan,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun FooterSection() {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = "[ STATUS: SYSTEM ACTIVE ]",
            style = MaterialTheme.typography.labelSmall,
            color = CyberCyan.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun SslPinTestSection() {
    var result by remember { mutableStateOf("Ready for execution...") }
    var isLoading by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CyberButton(
            text = if (isLoading) "EXECUTING..." else "RUN PINNED REQUEST",
            onClick = {
                isLoading = true
                result = "Initializing secure connection..."
                runPinnedRequest { message ->
                    result = message
                    isLoading = false
                }
            },
            enabled = !isLoading
        )

        Text(
            text = result,
            style = MaterialTheme.typography.bodySmall,
            color = if (result.contains("SUCCESS")) Color.Green else if (result.contains("FAILED")) Color.Red else CyberCyan
        )
    }
}

@Composable
fun RootCheckSection() {
    val context = LocalContext.current
    var result by remember { mutableStateOf("Ready for detection...") }
    var isLoading by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CyberButton(
            text = if (isLoading) "ANALYZING..." else "SCAN FOR ROOT",
            onClick = {
                isLoading = true
                result = "Scanning system environment..."
                thread {
                    val report = RootDetector.checkAll(context.applicationContext)
                    val builder = StringBuilder()
                    builder.append(if (report.isLikelyRooted) "[!] SYSTEM COMPROMISED\n\n" else "[+] SYSTEM SECURE\n\n")
                    for (r in report.results) {
                        val status = if (r.positive) "DETECTED" else "CLEAN"
                        builder.append("-$ ${r.name}: $status\n")
                    }
                    val text = builder.toString()
                    (context as? ComponentActivity)?.runOnUiThread {
                        result = text
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading
        )

        Text(
            text = result,
            style = MaterialTheme.typography.bodySmall,
            color = if (result.contains("SECURE")) Color.Green else if (result.contains("COMPROMISED")) Color.Red else CyberCyan
        )
    }
}

@Composable
fun CyberButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(2.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) CyberCyan.copy(alpha = 0.1f) else Color.DarkGray,
            contentColor = if (enabled) CyberCyan else Color.Gray
        ),
        border = BorderStroke(1.dp, if (enabled) CyberCyan else Color.Gray),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge
        )
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
                onResult(msg)
            }
        } catch (e: IOException) {
            val msg = "FAILED: ${e.javaClass.simpleName}"
            onResult(msg)
        }
    }
}
