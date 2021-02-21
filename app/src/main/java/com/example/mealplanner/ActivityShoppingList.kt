package com.example.mealplanner

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mealplanner.databinding.ActivityShoppingListBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.FileNotFoundException
import java.util.*


class ShoppingListActivity : AppCompatActivity() {
	private var _binding: ActivityShoppingListBinding? = null

	var allIngredients: MutableList<String> = mutableListOf()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val binding: ActivityShoppingListBinding = ActivityShoppingListBinding.inflate(layoutInflater)
		_binding = binding
		setContentView(binding.root)
		setSupportActionBar(findViewById(R.id.toolbar))
		supportActionBar!!.setDisplayHomeAsUpEnabled(true)
		supportActionBar!!.setDisplayShowHomeEnabled(true)

		binding.toolbar.setNavigationOnClickListener {
			finish()
		}
		binding.recyclerShoppingList.layoutManager = LinearLayoutManager(this)
		if (!loadShoppingList())    // if returns false -- no file found
			generateShoppingList()
		binding.recyclerShoppingList.adapter = RecyclerAdapterShoppingList(allIngredients)
		val setTouch = setSLTouchHelper(binding.recyclerShoppingList)
		setTouch.attachToRecyclerView(binding.recyclerShoppingList)
	}
	override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
		R.id.action_item -> {
			val builder = AlertDialog.Builder(this)
			val editText = EditText(this)
			builder.setMessage(R.string.dialog_add_item)
			builder.setView(editText)
				.setPositiveButton(R.string.dialog_ok
				) { dialog, _ ->
					onDialogPositiveClick(dialog, editText.text.toString())
				}
				.setNegativeButton(R.string.str_cancel
				) { dialog, _ ->
					onDialogNegativeClick(dialog)
				}
			builder.create()
			builder.show()
			true
		}
		R.id.action_save -> {
			this.openFileOutput(DEFAULT_SHOPPING_LIST_FILE, Context.MODE_PRIVATE).use {
				val jsonToFile = Json.encodeToString(allIngredients)
				it.write(jsonToFile.toByteArray())
			}
			println(allIngredients)
			val snackBar = Snackbar
					.make(_binding!!.recyclerShoppingList, getString(R.string.snackbar_sl_save), Snackbar.LENGTH_LONG)
			snackBar.show()
			true
		}
		R.id.action_load -> {
			loadShoppingList()
			_binding!!.recyclerShoppingList.adapter = RecyclerAdapterShoppingList(allIngredients)
			val snackBar = Snackbar
					.make(_binding!!.recyclerShoppingList, getString(R.string.snackbar_sl_load), Snackbar.LENGTH_LONG)
			snackBar.show()
			true
		}
		R.id.action_generate -> {
			generateShoppingList()
			_binding!!.recyclerShoppingList.adapter = RecyclerAdapterShoppingList(allIngredients)
			val snackBar = Snackbar
					.make(_binding!!.recyclerShoppingList, getString(R.string.snackbar_sl_generate), Snackbar.LENGTH_SHORT)
			snackBar.show()
			true
		}
		else -> {
			super.onOptionsItemSelected(item)
		}
	}
	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		val inflater: MenuInflater = menuInflater
		inflater.inflate(R.menu.menu_shopping_list, menu)
		return true
	}
	private fun loadShoppingList(): Boolean {
		val inputStream =
			try {
				this.openFileInput(DEFAULT_SHOPPING_LIST_FILE)
			} catch (e: FileNotFoundException) {
				return false
			}
		val inputString = inputStream.bufferedReader().readText()
		val ingredients: MutableList<String> = Json.decodeFromString(inputString)
		if(allIngredients.isNotEmpty())
			allIngredients.clear()
		allIngredients.addAll(ingredients)

		return true
	}
	private fun generateShoppingList() {
		val getAll: MutableList<String> = mutableListOf()
		var totalSize = masterMealPlanList.breakfast.size
		for (x in 0 until totalSize) {
			val split = masterMealPlanList.breakfast[x].ingredients.split("\n")
			for (y in split.indices) {
				if (split[y].isEmpty()) {
					// do nothing
				}
				else if (split[y][0].isDigit())
					getAll.add(split[y].substringAfter(" ").substringBefore(","))
				else
					getAll.add(split[y].substringBefore(","))
			}
		}
		totalSize = masterMealPlanList.lunch.size
		for (x in 0 until totalSize) {
			val split = masterMealPlanList.lunch[x].ingredients.split("\n")
			for (y in split.indices) {
				if (split[y].isEmpty()) {
					// do nothing
				}
				else if (split[y] == "\n") {
					// do nothing
				}
				else if (split[y][0].isDigit())
					getAll.add(split[y].substringAfter(" ").substringBefore(","))
				else
					getAll.add(split[y].substringBefore(","))
			}
		}
		totalSize = masterMealPlanList.dinner.size
		for (x in 0 until totalSize) {
			val split = masterMealPlanList.dinner[x].ingredients.split("\n")
			for (y in split.indices) {
				if (split[y].isEmpty()) {
					// do nothing
				}
				else if (split[y][0].isDigit())
					getAll.add(split[y].substringAfter(" ").substringBefore(","))
				else
					getAll.add(split[y].substringBefore(","))
			}
		}
		var size = getAll.size
		var x = 0
		while (x < size) {
			if (getAll[x].contains("to taste")) {
				getAll.removeAt(x)
				size--
			} else if (getAll[x].contains("water")) {
				getAll.removeAt(x)
				size--
			} else if (getAll[x].contains("Kosher salt")) {
				getAll[x] = "Kosher salt"
			} else if (getAll[x].contains("salt")) {
				getAll.removeAt(x)
				size--
			}

			when {
				getAll[x].contains(") ") -> {
					getAll[x] = getAll[x].substringAfter(") ")
				}
				getAll[x].contains(")") -> {
					getAll[x] = getAll[x].substringAfter(")")
				}
				getAll[x].startsWith("teaspoon") -> {
					getAll[x] = getAll[x].substringAfter(" ")
				}
				getAll[x].startsWith("tsp") -> {
					getAll[x] = getAll[x].substringAfter(" ")
				}
				getAll[x].startsWith("tablespoon") -> {
					getAll[x] = getAll[x].substringAfter(" ")
				}
				getAll[x].startsWith("tbsp") -> {
					getAll[x] = getAll[x].substringAfter(" ")
				}
				getAll[x].startsWith("cup") -> {
					getAll[x] = getAll[x].substringAfter(" ")
				}
				getAll[x].startsWith("large") -> {
					getAll[x] = getAll[x].substringAfter(" ")
				}
				getAll[x].startsWith("medium") -> {
					getAll[x] = getAll[x].substringAfter(" ")
				}
				getAll[x].startsWith("small") -> {
					getAll[x] = getAll[x].substringAfter(" ")
				}
				getAll[x].startsWith("chopped ") -> {
					getAll[x] = getAll[x].substringAfter(" ")
				}
				getAll[x].startsWith("diced ") -> {
					getAll[x] = getAll[x].substringAfter(" ")
				}
				getAll[x].startsWith("prepared") -> {
					getAll[x] = getAll[x].substringAfter(" ")
				}
				getAll[x].startsWith("ounce") -> {
					getAll[x] = getAll[x].substringAfter(" ")
				}
				getAll[x].startsWith("oz") -> {
					getAll[x] = getAll[x].substringAfter(" ")
				}
				getAll[x].startsWith("pound") -> {
					getAll[x] = getAll[x].substringAfter(" ")
				}
				getAll[x].startsWith("lb") -> {
					getAll[x] = getAll[x].substringAfter(" ")
				}
				getAll[x].startsWith("½") -> {
					getAll[x] = getAll[x].substringAfter(" ").substringAfter(" ")
				}
				getAll[x].startsWith("¼") -> {
					getAll[x] = getAll[x].substringAfter(" ").substringAfter(" ")
				}
				getAll[x].startsWith("¾") -> {
					getAll[x] = getAll[x].substringAfter(" ").substringAfter(" ")
				}
			}
			getAll[x] = getAll[x].capitalize(Locale.ENGLISH)
			x++
		}

		allIngredients = getAll.toSet().toMutableList()
	}
	private fun onDialogPositiveClick(dialog: DialogInterface, itemText: String) {
		if (itemText.isNotEmpty()) {
			val string = itemText.trim().capitalize(Locale.ENGLISH)
			allIngredients.add(string)
			_binding!!.recyclerShoppingList.adapter!!.notifyDataSetChanged()
			dialog.dismiss()
		}
	}
	private fun onDialogNegativeClick(dialog: DialogInterface) {
		dialog.cancel()
	}
	private fun setSLTouchHelper(recycler: RecyclerView): ItemTouchHelper {
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
				val saveSource = allIngredients[sourcePos]
				allIngredients[sourcePos] = allIngredients[targetPos]
				allIngredients[targetPos] = saveSource
				recyclerView.adapter!!.notifyItemMoved(sourcePos, targetPos)

				return true
			}
			override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
				val pos = viewHolder.adapterPosition
				allIngredients.removeAt(pos)
				recycler.adapter!!.notifyItemRemoved(pos)
			}
		}
		return ItemTouchHelper(itemTouchCallback)
	}
	override fun onDestroy() {
		super.onDestroy()
		_binding = null
	}
}
class RecyclerAdapterShoppingList(private var ingredients: MutableList<String>) :
		RecyclerView.Adapter<RecyclerAdapterShoppingList.ViewHolder>() {
	class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
		val tvItem: TextView = v.findViewById(R.id.tvItem)
	}

	override fun getItemCount() = ingredients.size

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val inflatedView = LayoutInflater.from(parent.context).inflate(
				R.layout.recycler_meal_plan,
				parent,
				false
		)
		return ViewHolder(inflatedView)
	}
	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.tvItem.text = ingredients[position]
	}
}