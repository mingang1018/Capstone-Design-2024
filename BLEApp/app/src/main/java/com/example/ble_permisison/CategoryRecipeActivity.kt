package com.example.ble_permisison

import android.os.Bundle
import android.util.Log
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.BufferedReader
import java.io.InputStreamReader

class CategoryRecipeActivity : AppCompatActivity() {

    private lateinit var recipeList: List<RecipeModel>
    private lateinit var filteredList: MutableList<RecipeModel>
    private lateinit var adapter: RecipeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_recipe)

        // 인텐트에서 카테고리 받기
        val category = intent.getStringExtra("CATEGORY")

        // CSV 파일에서 데이터 가져오기 (예시)
        recipeList = loadRecipeData()

        // 해당 카테고리로 필터링
        filteredList = recipeList.filter { it.category == category }.toMutableList()

        // RecyclerView 설정
        val recyclerView: RecyclerView = findViewById(R.id.recipe_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = RecipeAdapter(filteredList)
        recyclerView.adapter = adapter

        val searchView: SearchView = findViewById(R.id.search_recipe)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterRecipes(newText)
                return true
            }
        })
    }

    private fun loadRecipeData(): List<RecipeModel> {
        // CSV 파일에서 데이터를 불러오는 로직
        val recipeList = mutableListOf<RecipeModel>()
        try {
            val inputStream = assets.open("recipe_data_test_jw.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))
            // 첫 번째 줄은 헤더이므로 건너뛰기
            reader.readLine() // 첫 번째 줄 무시


            reader.forEachLine { line ->
                val tokens = line.split(",")

                // 레시피 정보가 충분히 있는지 확인 (최소 5개 이상의 항목)
                if (tokens.size >= 5) {
                    val name = tokens[0]            // 메뉴 이름 (RCP_NM)
                    val description = tokens[1]     // 레시피 설명 (RCP_DE)
                    val category = tokens[2]        // 카테고리 (RCP_CT)

                    // 재료를 쉼표 기준으로 분리하여 배열로 변환 (List<String>)
                    // 여기서 재료 목록에서 불필요한 줄바꿈을 제거합니다.
                    val ingredients = tokens[3].replace("\n", "").split(";").map { it.trim() }
                    Log.d("Data_IG", "Ingredients: ${ingredients}")  // 배열을 문자열로 변환하여 출력

                    // 영양 정보를 배열로 저장
                    val nutrient = listOf(
                        tokens[4],
                        tokens[5],
                        tokens[6],
                        tokens[7],
                        tokens[8]
                    )
                    val absSize = tokens[9]
                    // 조리 단계 가져오기 (RCP_RC1 ~ RCP_RC10)
                    val steps = tokens.subList(10, tokens.size).filter { it.isNotBlank() }

                    // RecipeModel에 데이터를 추가
                    recipeList.add(
                        RecipeModel(
                            name,
                            description,
                            category,
                            ingredients,
                            nutrient,
                            absSize,
                            steps
                        )
                    )
                }
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return recipeList
    }

    private fun filterRecipes(query: String?) {
        val searchText = query?.lowercase() ?: ""
        filteredList.clear()

        if (searchText.isNotEmpty()) {
            val filtered = recipeList.filter {
                it.name.lowercase().contains(searchText) || it.description.lowercase().contains(searchText)
            }
            filteredList.addAll(filtered)
        } else {
            // 검색어가 없으면 모든 레시피를 다시 보여줌
            filteredList.addAll(recipeList)
        }

        // 필터링된 데이터를 어댑터에 반영
        adapter.notifyDataSetChanged()
    }

}
