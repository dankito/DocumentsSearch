package net.dankito.documents.search.model

import java.util.*


open class Document(
		id: String,
		url: String,
		val content: String,
		fileSize: Long,
		checksum: String,
		lastModified: Date,
		contentType: String? = null,
		relativeContainingDirectoryPathInIndexPart: String? = null,
		title: String? = null,
		author: String? = null,
		length: Int? = null,
		language: String? = null,
		series: String? = null,
		/**
		 * The recipients of an email.
		 */
		val recipients: List<String>? = null,
		/**
		 * An email's attachments
		 */
		val attachments: List<Attachment>? = null
) : DocumentMetadata(id, url, fileSize, checksum, lastModified, contentType, relativeContainingDirectoryPathInIndexPart,
		title, author, length, language, series) {


	constructor(content: String, metadata: DocumentMetadata, recipients: List<String>? = null, attachments: List<Attachment>? = null) :
			this(metadata.id, metadata.url, content,
				metadata.size, metadata.checksum, metadata.lastModified,
				metadata.contentType, metadata.relativeContainingDirectoryPathInIndexPart,
				metadata.title, metadata.author, metadata.length,
				metadata.language, metadata.series,
				recipients, attachments)


	override fun toString(): String {
		return "${super.toString()}:\n$content"
	}

}