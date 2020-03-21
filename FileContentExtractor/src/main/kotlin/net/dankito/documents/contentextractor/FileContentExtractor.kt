package net.dankito.documents.contentextractor

import net.dankito.documents.contentextractor.model.FileContentExtractorSettings
import net.dankito.text.extraction.ITextExtractorRegistry
import net.dankito.text.extraction.TextExtractorRegistry
import net.dankito.text.extraction.TikaTextExtractor
import net.dankito.text.extraction.image.Tesseract4CommandlineImageTextExtractor
import net.dankito.text.extraction.image.model.OcrOutputType
import net.dankito.text.extraction.image.model.TesseractConfig
import net.dankito.text.extraction.model.PdfContentExtractorStrategy
import net.dankito.text.extraction.model.TikaSettings
import net.dankito.text.extraction.pdf.ImageBasedPdfTextExtractor
import net.dankito.text.extraction.pdf.OpenPdfPdfTextExtractor
import net.dankito.text.extraction.pdf.pdfToTextPdfTextExtractor
import net.dankito.text.extraction.pdf.pdfimagesImagesFromPdfExtractor
import java.io.File


open class FileContentExtractor(protected val settings: FileContentExtractorSettings) : IFileContentExtractor {

	protected val extractorRegistry: ITextExtractorRegistry


	init {
		extractorRegistry = initTextExtractorRegistry()
	}


	protected open fun initTextExtractorRegistry(): ITextExtractorRegistry {
		val tesseract4CommandlineImageTextExtractor = Tesseract4CommandlineImageTextExtractor(TesseractConfig(settings.ocrLanguages, OcrOutputType.Text,
				settings.tesseractPath, settings.tessdataDirectory))

		return TextExtractorRegistry(listOf(
				pdfToTextPdfTextExtractor(),
				OpenPdfPdfTextExtractor(),
				tesseract4CommandlineImageTextExtractor,
				ImageBasedPdfTextExtractor(tesseract4CommandlineImageTextExtractor, pdfimagesImagesFromPdfExtractor()),
				TikaTextExtractor(TikaSettings(PdfContentExtractorStrategy.NoOcr))
		))
	}


	override fun extractContent(file: File): String? {
		return extractorRegistry.extractTextWithBestExtractorForFile(file).text
	}

}