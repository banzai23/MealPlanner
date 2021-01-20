package com.example.mealplanner

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import com.example.mealplanner.databinding.FragmentEditRecipesBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class EditRecipeFragment(position: Int, editMaster: Boolean): Fragment(R.layout.fragment_edit_recipes) {
	private var _binding: FragmentEditRecipesBinding? = null
	private val pos = position  // this allows access to position in the following functions
	private val master = editMaster
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val binding = FragmentEditRecipesBinding.bind(view)
		var cat: Int
		_binding = binding

		val editList: Recipe =
		if (master)
			masterRecipeList
		else
			recipeList

		binding.etRecipe.setText(editList.recipe[pos].ingredients)
		binding.etInstructions.setText(editList.recipe[pos].instructions)
		binding.tvRecipe.text = editList.recipe[pos].name
		cat = editList.recipe[pos].cat

		// START Checkbox check for checks
		if (cat == BREAKFAST_CAT)
			binding.cb1.isChecked = true
		if (cat == LUNCH_CAT)
			binding.cb2.isChecked = true
		if (cat == BREAKFAST_CAT + LUNCH_CAT) {
			binding.cb1.isChecked = true
			binding.cb2.isChecked = true
		}
		if (cat == DINNER_CAT)
			binding.cb3.isChecked = true
		if (cat == BREAKFAST_CAT + DINNER_CAT) {
			binding.cb1.isChecked = true
			binding.cb3.isChecked = true
		}
		if (cat == LUNCH_CAT + DINNER_CAT) {
			binding.cb2.isChecked = true
			binding.cb3.isChecked = true
		}
		if (cat == BREAKFAST_CAT + LUNCH_CAT + DINNER_CAT) {
			binding.cb1.isChecked = true
			binding.cb2.isChecked = true
			binding.cb3.isChecked = true
		}
		// END Checkbox check for checks
		// START Click listeners for Checkboxes
		binding.cb1.setOnClickListener {
			if (binding.cb1.isChecked)
				cat += 1
			else if (!binding.cb1.isChecked)
				cat -= 1
		}
		binding.cb2.setOnClickListener {
			if (binding.cb2.isChecked)
				cat += 2
			else if (!binding.cb2.isChecked)
				cat -= 2
		}
		binding.cb3.setOnClickListener {
			if (binding.cb3.isChecked)
				cat += 4
			else if (!binding.cb3.isChecked)
				cat -= 4
		}

		if (!editList.recipe[pos].isMeal)
			binding.rbSide.isChecked = true

		binding.rbMealOrSide.setOnClickListener {
			editList.recipe[pos].isMeal = binding.rbMeal.isChecked
		}
		// END Click listeners for Checkboxes
		binding.btnCancel.setOnClickListener {
			exitEditRecipes()    // feed the category back to either the master or recipe for update
		}
		binding.btnSave.setOnClickListener {
			if (cat in 1..7) {
				if (master) {
					masterRecipeList.recipe[pos].cat = cat
					masterRecipeList.recipe[pos].ingredients = binding.etRecipe.text.toString()
					masterRecipeList.recipe[pos].instructions = binding.etInstructions.text.toString()
					requireContext().openFileOutput(DEFAULT_RECIPE_FILE, Context.MODE_PRIVATE).use {
						val jsonToFile = Json.encodeToString(masterRecipeList)
						it.write(jsonToFile.toByteArray())
					}
				} else {
					recipeList.recipe[pos].cat = cat
					recipeList.recipe[pos].ingredients = binding.etRecipe.text.toString()
					recipeList.recipe[pos].instructions = binding.etInstructions.text.toString()
					requireContext().openFileOutput(DEFAULT_RECIPE_FILE, Context.MODE_PRIVATE).use {
						val jsonToFile = Json.encodeToString(masterRecipeList)
						// even though recipeList was update, we still always
						// save the master because that has every recipe
						it.write(jsonToFile.toByteArray())
					}
				}
				exitEditRecipes() // feed the category back to either the master or recipe for update
			}
			else {
				Snackbar.make(binding.cb1, getString(R.string.warn_noCat), 5)
			}
		}
	}
	private fun exitEditRecipes() {
		val imm: InputMethodManager = requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
		imm.hideSoftInputFromWindow(requireView().windowToken, 0)

		requireActivity().supportFragmentManager.popBackStack()
	}
	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}
}
/*
class SavedRecipesFragment: Fragment(R.layout.fragment_saved_recipes) {
	private var _binding: FragmentSavedRecipesBinding? = null

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val binding = FragmentSavedRecipesBinding.bind(view)
		_binding = binding

		binding.recyclerSR.layoutManager = LinearLayoutManager(this.context)
		val srAdapter = RecyclerAdapterSR(this)
		binding.recyclerSR.adapter = srAdapter
	}
	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}
}
class RecyclerAdapterSR(fragment: Fragment?):
		RecyclerView.Adapter<ViewHolder>()
{
	private val attFragment = fragment
	class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
		val btnSR: Button = v.findViewById(R.id.btn_sr_recipe_name)
	}

	override fun getItemCount() = recipeList.recipe.size

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val inflatedView = LayoutInflater.from(parent.context).inflate(
				R.layout.recycler_saved_recipes,
				parent,
				false
		)
		return ViewHolder(inflatedView)
	}
	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.btnSR.text = recipeList.recipe[position].name
		holder.btnSR.setOnClickListener {
			println("Clicked it! $attFragment")
			val bundle = Bundle()
			bundle.putInt("pos", holder.adapterPosition)
			val fragmentManager = attFragment!!.childFragmentManager
			val transaction = fragmentManager.beginTransaction()
			val sREFrag: Fragment = SavedRecipesEditFragment()
			sREFrag.arguments = bundle
			println(bundle)
			println(bundle.getInt("pos"))
			transaction.add(R.id.layout_saved_recipes, sREFrag, "savedRecipesEdit")
			transaction.addToBackStack(null)
			transaction.commit()
		}
	}
}
*/
/*
class RecyclerAdapterSR():
	RecyclerView.Adapter<ViewHolder>()
{
	class ViewHolder(v: View, var listener: ItemClicked) : RecyclerView.ViewHolder(v), View.OnClickListener {
		val btnSR: Button
		init {
			btnSR = v.findViewById(R.id.btn_sr_recipe_name)
			v.setOnClickListener(this)
		}
		override fun onClick(v: View?) {
			println("Start transform!")
			listener.startTransform(v)
		}
		interface ItemClicked {
			fun startTransform(caller: View?)
		}
	}
	override fun getItemCount() = recipeList.recipe.size

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val inflatedView = LayoutInflater.from(parent.context).inflate(
				R.layout.recycler_saved_recipes,
				parent,
				false
		)
		return ViewHolder(inflatedView, object : ViewHolder.ItemClicked {
			override fun startTransform(caller: View?) {
				println("Made it! Adapterposition: $adapterPos")
				val bundle = Bundle()
				bundle.putInt("pos", adapterPos)
				val fragmentManager = Fragment().childFragmentManager
				val transaction = fragmentManager.beginTransaction()
				transaction.replace(R.id.root_layout, SavedRecipesEditFragment(), "savedRecipesEdit")
				transaction.addToBackStack(null)
				transaction.commit()
			}
		})
	}
	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.btnSR.text = recipeList.recipe[position].name
	/*	holder.btnSR.setOnClickListener {
			adapterPos = holder.adapterPosition
			println("Clicked it! AdapterPos: $adapterPos")
		} */
	}
}*/




