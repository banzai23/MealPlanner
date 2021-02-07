package com.example.mealplanner

import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.content.Intent
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

const val DEFAULT_NUM_DAYS = 7    // A week is the default view for the planner
const val DEFAULT_RECIPE_FILE = "savedRecipes.json"
const val DEFAULT_PLAN_FILE = "defaultPlan.json"
const val DEFAULT_SHOPPING_LIST_FILE = "shoppingList.txt"
const val DEFAULT_EMPTY_RECIPE = "                               "
const val BREAKFAST_CAT = 1
const val LUNCH_CAT = 2
const val DINNER_CAT = 4

lateinit var masterRecipeList: Recipe       // for storing every recipe
lateinit var recipeList: Recipe             // for RecyclerAdapter
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
                    var startDate: Long)
class MealPlanData {
	var dateIterator = intArrayOf(0, 0, 0)
	var mode = BREAKFAST_CAT            // defaults to viewing breakfast
	var position: Int = 0
	var isMealMode = true                       // defaults to meals, not sides
}
interface ActivityInterface {
	fun updateRecyclerDate(smoothTransition: Boolean, date: Long)
	fun updateRecyclerMP(smoothTransition: Boolean)
	fun updateRecyclerRecipes(smoothTransition: Boolean)
	fun fragmentTransaction(fragment: Fragment, tag: String)
	fun saveDefaultPlan()
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
		// load asset
		if (!assetsLoaded) {
			this.deleteFile(DEFAULT_PLAN_FILE)
			this.deleteFile(DEFAULT_RECIPE_FILE)
			var inputStream: InputStream =
			try {
				this.openFileInput(DEFAULT_RECIPE_FILE)
			} catch (e: FileNotFoundException) {
				this.resources.openRawResource(R.raw.savedrecipes)
			}
			var inputString = inputStream.bufferedReader().use { it.readText() }
			masterRecipeList = Json.decodeFromString(inputString)
			masterRecipeList.recipe.sortBy { it.toString() }

			recipeList = Recipe(mutableListOf())
			updateRecipeList(mealPlan.mode)

			inputStream =
			try {
				this.openFileInput(DEFAULT_PLAN_FILE)
			} catch (e: FileNotFoundException) {
				this.resources.openRawResource(R.raw.defaultmealplan)
			}
			inputString = inputStream.bufferedReader().use { it.readText() }
			masterMealPlanList = Json.decodeFromString(inputString)
			mealPlanList = Recipe(mutableListOf())
			when (mealPlan.mode) {
				BREAKFAST_CAT -> mealPlanList.recipe.addAll(masterMealPlanList.breakfast)
				LUNCH_CAT -> mealPlanList.recipe.addAll(masterMealPlanList.lunch)
				else -> mealPlanList.recipe.addAll(masterMealPlanList.dinner)
			}

			assetsLoaded = true
		}
		// asset loaded
		activityBinding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(activityBinding.root)
		val binding = activityBinding.contentMain
		setSupportActionBar(findViewById(R.id.toolbar))

		window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
		// hide the Android keyboard, unless TextInputEditText clicked/focused on
		// set the RecyclerDate
		binding.recyclerDate.layoutManager = LinearLayoutManager(this)
		binding.recyclerDate.adapter = RecyclerAdapterDate()
		// set up RecyclerMP

