package com.example.mealplanner

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mealplanner.databinding.FragmentLoadPlanBinding
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

class LoadPlanFragment : Fragment(R.layout.fragment_load_plan) {
	private var _binding: FragmentLoadPlanBinding? = null
	lateinit var updateAct: ActivityInterface

	override fun onAttach(context: Context) {
		super.onAttach(context)
		updateAct = context as ActivityInterface
	}
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val binding = FragmentLoadPlanBinding.bind(view)
		_binding = binding

		val directory = requireContext().filesDir.path+File.separatorChar+DEFAULT_PLAN_DIR
		val filesInDir = File(directory)
		val savedPlansList = filesInDir.list()

		binding.recyclerPlan.layoutManager = LinearLayoutManager(context)
		val adapterPlan = RecyclerAdapterPlans(savedPlansList)
		binding.recyclerPlan.adapter = adapterPlan
		binding.btnCancel.setOnClickListener {
			requireActivity().supportFragmentManager.popBackStack()
		}
		binding.btnLoad.setOnClickListener {
			adapterPlan.getSelectFile()
			val selectedFile: String = adapterPlan.getSelectFile()
			if (selectedFile != "0") {
				val file = File(directory, selectedFile)
				val inputString = file.readText()
				mealPlanList = Json.decodeFromString(inputString)

				updateAct.updateRecyclerDate(mealPlanList.startDate)
				updateAct.updateRecyclerMP()
				requireActivity().supportFragmentManager.popBackStack()
			}
		}
	}
	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}
}