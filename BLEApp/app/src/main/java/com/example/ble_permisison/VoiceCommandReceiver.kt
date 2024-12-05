// VoiceCommandReceiver.kt
package com.example.ble_permisison

interface VoiceCommandReceiver {
    fun onKeywordRecognized(recognizedText: String)
}
