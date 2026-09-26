plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.darktubbie.aeroplayer"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.darktubbie.aeroplayer"
        minSdk = 26
        targetSdk = 37
        versionCode = 4
        versionName = "0.5.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("../aero-player-release.jks")
            storePassword = providers.gradleProperty("AERO_STORE_PASSWORD").orNull
            keyAlias = providers.gradleProperty("AERO_KEY_ALIAS").orNull ?: "aero-player"
            keyPassword = providers.gradleProperty("AERO_KEY_PASSWORD").orNull
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }

        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))

    implementation("androidx.activity:activity-compose:1.11.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose")

    implementation("androidx.media3:media3-exoplayer:1.11.0")
    implementation("androidx.media3:media3-session:1.11.0")

    // Fase 8: escribir el .ape en la carpeta SAF ya seleccionada
    // por el usuario (DocumentFile es la forma estándar de crear/
    // sobrescribir archivos dentro de un árbol SAF).
    implementation("androidx.documentfile:documentfile:1.0.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}