/*
class SavedRecipesFragment: Fragment() {
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		return inflater.inflate(R.layout.saved_recipes, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val context: Context? = getContext()

		var button_dyn = arrayOfNulls<Button>(recipeList.recipe.size)
		val sv_layout = requireActivity().findViewById(R.id.layout_saved_recipes) as LinearLayout
		sv_layout.setOrientation(LinearLayout.VERTICAL)
		for (x in 0..recipeList.recipe.size - 1) {
			val row = LinearLayout(context)
			row.layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			)

			button_dyn[x] = Button(context)
			button_dyn[x]!!.layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			)
			button_dyn[x]!!.text = recipeList.recipe[x].name
			button_dyn[x]!!.setBackgroundColor(Color.parseColor("#000000"))
			button_dyn[x]!!.setTextColor(Color.parseColor("#ffffff"))
//			button_dyn[x]!!.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
//			button_dyn[x]!!.gravity = Gravity.CENTER
			button_dyn[x]!!.setPadding(0, 0, 0, 10)
			button_dyn[x]!!.setOnClickListener {
				sv_layout.removeAllViewsInLayout()
//				sv_layout.addView(button_dyn[x])

				val tv_saved_recipes = EditText(context)
				val btn_save_changes = Button(context)
				btn_save_changes.layoutParams = LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT
				)
				btn_save_changes.text = "Save"
				btn_save_changes.setBackgroundColor(Color.parseColor("#000000"))
				btn_save_changes.setTextColor(Color.parseColor("#ffffff"))
				btn_save_changes.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
				btn_save_changes.gravity = Gravity.CENTER
				btn_save_changes.setPadding(0, 0, 0, 10)
				btn_save_changes.setOnClickListener {
					recipeList.recipe[x].ingredients = tv_saved_recipes.text.toString()
					context!!.openFileOutput("savedRecipes.json", Context.MODE_PRIVATE).use {
						val json_to_file = Json.encodeToString(recipeList)
						it.write(json_to_file.toByteArray())
						// Snackbar.text("Saved!")
					}
					sv_layout.removeAllViewsInLayout()
					onActivityCreated(savedInstanceState)
				}
				val btn_discard = Button(context)
				btn_discard.layoutParams = LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT
				)
				btn_discard.text = "Discard"
				btn_discard.setBackgroundColor(Color.parseColor("#000000"))
				btn_discard.setTextColor(Color.parseColor("#ffffff"))
				btn_discard.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
				btn_discard.gravity = Gravity.CENTER
				btn_discard.setPadding(0, 0, 0, 10)
				btn_discard.setOnClickListener {
					sv_layout.removeAllViewsInLayout()
					onActivityCreated(savedInstanceState)
				}
				tv_saved_recipes.setText(recipeList.recipe[x].ingredients)
				sv_layout.addView(tv_saved_recipes)
				sv_layout.addView(btn_save_changes)
				sv_layout.addView(btn_discard)
			}
			row.addView(button_dyn[x])
			sv_layout.addView(row)
		}
	}
}
*/