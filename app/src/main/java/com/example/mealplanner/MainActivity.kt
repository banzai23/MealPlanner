package com.example.mealplanner

import android.content.ClipData
import android.content.ClipDescription
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mealplanner.databinding.ActivityMainBinding
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

const val DEFAULT_NUM_DAYS = 7
const val DEFAULT_RECIPE_FILE = "savedRecipes.json"
const val DEFAULT_PLAN_DIR = "plans"
const val DEFAULT_PLAN_FILE = "defaultPlan.json"

lateinit var mealPlanList: MealPlan
lateinit var recipeList: Recipe
var assetsLoaded: Boolean = false

@Serializable
data class Recipe(var recipe: MutableList<RecipeX>)
@Serializable
data class MealPlan(var recipe: MutableList<RecipeX>, var startDate: Long) {
	fun getSelectedStartDate(day_forward: Int): String {
		val gc = GregorianCalendar()
		gc.timeInMillis = startDate
		gc.add(Calendar.DATE, day_forward)
		val df: DateFormat = SimpleDateFormat("MM/dd E")
		return df.format(gc.time)
	}
	fun getDefaultSaveDate(): String {
		val gc = GregorianCalendar()
		var df = SimpleDateFormat("MMM d-")
		val str1: String
		val str2: String
		gc.timeInMillis = startDate
		gc.add(Calendar.DATE, 0)

		str1 = df.format(gc.time)

		gc.add(Calendar.DATE, 6)
		df = SimpleDateFormat("d, yyyy")
		str2 = df.format(gc.time)

		return (str1+str2)
	}
}
@Serializable
data class RecipeX(var name: String = "", var ingredients: String = "")

interface ActivityInterface {
	fun updateRecyclerDate(date: Long)
	fun updateRecyclerMP()
	fun fragmentTransaction(fragment: Fragment, tag: String)
}
class MainActivity : AppCompatActivity(), ActivityInterface {
	private lateinit var activityBinding: ActivityMainBinding
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// load asset
		if (!assetsLoaded) {
			var inputStream: InputStream =
			try {
				this.openFileInput(DEFAULT_RECIPE_FILE)
			} catch (e: FileNotFoundException) {
				this.resources.openRawResource(R.raw.savedrecipes)
			}

			val path = File(this.filesDir.path+File.separatorChar+DEFAULT_PLAN_DIR)
			path.mkdirs()   // if DEFAULT_PLAN_DIR doesn't exist, create it now

			var inputString = inputStream.bufferedReader().use { it.readText() }
			recipeList = Json.decodeFromString(inputString)
			recipeList.recipe.sortBy { it.toString() }

			inputStream =
			try {
				this.openFileInput(DEFAULT_PLAN_FILE)
			} catch(e: FileNotFoundException) {
				this.resources.openRawResource(R.raw.defaultmealplan)
			}
			inputString = inputStream.bufferedReader().use { it.readText() }
			mealPlanList = Json.decodeFromString(inputString)

			assetsLoaded = true
		}

		// asset loaded
		activityBinding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(activityBinding.root)
		setSupportActionBar(findViewById(R.id.toolbar))

		val binding = activityBinding.contentMain

		binding.recyclerDate.layoutManager = LinearLayoutManager(this)
		binding.recyclerDate.adapter = RecyclerAdapterDate()
		binding.recyclerDate.setHasFixedSize(true)

		binding.recyclerMP.layoutManager = LinearLayoutManager(this)
		binding.recyclerMP.adapter = RecyclerAdapterMealPlan(binding.recyclerMP)
		binding.recyclerMP.setHasFixedSize(true)
		val ithMP = setItemTouchHelper(binding.recyclerMP, mealPlanList.recipe)
		ithMP.attachToRecyclerView(binding.recyclerMP)

		binding.recyclerRecipes.layoutManager = LinearLayoutManager(this)
		binding.recyclerRecipes.adapter = RecyclerAdapterRecipes()
	}
	override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
		R.id.action_new -> {
			fragmentTransaction(NewPlanFragment(), "newPlan")
			true
		}
		R.id.action_random -> {
			for (x in 0 until mealPlanList.recipe.size) {
				val random = (0 until recipeList.recipe.size).random()
				mealPlanList.recipe[x] = recipeList.recipe[random].copy()
				activityBinding.contentMain.recyclerMP.adapter!!.notifyItemRangeChanged(0, DEFAULT_NUM_DAYS)
			}
			true
		}
		R.id.action_save -> {
			fragmentTransaction(SavePlanFragment(), "savePlan")
			true
		}
		R.id.action_load -> {
			fragmentTransaction(LoadPlanFragment(), "loadPlan")
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
		transaction.add(R.id.root_layout, fragment, tag)
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
}
private class SetDragListener(val posOfView: Int,
                              val recyclerView: RecyclerView) : View.OnDragListener
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
				// Gets the item containing the dragged data
				val item: ClipData.Item = event.clipData.getItemAt(0)

				(v as? ImageView)?.clearColorFilter()
				(v as? TextView)?.setBackgroundColor(Color.TRANSPARENT)
				println("DRAG DROPPED! Information: ")
				//	v.invalidate()

				val posOfDragged: Int = item.text.toString().toInt()
				println(posOfDragged)
				println(posOfView)
				mealPlanList.recipe[posOfView] = recipeList.recipe[posOfDragged].copy()
				recyclerView.adapter!!.notifyItemChanged(posOfView)
				return true
			}
			DragEvent.ACTION_DRAG_ENDED -> {
				// Turns off any color tinting
				(v as? ImageView)?.clearColorFilter()
				(v as? TextView)?.setBackgroundColor(Color.TRANSPARENT)
				// Invalidates the view to force a redraw
				v.invalidate()

				return true
			}
			else -> {
				// An unknown action type was received.
				return false
			}
		}
	}
}
class RecyclerAdapterMealPlan(ra: RecyclerView) :
		RecyclerView.Adapter<RecyclerAdapterMealPlan.ViewHolder>()
{
	private val recyclerAdapter = ra
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
		holder.tvItem.tag = position.toString()
		holder.tvItem.text = mealPlanList.recipe[position].name
		holder.tvItem.setOnDragListener(SetDragListener(position, recyclerAdapter))
	}
}
class RecyclerAdapterRecipes() :
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
		holder.tvItem.setOnClickListener {
			val actInt = holder.tvItem.context as ActivityInterface
			actInt.fragmentTransaction(EditRecipeFragment(position), "savedRecipes")
		}
		holder.tvItem.setOnLongClickListener {
			val data = ClipData.newPlainText("", position.toString())
			val shadowBuilder = View.DragShadowBuilder(it)
			it.startDrag(data, shadowBuilder, it, 0)
		}
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
		holder.tvItem.text = mealPlanList.getSelectedStartDate(position)
	}
}
fun setItemTouchHelper(recycler: RecyclerView, mutableList: MutableList<RecipeX>): ItemTouchHelper {
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
			Collections.swap(mutableList, source.adapterPosition, target.adapterPosition)
			recycler.adapter!!.notifyItemMoved(source.adapterPosition, target.adapterPosition)
			return true
		}
		override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
			mutableList.elementAt(viewHolder.adapterPosition).name = "                              "
			mutableList.elementAt(viewHolder.adapterPosition).ingredients = " "
			recycler.adapter!!.notifyItemChanged(viewHolder.adapterPosition)
		}
	}

	return ItemTouchHelper(itemTouchCallback)
}

// TODO: Set up a settings file
// TODO: