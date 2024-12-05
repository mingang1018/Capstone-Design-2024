package com.example.ble_permisison

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavGraph
import androidx.navigation.Navigation

import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.example.ble_permisison.BLE.BleScan.bleConnect
import com.example.ble_permisison.BLE.BleScan.receivedData
import com.example.ble_permisison.BLE.BleScan.sendDataToBleDevice
import com.github.anastr.speedviewlib.SpeedView


class HomeFragment : Fragment(), VoiceCommandReceiver {

    private lateinit var inputText: TextView
    private lateinit var speedView: SpeedView
    private lateinit var weightBar: ProgressBar
    private lateinit var slash: TextView
    private lateinit var setData: TextView
    private val handler = Handler() // Handler 생성
    private val updateInterval: Long = 500 // 1초마다 갱신
    var errorRate: Float? = null //퍼센트 오차율
    var errorValue: Float? = null //고정 오차율
    var currentMode = 0
    private lateinit var ttsIntent: TTSIntent
    private lateinit var ttsSwitch: Switch
    private lateinit var setInputData: String
    private lateinit var progressBar_setData: String
    // 변수 추가: 영점 설정이 이미 되었는지 확인하는 플래그
    private var isTerraSet: Boolean = false
    var ttsOnOff = 0
    private var oldRecivedData: Int = 0 // 빈 문자열로 초기화
    // Runnable 정의
    private val updateTask = object : Runnable {
        override fun run() {
            // UI를 주기적으로 업데이트
            updateUI()
            // Handler에 다시 이 Runnable을 일정 시간 후 실행하도록 설정
            handler.postDelayed(this, updateInterval)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = HomeFragmentArgs.fromBundle(requireArguments())
        setInputData = args.receivedWeight // 데이터를 미리 받아옴
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        inputText = view.findViewById(R.id.input_weight)
        weightBar = view.findViewById(R.id.progressBar)
        speedView = view.findViewById(R.id.speedView)
        slash = view.findViewById(R.id.slash)
        setData = view.findViewById(R.id.set_weight)
        ttsSwitch = view.findViewById(R.id.tts_switch)
        STTText.terraButton = view.findViewById<Button>(R.id.terra_button)
        val ttsSwitch: Switch = view.findViewById(R.id.tts_switch)
        ttsIntent = TTSIntent(requireActivity())

        // SharedPreferences에서 TTS 설정을 불러옴
        val sharedPref = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val isTtsEnabled = sharedPref.getBoolean("TTS_ENABLED", true) // 기본값은 true

        // Switch의 상태를 SharedPreferences에 저장된 값으로 설정
        ttsSwitch.isChecked = isTtsEnabled

        // Switch의 상태가 변경될 때 SharedPreferences에 저장
        ttsSwitch.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPref.edit()
            editor.putBoolean("TTS_ENABLED", isChecked)
            editor.apply()
        }

        val setButton = view.findViewById<Button>(R.id.setButton)
        setButton.setOnClickListener {
            findNavController().navigate(R.id.nav_weight)
        }



        STTText.terraButton.setOnClickListener {
            // 이미 영점 설정이 완료된 경우, 중복 실행을 방지
            if (isTerraSet) {
                Log.d("WeightFragment", "영점 설정이 이미 완료됨. 중복 호출 방지")
                isTerraSet = false
                return@setOnClickListener
            }

            try {
                val mainActivity = activity as? MainActivity
                mainActivity?.let {
                    sendDataToBleDevice(it, "Terra") // MainActivity 인스턴스를 사용하여 BLE 전송
                }
                ttsIntent.speakText(getString(R.string.terra_sound))

                // 영점 설정 완료 후 플래그를 true로 설정
                isTerraSet = true
            } catch (e: NumberFormatException) {
                Log.d("WeightFragment", "resetButton error")
            }
        }


        // SpeedView 단위 설정
        speedView.unit = "g"

        // SpeedView 초기 상태로 복귀
        speedView.clearSections() // 모든 섹션을 삭제
        speedView.speedTo(0f) // 속도를 0으로 설정
        speedView.maxSpeed = 5000f // 기본 최대값으로 설정
        speedView.addSections(
            com.github.anastr.speedviewlib.components.Section(
                0f, 0.7f, // 기본 섹션 추가 (예시로 50% 이하 빨간색 섹션)
                Color.GREEN, dpToPx(30f)
            )
        )
        speedView.addSections(
            com.github.anastr.speedviewlib.components.Section(
                0.7f, 0.85f, // 기본 섹션 추가 (예시로 50-75% 노란색 섹션)
                Color.YELLOW, dpToPx(30f)
            )
        )
        speedView.addSections(
            com.github.anastr.speedviewlib.components.Section(
                0.85f, 1f, // 기본 섹션 추가 (예시로 75-100% 초록색 섹션)
                Color.RED, dpToPx(30f)
            )
        )

//        val args = HomeFragmentArgs.fromBundle(requireArguments())
//
//        setInputData = args.receivedWeight
        setValueMode(setInputData.toInt())
        return view
    }

