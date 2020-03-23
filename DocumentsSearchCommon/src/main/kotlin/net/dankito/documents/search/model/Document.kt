package net.dankito.documents.search.model

import java.util.*


open class Document(
		id: String,
		url: String,
		val content: String,
		fileSize: Long,
		createdAt: Date,
		lastModified: Date,
		lastAccessed: Date
) : DocumentMetadata(id, url, fileSize, createdAt, lastModified, lastAccessed) {


	override fun toString(): String {
		return "${super.toString()}:\n$content"
	}

}