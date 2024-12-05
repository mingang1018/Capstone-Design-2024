package com.example.ble_permisison

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.ble_permisison.BLE.BleScan
import com.example.ble_permisison.BLE.BleScan.bleConnect
import com.example.ble_permisison.BLE.BleScan.receivedData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar


class RecipeDetailActivity : AppCompatActivity() {
    private lateinit var kcal_text: TextView
    private lateinit var car_text: TextView
    private lateinit var pro_text: TextView
    private lateinit var fat_text: TextView
    private lateinit var na_text: TextView
    private lateinit var progress_kcal: ProgressBar
    private lateinit var progress_car: ProgressBar
    private lateinit var progress_pro: ProgressBar
    private lateinit var progress_fat: ProgressBar
    private lateinit var progress_na: ProgressBar

    private lateinit var scale_OnOff_button: ImageButton
    private lateinit var scale_input_text: EditText
    private lateinit var scale_input_button: Button

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateRunnable: Runnable

    private var recommend_kcal: String = ""
    private var recommend_na: String = ""
    private var recommend_car: String = ""
    private var recommend_pro: String = ""
    private var recommend_fat: String = ""

    private var scaleOnOff: Boolean = false

    private var scalePercent: Float = 1f

    var recipeNutrientPublic: Array<String>? = null

    @SuppressLint("MissingInflatedId", "DefaultLocale", "ResourceAsColor",
        "UseCompatLoadingForDrawables", "ResourceType"
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail)

        updateRunnable = object : Runnable {
            override fun run() {
                if (scaleOnOff) {
                    // BLE로부터 실시간 데이터를 가져와 EditText에 설정
                    scale_input_text.setText(bleData.scaleData.toString())
                }
                // 1초마다 다시 실행
                handler.postDelayed(this, 1000)
            }
        }
        
        // 이제 findViewById를 호출합니다.
        kcal_text = findViewById(R.id.kcal_size_text)
        car_text = findViewById(R.id.car_size_text)
        pro_text = findViewById(R.id.pro_size_text)
        fat_text = findViewById(R.id.fat_size_text)
        na_text = findViewById(R.id.na_size_text)

        progress_kcal = findViewById(R.id.progressbar_kcal)
        progress_car = findViewById(R.id.progressbar_car)
        progress_pro = findViewById(R.id.progressbar_pro)
        progress_fat = findViewById(R.id.progressbar_fat)
        progress_na = findViewById(R.id.progressbar_na)

        scale_OnOff_button = findViewById(R.id.scale_button)
        scale_input_text = findViewById(R.id.weight_input)
        scale_input_button = findViewById(R.id.send_scale_button)

        // Intent로 전달된 데이터 받기
        val recipeName = intent.getStringExtra("RECIPE_NAME")
        val recipeDescription = intent.getStringExtra("RECIPE_DESCRIPTION")
        val recipeIngredients = intent.getStringArrayExtra("RECIPE_INGREDIENTS") // 배열로 전달받기
        val recipeNutrient = intent.getStringArrayExtra("RECIPE_NUTRIENT") // 영양 정보를 배열로 받음
        val recipeAbsSize = intent.getStringExtra("RECIPE_ABS_SIZE")
        val recipeSteps = intent.getStringArrayExtra("RECIPE_STEPS")

        recipeNutrientPublic = recipeNutrient
        loadPersonalInformation()

        val sendButton:Button = findViewById(R.id.send_button)

        val abs_text = findViewById<TextView>(R.id.reference_text)


        // 레이아웃의 TextView에 데이터 설정
        val nameTextView: TextView = findViewById(R.id.recipe_detail_name)
        val descriptionTextView: TextView = findViewById(R.id.recipe_detail_description)
//        val nutrientTextView: TextView = findViewById(R.id.recipe_detail_nutrient)

        var kcal_data = ((recipeNutrient?.get(0)?.toFloatOrNull() ?: 0f) * 100).toInt()
        var car_data = ((recipeNutrient?.get(1)?.toFloatOrNull() ?: 0f) * 100).toInt()
        var pro_data = ((recipeNutrient?.get(2)?.toFloatOrNull() ?: 0f) * 100).toInt()
        var fat_data = ((recipeNutrient?.get(3)?.toFloatOrNull() ?: 0f) * 100).toInt()
        var na_data = ((recipeNutrient?.get(4)?.toFloatOrNull() ?: 0f) * 100).toInt()



