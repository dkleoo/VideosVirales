package com.example.videosvirales.data.remote.editor

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.example.videosvirales.data.mappers.editor.ClipDraft
import com.example.videosvirales.data.mappers.editor.EditorError
import com.example.videosvirales.data.mappers.editor.ExportedClip
import com.example.videosvirales.data.mappers.editor.VideoFile
import com.example.videosvirales.data.mappers.editor.VideoSignal
import com.example.videosvirales.data.repositoyImpl.editor.EditorException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AndroidEditorServices(
    private val context: Context,
) : EditorServices {
    private val analyzer = AndroidVideoAnalyzer(context)
    private val exporter = AndroidVideoExporter(context)

    override suspend fun loadVideo(uri: String): VideoFile = withContext(Dispatchers.IO) {
        try {
            val parsedUri = Uri.parse(uri)
            val (displayName, sizeBytes) = queryFileInfo(parsedUri)
            val retriever = MediaMetadataRetriever()
            val durationMs = try {
                retriever.setDataSource(context, parsedUri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()
                    ?: 0L
            } finally {
                retriever.release()
            }
            if (durationMs <= 0) throw EditorException(EditorError.NO_VIDEO)
            VideoFile(
                uri = uri,
                displayName = displayName,
                durationMs = durationMs,
                sizeBytes = sizeBytes,
            )
        } catch (exception: EditorException) {
            throw exception
        } catch (exception: Exception) {
            throw EditorException(EditorError.NO_VIDEO, exception)
        }
    }

    override suspend fun analyze(
        video: VideoFile,
        onProgress: (Float) -> Unit,
    ): List<VideoSignal> = analyzer.analyze(video, onProgress)

    override suspend fun exportClip(
        video: VideoFile,
        clip: ClipDraft,
        outputDirectory: String,
    ): ExportedClip = exporter.export(video, clip, outputDirectory)

    override suspend fun openClip(uri: String) = withContext(Dispatchers.Main) {
        try {
            val clipUri = if (uri.startsWith("content://")) {
                Uri.parse(uri)
            } else {
                val file = if (uri.startsWith("file:")) {
                    File(Uri.parse(uri).path ?: uri)
                } else {
                    File(uri)
                }
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(clipUri, "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (exception: ActivityNotFoundException) {
            throw EditorException(EditorError.OPEN_FAILED, exception)
        } catch (exception: Exception) {
            throw EditorException(EditorError.OPEN_FAILED, exception)
        }
    }

    override fun defaultOutputDirectory(): String {
        val baseDirectory = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: context.filesDir
        val outputDirectory = File(baseDirectory, "VideosVirales").apply { mkdirs() }
        return outputDirectory.absolutePath
    }

    private fun queryFileInfo(uri: Uri): Pair<String, Long> {
        var displayName = "video.mp4"
        var sizeBytes = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    displayName = cursor.getString(nameIndex) ?: displayName
                }
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    sizeBytes = cursor.getLong(sizeIndex)
                }
            }
        }
        return displayName to sizeBytes
    }
}
