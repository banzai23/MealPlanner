package com.example.mealplanner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RecyclerAdapterRecipeManager(private val listener: RecyclerClickListener) :
		RecyclerView.Adapter<RecyclerAdapterRecipeManager.ViewHolder>() {
	class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
		val tvItem: TextView = v.findViewById(R.id.tv_Item)
		val btnEdit: Button = v.findViewById(R.id.btn_Edit)
		val btnDelete: Button = v.findViewById(R.id.btn_Delete)
	}

	override fun getItemCount() = masterRecipeList.recipe.size

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val inflatedView = LayoutInflater.from(parent.context).inflate(
			R.layout.recycler_recipe_manager,
				parent,
				false)
		return ViewHolder(inflatedView)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.tvItem.text = masterRecipeList.recipe[position].name
		holder.btnEdit.setOnClickListener { view -> listener.onClick(view, holder.adapterPosition) }
		holder.btnDelete.setOnClickListener { view -> listener.onClick(view, holder.adapterPosition) }
	}
}