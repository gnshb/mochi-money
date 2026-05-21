# Keep line numbers for crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin metadata for reflection-driven libraries.
-keep class kotlin.Metadata { *; }

# Compose runtime keeps generated classes alive through reflection in some cases.
-keep class androidx.compose.runtime.** { *; }

# Room generates DAO implementations referenced via reflection.
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Coroutines internals.
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# App domain models are persisted via Room mappers; keep them intact.
-keep class com.mochimoney.app.domain.model.** { *; }
-keep class com.mochimoney.app.data.local.** { *; }
