package net.dankito.documents.language

import com.google.common.base.Optional
import com.optimaize.langdetect.LanguageDetector
import com.optimaize.langdetect.LanguageDetectorBuilder
import com.optimaize.langdetect.i18n.LdLocale
import com.optimaize.langdetect.ngram.NgramExtractors
import com.optimaize.langdetect.profiles.LanguageProfile
import com.optimaize.langdetect.profiles.LanguageProfileReader
import com.optimaize.langdetect.text.CommonTextObjectFactories
import com.optimaize.langdetect.text.TextObjectFactory
import org.slf4j.LoggerFactory


open class OptimaizeLanguageDetector : ILanguageDetector {

    companion object {
        private val log = LoggerFactory.getLogger(OptimaizeLanguageDetector::class.java)
    }


    protected val languageProfiles: List<LanguageProfile> = LanguageProfileReader().readAllBuiltIn()

    protected val languageDetector: LanguageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard())
            .withProfiles(languageProfiles)
            .build()

    protected val textObjectFactory: TextObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText()


    override fun detectLanguage(text: String): DetectedLanguage {
        if (text.isNotBlank()) {
            val textObject = textObjectFactory.forText(text)

            val languageOptional = languageDetector.detect(textObject)

            return mapDetectedLanguage(languageOptional)
        }

        return DetectedLanguage.NotRecognized
    }

    protected open fun mapDetectedLanguage(languageOptional: Optional<LdLocale>): DetectedLanguage {
        if (languageOptional.isPresent) {
            try {
                return DetectedLanguage.valueOf(languageOptional.get().language)
            } catch (e: Exception) {
                log.error("Could not map Optimaize language string '${languageOptional.get().language}' to DetectedLanguage", e)
            }
        }

        return DetectedLanguage.NotRecognized
    }

}