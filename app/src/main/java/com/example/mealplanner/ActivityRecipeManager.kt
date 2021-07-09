package com.example.mealplanner

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mealplanner.databinding.ActivityRecipeManagerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

interface RecipeActivityInterface {
	fun updateRecycler()
}

class RecipeManagerActivity : AppCompatActivity(), RecipeActivityInterface {
	private var _binding: ActivityRecipeManagerBinding? = null
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val binding: ActivityRecipeManagerBinding = ActivityRecipeManagerBinding.inflate(layoutInflater)
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
					loadEditRecipe(position, false)
				} else if (view.id == R.id.btn_Delete) {
					val builder = AlertDialog.Builder(this@RecipeManagerActivity)
					val dialogString = getString(R.string.dialog_delete_recipe)+" \""+masterRecipeList.recipe[position].name+"\"?"
					builder.setMessage(dialogString)
					builder.setPositiveButton(R.string.dialog_yes
					) { dialog, _ ->
						masterRecipeList.recipe.removeAt(position)
						binding.recyclerRM.adapter!!.notifyItemRemoved(position)
						dialog.dismiss()
					}
					.setNegativeButton(R.string.dialog_no
					) { dialog, _ ->
						dialog.dismiss()
					}
					builder.create()
					builder.show()
				}
			}
		}
		binding.recyclerRM.layoutManager = LinearLayoutManager(this)
		binding.recyclerRM.adapter = RecyclerAdapterRecipeManager(rmClickListener)
	}
	override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
		R.id.action_new_recipe -> {
			masterRecipeList.recipe.add(RecipeX("", "", "", 0, true))
			loadEditRecipe(masterRecipeList.recipe.size - 1, true) // the position is the new size - 1
			true
		}
		R.id.action_new_recipe_internet -> {
			openDialog()
			true
		}
		else -> {
			super.onOptionsItemSelected(item)
		}
	}
	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		val inflater: MenuInflater = menuInflater
		inflater.inflate(R.menu.menu_recipe_manager, menu)
		return true
	}
	private fun openDialog() {
		val builder = AlertDialog.Builder(this)
		val editText = EditText(this)
		builder.setMessage(R.string.dialog_website_url)
		builder.setView(editText)
			.setPositiveButton(R.string.dialog_ok
			) { dialog, _ ->
				onDialogPositiveClick(dialog, editText.text.toString())
			}
			.setNegativeButton(R.string.str_cancel
			) { dialog, _ ->
				dialog.cancel()
			}
		builder.create()
		builder.show()
	}
	private fun showErrorDialog(errorCode: Int) {
		val builder = AlertDialog.Builder(this)
		if (errorCode == 1)
			builder.setMessage(R.string.dialog_recipe_internet_error1)
		else if (errorCode == 2)
			builder.setMessage(R.string.dialog_recipe_internet_error2)
		else if (errorCode == 3)
			builder.setMessage(R.string.dialog_recipe_internet_error3)
		else
			builder.setMessage(R.string.dialog_recipe_internet_error)

		builder.setPositiveButton(R.string.dialog_ok
		) { dialog, _ ->
			dialog.dismiss()
		}
		builder.create()
		builder.show()
	}

	private fun onDialogPositiveClick(dialog: DialogInterface, url: String) {
		_binding!!.progressBar.visibility = View.VISIBLE
		GlobalScope.launch(Dispatchers.IO) {
			val recipeAndReturn = getRecipeFromURL(url)
			runOnUiThread {
				if (recipeAndReturn.returnCode != 0) {
					dialog.dismiss()
					_binding!!.progressBar.visibility = View.INVISIBLE
					showErrorDialog(recipeAndReturn.returnCode)
				} else {
					dialog.dismiss()
					_binding!!.progressBar.visibility = View.INVISIBLE
					masterRecipeList.recipe.add(RecipeX(recipeAndReturn.name,
							recipeAndReturn.ingredients,
							recipeAndReturn.instructions,
							0, true))
					loadEditRecipe(masterRecipeList.recipe.size - 1, true) // the position is the new size - 1
				}
			}
		}
	}
	override fun updateRecycler() {
		_binding!!.recyclerRM.adapter!!.notifyDataSetChanged()
	}
	override fun onDestroy() {
		super.onDestroy()
		_binding = null
	}
	private fun loadEditRecipe(pos: Int, deleteOnCancel: Boolean) {
		val fragmentManager = supportFragmentManager
		val transaction = fragmentManager.beginTransaction()
		val findFrag = fragmentManager.findFragmentByTag("editRecipe")
		fragmentManager.executePendingTransactions()
		if (findFrag != null) {  // following code keeps only one instance of any fragment at one time
			transaction.replace(
					R.id.rm_root,
					findFrag,
					"editRecipe")
		} else {
			transaction.add(R.id.rm_root, EditRecipeFragment(pos, true,
					false, deleteOnCancel), "editRecipe")
			transaction.addToBackStack(null)
		}
		transaction.commit()
	}
}
class RecyclerAdapterRecipeManager(private val listener: RecyclerClickListener):
	RecyclerView.Adapter<RecyclerAdapterRecipeManager.ViewHolder>()
{
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