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
		gc.roll(Calendar.YEAR, false)
		binding.calendarView.minDate = gc.timeInMillis

		binding.btnSelectDate.setOnClickListener {
			// save what we were looking at before on the MealPlanAdapter
			gc.timeInMillis = masterMealPlanList.startDate
			var filename: String = gc.get(Calendar.WEEK_OF_YEAR).toString()+"_"+gc.get(Calendar.YEAR).toString()
			requireContext().openFileOutput(filename, Context.MODE_PRIVATE).use {
				val jsonToFile = Json.encodeToString(masterMealPlanList)
				it.write(jsonToFile.toByteArray())
			}
			// done saving, now load
			gc.timeInMillis = binding.calendarView.date
			// roll back to Sunday, startDate is always saved as the first date of the week
			while (gc.get(Calendar.DAY_OF_WEEK) != 1)
				gc.roll(GregorianCalendar.DAY_OF_YEAR, false)
			//
			val selectedDateToSunday = gc.timeInMillis
			filename = gc.get(Calendar.WEEK_OF_YEAR).toString()+"_"+gc.get(Calendar.YEAR).toString() // setting filename to week# of year + year
			val inputStream: InputStream
			try {
					inputStream = requireContext().openFileInput(filename)
					val inputString = inputStream.bufferedReader().use { it.readText() }
					masterMealPlanList = Json.decodeFromString(inputString)
				} catch (e: FileNotFoundException) {
					masterMealPlanList.breakfast.clear()
					masterMealPlanList.lunch.clear()
					masterMealPlanList.dinner.clear()
					for (x in 0 until 7) {
						val addBreak = RecipeX(DEFAULT_EMPTY_RECIPE, "", "", 1, true)
						masterMealPlanList.breakfast.add(x, addBreak)
						val addLunch = RecipeX(DEFAULT_EMPTY_RECIPE, "", "", 2, true)
						masterMealPlanList.lunch.add(x, addLunch)
						val addDinner = RecipeX(DEFAULT_EMPTY_RECIPE, "", "", 4, true)
						masterMealPlanList.dinner.add(x, addDinner)
					}
				}

			if (mealPlan.mode == BREAKFAST_CAT)
				mealPlanList.recipe = masterMealPlanList.breakfast
			else if (mealPlan.mode == LUNCH_CAT)
				mealPlanList.recipe = masterMealPlanList.lunch
			else
				mealPlanList.recipe = masterMealPlanList.dinner

			// done loading, now update recyclers
			updateAct.updateRecyclerDate(true, selectedDateToSunday)
			updateAct.updateRecyclerMP(true)

			updateAct.saveDefaultFiles()
			requireActivity().supportFragmentManager.popBackStack()
		}
		/*  // For development; deletes old weekly meal plan files
		binding.btnSelectDate.setOnClickListener {
			gc.timeInMillis = binding.calendarView.date
			val filename: String = gc.get(Calendar.WEEK_OF_YEAR).toString()
			requireContext().deleteFile(filename)
		} */
		binding.btnCancelCal.setOnClickListener {
			requireActivity().supportFragmentManager.popBackStack()
		}
	}
	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}
}
