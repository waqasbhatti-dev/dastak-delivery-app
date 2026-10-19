package com.example.data.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.localization.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isUrduSupported = MutableStateFlow(true)
    val isUrduSupported: StateFlow<Boolean> = _isUrduSupported.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private var isInitialized = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    _statusMessage.value = null
                }

                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    _statusMessage.value = "Playback error occurred"
                }
            })
            checkUrduSupport()
        } else {
            isInitialized = false
            Log.e("TtsManager", "TTS initialization failed with status $status")
        }
    }

    private fun checkUrduSupport(): Boolean {
        val ttsInstance = tts ?: return false
        val urduLocale = Locale("ur", "PK")
        val fallbackUrdu = Locale("ur")
        val res = ttsInstance.isLanguageAvailable(urduLocale)
        val resFallback = ttsInstance.isLanguageAvailable(fallbackUrdu)
        val isSupported = (res >= TextToSpeech.LANG_AVAILABLE) || (resFallback >= TextToSpeech.LANG_AVAILABLE)
        _isUrduSupported.value = isSupported
        return isSupported
    }

    fun speak(text: String, language: AppLanguage) {
        val ttsInstance = tts ?: return
        if (!isInitialized) {
            _statusMessage.value = "Speech engine initializing..."
            return
        }

        stop()

        val locale = if (language == AppLanguage.URDU) {
            val urPk = Locale("ur", "PK")
            val ur = Locale("ur")
            if (ttsInstance.isLanguageAvailable(urPk) >= TextToSpeech.LANG_AVAILABLE) {
                urPk
            } else if (ttsInstance.isLanguageAvailable(ur) >= TextToSpeech.LANG_AVAILABLE) {
                ur
            } else {
                _isUrduSupported.value = false
                _statusMessage.value = "urdu_voice_not_found"
                return
            }
        } else {
            Locale.ENGLISH
        }

        val result = ttsInstance.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            if (language == AppLanguage.URDU) {
                _isUrduSupported.value = false
                _statusMessage.value = "urdu_voice_not_found"
            } else {
                _statusMessage.value = "English voice is not available on this device"
            }
            return
        }

        _isSpeaking.value = true
        _statusMessage.value = null
        ttsInstance.speak(text, TextToSpeech.QUEUE_FLUSH, null, "terms_utterance")
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
        _statusMessage.value = null
    }

    fun pause() {
        stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
