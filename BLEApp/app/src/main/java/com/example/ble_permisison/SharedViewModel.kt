package com.example.ble_permission

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    private val _recommendedCalories = MutableLiveData<Int>()
    val recommendedCalories: LiveData<Int> get() = _recommendedCalories

    private val _recommendedSodium = MutableLiveData<Int>()
    val recommendedSodium: LiveData<Int> get() = _recommendedSodium

    private val _recommendedCarbs = MutableLiveData<Int>()
    val recommendedCarbs: LiveData<Int> get() = _recommendedCarbs

    private val _recommendedProteins = MutableLiveData<Int>()
    val recommendedProteins: LiveData<Int> get() = _recommendedProteins

    private val _recommendedFat = MutableLiveData<Int>()
    val recommendedFat: LiveData<Int> get() = _recommendedFat

    fun setRecommendedValues(
        calories: Int,
        sodium: Int,
        carbs: Int,
        proteins: Int,
        fat: Int
    ) {
        _recommendedCalories.value = calories
        _recommendedSodium.value = sodium
        _recommendedCarbs.value = carbs
        _recommendedProteins.value = proteins
        _recommendedFat.value = fat
    }
}
