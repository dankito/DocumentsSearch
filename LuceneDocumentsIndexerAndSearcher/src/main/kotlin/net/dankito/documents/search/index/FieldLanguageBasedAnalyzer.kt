package net.dankito.documents.search.index

import net.dankito.documents.language.DetectedLanguage
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.DelegatingAnalyzerWrapper
import org.apache.lucene.analysis.ar.ArabicAnalyzer
import org.apache.lucene.analysis.bg.BulgarianAnalyzer
import org.apache.lucene.analysis.bn.BengaliAnalyzer
import org.apache.lucene.analysis.ca.CatalanAnalyzer
import org.apache.lucene.analysis.cjk.CJKAnalyzer
import org.apache.lucene.analysis.cz.CzechAnalyzer
import org.apache.lucene.analysis.da.DanishAnalyzer
import org.apache.lucene.analysis.de.GermanAnalyzer
import org.apache.lucene.analysis.el.GreekAnalyzer
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.analysis.es.SpanishAnalyzer
import org.apache.lucene.analysis.et.EstonianAnalyzer
import org.apache.lucene.analysis.eu.BasqueAnalyzer
import org.apache.lucene.analysis.fa.PersianAnalyzer
import org.apache.lucene.analysis.fi.FinnishAnalyzer
import org.apache.lucene.analysis.fr.FrenchAnalyzer
import org.apache.lucene.analysis.ga.IrishAnalyzer
import org.apache.lucene.analysis.gl.GalicianAnalyzer
import org.apache.lucene.analysis.hi.HindiAnalyzer
import org.apache.lucene.analysis.hu.HungarianAnalyzer
import org.apache.lucene.analysis.id.IndonesianAnalyzer
import org.apache.lucene.analysis.it.ItalianAnalyzer
import org.apache.lucene.analysis.lt.LithuanianAnalyzer
import org.apache.lucene.analysis.lv.LatvianAnalyzer
import org.apache.lucene.analysis.nl.DutchAnalyzer
import org.apache.lucene.analysis.no.NorwegianAnalyzer
import org.apache.lucene.analysis.pt.PortugueseAnalyzer
import org.apache.lucene.analysis.ro.RomanianAnalyzer
import org.apache.lucene.analysis.ru.RussianAnalyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.analysis.sv.SwedishAnalyzer
import org.apache.lucene.analysis.th.ThaiAnalyzer
import org.apache.lucene.analysis.tr.TurkishAnalyzer


open class FieldLanguageBasedAnalyzer(protected val defaultAnalyzer: Analyzer = StandardAnalyzer()) : DelegatingAnalyzerWrapper(PER_FIELD_REUSE_STRATEGY) {

    protected var analyzerForNextField: Analyzer = defaultAnalyzer

    protected val cachedAnalyzersForLanguage = mutableMapOf<DetectedLanguage, Analyzer>()


    override fun getWrappedAnalyzer(fieldName: String): Analyzer {
        return analyzerForNextField
    }


    open fun setLanguageOfNextField(language: DetectedLanguage) {
        val analyzerForLanguage = getAnalyzerForLanguage(language)

        if (analyzerForLanguage != null) {
            analyzerForNextField =analyzerForLanguage
        }
        else {
            analyzerForNextField = defaultAnalyzer
        }
    }

    protected open fun getAnalyzerForLanguage(language: DetectedLanguage): Analyzer? {
        cachedAnalyzersForLanguage[language]?.let {
            return it
        }

        val newAnalyzer = createAnalyzerForLanguage(language) ?: defaultAnalyzer

        cachedAnalyzersForLanguage[language] = newAnalyzer

        return newAnalyzer
    }

