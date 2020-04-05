package net.dankito.documents.filesystem

import net.dankito.documents.search.model.IndexConfig
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean


open class FilesToIndexConfig constructor(
        val indexDirectory: File,
        val includeFilesRegexPatterns: List<String> = listOf(),
        val excludeFilesRegexPatterns: List<String> = listOf(),
        val abortOnError: Boolean = false,
        val ignoreFilesLargerThanCountBytes: Long? = null,
        val ignoreFilesSmallerThanCountBytes: Long? = null,
        val stopTraversal: AtomicBoolean = AtomicBoolean(false)
) {

    constructor(indexDirectory: File, config: IndexConfig, stopTraversal: AtomicBoolean, abortOnError: Boolean = false)
            : this(indexDirectory, config.includeRules, config.excludeRules, abortOnError,
            config.ignoreFilesLargerThanCountBytes, config.ignoreFilesSmallerThanCountBytes, stopTraversal)


    val includeFilesRegEx: List<Regex> = includeFilesRegexPatterns.map { createRegex(it) }

    val excludeFilesRegEx: List<Regex> = excludeFilesRegexPatterns.map { createRegex(it) }


    private fun createRegex(pattern: String): Regex {
        return Regex(pattern
                .replace(".", "[.]")
                .replace("*", ".*")
                .replace("?", "."),
                RegexOption.IGNORE_CASE)
    }


    override fun toString(): String {
        return "$indexDirectory, excludes: $excludeFilesRegexPatterns"
    }

}