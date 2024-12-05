package com.example.ble_permisison

data class RecipeModel(
    val name: String,       // 레시피 이름 (RCP_NM)
    val description: String, // 레시피 설명 (RCP_DE)
    val category: String,    // 레시피 카테고리 (RCP_CT)
    val ingredients: List<String>, // 재료 목록을 배열로 저장 (List<String>)
    val nutrient: List<String>,    // 영양 정보 (RCP_CAL 등)
    val absSize: String, //열량기준값
    val steps: List<String>  // 레시피의 조리 단계 목록 (RCP_RC1 ~ RCP_RC10)
)
