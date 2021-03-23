package com.example.mealplanner

import android.app.AlertDialog
import android.content.*
import android.os.Bundle
import android.view.*
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mealplanner.databinding.ActivityMainBinding
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.security.ProviderInstaller
import com.google.android.material.tabs.TabLayout
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.FileNotFoundException
import java.io.InputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

const val MAX_RECIPE_TITLE_SIZE = 40
const val DEFAULT_RECIPE_FILE = "savedRecipes.json"
const val DEFAULT_SHOPPING_LIST_FILE = "shoppingList.json"
const val DEFAULT_EMPTY_RECIPE = "                               "
const val BREAKFAST_CAT = 1
const val LUNCH_CAT = 2
const val DINNER_CAT = 4
const val SNACK_CAT = 10

lateinit var masterRecipeList: Recipe       // for storing every recipe
lateinit var recipeList: Recipe             // for RecyclerRecipes
lateinit var masterMealPlanList: MealPlan   // for storing every meal
lateinit var mealPlanList: Recipe           // for RecyclerMealPlan

var assetsLoaded: Boolean = false

@Serializable
data class Recipe(var recipe: MutableList<RecipeX>)
@Serializable
data class RecipeX(var name: String = "", var ingredients: String = "",
                   var instructions: String = "", var cat: Int, var isMeal: Boolean)
@Serializable
data class MealPlan(var breakfast: MutableList<RecipeX>,
                    var lunch: MutableList<RecipeX>,
                    var dinner: MutableList<RecipeX>,
                    var snack: MutableList<RecipeX>,
                    var startDate: Long)
class MealPlanData {
	var dateIterator = intArrayOf(0, 0, 0, 0)  // Size-4 array for the 4 categories of Meals
	var mode = BREAKFAST_CAT            // defaults to viewing breakfast
	var position: Int = 0               // easier way to hold position data, instead of passing
	var isMealMode = true               // defaults to meals, not sides
}
interface ActivityInterface {
	fun updateRecyclerDate(smoothTransition: Boolean, date: Long)
	fun updateRecyclerMP(smoothTransition: Boolean)
	fun updateRecyclerRecipes(smoothTransition: Boolean)
	fun fragmentTransaction(fragment: Fragment, tag: String)
	fun saveDefaultFiles()
}
interface RecyclerClickListener {
	fun onClick(view: View, position: Int)
}
interface RecyclerLongClickListener {
	fun onLongClick(view: View, position: Int)
}
var mealPlan = MealPlanData()
class MainActivity : AppCompatActivity(), ActivityInterface, PopupMenu.OnMenuItemClickListener {
	private lateinit var activityBinding: ActivityMainBinding
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		try {
			ProviderInstaller.installIfNeeded(this)
		} catch (e: GooglePlayServicesRepairableException) {
			GoogleApiAvailability.getInstance()
					.showErrorNotification(this, e.connectionStatusCode)
		} catch (e: GooglePlayServicesNotAvailableException) {
		}
		// load asset
		if (!assetsLoaded) {
		//	this.deleteFile("23_2021") // delete old files, for development

			var inputStream: InputStream =
					try {
						this.openFileInput(DEFAULT_RECIPE_FILE) // load this file, if not found, load the resource
					} catch (e: FileNotFoundException) {
						this.resources.openRawResource(R.raw.savedrecipes)
					}
			var inputString = inputStream.bufferedReader().readText()
			if (inputString.isEmpty()) {
				inputStream = this.resources.openRawResource(R.raw.savedrecipes)
				inputString = inputStream.bufferedReader().readText()
			} else if (inputString[0] != '{') {
				inputStream = this.resources.openRawResource(R.raw.savedrecipes)
				inputString = inputStream.bufferedReader().readText()
			}
			masterRecipeList = Json.decodeFromString(inputString)   // load to the variable from the file
			masterRecipeList.recipe.sortBy { it.toString() }    // sort alphabetically, just in case they aren't already

			recipeList = Recipe(mutableListOf())    // initializing recipeList
			updateRecipeList(mealPlan.mode)         // recipeList points to the default category
			// list in masterRecipeList
			val gc = GregorianCalendar()
			try {
				val filename: String = gc.get(Calendar.WEEK_OF_YEAR).toString() + "_" + gc.get(Calendar.YEAR).toString()
				inputStream = this.openFileInput(filename)
				inputString = inputStream.bufferedReader().readText()
				masterMealPlanList = Json.decodeFromString(inputString)
			} catch (e: FileNotFoundException) {
				while (gc.get(Calendar.DAY_OF_WEEK) != 1)
					gc.roll(GregorianCalendar.DAY_OF_YEAR, false)
				masterMealPlanList = MealPlan(mutableListOf(), mutableListOf(), mutableListOf(), mutableListOf(), gc.timeInMillis)
				for (x in 0 until 7) {
					val addBreak = RecipeX(DEFAULT_EMPTY_RECIPE, "", "", BREAKFAST_CAT, true)
					masterMealPlanList.breakfast.add(x, addBreak)
					val addLunch = RecipeX(DEFAULT_EMPTY_RECIPE, "", "", LUNCH_CAT, true)
					masterMealPlanList.lunch.add(x, addLunch)
					val addDinner = RecipeX(DEFAULT_EMPTY_RECIPE, "", "", DINNER_CAT, true)
					masterMealPlanList.dinner.add(x, addDinner)
					val addSnack = RecipeX(DEFAULT_EMPTY_RECIPE, "", "", SNACK_CAT, true)
					masterMealPlanList.snack.add(x, addSnack)
				}
			}

			mealPlanList = Recipe(mutableListOf())
			when (mealPlan.mode) {
				BREAKFAST_CAT -> mealPlanList.recipe.addAll(masterMealPlanList.breakfast)
				LUNCH_CAT -> mealPlanList.recipe.addAll(masterMealPlanList.lunch)
				DINNER_CAT -> mealPlanList.recipe.addAll(masterMealPlanList.dinner)
				else -> mealPlanList.recipe.addAll(masterMealPlanList.snack)
			}
			inputStream.close()
			assetsLoaded = true
		}
		// asset loaded, bind the viewIDs next
		activityBinding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(activityBinding.root)
		val binding = activityBinding.contentMain
		setSupportActionBar(findViewById(R.id.toolbar))

