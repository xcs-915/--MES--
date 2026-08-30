# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in [sdk]/tools/proguard/proguard-android.txt
# You can edit the build configuration file [project]/build.gradle to set custom
# ProGuard options.

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson
-keep class com.tns.mes.model.** { *; }
-keep class com.tns.mes.api.** { *; }
-keepattributes EnclosingMethod
