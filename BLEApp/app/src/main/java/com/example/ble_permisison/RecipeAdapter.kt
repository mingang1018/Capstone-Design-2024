package com.example.ble_permisison

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import android.content.Intent
import android.util.Log

class RecipeAdapter(private val recipeList: List<RecipeModel>) :
    RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        holder.bind(recipeList[position])
    }

    override fun getItemCount(): Int = recipeList.size

    class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.recipe_name)
        private val categoryTextView: TextView = itemView.findViewById(R.id.recipe_category)
        private val imageView: ImageView = itemView.findViewById(R.id.menu_image)

        fun bind(recipe: RecipeModel) {

            Log.d("BIND_DEBUG", "Name: ${recipe.name}, Category: ${recipe.category}, Description: ${recipe.description}, Ingredients: ${recipe.ingredients}, Steps: ${recipe.steps}")

            // 레시피 데이터를 UI에 바인딩
            nameTextView.text = recipe.name
            categoryTextView.text = recipe.category


            val imageResId = when (recipe.category) {
                "볶음/구이" -> R.mipmap.stir_fry_dish_icon  // 볶음/구이 카테고리일 때
                "국/탕/찌개" -> R.mipmap.soup_stew_dish_icon
                "전/튀김" -> R.mipmap.korean_pancake_fry_dish_icon
                "밥/죽/떡" -> R.mipmap.rice_porridge_rice_cake_dish_icon
                "디저트/샐러드" -> R.mipmap.dessert_salad_icon
                "무침" -> R.mipmap.seasoned_dish_icon
                else -> R.mipmap.ic_launcher                   // 기본 아이콘
            }

            imageView.setImageResource(imageResId)

// RecipeAdapter에서 클릭 이벤트 처리 시 재료 배열을 넘겨줌
            itemView.setOnClickListener {
                val intent = Intent(itemView.context, RecipeDetailActivity::class.java)
                intent.putExtra("RECIPE_NAME", recipe.name)
                intent.putExtra("RECIPE_DESCRIPTION", recipe.description)
                intent.putExtra("RECIPE_INGREDIENTS", recipe.ingredients.toTypedArray()) // 재료 배열로 전달
                intent.putExtra("RECIPE_NUTRIENT", recipe.nutrient.toTypedArray())
                intent.putExtra("RECIPE_ABS_SIZE", recipe.absSize)
                intent.putExtra("RECIPE_STEPS", recipe.steps.toTypedArray())
                itemView.context.startActivity(intent)
            }

        }
    }
}

