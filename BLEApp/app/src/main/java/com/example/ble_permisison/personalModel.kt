package com.example.ble_permisison

data class personalModel(
    val gender: String, //성별
    val height: Double, //키
    val weight: Double, //몸무게
    val age: Int, //나이
    val activityLevel: Int, //활동량
    val recommendedCalories: Double,
    val recommendedSodium: Double,
    val recommendedCarbohydrates: Double,
    val recommendedProteins: Double,
    val recommendedFat: Double
)
