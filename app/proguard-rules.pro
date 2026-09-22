# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep essential attributes for stack traces and reflection
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,*Annotation*,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault

# ==============================================================================
# Retrofit Proguard Rules
# ==============================================================================
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http/* <methods>;
}

# ==============================================================================
# OkHttp and Okio Proguard Rules
# ==============================================================================
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ==============================================================================
# Moshi Proguard Rules
# ==============================================================================
-dontwarn com.squareup.moshi.**
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonQualifier public @interface *
-keepclassmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}
# Keep Moshi generated JSON adapters and reflectively accessed models
-keep class *JsonAdapter { *; }
-keep class *JsonAdapter$* { *; }

# ==============================================================================
# Room Database Proguard Rules
# ==============================================================================
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Database class *
-keep @androidx.room.Entity class *
-keep class * extends androidx.room.Database
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}

# ==============================================================================
# Kotlin Coroutines Proguard Rules
# ==============================================================================
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.android.AndroidDispatcherFactory {
    public *** createDispatcher(...);
}

# ==============================================================================
# Application Models & Long-Term Memory Entities (Keep structures intact)
# ==============================================================================
-keep class com.example.data.** { *; }
-keep class com.example.ai.** { *; }
-keep class com.example.service.** { *; }
-keep class com.example.ui.** { *; }
