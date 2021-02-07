package com.example.mealplanner

import android.content.Intent
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

interface RecipeActivityInterface {
	fun updateRecycler()
}
class RecipeManagerActivity : AppCompatActivity(), RecipeActivityInterface {
	private var _binding: RecipeManagerActivityBinding? = null
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val binding: RecipeManagerActivityBinding = RecipeManagerActivityBinding.inflate(layoutInflater)
		_binding = binding
		setContentView(binding.root)
		setSupportActionBar(findViewById(R.id.toolbar))
		supportActionBar!!.setDisplayHomeAsUpEnabled(true)
		supportActionBar!!.setDisplayShowHomeEnabled(true)

		binding.toolbar.setNavigationOnClickListener {
			val intent = Intent()
			intent.putExtra("updateRecycler", true)
			setResult(0, intent)
			finish()
		}

		val rmClickListener = object : RecyclerClickListener {
			override fun onClick(view: View, position: Int) {
				if (view.id == R.id.btn_Edit) {
					val fragmentManager = supportFragmentManager
					val transaction = fragmentManager.beginTransaction()
					transaction.replace(
							R.id.rm_root,
							EditRecipeFragment(position, true, false, false),
							"editRecipe"
					)
					transaction.addToBackStack(null)
					transaction.commit()
				}
				else if (view.id == R.id.btn_Delete) {
					masterRecipeList.recipe.removeAt(position)
					binding.recyclerRM.adapter!!.notifyItemRemoved(position)
				}
			}
		}
		binding.recyclerRM.layoutManager = LinearLayoutManager(this)
		binding.recyclerRM.adapter = RecyclerAdapterRecipeManager(rmClickListener)

		binding.btnAddNew.setOnClickListener {
			val fragmentManager = supportFragmentManager
			val transaction = fragmentManager.beginTransaction()
			transaction.replace(
					R.id.rm_root,
					EditRecipeFragment(1001, true, true, false),
					"editRecipe"
			)
			transaction.addToBackStack(null)
			transaction.commit()
		}
	}
	override fun updateRecycler() {
		_binding!!.recyclerRM.adapter!!.notifyDataSetChanged()
		println("UPDATED RECYCLER!")
	}
	override fun onDestroy() {
		super.onDestroy()
		_binding = null
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
		holder.btnDelete.setOnClickListener(object : View.OnClickListener {
			override fun onClick(view: View) {
				listener.onClick(view, holder.adapterPosition)
			}
		})
	}
}