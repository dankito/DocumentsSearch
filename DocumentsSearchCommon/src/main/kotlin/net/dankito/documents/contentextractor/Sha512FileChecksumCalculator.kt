package net.dankito.documents.contentextractor

import net.dankito.utils.hashing.HashAlgorithm
import net.dankito.utils.hashing.HashService
import java.io.File


open class Sha512FileChecksumCalculator(protected val hashService: HashService = HashService()) : IFileChecksumCalculator {

    override fun calculateChecksum(file: File): String {
        return hashService.getFileHash(HashAlgorithm.SHA512, file)
    }

}