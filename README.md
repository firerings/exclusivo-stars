# Exclusivo Stars — Android

Cliente Android nativo para Mediaflix (marca "Exclusivo Stars").

- Kotlin, Android Views + XML (sin Jetpack Compose)
- Sin librerías runtime externas (HttpURLConnection + org.json, ambas
  incluidas en el SDK de Android)
- Login y registro en una sola pantalla con tabs deslizantes ("Iniciar
  sesión" / "Crear cuenta"), igual que la web
- Sesión vía cookie de Flask (CookieManager), autenticación contra
  endpoints JSON dedicados — ver `AuthApi.kt` para el contrato exacto
  que necesita el backend (`/api/login`, `/api/register`)
- Logo e ícono de app con el mismo trazo que `static/shared/logo.svg`
  del proyecto web

## Compilar

Vía GitHub Actions (`.github/workflows/build.yml`): se dispara en cada
push/PR a `main`, y también a mano desde la pestaña Actions
("Run workflow"). Como el proyecto no tiene `gradlew` commiteado (se
armó pensando en NanoIDE, que resuelve Gradle por su cuenta), el
workflow instala Gradle 8.9 en el runner con `gradle/actions/setup-gradle`.
El APK debug queda como artifact descargable de la ejecución
("exclusivo-stars-debug").

## Backend

El contrato JSON completo (qué espera recibir la app de cada
endpoint, campo por campo) vive en [`CONTRATO_APK.md`](CONTRATO_APK.md)
— ahí también está qué tiene implementado el backend real vs. el mock
en `mediaflix_mock/`.
