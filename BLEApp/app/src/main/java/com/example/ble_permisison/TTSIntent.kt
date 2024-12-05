package com.example.ble_permisison

import android.content.Context
import android.speech.tts.TextToSpeech
import android.widget.Toast
import java.util.Locale

class TTSIntent(private val context: Context) {
    private var textToSpeech: TextToSpeech? = null

    init {
        // TextToSpeech 초기화
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(context, "Language not supported", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun speakText(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH, utteranceId: String = "TTS") {
        val sharedPref = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val isTtsEnabled = sharedPref.getBoolean("TTS_ENABLED", true)

            if (isTtsEnabled) {
            textToSpeech?.speak(text, queueMode, null, utteranceId)
        }
    }

    fun setOnUtteranceCompletedListener(listener: TextToSpeech.OnUtteranceCompletedListener) {
        textToSpeech?.setOnUtteranceCompletedListener(listener)
    }

    // TTS 자원을 해제하는 함수
    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}
