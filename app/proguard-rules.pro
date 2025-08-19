# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes LineNumberTable,SourceFile

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep Parcelable classes
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep serialization classes
-keep class kotlinx.serialization.** { *; }
-keep class kotlin.Metadata { *; }

# Keep Room entities
-keep class com.iperf3client.data.database.entity.** { *; }
-keep class com.iperf3client.domain.model.** { *; }

# Keep Compose
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }

# Keep iperf3 related classes
-keep class com.iperf3client.domain.model.IperfEngine { *; }
-keep class com.iperf3client.data.engine.** { *; }

# Gson/JSON serialization
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**

# OkHttp and Retrofit (if used later)
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# CSV library
-keep class com.opencsv.** { *; }

# Charts library
-keep class com.github.mikephil.charting.** { *; }