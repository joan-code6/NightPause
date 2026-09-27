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
    namespace = "dev.nightpause.phone"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    defaultConfig { applicationId = "de.joancode.nightpause"; minSdk = 26; targetSdk = 36; versionCode = 5; versionName = "1.4" }
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
    implementation("androidx.activity:activity-compose:1.12.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.work:work-runtime-ktx:2.10.5")
    implementation("com.google.android.gms:play-services-wearable:20.0.1")
}
