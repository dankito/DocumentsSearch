package net.dankito.documents.search.model

import java.util.Date


open class SearchResultDocumentFileSource(
	val filename: String,
	val url: String,
	val content_type: String,
	val extension: String,
	val filesize: Long,
	val created: Date,
	val last_modified: Date,
	val last_accessed: Date,
	val indexing_date: Date
) {

	internal constructor() : this("" ,"", "", "", 0, Date(0), Date(0), Date(0), Date(0)) // for object deserializers


	override fun toString(): String {
		return "$filename ($url)"
	}

}