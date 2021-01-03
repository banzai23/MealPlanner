package com.example.mealplanner

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.mealplanner.databinding.FragmentNewPlanBinding

class NewPlanFragment : Fragment(R.layout.fragment_new_plan) {
	private var _binding: FragmentNewPlanBinding? = null
	lateinit var updateAct: ActivityInterface

	override fun onAttach(context: Context) {
		super.onAttach(context)
		updateAct = context as ActivityInterface
	}
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val binding = FragmentNewPlanBinding.bind(view)
		_binding = binding

		binding.calendarView.minDate = binding.calendarView.date

		binding.btnSelectDate.setOnClickListener {
			updateAct.updateRecyclerDate(binding.calendarView.date)

			for (x in 0 until mealPlanList.recipe.size) {
				mealPlanList.recipe[x].name = " "
				mealPlanList.recipe[x].ingredients = " "
			}
			updateAct.updateRecyclerMP()

			requireActivity().supportFragmentManager.popBackStack()
		}
		binding.btnCancelCal.setOnClickListener {
			requireActivity().supportFragmentManager.popBackStack()
		}
	}
	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}
}