    // 주기적으로 호출될 함수 정의
    @SuppressLint("ResourceType", "StringFormatInvalid")
    private fun updateUI() {
        Log.d("SuccessValue", "SuccessValue: ${bleData.settingSuccessData}")

        if (bleConnect == true && bleData != null && bleData.scaleData != null ) {
            var scaleValue: Int = bleData.scaleData
            var setInputValue: Int = bleData.setScaleData
            inputText.visibility = View.VISIBLE
            //이전 값과 다른 경우 tts실행
            if( scaleValue != oldRecivedData){
                oldRecivedData = scaleValue
                ttsOnOff = 1
            }

            if(bleConnect == true){
                layourView.batteryImage.visibility = View.VISIBLE
                layourView.batteryText.visibility = View.VISIBLE
                layourView.percent_text.visibility = View.VISIBLE

                // 배터리 잔량에 따른 이미지 변경 로직
                val batteryLevel = bleData.batteryData ?: 0 // null-safe 처리

                val batteryImageResource = when {
                    batteryLevel == 100 -> R.drawable.battery_full// 100% 이상
                    batteryLevel >= 85 -> R.drawable.battery_6// 80% 이상
                    batteryLevel >= 68 -> R.drawable.battery_5 // 68% 이상
                    batteryLevel >= 51 -> R.drawable.battery_4 // 51% 이상
                    batteryLevel >= 34 -> R.drawable.battery_3 // 34% 이상
                    batteryLevel >= 17 -> R.drawable.battery_2 // 17% 이상
                    batteryLevel >= 5 -> R.drawable.battery_1 // 5% 이상
                    batteryLevel > 0 -> R.drawable.battery_caution // 5% 미만
                    else -> R.drawable.battery_empty // 0% 이상
                }

                val batteryTextColor = when {
                    batteryLevel == 100 -> R.color.battery_text_color_normal// 100% 이상
                    batteryLevel >= 85 -> R.color.battery_text_color_normal// 80% 이상
                    batteryLevel >= 68 -> R.color.battery_text_color_normal // 68% 이상
                    batteryLevel >= 51 -> R.color.battery_text_color_normal // 51% 이상
                    batteryLevel >= 34 -> R.color.battery_text_color_normal // 34% 이상
                    batteryLevel >= 17 -> R.color.battery_text_color_normal // 17% 이상
                    batteryLevel >= 5 -> R.color.battery_text_color_normal // 5% 이상
                    batteryLevel > 0 -> R.color.battery_text_color_caution // 5% 미만
                    else -> R.color.battery_text_color_caution // 0% 이상

                }
                // 색상 값을 ContextCompat.getColor로 가져오도록 수정
                val batteryTextColorValue = ContextCompat.getColor(requireContext(), batteryTextColor)

                // TextView 색상 설정
                layourView.batteryText.setTextColor(batteryTextColorValue)
                layourView.batteryImage.setImageResource(batteryImageResource)
                layourView.percent_text.setTextColor(batteryTextColorValue)
                Log.d("BatteryValue", "BatteryValue: ${bleData.batteryData}")
                layourView.batteryText.text=getString(R.string.batteryValue_text, bleData.batteryData.toString())
            }else{
                layourView.batteryImage.visibility = View.INVISIBLE
                layourView.batteryText.visibility = View.INVISIBLE
                layourView.percent_text.visibility = View.INVISIBLE
            }

            setValueMode(setInputValue)


            if(currentMode == 0){
                // receivedData가 변경되었는지 확인하고 UI를 갱신
                inputText.text = getString(R.string.recivedDataText, scaleValue.toString())
                if(ttsOnOff == 1){
                    ttsIntent.speakText(inputText.text.toString())
                    ttsOnOff = 0
                }
                val speedValue = scaleValue.toFloat()
                speedView.speedTo(speedValue, 500) // 애니메이션 속도 500ms
                speedView.setSpeedAt(speedValue)
                weightBar.visibility = View.GONE
                slash.visibility = View.GONE
                setData.visibility = View.GONE
                weightBar.visibility = View.INVISIBLE
            }else if( currentMode == 1) {
                // receivedData가 변경되었는지 확인하고 또한 setData가 입력되었는지를 확인
                inputText.text = getString(R.string.recivedDataText, scaleValue.toString())
                if(ttsOnOff == 1){
                    ttsIntent.speakText(inputText.text.toString())
                    ttsOnOff = 0
                }

                val speedValue = scaleValue.toFloat()
                speedView.speedTo(speedValue, 500) // 애니메이션 속도 500ms
                speedView.setSpeedAt(speedValue)

                slash.visibility = View.VISIBLE
                setData.visibility = View.VISIBLE
                weightBar.visibility = View.VISIBLE
                if(setInputData == "0"){
                    setInputData = setInputValue.toString()
                    Log.e("SetInputVale", "SetInputData: $setInputData, setInputValue: $setInputValue")
                }
                progressBar_setData = setInputData
                setData.text = getString(R.string.set_weight, progressBar_setData)

                onProgressBarValueReceived(scaleValue.toDouble())
                setMaxWeight(setInputData.toFloat())
                onSpeedViewValueReceived(setInputData.toInt(), speedView.maxSpeed)
                speedView.speedTo(speedValue, 500) // 애니메이션 속도 500ms
                speedView.setSpeedAt(speedValue)
            }
        }
        else{
            inputText.visibility = View.INVISIBLE
            slash.visibility = View.GONE
            setData.visibility = View.GONE
            weightBar.visibility = View.INVISIBLE
        }
        if (bleData.settingSuccessData == 1){
            currentMode = 0
            // SpeedView 초기 상태로 복귀
            resetSpeedViewToDefault()
            Log.d("SuccessFragment","Success!")
        } else{
        }
    }


