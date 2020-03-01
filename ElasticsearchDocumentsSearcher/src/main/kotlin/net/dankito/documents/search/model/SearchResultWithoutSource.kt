package net.dankito.documents.search.model


open class SearchResultWithoutSource(
		val index: String,
		val type: String,
		val id: String,
		val score: Float
) {


	internal constructor() : this("", "", "", Float.NaN) // for object deserializers


	override fun toString(): String {
		return "$score $id"
	}

}