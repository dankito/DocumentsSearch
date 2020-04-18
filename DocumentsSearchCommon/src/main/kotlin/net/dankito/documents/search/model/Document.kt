package net.dankito.documents.search.model

import java.util.*


open class Document constructor(
		id: String,
		url: String,
		val content: String,
		fileSize: Long,
		checksum: String,
		lastModified: Date,
		contentType: String? = null,
		title: String? = null,
		author: String? = null,
		length: Int? = null,
		language: String? = null,
		series: String? = null
) : DocumentMetadata(id, url, fileSize, checksum, lastModified, contentType, title, author,
		length, language, series) {


	constructor(content: String, metadata: DocumentMetadata) : this(metadata.id, metadata.url, content,
			metadata.size, metadata.checksum, metadata.lastModified,
			metadata.contentType, metadata.title, metadata.author, metadata.length,
			metadata.language, metadata.series)


	override fun toString(): String {
		return "${super.toString()}:\n$content"
	}

}