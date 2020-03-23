package net.dankito.documents.search.model

import java.io.File
import java.util.*


open class DocumentMetadata(
		val id: String,
		val url: String,
		val fileSize: Long,
		val createdAt: Date,
		val lastModified: Date,
		val lastAccessed: Date
) {

	val filename: String
		get() {
			try {
				return File(url).name
			} catch (e: Exception) { } // log?

			val lastIndexOfSlash = url.lastIndexOf('/')
			if (lastIndexOfSlash >= 0) {
				return url.substring(lastIndexOfSlash + 1)
			}

			val lastIndexOfBackslash = url.lastIndexOf('\\')
			if (lastIndexOfBackslash >= 0) {
				return url.substring(lastIndexOfBackslash + 1)
			}

			return ""
		}


	override fun toString(): String {
		return "$filename ($url)"
	}

}