		window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
		// hide the Android keyboard, unless TextInputEditText clicked/focused on
		// set the RecyclerDate
		binding.recyclerDate.layoutManager = LinearLayoutManager(this)
		binding.recyclerDate.adapter = RecyclerAdapterDate()

		// set up RecyclerMP, MealPlan
		val mpClickListener = object : RecyclerClickListener {
			override fun onClick(view: View, position: Int) {
				mealPlan.position = position

				if (mealPlanList.recipe[position].isMeal && !mealPlan.isMealMode)   // defaults to Meal-Tab if a meal is selected
					binding.tabsMS.getTabAt(0)!!.select()
				else if (!mealPlanList.recipe[position].isMeal && mealPlan.isMealMode) // defaults to Side-Tab if a side is selected
					binding.tabsMS.getTabAt(1)!!.select()

				showPopup(view, position)
			}
		}
		binding.recyclerMP.layoutManager = LinearLayoutManager(this)
		binding.recyclerMP.adapter = RecyclerAdapterMealPlan(mpClickListener, binding.recyclerMP)
		val ithMP = setMPTouchHelper(binding.recyclerMP, binding.recyclerDate)  // it's called MPTouchHelper because
		ithMP.attachToRecyclerView(binding.recyclerMP)                          // it's specialized for the mealPlanList object
		// set up RecyclerRecipes
		val recClickListener = object : RecyclerClickListener {
			override fun onClick(view: View, position: Int) {
				fragmentTransaction(EditRecipeFragment(position,
						false, true, false),
						"editRecipe")
			}
		}
		val recLongClickListener = object : RecyclerLongClickListener {
			override fun onLongClick(view: View, position: Int) {
				val data = ClipData.newPlainText("", position.toString())
				val shadowBuilder = View.DragShadowBuilder(view)
				view.startDrag(data, shadowBuilder, view, 0)
			}
		}
		binding.recyclerRecipes.layoutManager = LinearLayoutManager(this)
		binding.recyclerRecipes.adapter = RecyclerAdapterRecipes(recClickListener, recLongClickListener)

