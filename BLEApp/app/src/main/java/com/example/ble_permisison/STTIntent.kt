package com.example.ble_permisison

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import java.util.Locale

class STTIntent(
    private val context: Context,
    private val textView: TextView,
    private val resultLauncher: ActivityResultLauncher<Intent>
) {

    // Function to start listening with STT
    fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something")
        }

        // Launch the STT intent
        resultLauncher.launch(intent)
    }

    // Optional: Function to handle STT results and update the TextView
    fun handleSTTResult(resultData: Intent?) {
        val results = resultData?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        if (!results.isNullOrEmpty()) {
            textView.text = results[0]
        }
    }
}
