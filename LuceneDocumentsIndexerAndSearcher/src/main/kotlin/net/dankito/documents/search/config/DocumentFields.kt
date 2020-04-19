package net.dankito.documents.search.config


class DocumentFields {

	companion object {

		const val IdFieldName = "id"
		const val UrlFieldName = "url"

		const val FilenameFieldName = "filename"
		const val ContainingDirectoryFieldName = "containing_directory"

		const val ContentFieldName = "content"

		const val ChecksumFieldName = "checksum"
		const val SizeFieldName = "size"

		const val LastModifiedFieldName = "last_modified"

		const val ContentTypeFieldName = "content_type"

		const val MetadataTitleFieldName = "metadata.title"
		const val MetadataAuthorFieldName = "metadata.author"
		const val MetadataSeriesFieldName = "metadata.series"

		const val RecipientFieldName = "recipient"

		const val AttachmentNameFieldName = "attachment.name"
		const val AttachmentSizeFieldName = "attachment.size"
		const val AttachmentContentTypeFieldName = "attachment.content_type"
		const val AttachmentContentFieldName = "attachment.content"

	}

}