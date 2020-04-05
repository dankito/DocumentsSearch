package net.dankito.documents.filesystem


enum class ExcludeReason {

    ExcludePatternMatches,

    ExcludedParentDirectory,

    FileSmallerThanMinFileSize,

    FileLargerThanMaxFileSize,

    ErrorOccurred

}