        Log.d("NutrientData","Kcal: ${kcal_data}, Car: ${car_data}, Pro: ${pro_data}, Fat: ${fat_data}, NA: ${na_data}")

        progress_kcal.progress = kcal_data
        exceedProgressBar(progress_kcal, kcal_data)
        progress_car.progress = car_data
        exceedProgressBar(progress_car, car_data)
        progress_pro.progress = pro_data
        exceedProgressBar(progress_pro, pro_data)
        progress_fat.progress = fat_data
        exceedProgressBar(progress_fat, fat_data)
        progress_na.progress = na_data
        exceedProgressBar(progress_na, na_data)

        val stepsTextView: TextView = findViewById(R.id.recipe_detail_steps)

        // 텍스트 설정
        nameTextView.text = recipeName ?: "이름 없음"
        descriptionTextView.text = recipeDescription ?: "설명 없음"

        // 재료 리스트를 CheckBox로 추가할 레이아웃
        val ingredientsLayout: LinearLayout = findViewById(R.id.ingredients_layout)


        // 재료 리스트를 CheckBox로 추가할 레이아웃
        val checkBoxList = mutableListOf<CheckBox>()

        if(scaleOnOff == true){
            scale_OnOff_button.background = getDrawable(R.drawable.image_button_background)
            editTextScaleClickOnOff(scale_input_text, scaleOnOff)
            handler.post(updateRunnable) // 업데이트 시작
            Log.d("ScaleOnOff","on")
        }else{
            scale_OnOff_button.background = getDrawable(R.drawable.image_button_background_off)
            editTextScaleClickOnOff(scale_input_text, scaleOnOff)
            handler.removeCallbacks(updateRunnable) // 업데이트 중단
            Log.d("ScaleOnOff","Off")
        }



        // 재료 배열이 null이 아니면 각 재료를 CheckBox로 추가
        recipeIngredients?.forEach { ingredient ->
            val checkBox = CheckBox(this).apply {
                text = ingredient
                setTextColor(Color.BLACK)  // 모든 CheckBox의 글자 색을 검은색으로 설정
                // "g" 또는 "ml"로 끝나지 않으면 CheckBox 비활성화
                isEnabled = ingredient.endsWith("g") || ingredient.endsWith("ml")
            }
            ingredientsLayout.addView(checkBox)
            checkBoxList.add(checkBox) // 추가된 CheckBox를 리스트에 저장
        }
        scale_OnOff_button.setOnClickListener{
            if(scaleOnOff == true){
                scaleOnOff = false
                editTextScaleClickOnOff(scale_input_text, scaleOnOff)
                scale_OnOff_button.background = getDrawable(R.drawable.image_button_background_off)
                handler.removeCallbacks(updateRunnable) // 업데이트 중단
                Log.d("ScaleOnOff","off")

            }else{
                if(bleConnect == true){
                    scaleOnOff = true
                    scale_OnOff_button.background = getDrawable(R.drawable.image_button_background)
                    editTextScaleClickOnOff(scale_input_text, scaleOnOff)
                    scale_input_text.setText(bleData.scaleData.toString())
                    handler.post(updateRunnable) // 업데이트 시작
                    Log.d("ScaleOnOff","on")
                }else{
                    Toast.makeText(this, getString(R.string.ble_connect_error),Toast.LENGTH_SHORT).show()
                    handler.removeCallbacks(updateRunnable) // 업데이트 중단
                }
            }
        }

