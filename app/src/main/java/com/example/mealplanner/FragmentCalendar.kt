package com.example.mealplanner

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.mealplanner.databinding.FragmentCalendarBinding
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.*

class CalendarFragment : Fragment(R.layout.fragment_calendar) {
	private var _binding: FragmentCalendarBinding? = null
	lateinit var updateAct: ActivityInterface

	override fun onAttach(context: Context) {
		super.onAttach(context)
		updateAct = context as ActivityInterface
	}
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val binding = FragmentCalendarBinding.bind(view)
		_binding = binding

		val gc = GregorianCalendar()
		gc.timeInMillis = binding.calendarView.date
		gc.set(Calendar.DAY_OF_YEAR, 1)
		binding.calendarView.minDate = gc.timeInMillis

		binding.btnSelectDate.setOnClickListener {
			// save what we were looking at before on the MealPlanAdapter
			gc.timeInMillis = mealPlanList.startDate
			var filename: String = gc.get(Calendar.WEEK_OF_YEAR).toString()
			requireContext().openFileOutput(filename, Context.MODE_PRIVATE).use {
				val jsonToFile = Json.encodeToString(mealPlanList)
				it.write(jsonToFile.toByteArray())
			}
			// done saving, now load
			gc.timeInMillis = binding.calendarView.date
			// roll back to Sunday
			while (gc.get(Calendar.DAY_OF_WEEK) != 1)
				gc.roll(GregorianCalendar.DATE, false)
			//
			val selectedDateToSunday = gc.timeInMillis
			filename = gc.get(Calendar.WEEK_OF_YEAR).toString() // setting filename to week# of year
			val inputStream: InputStream
			try {
					inputStream = requireContext().openFileInput(filename)
					val inputString = inputStream.bufferedReader().use { it.readText() }
					mealPlanList = Json.decodeFromString(inputString)
				} catch (e: FileNotFoundException) {
					for (x in 0 until mealPlanList.recipe.size) {
						mealPlanList.recipe[x].name = DEFAULT_EMPTY_RECIPE
						mealPlanList.recipe[x].ingredients = "  "
					}
				}
			// done loading, now update recyclers
			updateAct.updateRecyclerDate(selectedDateToSunday)
			updateAct.updateRecyclerMP()

			val actInt: ActivityInterface = context as ActivityInterface
			actInt.saveDefaultPlan()
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
