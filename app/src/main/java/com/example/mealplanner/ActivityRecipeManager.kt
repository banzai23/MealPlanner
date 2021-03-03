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
			openDialog(false)
			true
		}
		R.id.action_new_recipe_internet -> {
			openDialog(true)
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
	private fun openDialog(addInternet: Boolean) {
		val builder = AlertDialog.Builder(this)
		val editText = EditText(this)
		if (addInternet)
			builder.setMessage(R.string.dialog_website_url)
		else
			builder.setMessage(R.string.dialog_change_title)
		builder.setView(editText)
			.setPositiveButton(R.string.dialog_ok
			) { dialog, _ ->
				onDialogPositiveClick(dialog, addInternet, editText.text.toString())
			}
			.setNegativeButton(R.string.str_cancel
			) { dialog, _ ->
				onDialogNegativeClick(dialog)
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
		else
			builder.setMessage(R.string.dialog_recipe_internet_error)

		builder.setPositiveButton(R.string.dialog_ok
		) { dialog, _ ->
			dialog.dismiss()
		}
		builder.create()
		builder.show()
	}
	private fun onDialogPositiveClick(dialog: DialogInterface, parseWebsite: Boolean, urlOrTitle: String) {
		fun loadEditRecipe(pos: Int) {
			val fragmentManager = supportFragmentManager
			val transaction = fragmentManager.beginTransaction()
			transaction.replace(
					R.id.rm_root,
					EditRecipeFragment(pos, true,
							false, true),
					"editRecipe"
			)
			transaction.addToBackStack(null)
			transaction.commit()
		}

		val addThis =
			if (!parseWebsite)
				RecipeX(urlOrTitle.capitalize(Locale.ENGLISH), "", "", 0, true)
			else
				RecipeX("", "", "", 0, true)
		masterRecipeList.recipe.add(addThis)
		val pos = masterRecipeList.recipe.size - 1 // the position is the new size - 1
		// TODO: Add loading dialog
		if (parseWebsite) {
			val returnCode = getRecipeFromHTML(urlOrTitle, pos)
			if (returnCode != 0) {
				dialog.dismiss()
				masterRecipeList.recipe.removeLast()
				showErrorDialog(returnCode)
			}
			else {
				dialog.dismiss()
				loadEditRecipe(pos)
			}
		} else {
			dialog.dismiss()
			loadEditRecipe(pos)
		}
	}
	private fun onDialogNegativeClick(dialog: DialogInterface) {
		dialog.cancel()
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