        // 버튼 클릭 시 체크된 항목 수집 및 숫자 추출
        sendButton.setOnClickListener {
            if(bleConnect == true){
                // 체크된 항목 추출
                val selectedValues = checkBoxList.filter { it.isChecked }
                    .map { it.text.toString() }
                // 선택된 항목에서 숫자만 추출
                val bleSendData = selectedValues.mapNotNull { ingredient ->
                    // 정규식을 사용하여 숫자 부분 추출
                    val regex = Regex("\\d+")
                    regex.find(ingredient)?.value
                }.toTypedArray()

                // 선택된 값과 추출된 숫자들을 로그로 출력
                Log.d("SelectedValues", selectedValues.joinToString(", "))
                Log.d("BLESendData", bleSendData.joinToString(", "))

                // bleSendData 배열이 비어 있지 않은 경우, 데이터를 BLE 기기에 전송
                if (bleSendData.isNotEmpty()) {
                    // 데이터를 콤마로 구분된 문자열 형태로 변환
                    val dataToSend = bleSendData.joinToString(",")
                    Log.d("BLESendData", "전송할 데이터: $dataToSend")

                    // BLE 전송 함수 호출
                    BleScan.sendDataToBleDevice(this, "RecipeData")
                    // 1초 후에 데이터를 전송하도록 딜레이 설정
                    Handler(Looper.getMainLooper()).postDelayed({
                        BleScan.sendDataToBleDevice(this, dataToSend)
                    }, 1000) // 1000 밀리초 = 1초
                } else {
                    Toast.makeText(this, "전송할 데이터가 없습니다.", Toast.LENGTH_SHORT).show()
                }
                // 모든 CheckBox의 체크를 해제
                checkBoxList.forEach { checkBox ->
                    checkBox.isChecked = false
                }
                // 다이얼로그를 생성하여 선택된 값을 표시
                if(selectedValues.isNotEmpty()) {
                    showSelectedValuesDialog(selectedValues, bleSendData)
                } else{
                    Toast.makeText(this@RecipeDetailActivity, getString(R.string.checkbox_error), Toast.LENGTH_SHORT).show()
                }
            } else{
                Toast.makeText(this@RecipeDetailActivity, getString(R.string.ble_connect_error), Toast.LENGTH_SHORT).show()
            }

        }



