package com.example.ble_permisison

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.io.File
import java.util.Calendar

class SettingFragment : Fragment() {

    private lateinit var buttonMale: Button
    private lateinit var buttonFemale: Button
    private lateinit var heightInput: EditText
    private lateinit var weightInput: EditText
    private lateinit var ageInput: EditText
    private lateinit var birthInput: EditText
    private lateinit var activityLevelSpinner: Spinner
    private lateinit var recommendedCalories: TextView
    private lateinit var recommendedSodium: TextView
    private lateinit var recommendedCarbohydrates: TextView
    private lateinit var recommendedProteins: TextView
    private lateinit var recommendedFat: TextView

    private var gender: String = "male" // 초기값을 남성으로 설정
    private val fileName = "Data_Personal_information.csv"
    private var isDataLoaded = false // 데이터 로드 중인지 여부 확인

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_setting, container, false)

        // UI 요소 초기화
        buttonMale = view.findViewById(R.id.button_male)
        buttonFemale = view.findViewById(R.id.button_female)
        heightInput = view.findViewById(R.id.height_input)
        weightInput = view.findViewById(R.id.weight_input)
        ageInput = view.findViewById(R.id.age_input)
        activityLevelSpinner = view.findViewById(R.id.activity_level_spinner)
        recommendedCalories = view.findViewById(R.id.tv_recommended_calories)
        recommendedSodium = view.findViewById(R.id.tv_recommended_sodium)
        recommendedCarbohydrates = view.findViewById(R.id.tv_recommended_carbohydrates)
        recommendedProteins = view.findViewById(R.id.tv_recommended_proteins)
        recommendedFat = view.findViewById(R.id.tv_recommended_fat)
        birthInput = view.findViewById(R.id.birth_input) // 여기서 초기화

        // 초기 색상 설정
        setButtonState(isMaleSelected = true) // 초기값: 남자 선택

        // 남성 버튼 클릭 리스너
        buttonMale.setOnClickListener {
            gender = "male"
            setButtonState(isMaleSelected = true)
            calculateRecommendedIntake()
        }

        // 여성 버튼 클릭 리스너
        buttonFemale.setOnClickListener {
            gender = "female"
            setButtonState(isMaleSelected = false)
            calculateRecommendedIntake()
        }

        val birthInput = view.findViewById<EditText>(R.id.birth_input)

        // 생년월일 입력 필드를 클릭했을 때 DatePickerDialog를 보여줌
        birthInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(
                requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    // 선택된 날짜를 EditText에 설정 (월은 0부터 시작하므로 +1 필요)
                    birthInput.setText("$selectedYear-${selectedMonth + 1}-$selectedDay")

                    val age = calculateAge(selectedYear, selectedMonth, selectedDay)
                    ageInput.setText(age.toString())
                },
                year, month, day
            )
            datePicker.show()
        }

        // 활동 지수 스피너 변경 감지
        activityLevelSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                calculateRecommendedIntake()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

