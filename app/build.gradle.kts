import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Release signing is read from keystore.properties in the project root (git-ignored).
// See keystore.properties.example and the README.
val keystorePropertiesFile: File = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.deepkosh.goldpulse"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.deepkosh.goldpulse"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    androidResources {
        // The app is English-only; drop translations bundled in libraries.
        localeFilters += "en"
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // java.time on API 24/25
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
    }
}

if (!keystorePropertiesFile.exists()) {
    logger.warn("GoldPulse: keystore.properties not found - release builds will be unsigned. See README.md.")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.work.runtime.ktx)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
