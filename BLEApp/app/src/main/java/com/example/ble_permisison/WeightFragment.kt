package com.example.ble_permisison

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import com.example.ble_permisison.BLE.BleScan.sendDataToBleDevice
import com.google.android.material.button.MaterialButton
import java.util.Locale

class WeightFragment : Fragment() {

    private lateinit var ttsIntent: TTSIntent
    private lateinit var sttIntent: STTIntent
    private lateinit var sttResultLauncher: ActivityResultLauncher<Intent>
    lateinit var loopButton: MaterialButton
    lateinit var micButton : ImageButton
    private var isAwaitingConfirmation = false // Flag for confirmation response
    private var oldValue: String = ""
    private var finalTextValue: String = "" // 입력된 값을 저장할 변수
    private var STTText: String =""
    var loopValue: Boolean = false

    val inputValue = StringBuilder()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_weight, container, false)
        val numberDataTextView = view.findViewById<TextView>(R.id.number_data)
        numberDataTextView.text = getString(R.string.number_text_gram, "0") // 초기값 설정
        val buttons = listOf(
            view.findViewById<Button>(R.id.one),
            view.findViewById<Button>(R.id.two),
            view.findViewById<Button>(R.id.three),
            view.findViewById<Button>(R.id.four),
            view.findViewById<Button>(R.id.five),
            view.findViewById<Button>(R.id.six),
            view.findViewById<Button>(R.id.seven),
            view.findViewById<Button>(R.id.eight),
            view.findViewById<Button>(R.id.nine),
            view.findViewById<Button>(R.id.zero),
        )
        val enterButton = view.findViewById<Button>(R.id.enter)
        val deleteButton = view.findViewById<Button>(R.id.Delete)
        val resetButton = view.findViewById<Button>(R.id.Reset)



        loopButton = view.findViewById(R.id.loop_button)

        // 초기 스타일 설정
        updateButtonStyle()

        // 버튼 클릭 시 동적 변경
        loopButton.setOnClickListener {
            Log.d("LoopValue","LoopValue Before: ${loopValue})")
            Log.d("ButtonClick", "Before click: bleData.setloopData=${bleData.setloopData}")
            if(loopValue){
                try {
                    val mainActivity = activity as? MainActivity
                    mainActivity?.let {
                        sendDataToBleDevice(it, "LoopOff") // MainActivity 인스턴스를 사용하여 BLE 전송
                    }

                    // 영점 설정 완료 후 플래그를 true로 설정
                    loopValue = false
                    applyButtonCardOffStyle(loopButton)
                } catch (e: NumberFormatException) {
                    Log.d("WeightFragment", "resetButton error")
                }
            }else {
                try {
                    val mainActivity = activity as? MainActivity
                    mainActivity?.let {
                        sendDataToBleDevice(it, "LoopOn") // MainActivity 인스턴스를 사용하여 BLE 전송
                    }

                    // 영점 설정 완료 후 플래그를 true로 설정
                    loopValue = true
                    applyButtonCardStyle(loopButton)
                } catch (e: NumberFormatException) {
                    Log.d("WeightFragment", "resetButton error")
                }
            }
        }

        micButton = view.findViewById<ImageButton>(R.id.stt_button)



        ttsIntent = TTSIntent(requireActivity())
        // Register STT result launcher to handle STT results
        sttResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (!results.isNullOrEmpty()) {
                    STTText = results[0].lowercase(Locale.getDefault())
                    Log.d("STTResult", "Recognized text: $STTText")

                    if (isAwaitingConfirmation) {
                        // Handle confirmation response
                        handleConfirmationResponse(STTText, enterButton)
                    } else {
                        // Handle numeric input response
                        handleNumericInput(STTText, numberDataTextView)
                    }
                }
            }
        }


        // Initialize STTIntent and pass the resultLauncher
        sttIntent = STTIntent(requireContext(), numberDataTextView, sttResultLauncher)



        var exceeded_value: Int = 0

        // 숫자 버튼 클릭 시 처리
        buttons.forEach { button ->
            button.setOnClickListener {
                var proposedValue = inputValue.toString() + button.text.toString()

                if (isInputValid(proposedValue)) {
                    proposedValue = proposedValue.trimStart('0') // 0으로 시작하는 경우 제거
                    if (proposedValue.isEmpty() || proposedValue.startsWith(".")) {
                        proposedValue = "0$proposedValue" // 소수점이 앞에 오는 경우 "0."으로 만듦
                    }

                    val numericValue = proposedValue.toDouble()
                    if (numericValue > 5000) {
                        exceeded_value = 1
                        Toast.makeText(activity, "입력값은 5000을 초과할 수 없습니다.", Toast.LENGTH_SHORT).show()
                        ttsIntent.speakText(getString(R.string.exceeded_sound))
                        finalTextValue = "5000" // finalTextValue에 저장
                        numberDataTextView.text = getString(R.string.number_text_gram, finalTextValue) // getString 사용
                        inputValue.clear()
                        inputValue.append("5000")
                    } else {
                        inputValue.clear()
                        inputValue.append(proposedValue)
                        finalTextValue = inputValue.toString() // 입력 값을 finalTextValue에 저장
                        numberDataTextView.text = getString(R.string.number_text_gram, finalTextValue) // getString 사용
                    }

                    if (oldValue != inputValue.toString() && exceeded_value == 0) {
                        oldValue = inputValue.toString()
                        ttsIntent.speakText(numberDataTextView.text.toString())
                    }

                } else {
                    Toast.makeText(activity, "잘못된 입력입니다.", Toast.LENGTH_SHORT).show()
                    ttsIntent.speakText(getString(R.string.error_sound))
                }
                exceeded_value = 0
            }
        }

        // 삭제 버튼 클릭 시 처리
        deleteButton.setOnClickListener {
            if (inputValue.isNotEmpty()) {
                inputValue.deleteCharAt(inputValue.length - 1)
                finalTextValue = inputValue.toString() // 삭제 후 값 저장
                numberDataTextView.text = getString(R.string.number_text_gram, finalTextValue) // getString 사용
                if (inputValue.toString() != "0") {
                    ttsIntent.speakText(getString(R.string.delete_sound, numberDataTextView.text.toString()))
                }
            }
            if (inputValue.isEmpty()) {
                numberDataTextView.text = getString(R.string.number_text_gram, "0") // 입력이 없을 때 0으로 기본값 설정
                ttsIntent.speakText("0이므로 값을 입력해주세요.")
            }
        }

        resetButton.setOnClickListener {
            inputValue.clear()
            ttsIntent.speakText(getString(R.string.reset_sound))
            finalTextValue = "0"
            try {
                val mainActivity = activity as? MainActivity
                mainActivity?.let {
                    sendDataToBleDevice(it, finalTextValue) // MainActivity 인스턴스를 사용하여 BLE 전송
                    val action = WeightFragmentDirections.actionNavWeightToHome(finalTextValue)
                    Navigation.findNavController(view).navigate(action)
                }
            } catch (e: NumberFormatException) {
                Log.d("WeightFragment", "resetButton error")
            }
        }

        micButton.setOnClickListener{
            sttIntent.startListening().toString()
            Log.d("MicButtonResult","Result: ${STTText}")
        }



        // 엔터 버튼 클릭 시 BLE로 데이터 전송
        enterButton.setOnClickListener {
            val finalValue = finalTextValue

            if (finalValue.isBlank() || finalValue == "0" || finalValue == "0.0" || finalValue == ".") {
                Toast.makeText(activity, "올바른 값을 입력하거나 0이 아닌 값을 입력하세요.", Toast.LENGTH_SHORT).show()
                ttsIntent.speakText(getString(R.string.error_sound))
                return@setOnClickListener
            }

            try {
                val mainActivity = activity as? MainActivity
                mainActivity?.let {
                    sendDataToBleDevice(it, finalValue)
                    Log.d("SendData","데이터 전송됨: $finalValue")
                    ttsIntent.speakText(getString(R.string.enter_sound, finalValue))
                    val action = WeightFragmentDirections.actionNavWeightToHome(finalValue)
                    Navigation.findNavController(view).navigate(action)
                }
            } catch (e: NumberFormatException) {
                Toast.makeText(activity, "올바른 숫자를 입력하세요.", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    // 입력값 유효성 검사
    private fun isInputValid(input: String): Boolean {
        if (input.count { it == '.' } > 1) {
            Toast.makeText(activity, "소수점은 한 번만 입력할 수 있습니다", Toast.LENGTH_SHORT).show()
            return false
        }

        if (input.contains('.')) {
            val decimalPart = input.substringAfter('.')
            if (decimalPart.length > 1) {
                Toast.makeText(activity, "소수점은 첫 번째 자리까지 입력할 수 있습니다", Toast.LENGTH_SHORT).show()
                return false
            }
        }

        return try {
            input.toDouble()
            true
        } catch (e: NumberFormatException) {
            false
        }
    }

    private fun handleNumericInput(text: String, numberDataTextView: TextView) {
        val normalizedText = text.lowercase(Locale.getDefault()).replace(" ", "")
        Log.d("TextValue", "MicButton Text: $normalizedText")

        val gramValue: Int? = when {

            normalizedText.contains("kg") || normalizedText.contains("Kg") ||normalizedText.contains("kG") || normalizedText.contains("KG") -> {
                // "kg" 단위일 경우: "1.5kg" -> 1500
                Log.d("KG input","kg인식")
                val kgValueText = normalizedText.removeSuffix("kg")
                try {
                    val numberFormat = java.text.NumberFormat.getInstance(Locale.getDefault())
                    val kgValue = numberFormat.parse(kgValueText)?.toDouble() ?: 0.0
                    (kgValue * 1000).toInt()
                } catch (e: Exception) {
                    Log.d("ConversionError", "Invalid kg value: $kgValueText, error: ${e.message}")
                    null
                }
            }
            
            normalizedText.contains("g") -> {
                // "g" 단위일 경우: "500g" -> 500
                normalizedText.removeSuffix("g").toIntOrNull()
            }

            else -> {
                // 단위가 없을 경우 정수로 간주
                text.toIntOrNull()
            }
        }

        if (gramValue != null && gramValue in 1..4999) {
            finalTextValue = gramValue.toString()
            numberDataTextView.text = getString(R.string.number_text_gram, finalTextValue)

            // Queue TTS prompt for confirmation
            ttsIntent.speakText(getString(R.string.number_text_gram, finalTextValue), TextToSpeech.QUEUE_FLUSH)
            ttsIntent.speakText("값을 전송하실건가요?", TextToSpeech.QUEUE_ADD, "CONFIRMATION_PROMPT")

            // Set up listener to start confirmation STT after TTS prompt finishes
            ttsIntent.setOnUtteranceCompletedListener { utteranceId ->
                if (utteranceId == "CONFIRMATION_PROMPT") {
                    requireActivity().runOnUiThread {
                        isAwaitingConfirmation = true // Set flag for confirmation
                        sttIntent.startListening() // Start listening for confirmation
                    }
                }
            }
        } else {
            Toast.makeText(activity, "1에서 4999 사이의 값을 말씀해 주세요.", Toast.LENGTH_SHORT).show()
            ttsIntent.speakText("유효한 값을 말씀해 주세요.")
            Log.d("TextError", "${normalizedText}는 유효한 값이 아닙니다.")
        }
    }



    private fun handleConfirmationResponse(text: String, enterButton: Button) {
        isAwaitingConfirmation = false // Reset flag

        if (text.contains("네") || text.contains("예")) {
            // Simulate enterButton click to send data
            enterButton.performClick()
        } else {
            Toast.makeText(activity, "전송이 취소되었습니다.", Toast.LENGTH_SHORT).show()
            ttsIntent.speakText("전송이 취소되었습니다.")
        }
    }

    fun applyButtonCardStyle(button: MaterialButton) {
        Log.d("ButtonStyle", " ON ")
        button.strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.purple_700)
        button.backgroundTintList = ContextCompat.getColorStateList(requireContext(), android.R.color.white)
        button.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        button.invalidate() // 강제 UI 갱신
    }

    fun applyButtonCardOffStyle(button: MaterialButton) {
        Log.d("ButtonStyle", " OFF ")
        button.strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.purple_700)
        button.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.grayAccent)
        button.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        button.invalidate() // 강제 UI 갱신

    }

    private fun updateButtonStyle() {
        Log.d("UpdateStyle", "Updating button style. Current bleData.setloopData=${bleData.setloopData}")
        if (bleData.setloopData == 1) {
            loopValue = true
            applyButtonCardStyle(loopButton)
        } else {
            loopValue = false
            applyButtonCardOffStyle(loopButton)
        }
    }

}
