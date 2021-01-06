package com.example.mealplanner

import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mealplanner.databinding.ActivityMainBinding
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.FileNotFoundException
import java.io.InputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

const val DEFAULT_NUM_DAYS = 7
const val DEFAULT_RECIPE_FILE = "savedRecipes.json"
const val DEFAULT_PLAN_FILE = "defaultPlan.json"
const val DEFAULT_EMPTY_RECIPE = "                               "

lateinit var mealPlanList: MealPlan
lateinit var recipeList: Recipe
var assetsLoaded: Boolean = false

@Serializable
data class Recipe(var recipe: MutableList<RecipeX>)
@Serializable
data class MealPlan(var recipe: MutableList<RecipeX>, var startDate: Long)
@Serializable
data class RecipeX(var name: String = "", var ingredients: String = "")

interface ActivityInterface {
	fun updateRecyclerDate(date: Long)
	fun updateRecyclerMP()
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
			try {
				this.openFileInput(DEFAULT_RECIPE_FILE)
			} catch (e: FileNotFoundException) {
				this.resources.openRawResource(R.raw.savedrecipes)
			}
			var inputString = inputStream.bufferedReader().use { it.readText() }
			recipeList = Json.decodeFromString(inputString)
			recipeList.recipe.sortBy { it.toString() }

			inputStream =
			try {
				this.openFileInput(DEFAULT_PLAN_FILE)
			} catch (e: FileNotFoundException) {
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

		window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
		// hide the Android keyboard, unless TextInputEditText clicked/focused on
		// set the RecyclerDate, set fixed size, for DEFAULT_NUM_DAYS
		binding.recyclerDate.layoutManager = LinearLayoutManager(this)
		binding.recyclerDate.adapter = RecyclerAdapterDate()
		binding.recyclerDate.setHasFixedSize(true)
		// set up RecyclerMP
		val mpClickListener = object : RecyclerClickListener {
			override fun onClick(view: View, position: Int) {
				if(mealPlanList.recipe[position].name != DEFAULT_EMPTY_RECIPE) {
					fragmentTransaction(ViewMPRecipeFragment(position), "viewMPRecipe")
				}
			}
		}
		binding.recyclerMP.layoutManager = LinearLayoutManager(this)
		binding.recyclerMP.adapter = RecyclerAdapterMealPlan(mpClickListener, binding.recyclerMP)
		binding.recyclerMP.setHasFixedSize(true)
		val ithMP = setMPTouchHelper(binding.recyclerMP)    // it's called MPTouchHelper because
		ithMP.attachToRecyclerView(binding.recyclerMP)      // it is specialized for the mealPlanList object
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
	}
	override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
		R.id.action_new -> {
			fragmentTransaction(CalendarFragment(), "calendar")
			true
		}
		R.id.action_random -> {
			for (x in 0 until mealPlanList.recipe.size) {
				val random = (0 until recipeList.recipe.size).random()
				mealPlanList.recipe[x] = recipeList.recipe[random].copy()
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
class RecyclerAdapterMealPlan(rvClickListener: RecyclerClickListener, val recycler: RecyclerView) :
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
		gc.timeInMillis =  mealPlanList.startDate
		gc.add(Calendar.DATE, position)
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
			val sourcePos = source.adapterPosition
			val targetPos = target.adapterPosition
			val saveSource = mealPlanList.recipe[sourcePos].copy()
			mealPlanList.recipe[sourcePos] = mealPlanList.recipe[targetPos].copy()
			mealPlanList.recipe[targetPos] = saveSource

			recyclerView.adapter!!.notifyItemMoved(sourcePos, targetPos)

			return true
		}
		override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
			val pos = viewHolder.adapterPosition
			mealPlanList.recipe[pos].name = DEFAULT_EMPTY_RECIPE
			mealPlanList.recipe[pos].ingredients = "  "

			recycler.adapter!!.notifyItemChanged(pos)
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
				mealPlanList.recipe[posOfView] = recipeList.recipe[posOfDragged].copy()
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
// TODO: Set up a settings file
// TODO: Try to set up where a drop event triggers a DataSetChange or ItemRangeChange