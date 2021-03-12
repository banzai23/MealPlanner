package com.example.mealplanner

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.MalformedURLException
import java.net.URL
import java.net.UnknownHostException
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext

const val ALLRECIPES_URL = "allrecipes.com"
const val ALLRECIPES_NAME = "<meta property=\"og:title\" content=\""
const val ALLRECIPES_ING = "\"recipeIngredient\""
const val ALLRECIPES_ING_END = "],"
const val ALLRECIPES_INS = "\"recipeInstructions\""
const val ALLRECIPES_INS2 = "\"text\": \""

const val SITE_NAME = "\"og:title\" content=\""

const val YOAST_FIND = "\"recipeIngredient\":[\""
const val YOAST_ING_START = "\"recipeIngredient\":[\""
const val YOAST_ING_END = "\"]"
const val YOAST_INS_START = "\"recipeInstructions\":["
const val YOAST_INS_END = "]"
const val YOAST_INS2_START = "text\":\""
const val YOAST_INS2_END = "\",\""

const val H3ID_FIND = "h3 id=\"ingredients\""
const val H3ID_ING_START = "itemprop=\"ingredients\">"
const val H3ID_ING_END = "</ul>"
const val H3ID_ING2_START = ">"
const val H3ID_ING2_END = "<"
const val H3ID_INS_START = "itemprop=\"recipeInstructions\""
const val H3ID_INS_END = "</ol>"
const val H3ID_INS2_START = "<li>"
const val H3ID_INS2_END = "</li>"

const val SRP_FIND = ".simple-recipe-pro"
const val SRP_ING_START = "\"recipeIngredient\": ["
const val SRP_ING_END = "],"
const val SRP_ING2_START = "\""
const val SRP_ING2_END = "\""
const val SRP_INS_START = "\"recipeInstructions\":"
const val SRP_INS_END = "</ol>\""
const val SRP_INS2_START = "<li>"
const val SRP_INS2_END = "</li>"

class RecipeAndReturn(val name: String, val ingredients: String, val instructions: String, val returnCode: Int)

