package net.dankito.documents.search.model

import org.slf4j.LoggerFactory
import java.io.File
import java.util.*


open class DocumentMetadata(
		val id: String,
		val url: String,
		val fileSize: Long,
		val createdAt: Date,
		val lastModified: Date,
		val lastAccessed: Date,
		val mimeType: String? = null,
		val title: String? = null,
		val author: String? = null,
		val length: Int? = null,
		val category: String? = null,
		val language: String? = null,
		val series: String? = null,
		val keywords: List<String> = listOf()
) {

	companion object {
		private val log = LoggerFactory.getLogger(DocumentMetadata::class.java)
	}


	val containingDirectory: String?
		get() {
			try {
				File(url).parentFile?.let { parent ->
					return parent.name
				}
			} catch (e: Exception) {
				log.error("Could not extract containing directory from url '$url'", e)
			}

			return null
		}

	val filename: String
		get() {
			try {
				return File(url).name
			} catch (e: Exception) {
				log.error("Could not extract filename from url '$url'", e)
			}

			val lastIndexOfSlash = url.lastIndexOf('/')
			if (lastIndexOfSlash >= 0) {
				return url.substring(lastIndexOfSlash + 1)
			}

			val lastIndexOfBackslash = url.lastIndexOf('\\')
			if (lastIndexOfBackslash >= 0) {
				return url.substring(lastIndexOfBackslash + 1)
			}

			return url
		}

	val fileExtension: String?
		get() {
			try {
				return File(url).extension
			} catch (e: Exception) {
				log.error("Could not extract file extension from url '$url'", e)
			}

			val lastIndexOfDot = url.lastIndexOf('.')
			if (lastIndexOfDot >= 0) {
				return url.substring(lastIndexOfDot + 1)
			}

			return null
		}


	override fun toString(): String {
		return "$filename ($url)"
	}

}