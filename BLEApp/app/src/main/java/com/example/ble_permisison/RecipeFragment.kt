package com.example.ble_permisison

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.BufferedReader
import java.io.InputStreamReader
import android.util.Log
import de.hdodenhof.circleimageview.CircleImageView

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class RecipeFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var recipeList: List<RecipeModel> // 레시피 목록
    private lateinit var filteredList: MutableList<RecipeModel> // 필터링된 목록
    private lateinit var adapter: RecipeAdapter // RecyclerView 어댑터

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 레이아웃 인플레이트
        val view = inflater.inflate(R.layout.fragment_recipe, container, false)

        view.findViewById<CircleImageView>(R.id.stir_fry_dish_icon).setOnClickListener {
            openCategory("볶음/구이")
        }

        view.findViewById<CircleImageView>(R.id.rice_porridge_rice_cake_dish_icon).setOnClickListener {
            openCategory("밥/죽/떡")
        }

        view.findViewById<CircleImageView>(R.id.korean_pancake_fry_dish_icon).setOnClickListener {
            openCategory("전/튀김")
        }

        view.findViewById<CircleImageView>(R.id.seasoned_dish_icon).setOnClickListener {
            openCategory("무침")
        }

        view.findViewById<CircleImageView>(R.id.soup_stew_dish_icon).setOnClickListener {
            openCategory("국/탕/찌개")
        }

        view.findViewById<CircleImageView>(R.id.dessert_salad_icon).setOnClickListener {
            openCategory("디저트/샐러드")
        }


        // RecyclerView 설정
        val recyclerView: RecyclerView = view.findViewById(R.id.recipe_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // CSV 파일에서 데이터를 읽어옴
        recipeList = readCsvFile("recipe_data_test_jw.csv")
        filteredList = recipeList.toMutableList() // 필터링용 리스트 초기화

        // RecyclerView에 어댑터 설정
        adapter = RecipeAdapter(filteredList)
        recyclerView.adapter = adapter

        // SearchView 설정
        val searchView: SearchView = view.findViewById(R.id.search_recipe)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterRecipes(newText)
                return true
            }
        }
        )

        return view
    }

    private fun openCategory(category: String) {
        Log.d("RecipeFragment", "Selected Category: $category")
        if (category.isNullOrBlank()) {
            // 예외 처리
            Log.e("RecipeFragment", "Category is null or empty")
            return
        }
        val intent = Intent(context, CategoryRecipeActivity::class.java)
        intent.putExtra("CATEGORY", category)
        startActivity(intent)
    }

    // CSV 파일 읽기 함수
    private fun readCsvFile(fileName: String): List<RecipeModel> {
        val recipeList = mutableListOf<RecipeModel>()
        try {
            val inputStream = requireContext().assets.open(fileName)
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
                    Log.d("Data_ABS", "ABS: ${absSize}")
                    // 조리 단계 가져오기 (RCP_RC1 ~ RCP_RC10)
                    val steps = tokens.subList(10, tokens.size).filter { it.isNotBlank() }

                    // RecipeModel에 데이터를 추가
                    recipeList.add(RecipeModel(name, description, category, ingredients, nutrient, absSize, steps))
                }
            }

            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return recipeList
    }

    // 레시피 필터링 함수
    private fun filterRecipes(query: String?) {
        val searchText = query?.lowercase() ?: ""
        filteredList.clear()

        // 검색어와 일치하는 레시피만 필터링
        if (searchText.isNotEmpty()) {
            val filtered = recipeList.filter {
                it.name.lowercase().contains(searchText) || it.description.lowercase().contains(searchText)
            }
            filteredList.addAll(filtered)
        } else {
            filteredList.addAll(recipeList) // 검색어가 없으면 전체 목록을 보여줌
        }

        adapter.notifyDataSetChanged() // 필터링된 결과를 갱신
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            RecipeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