fun getRecipeFromURL(url: String): RecipeAndReturn {
	var urlPassed = false
	lateinit var reader: BufferedReader
	lateinit var input: InputStream
	while (!urlPassed) {
		var testUrl = url.trimEnd()

		if (!testUrl.startsWith("https://", true))
			testUrl = "https://$testUrl"
		var testUrl2 = testUrl.substringAfter("//").substringBefore("/")
		testUrl2 = testUrl2.substringAfterLast(".")
		if (testUrl2.length <= 1 || testUrl2.length >= 5) {
			println("Invalid URL; terminating program")
			return RecipeAndReturn("", "", "", 1)   // invalid URL ending address
		}

		val sslContext: SSLContext = SSLContext.getInstance("TLSv1.2")
		sslContext.init(null, null, null)
		val socketFactory = sslContext.socketFactory
		val connection: HttpsURLConnection
		try {
			connection = URL(testUrl).openConnection() as HttpsURLConnection
		} catch (e: MalformedURLException) {
			print("MalformedURL: $e")
			return RecipeAndReturn("", "", "", 1)
		} catch (e: IOException) {
			print("IO error: $e")
			return RecipeAndReturn("", "", "", 2)
		}
		connection.sslSocketFactory = socketFactory
		connection.setRequestProperty(
				"User-Agent",
				"Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:85.0) Gecko/20100101 Firefox/85.0")
		connection.connectTimeout = 5000
		connection.readTimeout = 5000
		try {
			connection.connect()
		} catch (e: UnknownHostException) {
			println("Error: $e")
			return RecipeAndReturn("", "", "", 3)
		}

		try {
			input = connection.inputStream
		} catch (e: IOException) {
			println("Error: $e")
			println("Could not get recipe from URL")
			return RecipeAndReturn("", "", "", 2)
		}
		reader = BufferedReader(InputStreamReader(input))
		urlPassed = true
	}
	var line: String? = reader.readLine()

	var name = ""
	var ingredients = ""
	var instructions = ""

	var nameFlag = true
	var ingFlag = true
	var insFlag = true
	var usesYoast = false
	var usesH3ID = false
	var usesSRP = false

	if (url.contains(ALLRECIPES_URL)) {
		while (line != null) {
			if (!nameFlag && !ingFlag && !insFlag) {
				break
			}
			if (nameFlag && line.contains(ALLRECIPES_NAME, true)) {
				name = line.substringAfter(ALLRECIPES_NAME).substringBefore("\">")
				nameFlag = false
			}
			if (ingFlag && line.contains(ALLRECIPES_ING, true)) {
				line = reader.readLine()
				while (!line!!.endsWith(ALLRECIPES_ING_END)) {
					ingredients += line.substringAfter("\"").substringBefore("\"") + "\n"
					line = reader.readLine()
				}
				ingFlag = false
			}
			if (insFlag && line.contains(ALLRECIPES_INS, true)) {
				line = reader.readLine()
				while (!line!!.endsWith("],")) {
					if (line.contains(ALLRECIPES_INS2))
						instructions += line.substringAfter(ALLRECIPES_INS2).substringBefore("\\") + "\n\n"
					line = reader.readLine()
				}
				insFlag = false
			}
			line = reader.readLine()
		}
		input.close()
	} else {
		while (line != null) {
			if (ingFlag) {
				if (line.contains(YOAST_FIND) && !usesSRP)
					usesYoast = true
				else if (line.contains(H3ID_FIND))
					usesH3ID = true
				else if (line.startsWith(SRP_FIND))
					usesSRP = true
			}
			if (nameFlag && line.contains(SITE_NAME, true)) {
				name = line.substringAfter(SITE_NAME).substringBefore("\"")
				if (name[0] == '"') {
					name = name.substringAfter("\"").substringBefore("\"")
				} else if (name[0] == '\'') {
					name = name.substringAfter("\'").substringBefore("\'")
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

					while (line!!.contains(YOAST_INS2_START)) {
						val instLine = line.substringAfter(YOAST_INS2_START).substringBefore(YOAST_INS2_END)
						if (instLine != instructions.trim().substringAfterLast("\n")) {
							instructions += instLine + "\n\n"
						}
						line = line.substringAfter(YOAST_INS2_END)
					}
					insFlag = false
				}
			} else if (usesH3ID) {
				if (ingFlag && line.contains(H3ID_ING_START, true)) {
					line = reader.readLine()
					while (!line!!.contains(H3ID_ING_END)) {
						ingredients += line.substringAfter(H3ID_ING2_START).substringBefore(H3ID_ING2_END) + "\n"
						line = reader.readLine()
					}
					ingFlag = false
				}
				if (insFlag && line.contains(H3ID_INS_START, true)) {
					line = reader.readLine()
					while (!line!!.contains(H3ID_INS_END)) {
						instructions += line.substringAfter(H3ID_INS2_START).substringBefore(H3ID_INS2_END) + "\n\n"
						line = reader.readLine()
					}
					insFlag = false
				}
			} else if (usesSRP) {
				if (ingFlag && line.contains(SRP_ING_START, true)) {
					line = reader.readLine()
					while (!line!!.contains(SRP_ING_END)) {
						ingredients += line.substringAfter(SRP_ING2_START).substringBefore(SRP_ING2_END) + "\n"
						line = reader.readLine()
					}
					ingFlag = false
				}
				if (insFlag && line.contains(SRP_INS_START, true)) {
					line = line.substringAfter(SRP_INS_START).substringBefore(SRP_INS_END)

					while (line!!.contains(SRP_INS2_START)) {
						val instLine = line.substringAfter(SRP_INS2_START).substringBefore(SRP_INS2_END)
						if (instLine != instructions.trim().substringAfterLast("\n")) {
							instructions += instLine + "\n\n"
						}
						line = line.substringAfter(SRP_INS2_END)
					}
					insFlag = false
				}
			}
			if (!nameFlag && !ingFlag && !insFlag)
				break
			line = reader.readLine()
		}
		input.close()
	}
	// sort and separate name
	when {
		name.contains(".") -> name = name.substringBefore(".")
		name.contains("|") -> name = name.substringBefore("|")
		name.contains("-") -> name = name.substringBeforeLast("-")
		name.contains("(") -> name = name.substringBeforeLast("(")
		name.contains("to cook", true) -> name = name.substringAfter("Cook").trim()
	}
	name = name.replace("&amp;", "&")

	if (ingredients == "" && instructions == "")
		return RecipeAndReturn("", "", "", 99)

	val regex = "<[^>]*>".toRegex()
	instructions = instructions.replace(regex, "")
	ingredients = ingredients.replace(regex, "")
	// replace characters for ingredients
	for (x in Replacements.item.indices step 2) {
		ingredients = ingredients.replace(Replacements.item[x], Replacements.item[x+1])
		instructions = instructions.replace(Replacements.item[x], Replacements.item[x+1])
	}
	ingredients = ingredients.trimEnd()
	instructions = instructions.trimEnd()

	if (name.length > MAX_RECIPE_TITLE_SIZE)
		name = name.dropLast(name.length - MAX_RECIPE_TITLE_SIZE)

	return RecipeAndReturn(name, ingredients, instructions, 0)
}
object Replacements {
	val item = listOf(
		"\\u2014", "-",
		"\\u2153", "⅓",
		"\\u2155", "⅕",
		"\\u00bc", "¼",
		"\\u00bd", "½",
		"\\u00be", "¾",
		"\\u00ba", "º",
		"\\u00b0", "º",
		"\\u2109", "º",
		"\\u00a0", "",
		"\\u00ae", "",
		"&amp;", "&",
		"&#039;", "'",
		"&#8220;", "\"",
		"&#8221;", "\"",
		"\\u00f1", "ñ",
		"&#8211;", "-",
		"\\/", "/",
		"\\r", "",
		"\\n", "",
		"&#215;", "×",
		"&#x27;", "'",
		"&quot;", "\"",
		"&#8217;", "'",
		"&nbsp;", "",
		"\\u00a0", "",
		"\\u2019", "'",
		"\\u00e9", "é",
		"\\u201d", "\"",
		"\\\"", "\"")
}