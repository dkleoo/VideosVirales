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
Subir video
 → Analizar mejores momentos (heurística local)
   → Configurar cantidad y duración
     → Editar subtítulos y animación por clip
       → Exportar MP4 vertical 1080x1920
```

- Android: selección con el selector del sistema, análisis con `MediaMetadataRetriever` y exportación con Media3 Transformer.
- Desktop: selección con diálogo nativo, análisis y exportación con FFmpeg.
- Desktop requiere un FFmpeg que incluya `libass`, `freetype` y `zoompan`. En macOS se puede usar `ffmpeg-full` de Homebrew.
- Android guarda los clips en `Movies/VideosVirales`; Desktop en `~/Movies/VideosVirales`.
- iOS no implementa este flujo por ahora.
- Los subtítulos son editables: la heurística no transcribe audio.

## Ejecutar

- Android: `./gradlew :androidApp:assembleDebug`
- Desktop: `./gradlew :desktopApp:run`
- iOS: abrir `iosApp/iosApp.xcodeproj`
- Tests: `./gradlew :shared:jvmTest :shared:testAndroidHostTest :shared:iosSimulatorArm64Test`
