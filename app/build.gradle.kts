import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}
// Firebase is enabled only when app/google-services.json exists (otherwise the app runs in demo/Pexels mode)
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun prop(name: String, default: String = ""): String =
    System.getenv(name)?.takeIf { it.isNotBlank() }
        ?: localProps.getProperty(name)?.takeIf { it.isNotBlank() }
        ?: default

// Google's official TEST ids (safe for development). Replace with your own for production.
val testAppId = "ca-app-pub-3940256099942544~3347511713"

android {
    namespace = "com.infinity.wallpapers"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.infinity.wallpapers"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        manifestPlaceholders["admobAppId"] = prop("ADMOB_APP_ID", testAppId)
        buildConfigField("String", "ADMOB_BANNER", "\"${prop("ADMOB_BANNER_ID", "ca-app-pub-3940256099942544/9214589741")}\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL", "\"${prop("ADMOB_INTERSTITIAL_ID", "ca-app-pub-3940256099942544/1033173712")}\"")
        buildConfigField("String", "ADMOB_REWARDED", "\"${prop("ADMOB_REWARDED_ID", "ca-app-pub-3940256099942544/5224354917")}\"")
        buildConfigField("String", "ADMOB_NATIVE", "\"${prop("ADMOB_NATIVE_ID", "ca-app-pub-3940256099942544/2247696110")}\"")
        buildConfigField("String", "PEXELS_API_KEY", "\"${prop("PEXELS_API_KEY")}\"")
        buildConfigField("String", "PRIVACY_POLICY_URL", "\"${prop("PRIVACY_POLICY_URL")}\"")
        buildConfigField("String", "SUPPORT_EMAIL", "\"${prop("SUPPORT_EMAIL")}\"")
    }

    val ksPath = prop("KEYSTORE_PATH")
    signingConfigs {
        if (ksPath.isNotEmpty() && file(ksPath).exists()) {
            create("release") {
                storeFile = file(ksPath)
                storePassword = prop("KEYSTORE_PASSWORD")
                keyAlias = prop("KEY_ALIAS")
                keyPassword = prop("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
            if (prop("ADMOB_APP_ID", testAppId) == testAppId) {
                logger.warn("WARNING: release build is using Google TEST AdMob ids. Set ADMOB_* before publishing.")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.04.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.9")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("io.coil-kt:coil-compose:2.7.0")

    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    ksp("androidx.room:room-compiler:2.7.1")

    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    implementation(platform("com.google.firebase:firebase-bom:33.12.0"))
    implementation("com.google.firebase:firebase-firestore")

    implementation("com.google.android.gms:play-services-ads:24.2.0")
    implementation("com.google.android.ump:user-messaging-platform:3.2.0")
}
