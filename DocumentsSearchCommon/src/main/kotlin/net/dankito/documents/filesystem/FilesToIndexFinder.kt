package net.dankito.documents.filesystem

import net.dankito.utils.filesystem.FilesystemWalker
import net.dankito.utils.filesystem.IFilesystemWalker
import net.dankito.utils.filesystem.VisitedFile
import java.io.File
import java.nio.file.FileVisitResult
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes


open class FilesToIndexFinder(protected val filesystemWalker: IFilesystemWalker = FilesystemWalker()) {

    open fun findFilesToIndex(config: FilesToIndexConfig): Pair<List<Path>, List<ExcludedFile>> {
        val includedFiles = mutableListOf<Path>()
        val excludedFiles = mutableListOf<ExcludedFile>()

        findFilesToIndex(config, { excludedFiles.add(it) }, { includedFiles.add(it.path) })

        return Pair(includedFiles, excludedFiles)
    }

    open fun findFilesToIndex(config: FilesToIndexConfig, excludedFiles: ((ExcludedFile) -> Unit)? = null, filesToIndex: (IncludedFile) -> Unit) {
        filesystemWalker.detailedWalk(config.indexDirectory.toPath(), config.abortOnError,
                { directory -> checkIfDirectoryShouldBeIgnored(directory, config, excludedFiles, filesToIndex) }, null) { visitedFile ->

            if (config.stopTraversal.get()) {
                return@detailedWalk FileVisitResult.TERMINATE
            }

            checkIfFileShouldBeIncludedOrExcluded(visitedFile, config, excludedFiles, filesToIndex)

            FileVisitResult.CONTINUE
        }
    }

    protected open fun checkIfFileShouldBeIncludedOrExcluded(visitedFile: VisitedFile, config: FilesToIndexConfig, excludedFiles: ((ExcludedFile) -> Unit)?, filesToIndex: (IncludedFile) -> Unit) {
        visitedFile.path?.let { file ->
            if (visitedFile.visitSuccessful == false) {
                excludedFiles?.invoke(ExcludedFile(file, ExcludeReason.ErrorOccurred))
            }
            else if (includeFile(file, config)) { // include overrules all other settings
                filesToIndex(IncludedFile(file, visitedFile))
            }
            else if (excludeFile(file, config)) {
                excludedFiles?.invoke(ExcludedFile(file, ExcludeReason.ExcludePatternMatches))
            }
            else if (fileIsLargerThanMaxFileSize(visitedFile.attributes, config)) {
                excludedFiles?.invoke(ExcludedFile(file, ExcludeReason.FileLargerThanMaxFileSize))
            }
            else if (fileIsSmallerThanMinFileSize(visitedFile.attributes, config)) {
                excludedFiles?.invoke(ExcludedFile(file, ExcludeReason.FileSmallerThanMinFileSize))
            }
            else {
                filesToIndex(IncludedFile(file, visitedFile))
            }
        }
    }

    protected open fun checkIfDirectoryShouldBeIgnored(directory: Path?, config: FilesToIndexConfig, ignoredFiles: ((ExcludedFile) -> Unit)?, filesToIndex: (IncludedFile) -> Unit): FileVisitResult {
        if (config.stopTraversal.get()) {
            return FileVisitResult.TERMINATE
        }

        directory?.let {
            if (excludeAppliesButIncludeDoesNot(directory, config)) {
                ignoredFiles?.invoke(ExcludedFile(directory, ExcludeReason.ExcludePatternMatches))

                if (config.includeFilesRegEx.isNotEmpty()) { // check if include rules include files of subtree
                    filesystemWalker.listFiles(directory).forEach { subFileInExcludedDirectory ->
                        if (includeFile(subFileInExcludedDirectory, config)) { // but if an include rule matches specific file, include it anyway
                            filesToIndex(IncludedFile(subFileInExcludedDirectory))
                        }
                        else {
                            ignoredFiles?.invoke(ExcludedFile(subFileInExcludedDirectory, ExcludeReason.ExcludedParentDirectory))
                        }
                    }
                }
                else if (ignoredFiles != null) {
                   filesystemWalker.listFiles(directory).forEach { subFileInExcludedDirectory ->
                       ignoredFiles(ExcludedFile(subFileInExcludedDirectory, ExcludeReason.ExcludedParentDirectory))
                   }
                }

                return FileVisitResult.SKIP_SUBTREE
            }
        }

        return FileVisitResult.CONTINUE
    }


    protected open fun excludeAppliesButIncludeDoesNot(path: Path, config: FilesToIndexConfig): Boolean {
        return includeFile(path, config) == false && excludeFile(path, config) // include overrules exclude
    }

    protected open fun includeFile(path: Path, config: FilesToIndexConfig): Boolean {
        return doesAnyRegExMatch(config.indexDirectory, path, config.includeFilesRegEx)
    }

    protected open fun excludeFile(path: Path, config: FilesToIndexConfig): Boolean {
        return doesAnyRegExMatch(config.indexDirectory, path, config.excludeFilesRegEx)
    }

    protected open fun doesAnyRegExMatch(indexDirectory: File, path: Path, regularExpressions: List<Regex>): Boolean {
        if (regularExpressions.isNotEmpty()) {
            val file = path.toFile()
            val absolutePath = file.absolutePath
            val relativePath = file.relativeTo(indexDirectory).path

            regularExpressions.forEach { regex ->
                if (regex.matches(relativePath) || regex.matches(absolutePath)) {
                    return true
                }
            }
        }

        return false
    }

    protected open fun fileIsLargerThanMaxFileSize(fileAttributes: BasicFileAttributes?, config: FilesToIndexConfig): Boolean {
        fileAttributes?.let {
            config.ignoreFilesLargerThanCountBytes?.let { maxFileSize ->
                if (fileAttributes.size() > maxFileSize) {
                    return true
                }
            }
        }

        return false
    }

    protected open fun fileIsSmallerThanMinFileSize(fileAttributes: BasicFileAttributes?, config: FilesToIndexConfig): Boolean {
        fileAttributes?.let {
            config.ignoreFilesSmallerThanCountBytes?.let { minFileSize ->
                if (fileAttributes.size() < minFileSize) {
                    return true
                }
            }
        }

        return false
    }

}