//        // CSV 파일에서 데이터 로드 시도
        loadUserData()

        return view
    }

    private fun loadUserData() {
        val fillAllFieldsTextView = view?.findViewById<TextView>(R.id.tv_fill_all_fields)
        val recommendedValuesLayout = view?.findViewById<View>(R.id.recommended_values_layout)
        try {
            val file = File(requireContext().filesDir, fileName)
            if (!file.exists()) return

            val lines = file.readLines()
            if (lines.size < 2) return // 데이터가 없는 경우 종료

            // 두 번째 행의 데이터를 쉼표(,)를 기준으로 나눔
            val data = lines[1].split(",")

            // 데이터 유효성 검사 (10개의 요소가 모두 있어야 함)
            if (data.size == 12) {
                // 모든 필드가 입력되었으면 메시지를 숨기고 권장 섭취량을 표시
                fillAllFieldsTextView?.visibility = View.GONE
                recommendedValuesLayout?.visibility = View.VISIBLE
                val gender = data[0]
                val height = data[1].toIntOrNull()
                val weight = data[2].toIntOrNull()
                val year = data[3].toIntOrNull()
                val month = data[4].toIntOrNull()
                val day = data[5].toIntOrNull()
                val activityLevel = data[6].toIntOrNull()
                val tdee = data[7].toIntOrNull()
                val sodium = data[8].toIntOrNull()
                val carbs = data[9].toIntOrNull()
                val protein = data[10].toIntOrNull()
                val fat = data[11].toIntOrNull()
                // 모든 데이터가 유효한 경우에만 UI에 적용
                if ((gender == "male" || gender == "female" ) &&height != null && weight != null && year != null && month != null && day != null && activityLevel != null) {
                    this.gender = gender
                    heightInput.setText(height.toString())
                    weightInput.setText(weight.toString())
                    // 생년월일을 사용하여 나이를 계산하고 ageInput에 설정
                    val calculatedAge = calculateAge(year, month - 1, day) // month는 0부터 시작하므로 -1
                    ageInput.setText(calculatedAge.toString())
                    // 생년월일을 다시 birthInput에도 설정
                    birthInput.setText("$year-${month}-$day")
                    activityLevelSpinner.setSelection(activityLevel)

                    // 성별에 따라 버튼 상태 설정
                    setButtonState(isMaleSelected = gender == "male")

                    // 데이터 로드 완료 플래그 설정
                    isDataLoaded = true

                    // 로드된 데이터로 권장 섭취량 계산 및 설정
                    calculateRecommendedIntake()

                    Log.d("SettingFragment", "User data loaded successfully from CSV")
                }
//                // 모든 데이터가 유효한 경우에만 UI에 적용
//                if (height != null && weight != null && age != null && activityLevel != null &&
//                    tdee != null && sodium != null && carbs != null && protein != null && fat != null
//                ) {
//                    // UI에 값 설정
//                    this.gender = gender
//                    heightInput.setText(height.toString())
//                    weightInput.setText(weight.toString())
//                    ageInput.setText(age.toString())
//                    activityLevelSpinner.setSelection(getSpinnerIndexFromActivityLevel(activityLevel))
//
//                    recommendedCalories.text = getString(R.string.recommended_cal, tdee)
//                    recommendedSodium.text = getString(R.string.recommended_na, sodium)
//                    recommendedCarbohydrates.text = getString(R.string.recommended_car, carbs)
//                    recommendedProteins.text = getString(R.string.recommended_pro, protein)
//                    recommendedFat.text = getString(R.string.recommended_fat, fat)
//
//                    // 성별에 따라 버튼 상태 설정
//                    setButtonState(isMaleSelected = gender == "male")
//
//                    // 프래그먼트에 로드 완료 로그 출력
//                    Log.d("SettingFragment", "User data loaded successfully from CSV")
//                }
            }
        } catch (e: Exception) {
            Log.e("SettingFragment", "Error loading user data", e)
        }
    }

    private fun getSpinnerIndexFromActivityLevel(activityLevel: Int): Double {
        var tdeeMultiplier = when (activityLevel) {
            0 -> 0.0
            1 -> 1.2
            2 -> 1.375
            3 -> 1.55
            4 -> 1.725
            5 -> 1.9
            else -> 1.0
        }
        return tdeeMultiplier
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

    private fun setButtonState(isMaleSelected: Boolean) {
        if (isMaleSelected) {
            buttonMale.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.blue)
            buttonFemale.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.gray)
        } else {
            buttonMale.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.gray)
            buttonFemale.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.pink)
        }
    }

    private fun calculateRecommendedIntake() {
        val height = heightInput.text.toString().toDoubleOrNull() ?: return
        val weight = weightInput.text.toString().toDoubleOrNull() ?: return
        val age = ageInput.text.toString().toIntOrNull() ?: return
        val activityLevel = activityLevelSpinner.selectedItemPosition
        Log.d("SettingFragment", "Activity level: $activityLevel")
        val birthInputText = birthInput.text.toString()
        val (year, month, day) = birthInputText.split("-").map { it.toIntOrNull() ?: return }
        val fillAllFieldsTextView = view?.findViewById<TextView>(R.id.tv_fill_all_fields)
        val recommendedValuesLayout = view?.findViewById<View>(R.id.recommended_values_layout)

        // 모든 필드가 입력되지 않은 경우 메시지를 표시하고 권장 섭취량 숨김
        if (height == null || weight == null || age == null) {
            fillAllFieldsTextView?.visibility = View.VISIBLE
            recommendedValuesLayout?.visibility = View.GONE
            return
        }

        // 모든 필드가 입력되었으면 메시지를 숨기고 권장 섭취량을 표시
        fillAllFieldsTextView?.visibility = View.GONE
        recommendedValuesLayout?.visibility = View.VISIBLE

        val bmr = if (gender == "male") {
            88.362 + (13.397 * weight) + (4.799 * height) - (5.677 * age)
        } else {
            447.593 + (9.247 * weight) + (3.098 * height) - (4.330 * age)
        }

        val tdeeMultiplier = when (activityLevel) {
            0 -> 0.0
            1 -> 1.2
            2 -> 1.375
            3 -> 1.55
            4 -> 1.725
            5 -> 1.9
            else -> 1.0
        }
        val tdee = bmr * tdeeMultiplier
        val recommendedSod = weight * 30
        val recommendedCarbs = (tdee * 0.55) / 4
        val recommendedPro = (tdee * 0.2) / 4
        val recommendedFt = (tdee * 0.25) / 9


        Log.d("SettingFragment", "Sending data - Calories: ${tdee.toInt()}, Sodium: ${recommendedSod.toInt()}, Carbs: ${recommendedCarbs.toInt()}, Proteins: ${recommendedPro.toInt()}, Fat: ${recommendedFt.toInt()}")

        recommendedCalories.text = getString(R.string.recommended_cal, tdee.toInt())
        recommendedSodium.text = getString(R.string.recommended_na, recommendedSod.toInt())
        recommendedCarbohydrates.text = getString(R.string.recommended_car, recommendedCarbs.toInt())
        recommendedProteins.text = getString(R.string.recommended_pro, recommendedPro.toInt())
        recommendedFat.text = getString(R.string.recommended_fat, recommendedFt.toInt())

        saveUserData(gender, height, weight, year, month, day, activityLevel, tdee.toInt(), recommendedSod.toInt(), recommendedCarbs.toInt(), recommendedPro.toInt(), recommendedFt.toInt())
    }

    private fun saveUserData(
        gender: String, height: Double, weight: Double, year: Int, month: Int, day: Int,
        activityLevel: Int, tdee: Int, sodium: Int, carbs: Int, protein: Int, fat: Int
    ) {
        try {
            val file = File(requireContext().filesDir, fileName)
            val header = "DATA_SEX,DATA_H,DATA_W,DATA_YEAR,DATA_MONTH,DATA_DAY,DATA_ACT,DATA_RCAL,DATA_NA,DATA_RCAR,DATA_RPRO,DATA_RFAT"
            val newData = "$gender,${height.toInt()},${weight.toInt()},$year,$month,$day,$activityLevel,$tdee,$sodium,$carbs,$protein,$fat"
            file.writeText("$header\n$newData")
            Log.d("SettingFragment", "User data saved: $newData")
            val savedData = file.readText()
            Log.d("SettingFragment", "File content after saving: $savedData")
        } catch (e: Exception) {
            Log.e("SettingFragment", "Error saving user data", e)
        }
    }
}
