package net.dankito.documents.filesystem

import net.dankito.documents.search.model.IndexedDirectoryConfig
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean


open class FilesToIndexConfig(
        val indexDirectory: File,
        val includeFilesRegexPatterns: List<String> = listOf(),
        val excludeFilesRegexPatterns: List<String> = listOf(),
        val abortOnError: Boolean = false,
        val ignoreFilesLargerThanCountBytes: Long? = null,
        val ignoreFilesSmallerThanCountBytes: Long? = null,
        val stopTraversal: AtomicBoolean = AtomicBoolean(false)
) {

    companion object {
        private val log = LoggerFactory.getLogger(FilesToIndexConfig::class.java)
    }


    constructor(config: IndexedDirectoryConfig, stopTraversal: AtomicBoolean, abortOnError: Boolean = false)
            : this(config.directory, config.includeRules, config.excludeRules, abortOnError,
            config.ignoreFilesLargerThanCountBytes, config.ignoreFilesSmallerThanCountBytes, stopTraversal)


    protected val irregularIncludeRulesField = mutableListOf<String>()

    protected val irregularExcludeRulesField = mutableListOf<String>()


    val includeFilesRegEx: List<Regex> = includeFilesRegexPatterns.mapNotNull { createRegex(it, irregularIncludeRulesField) }

    val excludeFilesRegEx: List<Regex> = excludeFilesRegexPatterns.mapNotNull { createRegex(it, irregularExcludeRulesField) }

    val irregularIncludeRules: List<String>
        get() = ArrayList(irregularIncludeRulesField)

    val irregularExcludeRules: List<String>
        get() = ArrayList(irregularExcludeRulesField)


    private fun createRegex(pattern: String, irregularRules: MutableList<String>): Regex? {
        try {
            return Regex(pattern
                    .replace(".", "[.]")
                    .replace("*", ".*")
                    .replace("?", "."),
                    RegexOption.IGNORE_CASE)
        } catch (e: Exception) {
            log.error("Could not create regular expression from pattern '$pattern'", e)

            irregularRules.add(pattern)
        }

        return null
    }


    override fun toString(): String {
        return "$indexDirectory, excludes: $excludeFilesRegexPatterns"
    }

}