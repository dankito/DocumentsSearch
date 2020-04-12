package net.dankito.documents.contentextractor

import net.dankito.documents.contentextractor.model.FileContentExtractorSettings
import net.dankito.text.extraction.ITextExtractorRegistry
import net.dankito.text.extraction.PlainTextFileTextExtractor
import net.dankito.text.extraction.TextExtractorRegistry
import net.dankito.text.extraction.TikaTextExtractor
import net.dankito.text.extraction.image.Tesseract4CommandlineImageTextExtractor
import net.dankito.text.extraction.image.model.OcrOutputType
import net.dankito.text.extraction.image.model.TesseractConfig
import net.dankito.text.extraction.model.ExtractionResultForExtractor
import net.dankito.text.extraction.model.PdfContentExtractorStrategy
import net.dankito.text.extraction.model.TikaSettings
import net.dankito.text.extraction.pdf.ImageOnlyPdfTextExtractor
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
				settings.tesseractPath, settings.tessdataDirectory, willMultipleTesseractInstancesRunInParallel = true))

		return TextExtractorRegistry(listOf(
				pdfToTextPdfTextExtractor(willMultipleInstancesRunInParallel = true),
				OpenPdfPdfTextExtractor(),
				tesseract4CommandlineImageTextExtractor,
				ImageOnlyPdfTextExtractor(tesseract4CommandlineImageTextExtractor, pdfimagesImagesFromPdfExtractor()),
				TikaTextExtractor(TikaSettings(false, PdfContentExtractorStrategy.NoOcr)),
				PlainTextFileTextExtractor()
		))
	}


	override fun extractContent(file: File): FileContentExtractionResult {
		return mapToFileContentExtractionResult(extractorRegistry.extractTextWithBestExtractorForFile(file))
	}

	override suspend fun extractContentSuspendable(file: File): FileContentExtractionResult {
		return mapToFileContentExtractionResult(extractorRegistry.extractTextWithBestExtractorForFileSuspendable(file))
	}

	protected open fun mapToFileContentExtractionResult(result: ExtractionResultForExtractor): FileContentExtractionResult {
		return FileContentExtractionResult(
				result.couldExtractText,
				result.error?.exception,
				result.text,
				result.contentType,
				result.metadata?.title,
				result.metadata?.author,
				result.metadata?.length,
				result.metadata?.category,
				result.metadata?.language,
				result.metadata?.series,
				result.metadata?.keywords ?: listOf()
		)
	}


}