        scale_input_button.setOnClickListener {
            if (scale_input_text.text.isNotEmpty()) {
                // 입력된 무게를 바탕으로 scalePercent를 계산
                hideKeyBoard()
                val inputWeight = scale_input_text.text.toString().toFloatOrNull()
                val originalWeight = recipeAbsSize?.toFloatOrNull()

                if (inputWeight != null && originalWeight != null) {
                    scalePercent = inputWeight / originalWeight
                    if(recommend_kcal.isNotEmpty() && recommend_na.isNotEmpty() && recommend_car.isNotEmpty() && recommend_pro.isNotEmpty() && recommend_fat.isNotEmpty()){
                        // 스케일링된 영양 성분 값들을 업데이트
                        kcal_text.text = getString(
                            R.string.kcal_max_text,
                            String.format("%.2f", recipeNutrientPublic?.get(0)?.toFloat()!! * scalePercent),
                            recommend_kcal
                        )
                        na_text.text = getString(
                            R.string.na_max_text,
                            String.format("%.2f", recipeNutrientPublic?.get(4)?.toFloat()!! * scalePercent),
                            recommend_na
                        )
                        car_text.text = getString(
                            R.string.car_max_text,
                            String.format("%.2f", recipeNutrientPublic?.get(1)?.toFloat()!! * scalePercent),
                            recommend_car
                        )
                        pro_text.text = getString(
                            R.string.pro_max_text,
                            String.format("%.2f", recipeNutrientPublic?.get(2)?.toFloat()!! * scalePercent),
                            recommend_pro
                        )
                        fat_text.text = getString(
                            R.string.fat_max_text,
                            String.format("%.2f", recipeNutrientPublic?.get(3)?.toFloat()!! * scalePercent),
                            recommend_fat
                        )

                        // 진행바 업데이트 및 초과 처리
                        val kcalProgress = ((recipeNutrientPublic?.get(0)?.toFloatOrNull() ?: 0f) * 100 * scalePercent).toInt()
                        progress_kcal.progress = kcalProgress
                        exceedProgressBar(progress_kcal, kcalProgress)

                        val naProgress = ((recipeNutrientPublic?.get(4)?.toFloatOrNull() ?: 0f) * 100 * scalePercent).toInt()
                        progress_na.progress = naProgress
                        exceedProgressBar(progress_na, naProgress)

                        val carProgress = ((recipeNutrientPublic?.get(1)?.toFloatOrNull() ?: 0f) * 100 * scalePercent).toInt()
                        progress_car.progress = carProgress
                        exceedProgressBar(progress_car, carProgress)

                        val proProgress = ((recipeNutrientPublic?.get(2)?.toFloatOrNull() ?: 0f) * 100 * scalePercent).toInt()
                        progress_pro.progress = proProgress
                        exceedProgressBar(progress_pro, proProgress)

                        val fatProgress = ((recipeNutrientPublic?.get(3)?.toFloatOrNull() ?: 0f) * 100 * scalePercent).toInt()
                        progress_fat.progress = fatProgress
                        exceedProgressBar(progress_fat, fatProgress)

                    } else{
                        kcal_text.text = getString(
                            R.string.kcal_size_text,
                            String.format("%.2f", recipeNutrientPublic?.get(0)?.toFloat()!! * scalePercent)
                        )
                        car_text.text = getString(
                            R.string.car_size_text,
                            String.format("%.2f", recipeNutrientPublic?.get(1)?.toFloat()!! * scalePercent)
                        )
                        pro_text.text = getString(
                            R.string.pro_size_text,
                            String.format("%.2f", recipeNutrientPublic?.get(2)?.toFloat()!! * scalePercent)
                        )
                        fat_text.text = getString(
                            R.string.fat_size_text,
                            String.format("%.2f", recipeNutrientPublic?.get(3)?.toFloat()!! * scalePercent)
                        )
                        na_text.text = getString(
                            R.string.na_size_text,
                            String.format("%.2f", recipeNutrientPublic?.get(4)?.toFloat()!! * scalePercent)
                        )

                        // 진행바 업데이트 및 초과 처리
                        val kcalProgress = ((recipeNutrientPublic?.get(0)?.toFloatOrNull() ?: 0f) * 100 * scalePercent).toInt()
                        progress_kcal.progress = kcalProgress
                        exceedProgressBar(progress_kcal, kcalProgress)

                        val naProgress = ((recipeNutrientPublic?.get(4)?.toFloatOrNull() ?: 0f) * 100 * scalePercent).toInt()
                        progress_na.progress = naProgress
                        exceedProgressBar(progress_na, naProgress)

                        val carProgress = ((recipeNutrientPublic?.get(1)?.toFloatOrNull() ?: 0f) * 100 * scalePercent).toInt()
                        progress_car.progress = carProgress
                        exceedProgressBar(progress_car, carProgress)

                        val proProgress = ((recipeNutrientPublic?.get(2)?.toFloatOrNull() ?: 0f) * 100 * scalePercent).toInt()
                        progress_pro.progress = proProgress
                        exceedProgressBar(progress_pro, proProgress)

                        val fatProgress = ((recipeNutrientPublic?.get(3)?.toFloatOrNull() ?: 0f) * 100 * scalePercent).toInt()
                        progress_fat.progress = fatProgress
                        exceedProgressBar(progress_fat, fatProgress)

                    }


                    // abs_text 업데이트
                    Toast.makeText(this@RecipeDetailActivity, getString(R.string.scale_send_text, scale_input_text.text.toString()), Toast.LENGTH_SHORT).show()
                    abs_text.text = getString(R.string.abs_text, scale_input_text.text.toString())
                } else {
                    // 유효하지 않은 입력일 경우 오류 메시지 출력
                    Toast.makeText(this@RecipeDetailActivity, getString(R.string.scale_send_error), Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this@RecipeDetailActivity, getString(R.string.scale_send_error), Toast.LENGTH_SHORT).show()
            }
        }





        abs_text.text = getString(R.string.abs_text, recipeAbsSize?:"Null")

        // 배열로 받은 영양 정보를 줄바꿈으로 포맷팅
        val formattedNutrient = recipeNutrient?.joinToString("\n") ?: "성분 없음"
        val formattedSteps = recipeSteps?.mapIndexed { index, step ->
            "${index + 1}. $step" // 각 단계에 번호를 추가
        }?.joinToString("\n\n") ?: "레시피 단계가 없습니다."

// TextView에 텍스트 설정
        stepsTextView.text = formattedSteps
    }

    private fun exceedProgressBar(progressBarView: ProgressBar, progressBarData: Int) {
        val progressBarMax = progressBarView.max
        val progress = progressBarView.progress

        Log.d("ProgressBarDebug", "Progress: $progress, Max: $progressBarMax")

        // progress가 max 값을 초과했는지 여부 확인
        if (progressBarData > progressBarMax) {
            Log.d("ProgressBarStatus", "Progress exceeds max value")
            // 추가로 경고 또는 UI 업데이트 처리 가능
            progressBarView.progressTintList = ContextCompat.getColorStateList(this, R.color.red_500)
        } else{
            progressBarView.progressTintList = ContextCompat.getColorStateList(this, R.color.progressNormal)
        }
    }


