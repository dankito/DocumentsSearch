package net.dankito.documents.search.model

import java.util.*


open class Document constructor(
		id: String,
		url: String,
		val content: String,
		fileSize: Long,
		checksum: String,
		createdAt: Date,
		lastModified: Date,
		lastAccessed: Date,
		contentType: String? = null,
		title: String? = null,
		author: String? = null,
		length: Int? = null,
		category: String? = null,
		language: String? = null,
		series: String? = null,
		keywords: List<String> = listOf()
) : DocumentMetadata(id, url, fileSize, checksum, createdAt, lastModified, lastAccessed, contentType, title, author,
		length, category, language, series, keywords) {


	constructor(content: String, metadata: DocumentMetadata) : this(metadata.id, metadata.url, content,
			metadata.fileSize, metadata.checksum, metadata.createdAt, metadata.lastModified, metadata.lastAccessed,
			metadata.contentType, metadata.title, metadata.author, metadata.length, metadata.category,
			metadata.language, metadata.series, metadata.keywords)


	override fun toString(): String {
		return "${super.toString()}:\n$content"
	}

}