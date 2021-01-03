package com.example.mealplanner

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mealplanner.databinding.FragmentSavePlanBinding
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

class SavePlanFragment : Fragment(R.layout.fragment_save_plan) {
	private var _binding: FragmentSavePlanBinding? = null

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val binding = FragmentSavePlanBinding.bind(view)
		_binding = binding

		val directory = requireContext().filesDir.path+File.separatorChar+DEFAULT_PLAN_DIR
		val filesInDir = File(directory)
		var savedPlansList = filesInDir.list()
		if(savedPlansList == null)
			savedPlansList = arrayOfNulls<String>(0)

		binding.recyclerPlan.layoutManager = LinearLayoutManager(this.context)
		binding.recyclerPlan.adapter = RecyclerAdapterPlans(savedPlansList)

		binding.etSavePlan.setText(mealPlanList.getDefaultSaveDate())

		binding.btnCancel.setOnClickListener {
			requireActivity().supportFragmentManager.popBackStack()
		}
		binding.btnSave.setOnClickListener {
			val str: String = binding.etSavePlan.text.toString()
			if(str.contains("/")) {
				println(str)
			}
			else {
				val file = File(directory+File.separatorChar, str)
				val fileStream: FileOutputStream = FileOutputStream(file)
				fileStream.use {
					val jsonToFile = Json.encodeToString(mealPlanList)
					it.write(jsonToFile.toByteArray())
				}
				requireActivity().supportFragmentManager.popBackStack()
			}
		}
	}
	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}
}
class RecyclerAdapterPlans(private val array: Array<String>) :
	RecyclerView.Adapter<RecyclerAdapterPlans.ViewHolder>()
{
	var selectedPos: Int = 0
	var selectedFile: String = "0"

	class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
		val tvPlan: TextView = v.findViewById(R.id.tv_plan)
	}
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val inflatedView = LayoutInflater.from(parent.context).inflate(
				R.layout.recycler_saved_plans,
				parent,
				false)
		return ViewHolder(inflatedView)
	}
	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.tvPlan.text = array[position] // get name of plans saved in current directory
		if (selectedPos == position) {
			holder.itemView.setBackgroundColor(Color.DKGRAY)
			selectedFile = array[position]
		}
		else
			holder.itemView.setBackgroundColor(Color.TRANSPARENT)

		holder.itemView.setOnClickListener {
			if (holder.adapterPosition != RecyclerView.NO_POSITION) {
				notifyItemChanged(selectedPos)
				selectedPos = holder.adapterPosition
				notifyItemChanged(selectedPos)
			}
		}
	}
	override fun getItemCount() = array.size

	public fun getSelectFile(): String {
		return selectedFile
	}
}