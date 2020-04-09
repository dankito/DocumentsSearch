package net.dankito.documents.search.config


class DocumentFields {

	companion object {

		const val UrlFieldName = "url"
		const val FilenameFieldName = "filename"
		const val ContainingDirectoryFieldName = "containing_directory"

		const val ContentFieldName = "content"

		const val FileChecksumFieldName = "checksum"
		const val FileSizeFieldName = "file_size"

		const val CreatedAtFieldName = "created_at"
		const val LastAccessedFieldName = "last_accessed"
		const val LastModifiedFieldName = "last_modified"

		const val ContentTypeFieldName = "content_type"

		const val MetadataTitleFieldName = "metadata.title"
		const val MetadataAuthorFieldName = "metadata.author"
		const val MetadataSeriesFieldName = "metadata.series"

	}

}