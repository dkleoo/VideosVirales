# VideosVirales

Aplicación Kotlin Multiplatform con Compose Multiplatform para Android, iOS y Desktop.

## Arquitectura

```text
Screen (Voyager)
 → ViewModel (ScreenModel + StateFlow)
   → UseCase
     → Repository (domain)
       → RepositoryImpl (data)
         → Services
           → DTOs / mappers
```

La UI no accede a repositorios ni servicios. El ViewModel no usa Ktor.

El cliente HTTP (Ktor) está registrado en Koin y listo para los servicios. Los endpoints reales se agregarán en `domain/util/EndPoints.kt` cuando exista una API definida; no se inventaron rutas.

## Módulos

- `shared`: código común, tema, navegación, DI y capas de dominio/datos.
- `androidApp`: aplicación Android.
- `desktopApp`: aplicación Desktop (JVM).
- `iosApp`: aplicación iOS (Xcode).

## Estructura compartida

```text
shared/src/commonMain/kotlin/com/example/videosvirales/
├── di/                         Koin + koinScreenModel + módulo de plataforma
├── domain/
│   ├── repository/
│   └── useCase/
├── data/
│   ├── mappers/
│   ├── params/
│   ├── repositoyImpl/
│   └── remote/
│       ├── editor/
│       ├── home/
│       └── network/
├── platform/                   expect/actual, incluido el selector de video
└── presentation/
    ├── screens/
    │   ├── components/
    │   ├── editor/
    │   ├── home/
    │   └── routes/
    ├── theme/
    └── util/
```

Los textos están en `shared/src/commonMain/composeResources` para `es`, `en` y `pt`.

## Editor de clips

Flujo:

```text
Crear clips automáticos
  Subir video → Analizar momentos → Configurar cantidad/duración/formato
  → Revisar previews y subtítulos → Exportar

Recortar manualmente
  Subir video → Elegir inicio/fin → Formato, subtítulo y narrador → Exportar

Unir videos
  Elegir 2+ videos → Confirmar orden → Formato y narrador → Exportar un MP4
```

- Android: selección con el selector del sistema, análisis con `MediaMetadataRetriever`, previews con frames recortados, transcripción con Vosk offline y exportación con Media3 Transformer.
- Desktop: selección con diálogo nativo, análisis y exportación con FFmpeg, previews con frames recortados y transcripción con `whisper-cli`.
- El video se ajusta al formato elegido recortando el excedente desde el centro.
- El recorte manual trabaja con un segmento por sesión y permite generar su subtítulo.
- La unión requiere al menos dos videos y respeta el orden de selección. Puedes reordenar los capítulos con las flechas.
- Cada capítulo de la unión tiene preview y un botón para reproducirlo antes de exportar.
- El narrador usa una voz masculina española de locutor: Piper local en Desktop (`es_ES-davefx-medium`) y TTS español del sistema en Android.
- Narración por escena: cada clip automático y cada video de la unión tienen su propio texto de narrador. También puedes narrar el recorte manual.
- La primera narración en Desktop descarga el modelo de Piper (~63 MB). Después funciona sin conexión.
- En Android necesitas una voz TTS en español instalada; si no existe, se muestra un error.
- Audio propio: puedes elegir un archivo de audio por escena/clip y un audio general para el video unido. Se mezcla con el audio original.
- Si una escena tiene audio propio, ese audio reemplaza al narrador TTS de esa escena. El audio general se corta a la duración del video.
- Formatos de audio: MP3, M4A, AAC, WAV, OGG y FLAC.
- El narrador se mezcla con el audio original.
- Desktop requiere un FFmpeg que incluya `libass` y `freetype`. En macOS se puede usar `ffmpeg-full` de Homebrew.
- La primera transcripción descarga un modelo de voz local (Vosk en Android, whisper.cpp en Desktop). Después funciona sin conexión.
- Android guarda los resultados en `Movies/VideosVirales`; Desktop en `~/Movies/VideosVirales`.
- Cada resultado tiene el botón `Abrir video`, que lo reproduce con el visor o programa predeterminado del sistema.
- iOS no implementa este flujo por ahora.

## Ejecutar

- Android: `./gradlew :androidApp:assembleDebug`
- Desktop: `./gradlew :desktopApp:run`
- iOS: abrir `iosApp/iosApp.xcodeproj`
- Tests: `./gradlew :shared:jvmTest :shared:testAndroidHostTest :shared:iosSimulatorArm64Test`
