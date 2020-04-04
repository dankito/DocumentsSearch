package net.dankito.documents.contentextractor

import java.io.File


interface IFileChecksumCalculator {

    fun calculateChecksum(file: File): String

}