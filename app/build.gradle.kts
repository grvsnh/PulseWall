plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.example.pulsewall"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.pulsewall"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
}