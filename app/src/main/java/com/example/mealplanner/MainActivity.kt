package com.example.mealplanner

import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.*
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
import java.io.InputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

const val DEFAULT_NUM_DAYS = 7    // A week is the default view for the planner
const val DEFAULT_RECIPE_FILE = "savedRecipes.json"
const val DEFAULT_PLAN_FILE = "defaultPlan.json"
const val DEFAULT_EMPTY_RECIPE = "                               "
const val BREAKFAST_CAT = 1
const val LUNCH_CAT = 2
const val DINNER_CAT = 4

lateinit var mealPlanList: MealPlan
lateinit var masterRecipeList: Recipe   // for storing every recipe
lateinit var recipeList: Recipe         // for RecyclerAdapter
var mealPlanOffset = 0
var assetsLoaded: Boolean = false

@Serializable
data class Recipe(var recipe: MutableList<RecipeX>)
@Serializable
data class RecipeX(var name: String = "", var ingredients: String = "", var cat: Int)
@Serializable
data class MealPlan(var recipe: MutableList<MealPlanX>, var startDate: Long)
@Serializable
data class MealPlanX(var name: String = "", var ingredients: String = "")


interface ActivityInterface {
	fun updateRecyclerDate(date: Long)
	fun updateRecyclerMP()
	fun updateRecyclerRecipes()
	fun fragmentTransaction(fragment: Fragment, tag: String)
	fun saveDefaultPlan()
}
interface RecyclerClickListener {
	fun onClick(view: View, position: Int)
}
interface RecyclerLongClickListener {
	fun onLongClick(view: View, position: Int)
}
class MainActivity : AppCompatActivity(), ActivityInterface {
	private lateinit var activityBinding: ActivityMainBinding
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		// load asset
		if (!assetsLoaded) {
			var inputStream: InputStream =
		//	try {
		//		this.openFileInput(DEFAULT_RECIPE_FILE)
		//	} catch (e: FileNotFoundException) {
				this.resources.openRawResource(R.raw.savedrecipes)
		//	}
			var inputString = inputStream.bufferedReader().use { it.readText() }
			masterRecipeList = Json.decodeFromString(inputString)
			masterRecipeList.recipe.sortBy { it.toString() }

			recipeList = Recipe(mutableListOf())
			updateRecipeList(mealPlanOffset)

			inputStream =
		//	try {
		//		this.openFileInput(DEFAULT_PLAN_FILE)
	//		} catch (e: FileNotFoundException) {
				this.resources.openRawResource(R.raw.defaultmealplan)
	//		}
			inputString = inputStream.bufferedReader().use { it.readText() }
			mealPlanList = Json.decodeFromString(inputString)

			assetsLoaded = true
		}
		// asset loaded
		activityBinding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(activityBinding.root)
		setSupportActionBar(findViewById(R.id.toolbar))

		val binding = activityBinding.contentMain

