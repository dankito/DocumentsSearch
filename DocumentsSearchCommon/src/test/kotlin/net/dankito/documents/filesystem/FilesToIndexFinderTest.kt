package net.dankito.documents.filesystem

import net.dankito.utils.io.FileUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path


class FilesToIndexFinderTest {

    private val fileUtils = FileUtils()

    private val indexDirectory = fileUtils.createDirectoryInTempDir("FilesToIndexFinderTest")


    private val underTest = FilesToIndexFinder()


    @AfterEach
    fun tearDown() {
        indexDirectory.deleteRecursively()
    }


    @Test
    fun excludeMatchesRelativePath() {

        // given
        val testFilename = "test.txt"
        createTestFile(testFilename)

        // when
        val result = underTest.findFilesToIndex(FilesToIndexConfig(indexDirectory, excludeFilesRegexPatterns = listOf(testFilename)))

        // then
        assertFileIsExcluded(result, testFilename)
    }

    @Test
    fun excludeMatchesRelativePath_OverriddenByInclude() {

        // given
        val testFilename = "test.txt"
        createTestFile(testFilename)

        // when
        val result = underTest.findFilesToIndex(FilesToIndexConfig(indexDirectory, listOf("*.txt"), listOf(testFilename)))

        // then
        assertFileIsIncluded(result, testFilename)
    }


    @Test
    fun parentDirectoryIsExcluded() {

        // given
        val parentDir = "documents"
        ensureDirectoriesInIndexDirectoryExist(parentDir)

        // when
        val result = underTest.findFilesToIndex(FilesToIndexConfig(indexDirectory, excludeFilesRegexPatterns = listOf(parentDir)))

        // then
        assertFileIsExcluded(result, parentDir)
    }

    @Test
    fun multiLevelParentDirectoryIsExcluded() {

        // given
        val parentDir1 = "home"
        val parentDir2 = "user"
        val parentDir3 = "documents"
        ensureDirectoriesInIndexDirectoryExist(parentDir1, parentDir2, parentDir3)

        // when
        val result = underTest.findFilesToIndex(FilesToIndexConfig(indexDirectory, excludeFilesRegexPatterns = listOf("$parentDir1/$parentDir2/$parentDir3")))

        // then
        assertFileIsExcluded(result, parentDir3, parentDir1, parentDir2)
    }


    @Test
    fun parentDirectoryIsExcluded_ChildrenGetPassedToExcludedCallback() {

        // given
        val parentDir = "documents"
        val testSubDirectory = "subdir"
        val testFilename = "test.txt"
        val testFilenameInSubDirectory = "test.jpg"

        ensureDirectoriesInIndexDirectoryExist(testSubDirectory)
        createTestFile(testFilename, parentDir)
        createTestFile(testFilenameInSubDirectory, parentDir, testSubDirectory)

        // when
        val result = underTest.findFilesToIndex(FilesToIndexConfig(indexDirectory, excludeFilesRegexPatterns = listOf(parentDir)))

        // then
        assertFilesAreExcluded(result,
                ExcludedFile(getPathInIndexDirectory(parentDir), ExcludeReason.ExcludePatternMatches),
                ExcludedFile(getPathInIndexDirectory(testFilename, parentDir), ExcludeReason.ExcludedParentDirectory),
                ExcludedFile(getPathInIndexDirectory(testFilenameInSubDirectory, parentDir, testSubDirectory), ExcludeReason.ExcludedParentDirectory)
        )
    }

    @Test
    fun parentDirectoryIsExcluded_ChildOverriddenByInclude() {

        // given
        val parentDir = "documents"
        val testSubDirectory = "subdir"
        val testFilename = "test.txt"
        val testFilenameInSubDirectory = "test.jpg"

        ensureDirectoriesInIndexDirectoryExist(testSubDirectory)
        createTestFile(testFilename, parentDir)
        createTestFile(testFilenameInSubDirectory, parentDir, testSubDirectory)

        // when
        val result = underTest.findFilesToIndex(FilesToIndexConfig(indexDirectory,
                listOf("*" + testFilenameInSubDirectory), listOf(parentDir)))

        // then
        assertThat(result.first).hasSize(1)
        assertPathMatches(result.first.first(), testFilenameInSubDirectory, parentDir, testSubDirectory)

        assertThat(result.second).hasSize(2)
        assertThat(result.second).contains(ExcludedFile(getPathInIndexDirectory(parentDir), ExcludeReason.ExcludePatternMatches))
        assertThat(result.second).contains(ExcludedFile(getPathInIndexDirectory(testFilename, parentDir), ExcludeReason.ExcludedParentDirectory))
    }


