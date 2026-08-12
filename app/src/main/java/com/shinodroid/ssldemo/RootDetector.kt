package com.shinodroid.ssldemo

import android.content.Context
import android.content.pm.PackageManager
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Utility object that performs a handful of common, well-documented
 * root-detection heuristics. None of these checks are foolproof on their
 * own (that's why real apps combine several signals), but together they
 * give a reasonable signal for basic root/tamper detection.
 *
 * Usage:
 *   val result = RootDetector.checkAll(context)
 *   if (result.isLikelyRooted) { ... }
 */
object RootDetector {

    /** Result of a single check. */
    data class CheckResult(
        val name: String,
        val positive: Boolean,
        val detail: String
    )

    /** Aggregate result of running every check. */
    data class RootCheckReport(
        val results: List<CheckResult>
    ) {
        val isLikelyRooted: Boolean get() = results.any { it.positive }
    }

    // ---------------------------------------------------------------
    // 1. Package Detection
    // ---------------------------------------------------------------

    private val ROOT_APP_PACKAGES = listOf(
        "com.topjohnwu.magisk",
        "eu.chainfire.supersu",
        "com.noshufou.android.su",
        "com.koushikdutta.superuser",
        "com.thirdparty.superuser"
    )

    fun checkRootPackages(context: Context): CheckResult {
        val pm = context.packageManager
        val found = mutableListOf<String>()
        for (pkg in ROOT_APP_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES)
                found.add(pkg)
            } catch (e: PackageManager.NameNotFoundException) {
                // not installed, expected in the common case
            }
        }
        return CheckResult(
            name = "Root Management App Detection",
            positive = found.isNotEmpty(),
            detail = if (found.isNotEmpty())
                "Found: ${found.joinToString(", ")}"
            else
                "No known root-management packages found"
        )
    }

    // ---------------------------------------------------------------
    // 2. Binary Detection
    // ---------------------------------------------------------------

    private val ROOT_BINARY_PATHS = listOf(
        "/system/bin/",
        "/system/xbin/",
        "/sbin/",
        "/data/local/xbin/",
        "/data/local/bin/",
        "/system/sd/xbin/",
        "/system/bin/failsafe/",
        "/data/local/"
    )

    private val ROOT_BINARY_NAMES = listOf("su", "busybox", "magisk")

    fun checkRootBinaries(): CheckResult {
        val found = mutableListOf<String>()
        for (path in ROOT_BINARY_PATHS) {
            for (binary in ROOT_BINARY_NAMES) {
                val file = java.io.File(path, binary)
                if (file.exists()) {
                    found.add(file.absolutePath)
                }
            }
        }
        return CheckResult(
            name = "Root Binary Detection",
            positive = found.isNotEmpty(),
            detail = if (found.isNotEmpty())
                "Found: ${found.joinToString(", ")}"
            else
                "No root binaries found in common paths"
        )
    }

    // ---------------------------------------------------------------
    // 3. Command Execution Check
    // ---------------------------------------------------------------

    fun checkSuExecutable(): CheckResult {
        // Try `which su` first — cheap and doesn't actually spawn a shell.
        val whichResult = runCommand(arrayOf("which", "su"))
        if (whichResult != null && whichResult.trim().isNotEmpty()) {
            return CheckResult(
                name = "Command Execution Check (su)",
                positive = true,
                detail = "`which su` resolved to: ${whichResult.trim()}"
            )
        }

        // Fall back to attempting to invoke su directly.
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val output = BufferedReader(InputStreamReader(process.inputStream)).readLine()
            val exists = output != null
            CheckResult(
                name = "Command Execution Check (su)",
                positive = exists,
                detail = if (exists) "su executed successfully: $output" else "su did not return output"
            )
        } catch (e: java.io.IOException) {
            CheckResult(
                name = "Command Execution Check (su)",
                positive = false,
                detail = "su not executable (IOException: ${e.message})"
            )
        } finally {
            process?.destroy()
        }
    }

    private fun runCommand(cmd: Array<String>): String? {
        return try {
            val process = Runtime.getRuntime().exec(cmd)
            val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
            process.waitFor()
            output
        } catch (e: Exception) {
            null
        }
    }

    // ---------------------------------------------------------------
    // 4. System Properties Check
    // ---------------------------------------------------------------

    /**
     * Reads a system property. Tries reflection into the hidden
     * `android.os.SystemProperties` class first, and falls back to
     * shelling out to `getprop` if reflection fails (e.g. on newer
     * Android versions that restrict hidden-API access).
     */
    private fun getSystemProperty(key: String): String? {
        // Attempt 1: reflection
        try {
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getMethod("get", String::class.java)
            val value = method.invoke(null, key) as? String
            if (!value.isNullOrEmpty()) return value
        } catch (e: Exception) {
            // reflection blocked or failed — fall through to getprop
        }

        // Attempt 2: `getprop <key>` via Runtime.exec
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("getprop", key))
            val result = BufferedReader(InputStreamReader(process.inputStream)).readLine()
            process.waitFor()
            result
        } catch (e: java.io.IOException) {
            null
        }
    }

    fun checkDangerousProperties(): CheckResult {
        val flags = mutableListOf<String>()

        val debuggable = getSystemProperty("ro.debuggable")
        if (debuggable == "1") flags.add("ro.debuggable=1")

        val secure = getSystemProperty("ro.secure")
        if (secure == "0") flags.add("ro.secure=0")

        val selinux = getSystemProperty("ro.build.selinux")
        if (selinux == "0") flags.add("ro.build.selinux=0")

        return CheckResult(
            name = "System Properties Check",
            positive = flags.isNotEmpty(),
            detail = if (flags.isNotEmpty())
                "Dangerous flags: ${flags.joinToString(", ")}"
            else
                "No dangerous system properties detected"
        )
    }

    // ---------------------------------------------------------------
    // Aggregate entry point
    // ---------------------------------------------------------------

    fun checkAll(context: Context): RootCheckReport {
        val results = listOf(
            checkRootPackages(context),
            checkRootBinaries(),
            checkSuExecutable(),
            checkDangerousProperties()
        )
        return RootCheckReport(results)
    }
}
