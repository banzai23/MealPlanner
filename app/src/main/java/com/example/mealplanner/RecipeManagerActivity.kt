package com.example.mealplanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mealplanner.databinding.RecipeManagerActivityBinding

class RecipeManagerActivity : AppCompatActivity() {
	private lateinit var binding: RecipeManagerActivityBinding
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = RecipeManagerActivityBinding.inflate(layoutInflater)
		setContentView(binding.root)
		setSupportActionBar(findViewById(R.id.toolbar))
		supportActionBar!!.setDisplayHomeAsUpEnabled(true)
		supportActionBar!!.setDisplayShowHomeEnabled(true)

		val rmClickListener = object : RecyclerClickListener {
			override fun onClick(view: View, position: Int) {
				val fragmentManager = supportFragmentManager
				val transaction = fragmentManager.beginTransaction()
				transaction.replace(
					R.id.rm_root,
					EditRecipeFragment(position, true),
					"editRecipe"
				)
				transaction.addToBackStack(null)
				transaction.commit()
			}
		}
		binding.recyclerRM.layoutManager = LinearLayoutManager(this)
		binding.recyclerRM.adapter = RecyclerAdapterRecipeManager(rmClickListener)
	}
}
class RecyclerAdapterRecipeManager(listenerPass: RecyclerClickListener):
	RecyclerView.Adapter<RecyclerAdapterRecipeManager.ViewHolder>()
{
	val listener = listenerPass
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
			false
		)
		return ViewHolder(inflatedView)
	}
	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.tvItem.text = masterRecipeList.recipe[position].name
		holder.btnEdit.setOnClickListener(object : View.OnClickListener {
			override fun onClick(view: View) {
				listener.onClick(view, holder.adapterPosition)
			}
		})
		// holder.btnDelete.setonClickListener {}
	}
}