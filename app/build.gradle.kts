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
dependencies {
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    // Carga async de posters con caché en disco/memoria y manejo de
    // scroll en RecyclerView ya resuelto — reinventar esto a mano con
    // HttpURLConnection sería mucho peor para una grilla con muchas
    // imágenes cargando/reciclándose en simultáneo.
    implementation("com.github.bumptech.glide:glide:4.16.0")
}
