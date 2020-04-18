package net.dankito.documents.search.model

import org.slf4j.LoggerFactory
import java.io.File
import java.util.*


open class DocumentMetadata(
		val id: String, // TODO: hash url?
		/**
		 * For files the absolute path of file, for emails <server_address>_<username>_<message_id>
		 */
		val url: String,
		/**
		 * The size of the file or email (for emails: only their approximate not their exact size).
		 */
		val size: Long,
		val checksum: String,
		val lastModified: Date, // Mails: Received won't work
		val contentType: String? = null,
		/**
		 * E.g. the title of a document or a song or the subject of an email
		 */
		val title: String? = null,
		/**
		 * E.g. the author of a book, the artist of a song or the sender of an email.
		 */
		val author: String? = null,
		/**
		 * E.g. how many pages a document or how long an audio / video (in milliseconds) is.
		 */
		val length: Int? = null,
		var language: String? = null,
		/**
		 * E.g. the series of a book or the album of a song.
		 */
		val series: String? = null
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