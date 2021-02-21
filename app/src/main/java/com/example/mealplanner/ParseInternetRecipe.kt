package com.example.mealplanner

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.net.URLConnection

const val ALLRECIPES_URL = "allrecipes.com"
const val ALLRECIPES_NAME = "<meta property=\"og:title\" content=\""
const val ALLRECIPES_ING = "\"recipeIngredient\""
const val ALLRECIPES_INS = "\"recipeInstructions\""
const val ALLRECIPES_INS2 = "\"text\": \""

const val SITE_NAME = "og:title"
const val SITE_ING = ""
const val SITE_INS = ""
const val SITE_INS2 = ""

const val YOAST_FIND = "yoast-schema-graph"
const val YOAST_ING_START = "\"recipeIngredient\":[\""
const val YOAST_ING_END = "\"]"
const val YOAST_INS_START = "\"recipeInstructions\":["
const val YOAST_INS_END = "]"
const val YOAST_INS2_START = "text\":\""
const val YOAST_INS2_END = "\","

fun getRecipeFromHTML(url: String): RecipeX {
	println(url)
	val connection: URLConnection = URL(url).openConnection()
	connection.connectTimeout = 5000
	connection.readTimeout = 5000
	connection.connect()

	val input: InputStream = connection.getInputStream()
	val reader = BufferedReader(InputStreamReader(input))
	var line: String
	var nextLine: String

	var name = ""
	var ingredients = ""
	var instructions = ""

	var nameFlag = true
	var ingFlag = true
	var insFlag = true
	var usesYoast = false


	if (url.contains(ALLRECIPES_URL)) {
		while (reader.readLine().also { line = it } != null) {
			if (!nameFlag && !ingFlag && !insFlag) {
				break
			}
			if (nameFlag && line.contains(ALLRECIPES_NAME, true)) {
				name = line.substringAfter(ALLRECIPES_NAME).substringBefore("\">")
				println(name)
				nameFlag = false
			}
			if (ingFlag && line.contains(ALLRECIPES_ING, true)) {
				nextLine = reader.readLine()
				while (!nextLine.endsWith("],")) {
					ingredients += nextLine.substringAfter("\"").substringBefore("\"")
					ingredients += "\n"
					nextLine = reader.readLine()
				}
				ingFlag = false
			}
			if (insFlag && line.contains(ALLRECIPES_INS, true)) {
				nextLine = reader.readLine()
				while (!nextLine.endsWith("],")) {
					if (nextLine.contains(ALLRECIPES_INS2)) {
						instructions += nextLine.substringAfter(ALLRECIPES_INS2).substringBefore("\\")
						instructions += "\n\n"
					}
					nextLine = reader.readLine()
				}
				insFlag = false
			}
		}
		input.close()
	} else {
		while (reader.readLine().also { line = it } != null) {
			if (ingFlag) {
				if (line.contains(YOAST_FIND))
					usesYoast = true
			}
			if (nameFlag && line.contains(SITE_NAME, true)) {
				name = line.substringAfter("content=")
				if (name[0] == '"') {
					name = name.substringAfter("\"").substringBefore("\"")
				} else if (name[0] == '\'') {
					name = name.substringAfter("\'").substringBefore("\'")
				}

				when {
					name.contains(".") -> name = name.substringBefore(".")
					name.contains("|") -> name = name.substringBefore("|")
					name.contains("-") -> name = name.substringBefore("-")
				}
				nameFlag = false
			}

			if (usesYoast) {
				if (ingFlag && line.contains(YOAST_ING_START, true)) {
					ingredients = line.substringAfter(YOAST_ING_START).substringBefore(YOAST_ING_END)
					ingredients = ingredients.replace("\",\"", "\n")
					ingFlag = false
				}
				if (insFlag && line.contains(YOAST_INS_START, true)) {
					line = line.substringAfter(YOAST_INS_START).substringBefore(YOAST_INS_END)

					while (line.contains(YOAST_INS2_START)) {
						instructions += line.substringAfter(YOAST_INS2_START).substringBefore(YOAST_INS2_END)
						instructions += "\n\n"
						line = line.substringAfter(YOAST_INS2_END)
					}
					instructions = instructions.replace("&#039;", "'")
					insFlag = false
				}
			}
			if (!nameFlag && !ingFlag && !insFlag) {
				break
			}
		}
	}



	return RecipeX(name, ingredients, instructions, 0, true)
}