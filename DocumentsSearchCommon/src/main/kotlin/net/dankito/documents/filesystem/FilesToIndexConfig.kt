package net.dankito.documents.filesystem

import java.io.File


open class FilesToIndexConfig(
        val indexDirectory: File,
        val includeFilesRegexPatterns: List<String> = listOf(),
        val excludeFilesRegexPatterns: List<String> = listOf(),
        val abortOnError: Boolean = false,
        val ignoreFilesLargerThanCountBytes: Long? = null,
        val ignoreFilesSmallerThanCountBytes: Long? = null
) {

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