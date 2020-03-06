package net.dankito.documents.search.model

import java.util.Date


open class Document(
		val id: String,
		val filename: String,
		val url: String,
		val content: String,
		val fileSize: Long,
		val createdAt: Date,
		val lastModified: Date,
		val lastAccessed: Date
) {


	override fun toString(): String {
		return "$filename ($url):\n$content"
	}

}