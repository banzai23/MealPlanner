package com.example.mealplanner

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.mealplanner.databinding.FragmentBlankViewBinding

class ViewMPRecipeFragment(position: Int): Fragment(R.layout.fragment_blank_view) {
	private var _binding: FragmentBlankViewBinding? = null
	private val pos = position
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val binding = FragmentBlankViewBinding.bind(view)
		_binding = binding

		binding.tvMp.text = mealPlanList.recipe[pos].name
		binding.tvIngredients.text = mealPlanList.recipe[pos].ingredients

		binding.btnBack.setOnClickListener {
			requireActivity().supportFragmentManager.popBackStack()
		}
	}
	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}
}