    @SuppressLint("MissingInflatedId")
    private fun showSelectedValuesDialog(selectedValues: List<String>, BLESendData: Array<String>) {
        // 다이얼로그를 위한 커스텀 뷰를 인플레이트
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_selected_values, null)
        val selectedValuesContainer = dialogView.findViewById<LinearLayout>(R.id.selected_values_container)
        val currentWeight = dialogView.findViewById<TextView>(R.id.current_weight)
        val recipeSet_Weight = dialogView.findViewById<TextView>(R.id.recipe_set_weight)
        // 선택된 값들을 TextView로 추가 및 저장할 리스트 생성

        recipeSet_Weight.text = getString(R.string.set_weight, BLESendData[0])

        val textViewList = selectedValues.mapIndexed { index, value ->
            TextView(this).apply {
                text = "${index + 1}. $value \n"
                setTextColor(Color.BLACK)
                textSize = 16f
            }.also { textView ->
                selectedValuesContainer.addView(textView)
            }
        }

        // 다이얼로그 생성 및 표시 (취소할 수 없도록 설정)
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)  // Prevent dismissing the dialog by tapping outside or pressing the back button
            .create()
        alertDialog.show()

        // 현재 진행 중인 항목의 인덱스를 저장
        var currentStepIndex = 0

        // CoroutineScope for managing the coroutine lifecycle
        val scope = CoroutineScope(Dispatchers.Main)

        // Launch a coroutine to continuously monitor `recipeBoolen`
        scope.launch {
            while (alertDialog.isShowing && currentStepIndex < textViewList.size) {
                // Check if `recipeBoolen` is true and update the UI
                Log.d("RecipeIndex","RecipeValueIndex: ${currentStepIndex}, ListSize: ${textViewList.size}, RecipeValue: ${BLESendData[currentStepIndex]}")
                if(bleData.recipeNextData == 0){
                    currentWeight.text = getString(R.string.current_weight, bleData.scaleData.toString())
                }
                if (bleData.recipeNextData == 1 && currentStepIndex < textViewList.size) {
                    try {
                        Thread.sleep(2000) // 2s 지연
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }

                    Log.d("RecipeBoolen", "RecipeNextStep: ${BleScan.recipeBoolen}")
                    val currentTextView = textViewList[currentStepIndex]
                    // Apply strikethrough
                    currentTextView.paintFlags =
                        currentTextView.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                    currentStepIndex++
                    if(currentStepIndex < textViewList.size) {
                        recipeSet_Weight.text =
                            getString(R.string.set_weight, BLESendData[currentStepIndex])
                    }
                    // Reset `recipeBoolen` to false
                    BleScan.recipeBoolen = false
                    Log.d("DialogStep","CurrentStepIndex: ${currentStepIndex}, ListSize: ${textViewList.size}")
                    // Check if all items have been struck through, then close the dialog
                    if (currentStepIndex >= textViewList.size) {
                        Log.d("DialogStep","Dialog End")
                        Toast.makeText(this@RecipeDetailActivity, getString(R.string.recipe_weight_Completed), Toast.LENGTH_SHORT).show()
                        alertDialog.dismiss()

                    }
                }

                // Add a small delay to avoid busy-waiting
                delay(200) // Check every 500ms
            }
        }

        // Cancel the coroutine when the dialog is dismissed to avoid memory leaks
        alertDialog.setOnDismissListener {
            scope.cancel()
        }
    }

    @SuppressLint("StringFormatMatches")
    private fun loadPersonalInformation() {
        try {
            val fileName = "Data_Personal_information.csv"
            val file = File(filesDir, fileName)

            // 파일이 존재하지 않으면 종료
            if (!file.exists()) {
                Log.e("RecipeDetailActivity", "CSV file does not exist.")
                kcal_text.text = getString(R.string.kcal_size_text, recipeNutrientPublic?.get(0)?.toString())
                car_text.text = getString(R.string.car_size_text, recipeNutrientPublic?.get(1)?.toString())
                pro_text.text = getString(R.string.pro_size_text, recipeNutrientPublic?.get(2)?.toString())
                fat_text.text = getString(R.string.fat_size_text, recipeNutrientPublic?.get(3)?.toString())
                na_text.text = getString(R.string.na_size_text, recipeNutrientPublic?.get(4)?.toString())
                return
            }

            // 파일을 줄 단위로 읽고 두 번째 줄 가져오기
            val lines = file.readLines()
            if (lines.size < 2) {
                kcal_text.text = getString(R.string.kcal_size_text, recipeNutrientPublic?.get(0)?.toString())
                car_text.text = getString(R.string.car_size_text, recipeNutrientPublic?.get(1)?.toString())
                pro_text.text = getString(R.string.pro_size_text, recipeNutrientPublic?.get(2)?.toString())
                fat_text.text = getString(R.string.fat_size_text, recipeNutrientPublic?.get(3)?.toString())
                na_text.text = getString(R.string.na_size_text, recipeNutrientPublic?.get(4)?.toString())

                Log.e("RecipeDetailActivity", "No data found in the second line of the CSV.")
                return
            }

            val data = lines[1].split(",")
            if (data.size == 12) {
                // 데이터를 로그로 확인
                Log.d("CSVData", "DATA_SEX: ${data[0]}")
                Log.d("CSVData", "DATA_H: ${data[1]}")
                Log.d("CSVData", "DATA_W: ${data[2]}")
                Log.d("CSVData", "DATA_YEAR: ${data[3]}")
                Log.d("CSVData", "DATA_MONTH: ${data[4]}")
                Log.d("CSVData", "DATA_DAY: ${data[5]}")
                Log.d("CSVData", "DATA_ACT: ${data[6]}")
                Log.d("CSVData", "DATA_RCAL: ${data[7]}")
                Log.d("CSVData", "DATA_NA: ${data[8]}")
                Log.d("CSVData", "DATA_RCAR: ${data[9]}")
                Log.d("CSVData", "DATA_RPRO: ${data[10]}")
                Log.d("CSVData", "DATA_RFAT: ${data[11]}")


                recommend_kcal = data[7]
                recommend_na = data[8]
                recommend_car = data[9]
                recommend_pro = data[10]
                recommend_fat = data[11]

                Log.d("RecommendData", "recommend_kcal: ${recommend_kcal}, recommend_na: ${recommend_na}, recommend_car: ${recommend_car}, recommend_pro: ${recommend_pro}, recommend_fat: ${recommend_fat}")

                // 생년월일 정보를 이용해 나이를 계산
                val year = data[3].toIntOrNull()
                val month = data[4].toIntOrNull()
                val day = data[5].toIntOrNull()
                val age = if (year != null && month != null && day != null) {
                    calculateAge(year, month - 1, day) // month는 0부터 시작하므로 -1
                } else {
                    null
                }

                kcal_text.text = getString(R.string.kcal_max_text, recipeNutrientPublic?.get(0)?.toString(), recommend_kcal)
                na_text.text = getString(R.string.na_max_text, recipeNutrientPublic?.get(4)?.toString(), recommend_na)
                car_text.text = getString(R.string.car_max_text, recipeNutrientPublic?.get(1)?.toString(), recommend_car)
                pro_text.text = getString(R.string.pro_max_text, recipeNutrientPublic?.get(2)?.toString(), recommend_pro)
                fat_text.text = getString(R.string.fat_max_text, recipeNutrientPublic?.get(3)?.toString(), recommend_na)


                progress_kcal.max = data[7].toInt()*100
                progress_na.max = data[8].toInt()*100
                progress_car.max = data[9].toInt()*100
                progress_pro.max = data[10].toInt()*100
                progress_fat.max = data[11].toInt()*100

            } else {
                Log.e("RecipeDetailActivity", "Unexpected data format in the CSV.")
            }
        } catch (e: Exception) {
            Log.e("RecipeDetailActivity", "Error reading CSV file", e)
        }
    }

    private fun calculateAge(year: Int, month: Int, day: Int): Int {
        val today = Calendar.getInstance()
        val birthDate = Calendar.getInstance()
        birthDate.set(year, month, day)

        var age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR)

        // 생일이 지나지 않았다면 나이를 하나 줄임
        if (today.get(Calendar.MONTH) < birthDate.get(Calendar.MONTH) ||
            (today.get(Calendar.MONTH) == birthDate.get(Calendar.MONTH) &&
                    today.get(Calendar.DAY_OF_MONTH) < birthDate.get(Calendar.DAY_OF_MONTH))
        ) {
            age--
        }

        return age
    }

    private fun hideKeyBoard(){
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun editTextScaleClickOnOff(textvalue: EditText, bleOnOff: Boolean) {
        if (bleOnOff) {
            textvalue.isFocusable = false
            textvalue.isClickable = false
            textvalue.isFocusableInTouchMode = false
        } else {
            textvalue.isFocusable = true
            textvalue.isClickable = true
            textvalue.isFocusableInTouchMode = true
        }
    }

}


