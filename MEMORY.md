# Reglas de negocio

## Editor de clips

- Flujo: subir video → analizar mejores momentos → configurar cantidad y duración → revisar subtítulos y animación → exportar.
- Cantidad de clips: 1 a 10. Valor por defecto: 3.
- Duración por clip: 5 a 60 segundos. Valor por defecto: 15 segundos.
- La duración solicitada se limita a la duración real del video.
- Los momentos se eligen por ventanas no superpuestas con el mayor puntaje. Si no alcanzan, se completan con ventanas distribuidas uniformemente.
- La detección local es heurística: cambios visuales/escena en Android y cambios de escena + energía de audio en Desktop. No transcribe voz ni usa modelos externos.
- Subtítulos: uno por clip, editable por el usuario. Un subtítulo vacío no se renderiza.
- Animaciones disponibles: ninguna, zoom in, zoom out, fade y slide up. Zoom afecta al video; fade, slide up y el efecto de entrada afectan al subtítulo.
- Salida: MP4 vertical 1080x1920 con video H.264 y audio AAC.
- Android exporta con Media3 Transformer y publica los clips en `Movies/VideosVirales` vía MediaStore (galería).
- Cada clip exportado se puede abrir con el programa o visor predeterminado del sistema desde el paso final.
- En Android la apertura usa `ACTION_VIEW` con permiso temporal de lectura; para archivos internos usa `FileProvider`. En Desktop usa `Desktop.open`.
- Desktop exporta con FFmpeg (requiere libass, freetype y zoompan) y guarda en `~/Movies/VideosVirales`.
- iOS no soporta este flujo; las implementaciones son stubs que reportan la función como no disponible.
- Si la duración total del video no permite la cantidad de clips solicitada sin superponerse, se permiten solapamientos para completar la cantidad pedida.