    override fun onKeywordRecognized(recognizedText: String) {
        Log.d("HomeFragmentKeyword", "Recognized in HomeFragment: $recognizedText")
    }


    override fun onDetach() {
        super.onDetach()
        // Fragment가 분리될 때 리스너를 제거하여 메모리 누수 방지
        (requireActivity() as? MainActivity)?.keywordDetectedListener = null
    }

    override fun onResume() {
        super.onResume()
        // Fragment가 활성화될 때 UI 갱신을 시작
        handler.post(updateTask)
    }

    override fun onPause() {
        super.onPause()
        // Fragment가 비활성화될 때 Handler의 업데이트를 중지
        handler.removeCallbacks(updateTask)
    }



    private fun onProgressBarValueReceived(capstonTestValue: Double) {
        val setDataValue = progressBar_setData.toDoubleOrNull()

        if (setDataValue != null && setDataValue != 0.0 ) {
            val result = capstonTestValue / setDataValue
            val percent = (result * 1000).toInt()

            Log.d("Calculated Percent", "Percent: $percent")

            // Get the LayerDrawable from the ProgressBar
            val progressBarDrawable = weightBar.progressDrawable as LayerDrawable

            // Get the ClipDrawable which wraps the actual progress drawable
            val clipDrawable = progressBarDrawable.findDrawableByLayerId(android.R.id.progress) as ClipDrawable

            // Get the underlying GradientDrawable inside the ClipDrawable
            val gradientDrawable = clipDrawable.drawable as GradientDrawable

            if (percent > 1000) {
                // Change the progress bar color to red
                gradientDrawable.colors = intArrayOf(
                    ContextCompat.getColor(requireActivity(), R.color.red_500),
                    ContextCompat.getColor(requireActivity(), R.color.red_700),
                )

            } else {
                // Revert to the original colors
                gradientDrawable.colors = intArrayOf(
                    ContextCompat.getColor(requireActivity(), R.color.start),
                    ContextCompat.getColor(requireActivity(), R.color.end)
                )

            }
            weightBar.progress = percent
        }
    }


