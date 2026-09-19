plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.calculatorvault"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.calculatorvault"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "2.0"
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
}