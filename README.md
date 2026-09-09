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

## Backend pendiente

`AuthApi.kt` y `PeliculasApi.kt` documentan los contratos JSON
esperados:

- `/api/login`, `/api/register` — rutas nuevas en `auth/routes.py`,
  `@csrf.exempt` igual que `reindexar`, devolviendo `{"ok": bool,
  "error": str|null, "user": {...}}` y dejando la sesión iniciada
  igual que hoy (`session["user_id"] = user.id`).
- `/api/peliculas` — ruta nueva en `blueprints/peliculas.py`, GET (no
  necesita `@csrf.exempt`, CSRFProtect no protege GET), devolviendo
  `{"ok": true, "peliculas": [...]}` con los mismos nombres de campo
  que ya arma `indexador.py` (`nombre`, `titulo`, `sinopsis`, `anio`,
  `duracion`, `estado`) más `poster_url` resuelto a URL absoluta. Falta
  decidir qué devuelve esta ruta si no hay sesión — hoy
  `cargar_usuario_y_exigir_login` redirige a `/login` (HTML), lo que
  rompería el parseo JSON de la app.
