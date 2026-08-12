# Keep line numbers for stack traces (so we can still read logcat output clearly)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Standard Compose/Kotlin metadata retention (needed for the app to run correctly,
# does NOT protect OkHttp class names)
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# Deliberately NOT adding:
#   -keep class okhttp3.** { *; }
# We want R8 to rename CertificatePinner and check$okhttp exactly as it would
# in a real unprotected commercial app, so this test is realistic.