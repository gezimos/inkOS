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
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-dontwarn javax.annotation.processing.Processor
-dontwarn javax.annotation.processing.AbstractProcessor
-dontwarn javax.annotation.processing.SupportedOptions

# ── Gson TypeToken reflection ────────────────────────────────────────────────
# Gson uses reflection via TypeToken to deserialize generic types. R8 strips
# the generic signature and renames fields, causing runtime crashes.
-keepattributes Signature
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Data classes deserialized by Gson (field names must survive minification)
-keep class com.github.gezimos.inkos.services.NotificationManager$ConversationNotification { *; }
-keep class com.github.gezimos.inkos.helper.PinnedShortcutData { *; }
