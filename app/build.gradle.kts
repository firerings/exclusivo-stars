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
    // ConstraintLayout: necesario para el hero de pelicula_detalle
    // (tarjeta ancha con ratio 16:9 anclada abajo del contenedor,
    // ver activity_pelicula_detalle.xml) — no se puede lograr ese
    // ratio responsive con FrameLayout/LinearLayout puro.
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    // Carga async de posters con caché en disco/memoria y manejo de
    // scroll en RecyclerView ya resuelto — reinventar esto a mano con
    // HttpURLConnection sería mucho peor para una grilla con muchas
    // imágenes cargando/reciclándose en simultáneo.
    implementation("com.github.bumptech.glide:glide:4.16.0")
    // BottomNavigationView + ripple/elevation/estados ya resueltos para
    // la navegación entre secciones (Películas/Series/Reels/Música) —
    // reinventar esto a mano no suma nada, es el widget estándar para
    // este patrón de navegación en Android.
    implementation("com.google.android.material:material:1.12.0")
    // Media3 (sucesor de ExoPlayer clásico, mismo motor por debajo):
    // media3-exoplayer es el core, -hls suma el extractor de
    // master.m3u8 (películas "procesadas"), -ui da PlayerView con los
    // controles nativos listos. El archivo directo de una película
    // "pendiente" lo reproduce el propio core sin módulo aparte
    // (mp4/mkv vía extractores estándar).
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
}