    protected open fun createAnalyzerForLanguage(language: DetectedLanguage): Analyzer? {
        return when (language) {

//            DetectedLanguage.af ->  Afrikaans
//                    DetectedLanguage.an -> Aragonese
            DetectedLanguage.ar -> ArabicAnalyzer()
//                    DetectedLanguage.ast -> Asturian
//                    DetectedLanguage.be -> Belarusian
//                    DetectedLanguage.br -> Breton
            DetectedLanguage.bg -> BulgarianAnalyzer()
            DetectedLanguage.bn -> BengaliAnalyzer()
            DetectedLanguage.ca -> CatalanAnalyzer()
            DetectedLanguage.cs -> CzechAnalyzer()
//                    DetectedLanguage.cy -> Welsh
            DetectedLanguage.da -> DanishAnalyzer()
            DetectedLanguage.de -> GermanAnalyzer()
            DetectedLanguage.el -> GreekAnalyzer()
            DetectedLanguage.en -> EnglishAnalyzer()
            DetectedLanguage.es -> SpanishAnalyzer()
            DetectedLanguage.et -> EstonianAnalyzer()
            DetectedLanguage.eu -> BasqueAnalyzer()
            DetectedLanguage.fa -> PersianAnalyzer()
            DetectedLanguage.fi -> FinnishAnalyzer()
            DetectedLanguage.fr -> FrenchAnalyzer()
            DetectedLanguage.ga -> IrishAnalyzer()
            DetectedLanguage.gl -> GalicianAnalyzer()
//                    DetectedLanguage.gu -> Gujarati // IndicNormalizer?
//                    DetectedLanguage.he -> Hebrew
            DetectedLanguage.hi -> HindiAnalyzer()
//                    DetectedLanguage.hr -> Croatian
//                    DetectedLanguage.ht -> Haitian
            DetectedLanguage.hu -> HungarianAnalyzer()
            DetectedLanguage.id -> IndonesianAnalyzer()
//                    DetectedLanguage.is -> Icelandic
            DetectedLanguage.it -> ItalianAnalyzer()
            DetectedLanguage.ja, DetectedLanguage.ko, DetectedLanguage.zh_cn, DetectedLanguage.zh_tw -> CJKAnalyzer()
//                    DetectedLanguage.km -> Khmer
//                    DetectedLanguage.kn -> Kannada // IndicNormalizer?
            DetectedLanguage.lt -> LithuanianAnalyzer()
            DetectedLanguage.lv -> LatvianAnalyzer()
//                    DetectedLanguage.mk -> Macedonian
//                    DetectedLanguage.ml -> Malayalam // IndicNormalizer?
//                    DetectedLanguage.mr -> Marathi
//                    DetectedLanguage.ms -> Malay
//                    DetectedLanguage.mt -> Maltese
//                    DetectedLanguage.ne -> Nepali
            DetectedLanguage.nl -> DutchAnalyzer()
            DetectedLanguage.no -> NorwegianAnalyzer()
//                    DetectedLanguage.oc -> Occitan
//                    DetectedLanguage.pa -> Punjabi
//                    DetectedLanguage.pl -> Polish
            DetectedLanguage.pt -> PortugueseAnalyzer() // what about BrazilianAnalyzer?
            DetectedLanguage.ro -> RomanianAnalyzer()
            DetectedLanguage.ru -> RussianAnalyzer()
//                    DetectedLanguage.sk -> Slovak
//                    DetectedLanguage.sl -> Slovene
//                    DetectedLanguage.so -> Somali
//                    DetectedLanguage.sq -> Albanian
//                    DetectedLanguage.sr -> SerbianAnalyzer() // SerbianNormalizationFilter
            DetectedLanguage.sv -> SwedishAnalyzer()
//                    DetectedLanguage.sw -> Swahili
//                    DetectedLanguage.ta -> Tamil // IndicNormalizer?
//                    DetectedLanguage.te -> Telugu // IndicNormalizer?
            DetectedLanguage.th -> ThaiAnalyzer()
//                    DetectedLanguage.tl -> Tagalog
            DetectedLanguage.tr -> TurkishAnalyzer()
//                    DetectedLanguage.uk -> Ukrainian
//                    DetectedLanguage.ur -> Urdu
//                    DetectedLanguage.vi -> Vietnamese
//                    DetectedLanguage.wa -> Walloon
//                    DetectedLanguage.yi -> Yiddish
            else -> null

        }
    }

}