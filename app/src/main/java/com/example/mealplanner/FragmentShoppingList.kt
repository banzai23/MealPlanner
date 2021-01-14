package com.example.mealplanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mealplanner.databinding.FragmentShoppingListBinding

class ShoppingListFragment : Fragment(R.layout.fragment_shopping_list) {
	private var _binding: FragmentShoppingListBinding? = null

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val binding = FragmentShoppingListBinding.bind(view)
		_binding = binding

		binding.tvTitle.text = "Shopping List"

		val getAll: MutableList<String> = mutableListOf()
		for (x in 0 until mealPlanList.recipe.size) {
			val split = mealPlanList.recipe[x].ingredients.trim().split("\n")
			getAll.addAll(split)
		}
		val allIngredients = getAll.toSet().toMutableList()
		binding.recyclerShoppingList.layoutManager = LinearLayoutManager(requireContext())
		binding.recyclerShoppingList.adapter = RecyclerAdapterShoppingList(allIngredients)
		val setTouch = setSLTouchHelper(allIngredients, binding.recyclerShoppingList)
		setTouch.attachToRecyclerView(binding.recyclerShoppingList)

		binding.btnBack.setOnClickListener {
			requireActivity().supportFragmentManager.popBackStack()
		}
	}
	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}
}
class RecyclerAdapterShoppingList(private val ingredients: MutableList<String>) :
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
fun setSLTouchHelper(ingredients: MutableList<String>, recycler: RecyclerView): ItemTouchHelper {
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
			val saveSource = ingredients[sourcePos]
			ingredients[sourcePos] = ingredients[targetPos]
			ingredients[targetPos] = saveSource
			recyclerView.adapter!!.notifyItemMoved(sourcePos, targetPos)

			return true
		}
		override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
			val pos = viewHolder.adapterPosition
			ingredients.removeAt(pos)
			recycler.adapter!!.notifyItemRemoved(pos)
		}
	}
	return ItemTouchHelper(itemTouchCallback)
}