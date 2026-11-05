import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
    id("com.google.gms.google-services")
    kotlin("plugin.serialization") version "2.4.20"
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun signingValue(key: String, environmentName: String): String? =
    keystoreProperties.getProperty(key) ?: System.getenv(environmentName)

val releaseStoreFile: String? = signingValue("storeFile", "KQ_KEYSTORE_FILE")
val hasReleaseSigning: Boolean = releaseStoreFile != null && rootProject.file(releaseStoreFile).exists()

// Defaults to the hosted API. To use an API running on your own PC instead, set
// api.baseUrl in local.properties (for example http://localhost:5000).
val apiBaseUrl: String = (localProperties.getProperty("api.baseUrl") ?: "https://prog7314-kitchenquest-g7bd.onrender.com/")
    .let { if (it.endsWith("/")) it else "$it/" }

room {
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "com.example.kitchenquest"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.kitchenquest"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "2.0.0"

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = signingValue("storePassword", "KQ_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "KQ_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "KQ_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    // Lets the migration tests read the exported database schemas. Only debug builds
    // include them, so they never end up in a release build.
    sourceSets {
        getByName("debug").assets.directories.add("$projectDir/schemas")
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-messaging")

    // Coroutines
    implementation(libs.kotlinx.coroutines.play.services)

    // On-device text translation between English and Afrikaans (models downloaded on demand).
    implementation("com.google.mlkit:translate:17.0.3")

    // Biometric unlock (fingerprint or supported face recognition)
    implementation("androidx.biometric:biometric:1.1.0")

    // Credential Manager
    implementation("androidx.credentials:credentials:1.6.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")

    // Google Identity
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Supabase
    implementation(platform("io.github.jan-tennert.supabase:bom:3.8.0"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")

    // Ktor
    implementation("io.ktor:ktor-client-android:3.5.2")

    // Local database and background sync
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)

    // Networking (REST API)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
}