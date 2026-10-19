# Reglas de negocio

## Editor de clips

- Flujo: subir video → analizar mejores momentos → configurar cantidad, duración y formato → revisar previews y subtítulos → exportar.
- Cantidad de clips: 1 a 10. Valor por defecto: 3.
- Duración por clip: 5 a 60 segundos. Valor por defecto: 15 segundos.
- La duración solicitada se limita a la duración real del video.
- Formatos de salida: 9:16 (1080x1920), 1:1 (1080x1080) y 16:9 (1920x1080).
- El video se escala para cubrir el formato y se recorta al centro cuando sobra imagen.
- Cada clip muestra una preview con el recorte que se aplicará al exportar.
- Los momentos se eligen por ventanas no superpuestas con el mayor puntaje. Si no alcanzan, se completan con ventanas distribuidas uniformemente.
- La detección local es heurística: cambios visuales/escena en Android y cambios de escena + energía de audio en Desktop.
- Los subtítulos se generan automáticamente si hay voz: Vosk offline en Android y whisper.cpp en Desktop.
- La primera transcripción descarga un modelo de voz local si no existe. Después funciona sin conexión.
- Los subtítulos son editables por clip. Un subtítulo vacío no se renderiza.
- No se aplican animaciones de zoom, fade ni movimiento; los clips son estáticos con subtítulos.
- Salida: MP4 con video H.264 y audio AAC en el formato seleccionado.
- Android exporta con Media3 Transformer y publica los clips en `Movies/VideosVirales` vía MediaStore (galería).
- Cada clip exportado se puede abrir con el programa o visor predeterminado del sistema desde el paso final.
- En Android la apertura usa `ACTION_VIEW` con permiso temporal de lectura; para archivos internos usa `FileProvider`. En Desktop usa `Desktop.open`.
- Desktop exporta con FFmpeg (requiere libass y freetype) y guarda en `~/Movies/VideosVirales`.
- Desktop requiere `whisper-cli` para la transcripción automática.
- iOS no soporta este flujo; las implementaciones son stubs que reportan la función como no disponible.
- Si la duración total del video no permite la cantidad de clips solicitada sin superponerse, se permiten solapamientos para completar la cantidad pedida.
