plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.gamev2"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.gamev2"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "2.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
