import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val playSigningProperties = Properties().apply {
    val signingFile = file(System.getenv("NIGHTPAUSE_SIGNING_PROPERTIES")
        ?: "${System.getProperty("user.home")}/.config/nightpause/signing.properties")
    if (signingFile.exists()) load(signingFile.inputStream())
}

android {
    namespace = "dev.nightpause.wear"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    // Wear Data Layer only connects companion apps with matching application IDs.
    // Play uses a separate Wear OS release under the same listing; keep its code unique.
    defaultConfig { applicationId = "de.joancode.nightpause"; minSdk = 30; targetSdk = 35; versionCode = 6; versionName = "1.4" }
    signingConfigs {
        if (playSigningProperties.getProperty("storeFile") != null) {
            create("play") {
                storeFile = file(playSigningProperties.getProperty("storeFile"))
                storePassword = playSigningProperties.getProperty("storePassword")
                keyAlias = playSigningProperties.getProperty("keyAlias")
                keyPassword = playSigningProperties.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        getByName("release") {
            signingConfigs.findByName("play")?.let { signingConfig = it }
            isMinifyEnabled = false
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    buildFeatures { compose = true }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.12.00"))
    implementation("androidx.compose.animation:animation")
    implementation("androidx.activity:activity-compose:1.12.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.wear.compose:compose-material3:1.5.0")
    implementation("androidx.health:health-services-client:1.1.0")
    implementation("com.google.android.gms:play-services-wearable:20.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.10.2")
    implementation("androidx.work:work-runtime-ktx:2.10.5")
}
