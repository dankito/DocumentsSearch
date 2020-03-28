package net.dankito.documents.contentextractor

import java.io.File


interface IFileContentExtractor {

	fun extractContent(file: File): FileContentExtractionResult

    suspend fun extractContentSuspendable(file: File): FileContentExtractionResult

}