		window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
		// hide the Android keyboard, unless TextInputEditText clicked/focused on
		// set the RecyclerDate, set fixed size, for DEFAULT_NUM_DAYS
		binding.recyclerDate.layoutManager = LinearLayoutManager(this)
		binding.recyclerDate.adapter = RecyclerAdapterDate()
		binding.recyclerDate.setHasFixedSize(true)
		// set up RecyclerMP
		val mpClickListener = object : RecyclerClickListener {
			override fun onClick(view: View, position: Int) {
				val pos = position+mealPlanOffset
				if(mealPlanList.recipe[pos].name != DEFAULT_EMPTY_RECIPE) {
					fragmentTransaction(ViewMPRecipeFragment(pos), "viewMPRecipe")
				}
			}
		}
		binding.recyclerMP.layoutManager = LinearLayoutManager(this)
		binding.recyclerMP.adapter = RecyclerAdapterMealPlan(mpClickListener, binding.recyclerMP)
		binding.recyclerMP.setHasFixedSize(true)
		val ithMP = setMPTouchHelper(binding.recyclerMP)    // it's called MPTouchHelper because
		ithMP.attachToRecyclerView(binding.recyclerMP)      // it's specialized for the mealPlanList object
		// set up RecyclerRecipes
		val recClickListener = object : RecyclerClickListener {
			override fun onClick(view: View, position: Int) {
				fragmentTransaction(EditRecipeFragment(position), "savedRecipes")
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

		activityBinding.tabLayout.addOnTabSelectedListener( object : TabLayout.OnTabSelectedListener {
			override fun onTabSelected(tab: TabLayout.Tab?) {
				mealPlanOffset = tab!!.position * 7
				updateRecipeList(mealPlanOffset)
				binding.recyclerMP.adapter!!.notifyDataSetChanged()
				binding.recyclerRecipes.adapter!!.notifyDataSetChanged()
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
			for (x in mealPlanOffset until mealPlanOffset+7) {
				val random = (0 until recipeList.recipe.size).random()
				mealPlanList.recipe[x].name = recipeList.recipe[random].name
				mealPlanList.recipe[x].ingredients = recipeList.recipe[random].ingredients
			}
			activityBinding.contentMain.recyclerMP.adapter!!.notifyItemRangeChanged(0, DEFAULT_NUM_DAYS)
			true
		}
		R.id.action_list -> {
			fragmentTransaction(ShoppingListFragment(), "shoppingList")
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
	override fun onBackPressed() {
		super.onBackPressed()
		if (supportFragmentManager.backStackEntryCount > 0) {
			supportFragmentManager.popBackStack()
		}
	}
	override fun fragmentTransaction(fragment: Fragment, tag: String) {
		val fragmentManager = supportFragmentManager
		val transaction = fragmentManager.beginTransaction()
		transaction.replace(R.id.root_layout, fragment, tag)
		transaction.addToBackStack(null)
		transaction.commit()
	}
	override fun updateRecyclerDate(date: Long) {
		mealPlanList.startDate = date
		activityBinding.contentMain.recyclerDate.adapter!!.notifyItemRangeChanged(0, DEFAULT_NUM_DAYS)
	}
	override fun updateRecyclerMP() {
		activityBinding.contentMain.recyclerMP.adapter!!.notifyItemRangeChanged(0, DEFAULT_NUM_DAYS)
	}
	override fun updateRecyclerRecipes() {
		activityBinding.contentMain.recyclerRecipes.adapter!!.notifyDataSetChanged()
	}
	override fun saveDefaultPlan() {
		this.openFileOutput(DEFAULT_PLAN_FILE, Context.MODE_PRIVATE).use {
			val jsonToFile = Json.encodeToString(mealPlanList)
			it.write(jsonToFile.toByteArray())
		}
	}
	override fun onDestroy() {
		saveDefaultPlan()
		super.onDestroy()
	}
	private fun updateRecipeList(offset: Int) {
		recipeList.recipe.clear()
		when (offset) {
			0 -> {
				for (x in 0 until masterRecipeList.recipe.size) {
					val cat = masterRecipeList.recipe[x].cat
					if (cat == 1 || cat == 3 || cat == 5 || cat == 7) {
						recipeList.recipe.add(masterRecipeList.recipe[x])
					}
				}
			}
			7 -> {
				for (x in 0 until masterRecipeList.recipe.size) {
					val cat = masterRecipeList.recipe[x].cat
					if (cat == 2 || cat == 3 || cat == 6 || cat == 7) {
						recipeList.recipe.add(masterRecipeList.recipe[x])
					}
				}
			}
			14 -> {
				for (x in 0 until masterRecipeList.recipe.size) {
					recipeList.recipe.add(masterRecipeList.recipe[x])
				}
			}
		}
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
	override fun getItemCount() = DEFAULT_NUM_DAYS

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val inflatedView = LayoutInflater.from(parent.context).inflate(
				R.layout.recycler_meal_plan,
				parent,
				false
		)
		return ViewHolder(inflatedView)
	}
	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.bind(position+mealPlanOffset)
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
	override fun getItemCount() = DEFAULT_NUM_DAYS

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val inflatedView = LayoutInflater.from(parent.context).inflate(
				R.layout.recycler_meal_plan,
				parent,
				false
		)
		return ViewHolder(inflatedView)
	}
	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val gc = GregorianCalendar()
		val today = gc.get(Calendar.DATE)
		gc.timeInMillis =  mealPlanList.startDate
		gc.add(Calendar.DATE, position)

		if (gc.get(Calendar.DATE) == today) {
			holder.tvItem.typeface = Typeface.DEFAULT_BOLD
		}

		val df: DateFormat = SimpleDateFormat("MM/dd E")
		holder.tvItem.text = df.format(gc.time)
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
			val sourcePos = source.adapterPosition+mealPlanOffset
			val targetPos = target.adapterPosition+mealPlanOffset
			val saveSource = mealPlanList.recipe[sourcePos].copy()
			mealPlanList.recipe[sourcePos] = mealPlanList.recipe[targetPos].copy()
			mealPlanList.recipe[targetPos] = saveSource

			recyclerView.adapter!!.notifyItemMoved(source.adapterPosition, target.adapterPosition)

			return true
		}
		override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
			val pos = viewHolder.adapterPosition+mealPlanOffset
			mealPlanList.recipe[pos].name = DEFAULT_EMPTY_RECIPE
			mealPlanList.recipe[pos].ingredients = "  "

			recycler.adapter!!.notifyItemChanged(viewHolder.adapterPosition)
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
				val pos = posOfView+mealPlanOffset
				mealPlanList.recipe[pos].name = recipeList.recipe[posOfDragged].name
				mealPlanList.recipe[pos].ingredients = recipeList.recipe[posOfDragged].ingredients
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
// TODO: Need to add an AddRecipeFragment for adding recipes easier
// TODO: Upload recipe from a website -- website parser or Copy/paste parser