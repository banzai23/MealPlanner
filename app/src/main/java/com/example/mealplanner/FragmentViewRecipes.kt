package com.example.mealplanner

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.mealplanner.databinding.FragmentViewRecipesBinding

class ViewRecipesFragment(private val pos: Int, private val master: Boolean) : Fragment(R.layout.fragment_view_recipes) {
	private var _binding: FragmentViewRecipesBinding? = null
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val binding = FragmentViewRecipesBinding.bind(view)
		_binding = binding

		if (master) {
			binding.tvName.text = masterRecipeList.recipe[pos].name
			binding.tvIngredients.text = masterRecipeList.recipe[pos].ingredients
			binding.tvInstructions.text = masterRecipeList.recipe[pos].instructions
		} else {
			binding.tvName.text = mealPlanList.recipe[pos].name
			binding.tvIngredients.text = mealPlanList.recipe[pos].ingredients
			binding.tvInstructions.text = mealPlanList.recipe[pos].instructions
		}

		binding.btnBack.setOnClickListener {
			requireActivity().supportFragmentManager.popBackStack()
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}
}