package net.dankito.documents.language


interface ILanguageDetector {

    fun detectLanguage(text: String): DetectedLanguage

}