    @Test
    fun ignoreFilesSmallerThan() {

        // given
        val testFilename = "test.txt"
        createTestFile(testFilename)

        // when
        val result = underTest.findFilesToIndex(FilesToIndexConfig(indexDirectory, ignoreFilesSmallerThanCountBytes = 5))

        // then
        assertFileIsExcluded(result, ExcludeReason.FileSmallerThanMinFileSize, testFilename)
    }

    @Test
    fun ignoreFilesSmallerThan_OverriddenByInclude() {

        // given
        val testFilename = "test.txt"
        createTestFile(testFilename)

        // when
        val result = underTest.findFilesToIndex(FilesToIndexConfig(indexDirectory, listOf("*.txt"),
                ignoreFilesSmallerThanCountBytes = 5))

        // then
        assertFileIsIncluded(result, testFilename)
    }


    @Test
    fun ignoreFilesLargerThan() {

        // given
        val fileSize = 10L
        val testFilename = "test.txt"
        createTestFileOfSize(fileSize, testFilename)

        // when
        val result = underTest.findFilesToIndex(FilesToIndexConfig(indexDirectory, ignoreFilesLargerThanCountBytes = fileSize - 1))

        // then
        assertFileIsExcluded(result, ExcludeReason.FileLargerThanMaxFileSize, testFilename)
    }

    @Test
    fun ignoreFilesLargerThan_OverriddenByInclude() {

        // given
        val fileSize = 10L
        val testFilename = "test.txt"
        createTestFileOfSize(fileSize, testFilename)

        // when
        val result = underTest.findFilesToIndex(FilesToIndexConfig(indexDirectory, listOf("*.txt"),
                ignoreFilesLargerThanCountBytes = fileSize -1))

        // then
        assertFileIsIncluded(result, testFilename)
    }


    private fun assertFileIsIncluded(result: Pair<List<Path>, List<ExcludedFile>>, filename: String, vararg parentDirectories: String) {
        assertThat(result.second).isEmpty()

        assertThat(result.first).hasSize(1)
        assertPathMatches(result.first.first(), filename, *parentDirectories)
    }

    private fun assertFileIsExcluded(result: Pair<List<Path>, List<ExcludedFile>>, filename: String, vararg parentDirectories: String) {
        assertFileIsExcluded(result, ExcludeReason.ExcludePatternMatches, filename, *parentDirectories)
    }

    private fun assertFileIsExcluded(result: Pair<List<Path>, List<ExcludedFile>>, reason: ExcludeReason,
                                     filename: String, vararg parentDirectories: String) {
        assertThat(result.first).isEmpty()

        assertThat(result.second).hasSize(1)
        assertThat(result.second.first().reason).isEqualByComparingTo(reason)
        assertPathMatches(result.second.first().path, filename, *parentDirectories)
    }

    private fun assertFilesAreExcluded(result: Pair<List<Path>, List<ExcludedFile>>, vararg excludedFiles: ExcludedFile) {
        assertThat(result.first).isEmpty()

        assertThat(result.second).hasSize(excludedFiles.size)

        excludedFiles.forEach { excludedFile ->
            assertThat(result.second).contains(excludedFile)
        }
    }

    private fun assertPathMatches(path: Path, filename: String, vararg parentDirectories: String) {
        val testFile = getFileInIndexDirectory(filename, parentDirectories)

        assertThat(path.toFile()).isEqualTo(testFile)
    }


    private fun createTestFile(filename: String, vararg parentDirectories: String): File {
        val testFile = getFileInIndexDirectory(filename, parentDirectories)

        testFile.createNewFile()

        return testFile
    }

    private fun createTestFileOfSize(sizeInBytes: Long, filename: String, vararg parentDirectories: String): File {
        val testFile = createTestFile(filename, *parentDirectories)

        fileUtils.writeRandomContentToFile(testFile, sizeInBytes)

        return testFile
    }

    private fun getPathInIndexDirectory(filename: String, vararg parentDirectories: String): Path {
        return getFileInIndexDirectory(filename, parentDirectories).toPath()
    }

    private fun getFileInIndexDirectory(filename: String, parentDirectories: Array<out String>): File {
        val parentDirectory = ensureDirectoriesInIndexDirectoryExist(*parentDirectories)

        return File(parentDirectory, filename)
    }

    private fun ensureDirectoriesInIndexDirectoryExist(vararg parentDirectories: String): File {
        var directory = indexDirectory

        parentDirectories.forEach { parentDir ->
            directory = File(directory, parentDir)
        }

        directory.mkdirs()

        return directory
    }

}