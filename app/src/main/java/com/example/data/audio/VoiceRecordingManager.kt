package com.example.data.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileInputStream

class VoiceRecordingManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentOutputFile: File? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _recordedAudioData = MutableStateFlow<String?>(null)
    val recordedAudioData: StateFlow<String?> = _recordedAudioData.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun startRecording(): Boolean {
        return try {
            stopPlayback()
            val outputDir = context.cacheDir
            val outputFile = File.createTempFile("dastak_voice_", ".m4a", outputDir)
            currentOutputFile = outputFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            _isRecording.value = true
            _errorMessage.value = null
            true
        } catch (e: Exception) {
            Log.e("VoiceRecording", "Failed to start recording", e)
            _errorMessage.value = e.localizedMessage ?: "Recording failed"
            _isRecording.value = false
            currentOutputFile?.delete()
            currentOutputFile = null
            false
        }
    }

    fun stopRecording(): String? {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            _isRecording.value = false

            val file = currentOutputFile
            if (file != null && file.exists() && file.length() > 0) {
                // Encode to data URL string for persistence
                val bytes = FileInputStream(file).use { it.readBytes() }
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                val dataUri = "data:audio/m4a;base64,$base64"
                _recordedAudioData.value = dataUri
                return dataUri
            }
        } catch (e: Exception) {
            Log.e("VoiceRecording", "Failed to stop recording", e)
            _errorMessage.value = e.localizedMessage ?: "Failed to save recording"
        } finally {
            _isRecording.value = false
        }
        return null
    }

    fun playRecording(audioDataOrPath: String? = _recordedAudioData.value) {
        if (audioDataOrPath.isNullOrBlank()) return
        stopPlayback()

        try {
            val player = MediaPlayer()
            if (audioDataOrPath.startsWith("data:audio")) {
                // Decode to temp file for MediaPlayer
                val commaIndex = audioDataOrPath.indexOf(",")
                val base64 = if (commaIndex != -1) audioDataOrPath.substring(commaIndex + 1) else audioDataOrPath
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                val tempFile = File.createTempFile("playback_", ".m4a", context.cacheDir)
                tempFile.writeBytes(bytes)
                player.setDataSource(tempFile.absolutePath)
            } else if (currentOutputFile != null && currentOutputFile!!.exists()) {
                player.setDataSource(currentOutputFile!!.absolutePath)
            } else {
                player.setDataSource(audioDataOrPath)
            }

            player.prepare()
            player.setOnCompletionListener {
                _isPlaying.value = false
                player.release()
                mediaPlayer = null
            }
            player.start()
            mediaPlayer = player
            _isPlaying.value = true
        } catch (e: Exception) {
            Log.e("VoiceRecording", "Failed to play audio", e)
            _errorMessage.value = "Playback failed"
            _isPlaying.value = false
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (e: Exception) {
            Log.w("VoiceRecording", "Error stopping player", e)
        } finally {
            mediaPlayer = null
            _isPlaying.value = false
        }
    }

    fun clearRecording() {
        stopPlayback()
        currentOutputFile?.delete()
        currentOutputFile = null
        _recordedAudioData.value = null
        _errorMessage.value = null
    }

    fun release() {
        try {
            mediaRecorder?.release()
        } catch (e: Exception) { /* ignore */ }
        stopPlayback()
        currentOutputFile?.delete()
    }
}