		val mpClickListener = object : RecyclerClickListener {
			override fun onClick(view: View, position: Int) {
				mealPlan.position = position

				if (mealPlanList.recipe[position].isMeal && !mealPlan.isMealMode)
					binding.tabsMS.getTabAt(0)!!.select()
				else if (!mealPlanList.recipe[position].isMeal && mealPlan.isMealMode)
					binding.tabsMS.getTabAt(1)!!.select()

				showPopup(view, position)
			}
		}
		binding.recyclerMP.layoutManager = LinearLayoutManager(this)
		binding.recyclerMP.adapter = RecyclerAdapterMealPlan(mpClickListener, binding.recyclerMP)
		val ithMP = setMPTouchHelper(binding.recyclerMP)    // it's called MPTouchHelper because
		ithMP.attachToRecyclerView(binding.recyclerMP)      // it's specialized for the mealPlanList object
		// set up RecyclerRecipes
		val recClickListener = object : RecyclerClickListener {
			override fun onClick(view: View, position: Int) {
				fragmentTransaction(EditRecipeFragment(position, false,
						false, true), "savedRecipes")
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
			override fun onTabSelected(tab: TabLayout.Tab) {
				if (tab.position == 0) {
					mealPlan.mode = BREAKFAST_CAT
					mealPlanList.recipe = masterMealPlanList.breakfast
				} else if (tab.position == 1) {
					mealPlan.mode = LUNCH_CAT
					mealPlanList.recipe = masterMealPlanList.lunch
				} else {
					mealPlan.mode = DINNER_CAT
					mealPlanList.recipe = masterMealPlanList.dinner
				}

				if (!mealPlan.isMealMode)
					activityBinding.contentMain.tabsMS.getTabAt(0)!!.select()
				else {  // so we don't call these twice for no reason
					updateRecipeList(mealPlan.mode)
					updateRecyclerRecipes(false)
				}
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

				updateRecipeList(mealPlan.mode)
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
		R.id.action_random -> {
			activityBinding.contentMain.tabsMS.getTabAt(0)!!.select()
			randomizeMealPlan(mealPlan.mode)
			true
		}
		R.id.action_list -> {
			fragmentTransaction(ShoppingListFragment(), "shoppingList")
			true
		}
		R.id.action_recipe_manager -> {
			val intent = Intent(this, RecipeManagerActivity::class.java)
			startActivityForResult(intent, 1)
			true
		}
		R.id.home -> {
			val intent = Intent(this, MainActivity::class.java)
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
			startActivity(intent)
			true
		}
		else -> {
			// If we got here, the user's action was not recognized.
			// Invoke the superclass to handle it.
			super.onOptionsItemSelected(item)
		}
	}
	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		val inflater: MenuInflater = menuInflater
		inflater.inflate(R.menu.menu_main, menu)
		return true
	}
	fun showPopup(v: View, pos: Int) {
		PopupMenu(v.context, v).apply {
			setOnMenuItemClickListener(this@MainActivity)
			if (mealPlanList.recipe[pos].name != DEFAULT_EMPTY_RECIPE) {
				menu.add(0, v.id, 0, v.context.getString(R.string.context_view_recipe))
				if (mealPlanList.recipe[pos].isMeal)
					menu.add(1, v.id, 1, v.context.getString(R.string.context_add_side))
			}
			if (recipeList.recipe.size > 1)
				menu.add(2, v.id, 2, v.context.getString(R.string.context_randomize))

			show()
		}
	}
	override fun onMenuItemClick(item: MenuItem): Boolean {
		return when (item.groupId) {
			0 -> {
				if (mealPlanList.recipe[mealPlan.position].name != DEFAULT_EMPTY_RECIPE) {
					fragmentTransaction(ViewMPRecipeFragment(mealPlan.position), "viewMPRecipe")
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
		updateRecipeList(mealPlan.mode)
		updateRecyclerRecipes(false)
	}
	override fun fragmentTransaction(fragment: Fragment, tag: String) {
		val fragmentManager = supportFragmentManager
		val transaction = fragmentManager.beginTransaction()
		val findFrag = fragmentManager.findFragmentByTag(tag)
		fragmentManager.executePendingTransactions()
		if (findFrag != null)
			transaction.replace(R.id.root_layout, findFrag, tag)
		else {
			transaction.add(R.id.root_layout, fragment, tag)
			transaction.addToBackStack(null)
		}
		transaction.commit()
	}
	override fun updateRecyclerDate(smoothTransition: Boolean, date: Long) {
		mealPlan.dateIterator = intArrayOf(0, 0, 0)
		println(mealPlan.dateIterator)
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
		if (smoothTransition)
			activityBinding.contentMain.recyclerRecipes.adapter!!.notifyItemRangeChanged(0, mealPlanList.recipe.size)
		else
			activityBinding.contentMain.recyclerRecipes.adapter!!.notifyDataSetChanged()
	}
	override fun saveDefaultPlan() {
		this.openFileOutput(DEFAULT_PLAN_FILE, Context.MODE_PRIVATE).use {
			val jsonToFile = Json.encodeToString(masterMealPlanList)
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
		}
	}
	private fun addSideDish(position: Int) {
		if (mealPlan.mode == BREAKFAST_CAT) {
			val addThis = RecipeX(DEFAULT_EMPTY_RECIPE, "", "", 1, false)
			masterMealPlanList.breakfast.add(position + 1, addThis)
			mealPlanList.recipe = masterMealPlanList.breakfast
		} else if (mealPlan.mode == LUNCH_CAT) {
			val addThis = RecipeX(DEFAULT_EMPTY_RECIPE, "", "", 2, false)
			masterMealPlanList.lunch.add(position + 1, addThis)
			mealPlanList.recipe = masterMealPlanList.lunch
		} else {
			val addThis = RecipeX(DEFAULT_EMPTY_RECIPE, "", "", 4, false)
			masterMealPlanList.dinner.add(position + 1, addThis)
			mealPlanList.recipe = masterMealPlanList.dinner
		}

		updateRecyclerMP(false)
		updateRecyclerDate(false, masterMealPlanList.startDate)
	}
	private fun randomizeSingleMeal(mealMode: Int, pos: Int) {
		var random = (0 until recipeList.recipe.size).random()
		if (mealMode != DINNER_CAT) {
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
		val shuffle = recipeList.copy()   // copying the list and
		shuffle.recipe.shuffle()          // shuffling so it doesn't affect the original
		var select = 0
		if (mealMode == DINNER_CAT) {
			var size = shuffle.recipe.size
			var x = 0
			while (x < size) {
				if (shuffle.recipe[x].cat < DINNER_CAT) {
					shuffle.recipe.removeAt(x)
					size--
				}
				x++
			}
		}
		for (x in 0 until arraySize) {
			if (mealPlanList.recipe[x].isMeal) {
				mealPlanList.recipe[x].name = recipeList.recipe[select].name
				mealPlanList.recipe[x].ingredients = recipeList.recipe[select].ingredients
				mealPlanList.recipe[x].instructions = recipeList.recipe[select].instructions
				mealPlanList.recipe[x].cat = recipeList.recipe[select].cat

				select++
				if (select == shuffle.recipe.size)
					select = 0
			}
		}
		updateRecyclerMP(true)
	}
	override fun onDestroy() {
		saveDefaultPlan()
		super.onDestroy()
	}
}
class RecyclerAdapterRecipes(rvClickListener: RecyclerClickListener,
                             rvLongClickListener: RecyclerLongClickListener) :
		RecyclerView.Adapter<RecyclerAdapterRecipes.ViewHolder>() {
	val clickListener = rvClickListener
	val longClickListener = rvLongClickListener
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
		holder.tvItem.setOnClickListener(object : View.OnClickListener {
			override fun onClick(view: View) {
				clickListener.onClick(view, holder.adapterPosition)
			}
		})
		holder.tvItem.setOnLongClickListener(object : View.OnLongClickListener {
			override fun onLongClick(view: View): Boolean {
				longClickListener.onLongClick(view, holder.adapterPosition)
				return true
			}
		})
	}
}
class RecyclerAdapterMealPlan(rvClickListener: RecyclerClickListener, private val recycler: RecyclerView) :
		RecyclerView.Adapter<RecyclerAdapterMealPlan.ViewHolder>()
{
	val clickListener = rvClickListener
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
		holder.itemView.setOnClickListener(object : View.OnClickListener {
			override fun onClick(view: View) {
				clickListener.onClick(view, holder.adapterPosition)
			}
		})
		holder.itemView.setOnDragListener(SetDragListener(position, recycler))
	}
}
class RecyclerAdapterDate() :
	RecyclerView.Adapter<RecyclerAdapterDate.ViewHolder>()
{
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
		if (mealPlanList.recipe[position].isMeal) {
			val gc = GregorianCalendar()
			gc.timeInMillis = masterMealPlanList.startDate

			if (mealPlan.mode == DINNER_CAT) {
				gc.add(Calendar.DATE, mealPlan.dateIterator[2])
				mealPlan.dateIterator[2]++
			} else {
				val x = mealPlan.mode - 1
				gc.add(Calendar.DATE, mealPlan.dateIterator[x])
				mealPlan.dateIterator[x]++
			}

			val df: DateFormat = SimpleDateFormat("MM/dd E")
			holder.tvItem.text = df.format(gc.time)
		}
		else
			holder.tvItem.text = " "
	}
}
fun setMPTouchHelper(recycler: RecyclerView): ItemTouchHelper {
	val itemTouchCallback = object : ItemTouchHelper.Callback() {
		override fun isLongPressDragEnabled() = true
		override fun isItemViewSwipeEnabled() = true
		override fun getMovementFlags(
				recyclerView: RecyclerView, viewHolder:
				RecyclerView.ViewHolder
		): Int {
			val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
			val swipeFlags = ItemTouchHelper.START or ItemTouchHelper.END
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
				mealPlanList.recipe[pos].ingredients = " "
				mealPlanList.recipe[pos].instructions = " "
				mealPlanList.recipe[pos].cat = mealPlan.mode

				recycler.adapter!!.notifyItemChanged(viewHolder.adapterPosition)
			} else {
				mealPlanList.recipe.removeAt(pos)
				recycler.adapter!!.notifyItemRemoved(viewHolder.adapterPosition)
			}
		}
	}
	return ItemTouchHelper(itemTouchCallback)
}
private class SetDragListener(val posOfView: Int, val recycler: RecyclerView) : View.OnDragListener
{
	override fun onDrag(v: View, event: DragEvent): Boolean {
		when (event.action) {
			DragEvent.ACTION_DRAG_STARTED -> {
				if (event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
					v.invalidate()
					return true
				} else {
					return false
				}
			}
			DragEvent.ACTION_DRAG_ENTERED -> {
				v.invalidate()
				return true
			}
			DragEvent.ACTION_DRAG_LOCATION ->
				// Ignore the event
				return true
			DragEvent.ACTION_DRAG_EXITED -> {
				v.invalidate()
				return true
			}
			DragEvent.ACTION_DROP -> {
				val item: ClipData.Item = event.clipData.getItemAt(0)

				//  (v as? ImageView)?.clearColorFilter()
				//  (v as? TextView)?.setBackgroundColor(Color.TRANSPARENT)
				//  v.invalidate()
				val posOfDragged: Int = item.text.toString().toInt()
				val pos = posOfView
				mealPlanList.recipe[pos].name = recipeList.recipe[posOfDragged].name
				mealPlanList.recipe[pos].ingredients = recipeList.recipe[posOfDragged].ingredients
				mealPlanList.recipe[pos].instructions = recipeList.recipe[posOfDragged].instructions
				mealPlanList.recipe[pos].cat = recipeList.recipe[posOfDragged].cat
				recycler.adapter!!.notifyItemChanged(posOfView)
				return true
			}
			DragEvent.ACTION_DRAG_ENDED -> {
				// do nothing
				return true
			}
			else -> {
				// An unknown action type was received.
				return false
			}
		}
	}
}

// TODO: Upload recipe from a website -- website parser or Copy/paste parser
// TODO: cup(s) ounce(s) tablespoon(s) teaspoon(s)
// TODO: Save shopping list, only 1 no duplicates. shoppingList.json