    //SpeedView(저울계)를 변경하는 코드, setupdata는 설정한 값, maxSpeedValue는 표시할 최대 무게치
    private fun onSpeedViewValueReceived(setupData: Int, maxSpeedValue: Float) {
        var tolerance: Float = 0.0f
        speedView.clearSections()
        if(errorRate != null && errorValue == null){
            // 오차율로 범위 계산
            tolerance = setupData * errorRate!!
        }
        else if(errorRate == null && errorValue != null){
            tolerance = errorValue!!
        }
        val lowAdjustedFinalValue = setupData - tolerance
        val lowAdjustedYellowValue:Float = (setupData - tolerance)*0.7f
        val highAdjustedFinalValue = (setupData + tolerance)
        val highAdjustedYellowValue: Float = (setupData + tolerance)*1.3f
        if(highAdjustedFinalValue/ maxSpeedValue < 1f){
            if(highAdjustedYellowValue >= maxSpeedValue){
                speedView.addSections(
                    com.github.anastr.speedviewlib.components.Section(
                        0f, lowAdjustedYellowValue/maxSpeedValue,
                        Color.RED, dpToPx(30f)
                    )
                )
                speedView.addSections(
                    com.github.anastr.speedviewlib.components.Section(
                        lowAdjustedYellowValue/maxSpeedValue, lowAdjustedFinalValue/ maxSpeedValue,
                        Color.YELLOW, dpToPx(30f)
                    )
                )
                speedView.addSections(
                    com.github.anastr.speedviewlib.components.Section(
                        lowAdjustedFinalValue/maxSpeedValue, highAdjustedFinalValue/maxSpeedValue,
                        Color.GREEN, dpToPx(30f)
                    )
                )
                speedView.addSections(
                    com.github.anastr.speedviewlib.components.Section(
                        highAdjustedFinalValue/maxSpeedValue, 1f,
                        Color.YELLOW, dpToPx(30f)
                    )
                )
            } else{
                speedView.addSections(
                    com.github.anastr.speedviewlib.components.Section(
                        0f, lowAdjustedYellowValue/maxSpeedValue,
                        Color.RED, dpToPx(30f)
                    )
                )
                speedView.addSections(
                    com.github.anastr.speedviewlib.components.Section(
                        lowAdjustedYellowValue/maxSpeedValue, lowAdjustedFinalValue/ maxSpeedValue,
                        Color.YELLOW, dpToPx(30f)
                    )
                )
                speedView.addSections(
                    com.github.anastr.speedviewlib.components.Section(
                        lowAdjustedFinalValue/maxSpeedValue, highAdjustedFinalValue/maxSpeedValue,
                        Color.GREEN, dpToPx(30f)
                    )
                )
                speedView.addSections(
                    com.github.anastr.speedviewlib.components.Section(
                        highAdjustedFinalValue/maxSpeedValue, highAdjustedYellowValue/maxSpeedValue,
                        Color.YELLOW, dpToPx(30f)
                    )
                )
                speedView.addSections(
                    com.github.anastr.speedviewlib.components.Section(
                        highAdjustedYellowValue/maxSpeedValue, 1f,
                        Color.RED, dpToPx(30f)
                    )
                )
            }
        }else{
            speedView.addSections(
                com.github.anastr.speedviewlib.components.Section(
                    0f, lowAdjustedYellowValue / maxSpeedValue,
                    Color.RED, dpToPx(30f)
                )
            )

            speedView.addSections(
                com.github.anastr.speedviewlib.components.Section(
                    lowAdjustedYellowValue / maxSpeedValue, lowAdjustedFinalValue / maxSpeedValue,
                    Color.YELLOW, dpToPx(30f)
                )
            )
            speedView.addSections(
                com.github.anastr.speedviewlib.components.Section(
                    lowAdjustedFinalValue / maxSpeedValue, 1f,
                    Color.GREEN, dpToPx(30f)
                )
            )
        }
    }

