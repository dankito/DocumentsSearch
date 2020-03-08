package net.dankito.documents.contentextractor.model

import net.dankito.text.extraction.image.model.OcrLanguage
import java.io.File


open class FileContentExtractorSettings(
		val ocrLanguages: List<OcrLanguage> = listOf(OcrLanguage.English, OcrLanguage.German),
		val tesseractPath: File? = null,
		val tessdataDirectory: File? = null
) {

	override fun toString(): String {
		return "ocrLanguages=$ocrLanguages, tesseractPath=$tesseractPath, tessdataDirectory=$tessdataDirectory"
	}

}