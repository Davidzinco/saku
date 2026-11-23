import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "id.saku.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "id.saku.app"
        minSdk = 26
        targetSdk = 37
        // V1.5 #8: versi dibaca PackageManager untuk ditampilkan di Setting.
        // Satu versi = satu versionCode: V1.6 -> 6, V1.6.8 -> 7.
        versionCode = 7
        versionName = "1.6.8"
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.15.0")
    // V1.4 #5: penyimpanan per-record, menggantikan satu string JSON di SharedPreferences
    implementation("androidx.datastore:datastore-preferences:1.1.7")

    // Liquid Glass (Backdrop) dari https://github.com/Kyant0/AndroidLiquidGlass
    // Dipakai untuk dock dan FAB; bisa dimatikan lewat Setting > Theme > Dock.
    // shapes dipakai lens() di dalam backdrop saat runtime (Android 13+), jadi
    // dideklarasikan eksplisit supaya tidak cuma mengandalkan dependensi transitif.
    implementation("io.github.kyant0:backdrop:2.0.1")
    implementation("io.github.kyant0:shapes:1.2.1")

    testImplementation("junit:junit:4.13.2")
}
