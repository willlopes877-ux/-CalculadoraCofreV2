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
        versionCode = 3
        versionName = "3.0"
    }
}

configurations.configureEach {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}