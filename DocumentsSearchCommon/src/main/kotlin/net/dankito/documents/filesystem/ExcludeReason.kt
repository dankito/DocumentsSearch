package net.dankito.documents.filesystem


enum class ExcludeReason {

    ExcludePatternMatches,

    FileSmallerThanMinFileSize,

    FileLargerThanMaxFileSize,

    Error

}