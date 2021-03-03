package com.example.mealplanner

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.PopupMenu
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

class ShoppingListActivity : AppCompatActivity(), PopupMenu.OnMenuItemClickListener {
	private var _binding: ActivityShoppingListBinding? = null
	lateinit var listClickListener: RecyclerClickListener
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

		listClickListener = object : RecyclerClickListener {
			override fun onClick(view: View, position: Int) {
				showPopup(view, position)
			}
		}
		binding.recyclerShoppingList.adapter = RecyclerAdapterShoppingList(listClickListener, allIngredients)
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
			_binding!!.recyclerShoppingList.adapter = RecyclerAdapterShoppingList(listClickListener, allIngredients)
			val snackBar = Snackbar
					.make(_binding!!.recyclerShoppingList, getString(R.string.snackbar_sl_load), Snackbar.LENGTH_LONG)
			snackBar.show()
			true
		}
		R.id.action_generate -> {
			generateShoppingList()
			_binding!!.recyclerShoppingList.adapter = RecyclerAdapterShoppingList(listClickListener, allIngredients)
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
				else if (split[y][0].isDigit()) {
					if (split[y].contains("("))
						getAll.add(split[y].substringAfter(" ").substringBefore("("))
					else
						getAll.add(split[y].substringAfter(" ").substringBefore(","))
				}
				else
					getAll.add(split[y].substringBefore(",").substringBefore("("))
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
			} else if (getAll[x].contains("salt")) {
				getAll.removeAt(x)
				size--
			}

			getAll[x] = getAll[x].trim()
			when {
			/*	getAll[x].contains(") ") ->
					getAll[x] = getAll[x].substringAfter(") ")
				getAll[x].contains(")") ->
					getAll[x] = getAll[x].substringAfter(")") */
				getAll[x].contains("teaspoons ", true) ->
					getAll[x] = getAll[x].substringAfter("teaspoons ")
				getAll[x].contains("teaspoon ", true) ->
					getAll[x] = getAll[x].substringAfter("teaspoon ")
				getAll[x].contains("tablespoons ", true) ->
					getAll[x] = getAll[x].substringAfter("tablespoons ")
				getAll[x].contains("tablespoon ", true) ->
					getAll[x] = getAll[x].substringAfter("tablespoon ")
				getAll[x].startsWith("tsp", true) ->
					getAll[x] = getAll[x].substringAfter(" ")
				getAll[x].startsWith("tsp.", true) ->
					getAll[x] = getAll[x].substringAfter(" ")
				getAll[x].startsWith("tbsp", true) ->
					getAll[x] = getAll[x].substringAfter(" ")
				getAll[x].startsWith("tbsp.", true) ->
					getAll[x] = getAll[x].substringAfter(" ")
				getAll[x].startsWith("cup", true) ->
					getAll[x] = getAll[x].substringAfter(" ")
				getAll[x].startsWith("chopped", true) ->
					getAll[x] = getAll[x].substringAfter(" ")
				getAll[x].startsWith("diced", true) ->
					getAll[x] = getAll[x].substringAfter(" ")
				getAll[x].startsWith("prepared", true) ->
					getAll[x] = getAll[x].substringAfter(" ")
				getAll[x].startsWith("ounce", true) ->
					getAll[x] = getAll[x].substringAfter(" ")
				getAll[x].startsWith("oz", true) ->
					getAll[x] = getAll[x].substringAfter(" ")
				getAll[x].startsWith("pound", true) ->
					getAll[x] = getAll[x].substringAfter(" ")
				getAll[x].startsWith("lb", true) ->
					getAll[x] = getAll[x].substringAfter(" ")
				getAll[x].startsWith("½") ->
					getAll[x] = getAll[x].substringAfter(" ").substringAfter(" ")
				getAll[x].startsWith("¼") ->
					getAll[x] = getAll[x].substringAfter(" ").substringAfter(" ")
				getAll[x].startsWith("¾") ->
					getAll[x] = getAll[x].substringAfter(" ").substringAfter(" ")
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
	private fun onDialogEdit(dialog: DialogInterface, pos: Int, text: String) {
		if (text.isNotBlank()) {
			allIngredients[pos] = text.trim().capitalize(Locale.ENGLISH)
			_binding!!.recyclerShoppingList.adapter!!.notifyDataSetChanged()
		}
		dialog.dismiss()
	}
	private fun onDialogNegativeClick(dialog: DialogInterface) {
		dialog.cancel()
	}
	private fun showPopup(v: View, pos: Int) {
		PopupMenu(v.context, v).apply {
			setOnMenuItemClickListener(this@ShoppingListActivity)
			menu.add(pos, v.id, 0, v.context.getString(R.string.context_edit_list))
			show()
		}
	}
	override fun onMenuItemClick(item: MenuItem): Boolean {
		return if (item.groupId >= 0)
		{
			editRecipePopup(item.groupId)
			true
		} else {
			false
		}
	}
	private fun editRecipePopup(pos: Int) {
		val builder = AlertDialog.Builder(this)
		val editText = EditText(this)
		editText.setText(allIngredients[pos])
		builder.setMessage(R.string.dialog_edit_list)
		builder.setView(editText)
			.setPositiveButton(R.string.dialog_ok
			) { dialog, _ ->
				onDialogEdit(dialog, pos, editText.text.toString())
			}
			.setNegativeButton(R.string.str_cancel
			) { dialog, _ ->
				onDialogNegativeClick(dialog)
			}
		builder.create()
		builder.show()
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
class RecyclerAdapterShoppingList(val clickListener: RecyclerClickListener, private var ingredients: MutableList<String>) :
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
		holder.tvItem.setOnClickListener { view -> clickListener.onClick(view, holder.adapterPosition) }
	}
}