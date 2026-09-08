plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.exclusivostars.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.exclusivostars.app"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
    buildTypes {
        release { isMinifyEnabled = false }
    }
}
kotlin {
    jvmToolchain(17)
}
