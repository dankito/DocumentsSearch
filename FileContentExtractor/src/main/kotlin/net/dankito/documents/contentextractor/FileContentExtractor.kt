package net.dankito.documents.contentextractor

import net.dankito.documents.contentextractor.model.FileContentExtractorSettings
import net.dankito.text.extraction.ITextExtractorRegistry
import net.dankito.text.extraction.TextExtractorRegistry
import net.dankito.text.extraction.TikaTextExtractor
import net.dankito.text.extraction.image.Tesseract4ImageTextExtractor
import net.dankito.text.extraction.image.model.OcrOutputType
import net.dankito.text.extraction.image.model.TesseractConfig
import net.dankito.text.extraction.model.PdfContentExtractorStrategy
import net.dankito.text.extraction.model.TikaSettings
import net.dankito.text.extraction.pdf.OpenPdfPdfTextExtractor
import net.dankito.text.extraction.pdf.pdfToTextPdfTextExtractor
import java.io.File


open class FileContentExtractor(protected val settings: FileContentExtractorSettings) : IFileContentExtractor {

	protected val extractorRegistry: ITextExtractorRegistry


	init {
		extractorRegistry = initTextExtractorRegistry()
	}


	protected open fun initTextExtractorRegistry(): ITextExtractorRegistry {
		return TextExtractorRegistry(listOf(
				pdfToTextPdfTextExtractor(),
				OpenPdfPdfTextExtractor(),
				Tesseract4ImageTextExtractor(TesseractConfig(settings.ocrLanguages, OcrOutputType.Text,
						settings.tesseractPath, settings.tessdataDirectory)),
				TikaTextExtractor(TikaSettings(PdfContentExtractorStrategy.OcrAndText, settings.ocrLanguages,
						OcrOutputType.Text, settings.tesseractPath, settings.tessdataDirectory))
		))
	}


	override fun extractContent(file: File): String? {
		return extractorRegistry.extractTextWithBestExtractorForFile(file).text
	}

}