    // SpeedView를 초기 상태로 되돌리는 메서드
    private fun resetSpeedViewToDefault() {
        speedView.clearSections() // 모든 섹션 삭제
        speedView.speedTo(0f) // 속도 초기화
        speedView.maxSpeed = 5000f // 기본 최대값 설정

        speedView.addSections(
            com.github.anastr.speedviewlib.components.Section(
                0f, 0.7f,
                Color.GREEN, dpToPx(30f)
            )
        )
        speedView.addSections(
            com.github.anastr.speedviewlib.components.Section(
                0.7f, 0.85f,
                Color.YELLOW, dpToPx(30f)
            )
        )
        speedView.addSections(
            com.github.anastr.speedviewlib.components.Section(
                0.85f, 1f,
                Color.RED, dpToPx(30f)
            )
        )
    }

    private fun dpToPx(dp: Float): Float {
        return dp * Resources.getSystem().displayMetrics.density
    }

    private fun setMaxWeight(setData: Float){
        if (setData <= 50.0f){
            speedView.maxSpeed = 70.0f
            errorRate = 0.09f //오차율 9.0%
            errorValue = null
        }
        else if(50f < setData && setData<= 100f){
            speedView.maxSpeed = 130.0f
            errorRate = null
            errorValue = 4.5f //오차값 4.5
        }
        else if(100f < setData && setData<= 200f){
            speedView.maxSpeed = 230.0f
            errorRate = 0.045f //오차율 4.5%
            errorValue = null
        }
        else if(200f < setData && setData <=300f){
            speedView.maxSpeed = 330.0f
            errorRate = null
            errorValue = 9.0f //오차값 9
        }
        else if(300f < setData && setData <= 500f){
            speedView.maxSpeed = 550.0f
            errorRate = 0.03f //오차율 3%
            errorValue = null
        }
        else if(500f < setData && setData <= 1000.0f){
            speedView.maxSpeed = 1100.0f
            errorRate = null
            errorValue = 18.0f //오차값 18
        }
        else if(1000f < setData && setData <= 1100f){
            speedView.maxSpeed = 1200.0f
            errorRate = 0.015f
            errorValue = null
        }
        else if(1100f < setData && setData <= 2000f){
            speedView.maxSpeed = 2200.0f
            errorRate = 0.015f
            errorValue = null
        }
        else if(2000f < setData && setData <= 3000f){
            speedView.maxSpeed = 3200.0f
            errorRate = 0.015f
            errorValue = null
        }
        else if(3000f < setData && setData <= 4100f){
            speedView.maxSpeed = 4250.0f
            errorRate = 0.015f
            errorValue = null
        }else {
            speedView.maxSpeed = 5000f
            errorRate = 0.015f
            errorValue = null
        }

    }

    private fun setValueMode(setScale: Int){
        if(setScale != 0 && setScale.toString().isNotEmpty()){
            currentMode = 1
        } else{
            currentMode = 0
        }
    }

}
