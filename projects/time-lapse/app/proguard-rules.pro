# Add project specific ProGuard rules here.

# Camera2 API
-keep class android.hardware.camera2.** { *; }
-keep class androidx.camera.** { *; }

# Google Drive API
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.** { *; }
-keep class com.google.auth.** { *; }
-dontwarn com.google.api.client.**
-dontwarn com.google.apis.**

# NanoHTTPD
-keep class fi.iki.elonen.** { *; }
-keep class org.nanohttpd.** { *; }
-dontwarn fi.iki.elonen.**
-dontwarn org.nanohttpd.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep data models
-keep class com.timelapse.app.SessionConfig { *; }
-keep class com.timelapse.app.SessionStatus { *; }
-keep class com.timelapse.app.CameraInfo { *; }
