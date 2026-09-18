# Contrato API — backend ↔ apk Android

Este documento es la fuente de verdad de qué necesita recibir la app
Android de cualquier backend que le hable (el real, en Flask, o el
mock en `mediaflix_mock/`). El `README.md` de este repo tenía una
sección "Backend pendiente" que quedó desactualizada apenas el
backend real implementó esos endpoints — este archivo reemplaza esa
sección y es el que hay que mantener al día de ahora en más.

Convenciones:
- Todas las rutas son `GET` salvo que se indique lo contrario.
- Todo campo marcado **opcional** puede venir `null` o ausente — la
  app ya maneja ambos casos como "no hay dato", nunca asume que va a
  estar presente.
- Las rutas `/api/*` (salvo login/register) requieren sesión iniciada
  (cookie de Flask) y devuelven `401` en JSON si no la hay — nunca un
  redirect HTML a `/login`, porque eso rompería el parseo JSON de la
  app.
- `ok: false` siempre viene con `error: string` explicando qué pasó.

---

## Auth

### `POST /api/login`
Body: `{ "email": string, "password": string, "remember": boolean }`

Respuesta OK: `{ "ok": true, "user": { "name": string } }`
Respuesta error: `{ "ok": false, "error": string }`

### `POST /api/register`
Body: `{ "name": string, "email": string, "password": string, "confirm": string }`

Misma forma de respuesta que login.

### `GET /api/me`
Respuesta OK: `{ "ok": true, "user": { "name": string, "email": string } }`
Sin sesión: `401 { "ok": false, "error": string }`

---

## Películas

### `GET /api/peliculas`
Lista para la grilla. Respuesta: `{ "ok": true, "peliculas": [Pelicula] }`

**Pelicula** (forma resumida, para la grilla):
| campo | tipo | opcional |
|---|---|---|
| `nombre` | string | no — identificador único, se usa para pedir el detalle |
| `titulo` | string | no |
| `sinopsis` | string | sí |
| `anio` | string | sí |
| `duracion` | string | sí |
| `poster_url` | string | sí |
| `fondo_url` | string | sí |
| `logo_url` | string | sí |
| `estado` | string | sí — `"procesada"` o `"pendiente"` |
| `pais` | string (ISO 3166-1 **alpha-3**, ej. `"USA"`) | sí — **todavía no lo manda ningún backend, ver nota abajo** |

### `GET /api/pelicula/<nombre>`
Ficha completa + datos de reproducción, todo junto (a diferencia de
series, una película es en sí misma el reproducible).

Respuesta: `{ "ok": true, "pelicula": {...} }` con todos los campos de
arriba más:

| campo | tipo | opcional |
|---|---|---|
| `genero` | string[] | sí |
| `director` | string | sí |
| `puntuacion` | number | sí |
| `tarjeta_url` | string | sí |
| `reparto` | Actor[] | sí — ver forma abajo |
| `audio_texto` | string | sí |
| `modo_directo` | boolean | no |
| `source_url` | string | sí — url del video |
| `subtitulos` | Subtitulo[] | sí |
| `crop` | object | sí — corrección de recorte del video |

`404 { "ok": false, "error": "Película no encontrada." }` si no existe.

**Actor**: `{ "nombre": string, "slug": string|null, "foto_url": string|null }`

---

## Series

### `GET /api/series`
Lista para la grilla. Respuesta: `{ "ok": true, "series": [Serie] }`

**Serie** (forma resumida):
| campo | tipo | opcional |
|---|---|---|
| `slug` | string | no — identificador único |
| `titulo` | string | no |
| `sinopsis` | string | sí |
| `anio` | string | sí |
| `poster_url` | string | sí |
| `n_episodios` | number | no (`0` si no hay dato) |
| `pais` | string (ISO alpha-3) | sí — mismo caso que en Pelicula, pendiente |

### `GET /api/serie/<slug>` — **NUEVO**
Ficha de la serie: metadata + árbol de temporadas/episodios. A
diferencia de película, **no trae `source_url`** — una serie no se
reproduce directo, hay que elegir un episodio primero (ver el
endpoint de abajo).

Respuesta: `{ "ok": true, "serie": {...} }`

| campo | tipo | opcional |
|---|---|---|
| `slug` | string | no |
| `titulo` | string | no |
| `sinopsis` | string | sí |
| `anio` | string | sí |
| `duracion` | string | sí — duración de un episodio típico, no de la serie entera |
| `genero` | string[] | sí |
| `creador` | string | sí |
| `puntuacion` | number | sí |
| `poster_url` | string | sí |
| `logo_url` | string | sí |
| `reparto` | Actor[] | sí |
| `n_episodios` | number | no |
| `temporadas` | Temporada[] | no |

**Temporada**: `{ "codigo": string, "numero": number, "episodios": Episodio[] }`
`codigo` es el identificador que hay que mandar en la URL de
reproducción de abajo (ej. `"S01"`) — **no** uses `numero` para armar
esa URL, son formatos distintos.

**Episodio**: `{ "codigo": string, "numero": number, "estado": string }`
`estado` es `"procesada"` o `"pendiente"`, mismo criterio que
`Pelicula.estado`.

`404 { "ok": false, "error": "Serie no encontrada." }` si el slug no existe.

### `GET /api/serie/<slug>/<temporada>/<episodio>` — **NUEVO**
Ficha + datos de reproducción de UN episodio puntual. `<temporada>` y
`<episodio>` son los `codigo` que devuelve el endpoint de arriba
(ej. `/api/serie/breaking-bad/S01/E03`), no números sueltos.

Respuesta: `{ "ok": true, "episodio": {...} }`

| campo | tipo | opcional |
|---|---|---|
| `titulo_episodio` | string | no |
| `modo_directo` | boolean | no |
| `source_url` | string | sí |
| `subtitulos` | Subtitulo[] | sí |
| `sprite` | string | sí — hoja de miniaturas para la barra de progreso |
| `crop` | object | sí |
| `audio_texto` | string | sí |
| `next_episode` | NextEpisode \| null | sí — para el autoplay |

**NextEpisode**: `{ "titulo": string, "temporada": string, "episodio": string }`
`null` si el episodio actual es el último de toda la serie.

`404 { "ok": false, "error": "Episodio no encontrado." }` si la
combinación temporada/episodio no existe.

---

## Estado de implementación por backend

| Endpoint | Backend real (Flask) | `mediaflix_mock/` |
|---|---|---|
| `/api/login`, `/api/register`, `/api/me` | ✅ | ✅ |
| `/api/peliculas`, `/api/pelicula/<nombre>` | ✅ | ✅ |
| `/api/series` | ✅ | ✅ |
| `/api/serie/<slug>` | ❌ pendiente | ✅ |
| `/api/serie/<slug>/<temporada>/<episodio>` | ❌ pendiente | ✅ |
| `pais` en Pelicula/Serie | ❌ pendiente | ❌ pendiente |

El mock genera `temporadas`/`episodios` de forma sintética (reparte
`n_episodios` entre `n_temporadas` a ojo) porque no tiene contenido
real indexado carpeta por carpeta como el backend real — el **shape**
del JSON es el que importa acá, no los números exactos que devuelve
el mock.

**Pendiente en el backend real:** implementar los dos endpoints
nuevos en `blueprints/series.py` (hoy `/serie/<slug>` y
`/serie/<slug>/<t>/<e>` solo devuelven HTML) y decidir de dónde sale
`pais` (¿campo manual en `info.json`, o lo trae `enriquecer_tmdb.py`
automático vía `origin_country` de TMDB?).
