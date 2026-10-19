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
Subir video
 → Analizar mejores momentos (heurística local)
   → Configurar cantidad, duración y formato
     → Revisar previews y subtítulos por clip
       → Exportar MP4 en 9:16, 1:1 o 16:9
```

- Android: selección con el selector del sistema, análisis con `MediaMetadataRetriever`, previews con frames recortados, transcripción con Vosk offline y exportación con Media3 Transformer.
- Desktop: selección con diálogo nativo, análisis y exportación con FFmpeg, previews con frames recortados y transcripción con `whisper-cli`.
- El video se ajusta al formato elegido recortando el excedente desde el centro.
- Desktop requiere un FFmpeg que incluya `libass` y `freetype`. En macOS se puede usar `ffmpeg-full` de Homebrew.
- La primera transcripción descarga un modelo de voz local (Vosk en Android, whisper.cpp en Desktop). Después funciona sin conexión.
- Android guarda los clips en `Movies/VideosVirales`; Desktop en `~/Movies/VideosVirales`.
- Cada clip tiene el botón `Abrir video`, que lo reproduce con el visor o programa predeterminado del sistema.
- iOS no implementa este flujo por ahora.

## Ejecutar

- Android: `./gradlew :androidApp:assembleDebug`
- Desktop: `./gradlew :desktopApp:run`
- iOS: abrir `iosApp/iosApp.xcodeproj`
- Tests: `./gradlew :shared:jvmTest :shared:testAndroidHostTest :shared:iosSimulatorArm64Test`