		binding.tabsBLD.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
			override fun onTabSelected(tab: TabLayout.Tab) {    // tabsBreakfastLunchDinner
				when (tab.position) {
					0 -> {
						mealPlan.mode = BREAKFAST_CAT
						mealPlanList.recipe = masterMealPlanList.breakfast
					}
					1 -> {
						mealPlan.mode = LUNCH_CAT
						mealPlanList.recipe = masterMealPlanList.lunch
					}
					2 -> {
						mealPlan.mode = DINNER_CAT
						mealPlanList.recipe = masterMealPlanList.dinner
					}
					else -> {
						mealPlan.mode = SNACK_CAT
						mealPlanList.recipe = masterMealPlanList.snack
					}
				}

				if (!mealPlan.isMealMode)
					activityBinding.contentMain.tabsMS.getTabAt(0)!!.select()
				else  // so we don't call the following functions twice for no reason
					updateRecyclerRecipes(false)
				updateRecyclerDate(false, masterMealPlanList.startDate)
				updateRecyclerMP(false)
			}

			override fun onTabReselected(tab: TabLayout.Tab?) {
				println("Tab reselected!")
			}

			override fun onTabUnselected(tab: TabLayout.Tab?) {
				println("Tab unselected!")
			}
		})
		binding.tabsMS.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
			override fun onTabSelected(tab: TabLayout.Tab) {
				if (tab.position == 0)
					mealPlan.isMealMode = true
				else if (tab.position == 1)
					mealPlan.isMealMode = false

				updateRecyclerRecipes(false)
			}

			override fun onTabReselected(tab: TabLayout.Tab?) {
				println("Tab reselected!")
			}

			override fun onTabUnselected(tab: TabLayout.Tab?) {
				println("Tab unselected!")
			}
		})
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
		R.id.action_new -> {
			fragmentTransaction(CalendarFragment(), "calendar")
			true
		}
		R.id.action_random -> { // select Meals tab, I only want it to randomize from the Meals list
			activityBinding.contentMain.tabsMS.getTabAt(0)!!.select()
			randomizeMealPlan(mealPlan.mode)
			true
		}
		R.id.action_list -> {
			val intent = Intent(this, ShoppingListActivity::class.java)
			startActivity(intent)
			true
		}
		R.id.action_recipe_manager -> {
			val intent = Intent(this, RecipeManagerActivity::class.java)
			startActivityForResult(intent, 1)
			true
		}
		/*
		R.id.home -> {
			val intent = Intent(this, MainActivity::class.java)
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) // CLEAR_TASK
			startActivity(intent)
			true
		} */
		R.id.action_exit -> {
			showExitDialog()
			true
		}
		else -> {
			super.onOptionsItemSelected(item)
		}
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		val inflater: MenuInflater = menuInflater
		inflater.inflate(R.menu.menu_main, menu)
		return true
	}

	private fun showExitDialog() {
		saveDefaultFiles()

		val builder = AlertDialog.Builder(this)
		builder.setMessage(R.string.dialog_exit)
		builder.setPositiveButton(R.string.dialog_yes
		) { _, _ ->
			finishAffinity()
		}
				.setNegativeButton(R.string.dialog_no
				) { dialog, _ ->
					dialog.dismiss()
				}
		builder.create()
		builder.show()
	}

	private fun showPopup(v: View, pos: Int) {
		PopupMenu(v.context, v).apply {
			setOnMenuItemClickListener(this@MainActivity)
			if (mealPlanList.recipe[pos].name != DEFAULT_EMPTY_RECIPE) {
				menu.add(0, v.id, 0, v.context.getString(R.string.context_view_recipe))
				if (mealPlanList.recipe[pos].isMeal)
					menu.add(1, v.id, 1, v.context.getString(R.string.context_add_side))
			}
			if (recipeList.recipe.size >= 1)
				menu.add(2, v.id, 2, v.context.getString(R.string.context_randomize))

			show()
		}
	}

	override fun onMenuItemClick(item: MenuItem): Boolean {
		return when (item.groupId) {
			0 -> {
				if (mealPlanList.recipe[mealPlan.position].name != DEFAULT_EMPTY_RECIPE) {
					fragmentTransaction(ViewRecipesFragment(mealPlan.position), "viewRecipe")
				}
				true
			}
			1 -> {
				if (item.title == getString(R.string.context_add_side))
					addSideDish(mealPlan.position)
				true
			}
			2 -> {
				if (item.title == getString(R.string.context_randomize)) {
					randomizeSingleMeal(mealPlan.mode, mealPlan.position)
				}
				true
			}
			else -> {
				super.onContextItemSelected(item)
			}
		}
	}

	override fun onBackPressed() {
		super.onBackPressed()
		if (supportFragmentManager.backStackEntryCount > 0) {
			supportFragmentManager.popBackStack()
		}
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		saveDefaultFiles()
		updateRecyclerRecipes(false)
	}

	override fun fragmentTransaction(fragment: Fragment, tag: String) {
		val fragmentManager = supportFragmentManager
		val transaction = fragmentManager.beginTransaction()
		val findFrag = fragmentManager.findFragmentByTag(tag)
		fragmentManager.executePendingTransactions()
		if (findFrag != null)   // following code keeps only one instance of any fragment at one time
			transaction.replace(R.id.root_layout, findFrag, tag)
		else {
			transaction.add(R.id.root_layout, fragment, tag)
			transaction.addToBackStack(null)
		}
		transaction.commit()
	}

	override fun updateRecyclerDate(smoothTransition: Boolean, date: Long) {
		masterMealPlanList.startDate = date
		if (smoothTransition)
			activityBinding.contentMain.recyclerDate.adapter!!.notifyItemRangeChanged(0, mealPlanList.recipe.size)
		else
			activityBinding.contentMain.recyclerDate.adapter!!.notifyDataSetChanged()
	}

	override fun updateRecyclerMP(smoothTransition: Boolean) {
		if (smoothTransition)
			activityBinding.contentMain.recyclerMP.adapter!!.notifyItemRangeChanged(0, mealPlanList.recipe.size)
		else
			activityBinding.contentMain.recyclerMP.adapter!!.notifyDataSetChanged()
	}

	override fun updateRecyclerRecipes(smoothTransition: Boolean) {
		updateRecipeList(mealPlan.mode)
		if (smoothTransition)
			activityBinding.contentMain.recyclerRecipes.adapter!!.notifyItemRangeChanged(0, mealPlanList.recipe.size)
		else
			activityBinding.contentMain.recyclerRecipes.adapter!!.notifyDataSetChanged()
	}

	override fun saveDefaultFiles() {
		val gc = GregorianCalendar()
		gc.timeInMillis = masterMealPlanList.startDate
		val filename: String = gc.get(Calendar.WEEK_OF_YEAR).toString() + "_" + gc.get(Calendar.YEAR).toString()
		this.openFileOutput(filename, Context.MODE_PRIVATE).use {
			val jsonToFile = Json.encodeToString(masterMealPlanList)
			it.write(jsonToFile.toByteArray())
		}
		this.openFileOutput(DEFAULT_RECIPE_FILE, Context.MODE_PRIVATE).use {
			val jsonToFile = Json.encodeToString(masterRecipeList)
			it.write(jsonToFile.toByteArray())
		}
	}

	private fun updateRecipeList(mode: Int) {
		recipeList.recipe.clear()
		when (mode) {
			BREAKFAST_CAT -> {
				for (x in 0 until masterRecipeList.recipe.size) {
					val cat = masterRecipeList.recipe[x].cat
					if (cat == BREAKFAST_CAT ||
							cat == BREAKFAST_CAT + LUNCH_CAT ||
							cat == BREAKFAST_CAT + DINNER_CAT ||
							cat == BREAKFAST_CAT + LUNCH_CAT + DINNER_CAT) {
						if (mealPlan.isMealMode && masterRecipeList.recipe[x].isMeal)
							recipeList.recipe.add(masterRecipeList.recipe[x])
						else if (!mealPlan.isMealMode && !masterRecipeList.recipe[x].isMeal)
							recipeList.recipe.add(masterRecipeList.recipe[x])
					}
				}
			}
			LUNCH_CAT -> {
				for (x in 0 until masterRecipeList.recipe.size) {
					val cat = masterRecipeList.recipe[x].cat
					if (cat == LUNCH_CAT ||
							cat == BREAKFAST_CAT + LUNCH_CAT ||
							cat == LUNCH_CAT + DINNER_CAT ||
							cat == LUNCH_CAT + BREAKFAST_CAT + DINNER_CAT) {
						if (mealPlan.isMealMode && masterRecipeList.recipe[x].isMeal)
							recipeList.recipe.add(masterRecipeList.recipe[x])
						else if (!mealPlan.isMealMode && !masterRecipeList.recipe[x].isMeal)
							recipeList.recipe.add(masterRecipeList.recipe[x])
					}
				}
			}
			DINNER_CAT -> {
				for (x in 0 until masterRecipeList.recipe.size) {
					if (mealPlan.isMealMode && masterRecipeList.recipe[x].isMeal)
						recipeList.recipe.add(masterRecipeList.recipe[x])
					else if (!mealPlan.isMealMode && !masterRecipeList.recipe[x].isMeal)
						recipeList.recipe.add(masterRecipeList.recipe[x])
				}
			}
			SNACK_CAT -> {
				for (x in 0 until masterRecipeList.recipe.size) {
					val cat = masterRecipeList.recipe[x].cat
					if (cat == SNACK_CAT) {
						if (mealPlan.isMealMode && masterRecipeList.recipe[x].isMeal)
							recipeList.recipe.add(masterRecipeList.recipe[x])
						else if (!mealPlan.isMealMode && !masterRecipeList.recipe[x].isMeal)
							recipeList.recipe.add(masterRecipeList.recipe[x])
					}
				}
			}
		}
	}

	private fun addSideDish(position: Int) {
		when (mealPlan.mode) {
			BREAKFAST_CAT -> {
				val addThis = RecipeX(DEFAULT_EMPTY_RECIPE, "", "", BREAKFAST_CAT, false)
				masterMealPlanList.breakfast.add(position + 1, addThis)
				mealPlanList.recipe = masterMealPlanList.breakfast
			}
			LUNCH_CAT -> {
				val addThis = RecipeX(DEFAULT_EMPTY_RECIPE, "", "", LUNCH_CAT, false)
				masterMealPlanList.lunch.add(position + 1, addThis)
				mealPlanList.recipe = masterMealPlanList.lunch
			}
			DINNER_CAT -> {
				val addThis = RecipeX(DEFAULT_EMPTY_RECIPE, "", "", DINNER_CAT, false)
				masterMealPlanList.dinner.add(position + 1, addThis)
				mealPlanList.recipe = masterMealPlanList.dinner
			}
			SNACK_CAT -> {
				val addThis = RecipeX(DEFAULT_EMPTY_RECIPE, "", "", SNACK_CAT, false)
				masterMealPlanList.snack.add(position + 1, addThis)
				mealPlanList.recipe = masterMealPlanList.snack
			}
		}

		updateRecyclerMP(false)
		updateRecyclerDate(false, masterMealPlanList.startDate)
	}

	private fun randomizeSingleMeal(mealMode: Int, pos: Int) {
		var random = (0 until recipeList.recipe.size).random()
		if (mealMode != DINNER_CAT) {   // while the meal is the same, keep rolling random
			while (mealPlanList.recipe[pos].name == recipeList.recipe[random].name)
				random = (0 until recipeList.recipe.size).random()
		} else {
			var cat = recipeList.recipe[random].cat
			while (cat < DINNER_CAT ||
					mealPlanList.recipe[pos].name == recipeList.recipe[random].name) {
				random = (0 until recipeList.recipe.size).random()
				cat = recipeList.recipe[random].cat
			}
		}
		mealPlanList.recipe[pos].name = recipeList.recipe[random].name
		mealPlanList.recipe[pos].ingredients = recipeList.recipe[random].ingredients
		mealPlanList.recipe[pos].instructions = recipeList.recipe[random].instructions
		mealPlanList.recipe[pos].cat = recipeList.recipe[random].cat

		updateRecyclerMP(true)
	}

	private fun randomizeMealPlan(mealMode: Int) {
		val arraySize = mealPlanList.recipe.size
		val shuffle = Recipe(mutableListOf())  // copying the list and

		when (mealMode) {
			DINNER_CAT -> {
				for (x in 0 until recipeList.recipe.size) {
					if (recipeList.recipe[x].cat >= DINNER_CAT)
						shuffle.recipe.add(recipeList.recipe[x])
				}
			}
			else -> {
				for (x in 0 until recipeList.recipe.size)       // breakfast and lunch can straight add all
					shuffle.recipe.add(recipeList.recipe[x])
			}
		}
		shuffle.recipe.shuffle()
		var select = 0
		for (x in 0 until arraySize) {
			if (mealPlanList.recipe[x].isMeal) {
				mealPlanList.recipe[x].name = shuffle.recipe[select].name
				mealPlanList.recipe[x].ingredients = shuffle.recipe[select].ingredients
				mealPlanList.recipe[x].instructions = shuffle.recipe[select].instructions
				mealPlanList.recipe[x].cat = shuffle.recipe[select].cat

				select++
				if (select == shuffle.recipe.size)
					select = 0
			}
		}
		updateRecyclerMP(true)
	}

	override fun onDestroy() {
		super.onDestroy()
	}

	class RecyclerAdapterRecipes(private val clickListener: RecyclerClickListener,
	                             private val longClickListener: RecyclerLongClickListener) :
			RecyclerView.Adapter<RecyclerAdapterRecipes.ViewHolder>() {
		class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
			val tvItem: TextView = v.findViewById(R.id.tvItem)
		}

		override fun getItemCount() = recipeList.recipe.size

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			val inflatedView = LayoutInflater.from(parent.context).inflate(
					R.layout.recycler_meal_plan,
					parent,
					false
			)
			return ViewHolder(inflatedView)
		}

		override fun onBindViewHolder(holder: ViewHolder, position: Int) {
			holder.tvItem.text = recipeList.recipe[position].name
			holder.tvItem.setOnClickListener { view -> clickListener.onClick(view, holder.adapterPosition) }
			holder.tvItem.setOnLongClickListener { view ->
				longClickListener.onLongClick(view, holder.adapterPosition)
				true
			}
		}
	}

	class RecyclerAdapterMealPlan(private val clickListener: RecyclerClickListener, private val recycler: RecyclerView) :
			RecyclerView.Adapter<RecyclerAdapterMealPlan.ViewHolder>() {
		class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
			private val tvItem: TextView = v.findViewById(R.id.tvItem)

			fun bind(position: Int) {
				tvItem.tag = position.toString()
				tvItem.text = mealPlanList.recipe[position].name
			}
		}

		override fun getItemCount() = mealPlanList.recipe.size

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			val inflatedView = LayoutInflater.from(parent.context).inflate(
					R.layout.recycler_meal_plan,
					parent,
					false
			)
			return ViewHolder(inflatedView)
		}

		override fun onBindViewHolder(holder: ViewHolder, position: Int) {
			holder.bind(position)
			holder.itemView.setOnClickListener { view -> clickListener.onClick(view, holder.adapterPosition) }
			holder.itemView.setOnDragListener(SetDragListener(position, recycler))
		}
	}

	class RecyclerAdapterDate() :
			RecyclerView.Adapter<RecyclerAdapterDate.ViewHolder>() {
		class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
			val tvItem: TextView = v.findViewById(R.id.tvItem)
		}

		override fun getItemCount() = mealPlanList.recipe.size

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			val inflatedView = LayoutInflater.from(parent.context).inflate(
					R.layout.recycler_meal_plan,
					parent,
					false
			)
			return ViewHolder(inflatedView)
		}

		override fun onBindViewHolder(holder: ViewHolder, position: Int) {
			if (position == 0) {
				mealPlan.dateIterator = intArrayOf(0, 0, 0, 0)
			}
			if (mealPlanList.recipe[position].isMeal) {
				val gc = GregorianCalendar()
				gc.timeInMillis = masterMealPlanList.startDate

				if (mealPlan.mode == DINNER_CAT) {
					gc.add(Calendar.DATE, mealPlan.dateIterator[2])
					mealPlan.dateIterator[2]++
				} else if (mealPlan.mode == SNACK_CAT) {
					gc.add(Calendar.DATE, mealPlan.dateIterator[3])
					mealPlan.dateIterator[3]++
				} else {
					val x = mealPlan.mode - 1   // the position for Breakfast is 0 (Cat - 1), Lunch is 1 (Cat - 1)
					gc.add(Calendar.DATE, mealPlan.dateIterator[x])
					mealPlan.dateIterator[x]++
				}

				val df: DateFormat = SimpleDateFormat("MM/dd E", Locale.ENGLISH)
				holder.tvItem.text = df.format(gc.time)
			} else
				holder.tvItem.text = " "    // if not a meal, a side, then show no date for the row
		}
	}

	private fun setMPTouchHelper(recycler: RecyclerView, dateRec: RecyclerView): ItemTouchHelper {
		val itemTouchCallback = object : ItemTouchHelper.Callback() {
			override fun isLongPressDragEnabled() = true
			override fun isItemViewSwipeEnabled() = true
			override fun getMovementFlags(
					recyclerView: RecyclerView, viewHolder:
					RecyclerView.ViewHolder
			): Int {
				val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
				val swipeFlags = ItemTouchHelper.END
				return makeMovementFlags(dragFlags, swipeFlags)
			}

			override fun onMove(
					recyclerView: RecyclerView, source: RecyclerView.ViewHolder,
					target: RecyclerView.ViewHolder
			): Boolean {
				if (source.itemViewType != target.itemViewType)
					return false
				val sourcePos = source.adapterPosition
				val targetPos = target.adapterPosition
				val saveSource = mealPlanList.recipe[sourcePos].copy()
				mealPlanList.recipe[sourcePos] = mealPlanList.recipe[targetPos].copy()
				mealPlanList.recipe[targetPos] = saveSource

				recyclerView.adapter!!.notifyItemMoved(source.adapterPosition, target.adapterPosition)

				return true
			}

			override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
				val pos = viewHolder.adapterPosition
				if (mealPlanList.recipe[pos].isMeal) {
					mealPlanList.recipe[pos].name = DEFAULT_EMPTY_RECIPE
					mealPlanList.recipe[pos].ingredients = ""
					mealPlanList.recipe[pos].instructions = ""
					mealPlanList.recipe[pos].cat = mealPlan.mode

					recycler.adapter!!.notifyItemChanged(viewHolder.adapterPosition)
				} else {
					mealPlanList.recipe.removeAt(pos)
					recycler.adapter!!.notifyDataSetChanged()
					dateRec.adapter!!.notifyDataSetChanged()
				}
			}
		}
		return ItemTouchHelper(itemTouchCallback)
	}

	private class SetDragListener(val posOfView: Int, val recycler: RecyclerView) : View.OnDragListener {
		override fun onDrag(v: View, event: DragEvent): Boolean {
			if (event.action == DragEvent.ACTION_DRAG_STARTED) {
				if (event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
					v.invalidate()
					return true
				} else {
					return false
				}
			}
			if (event.action == DragEvent.ACTION_DROP) {
				val item: ClipData.Item = event.clipData.getItemAt(0)

				val posOfDragged: Int = item.text.toString().toInt()
				val pos = posOfView
				mealPlanList.recipe[pos].name = recipeList.recipe[posOfDragged].name
				mealPlanList.recipe[pos].ingredients = recipeList.recipe[posOfDragged].ingredients
				mealPlanList.recipe[pos].instructions = recipeList.recipe[posOfDragged].instructions
				mealPlanList.recipe[pos].cat = recipeList.recipe[posOfDragged].cat
				recycler.adapter!!.notifyItemChanged(posOfView)
				return true
			} else {
				return false
			}
		}
	}
}