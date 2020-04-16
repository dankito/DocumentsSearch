package net.dankito.documents.mail

import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import net.dankito.documents.IIndexHandler
import net.dankito.documents.contentextractor.IFileContentExtractor
import net.dankito.documents.search.index.IDocumentsIndexer
import net.dankito.documents.search.model.Document
import net.dankito.documents.search.model.DocumentMetadata
import net.dankito.documents.search.model.IndexConfig
import net.dankito.documents.search.model.IndexedMailAccountConfig
import net.dankito.mail.EmailFetcher
import net.dankito.mail.model.Email
import net.dankito.mail.model.FetchEmailOptions
import net.dankito.mail.model.MailAccount
import net.dankito.utils.hashing.HashAlgorithm
import net.dankito.utils.hashing.HashService
import net.dankito.utils.html.toPlainText
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*


open class MailAccountIndexHandler(
        protected val contentExtractor: IFileContentExtractor,
        protected val hashService: HashService = HashService(),
        protected val indexUpdatedEventBus: PublishSubject<IndexConfig>
) : IIndexHandler<IndexedMailAccountConfig> {

    companion object {
        private val log = LoggerFactory.getLogger(MailAccountIndexHandler::class.java)
    }


    protected val mailFetcher = EmailFetcher()


    override suspend fun updateIndexPartItems(index: IndexConfig, indexPart: IndexedMailAccountConfig,
                                              currentItemsInIndex: MutableMap<String, DocumentMetadata>, indexer: IDocumentsIndexer) {

        withContext(Dispatchers.IO) {
            val account = mapToMailAccount(indexPart)

            mailFetcher.fetchMails(FetchEmailOptions(account, null, null, true, false, false, true, false, 10)) { fetchResult ->
                for (mail in fetchResult.retrievedChunk) {
                    async(Dispatchers.IO) {
                        val mailId = getIdForMail(indexPart, mail)

                        if (isNewOrUpdatedMail(indexPart, mail, mailId, currentItemsInIndex)) {
                            extractContentAndIndex(indexPart, mail, mailId, indexer)

                            indexUpdatedEventBus.onNext(index)
                        }

                        currentItemsInIndex.remove(mailId)
                    }
                }
            }
        }
    }

    protected open fun isNewOrUpdatedMail(indexPart: IndexedMailAccountConfig, mail: Email, mailId: String, currentItemsInIndex: Map<String, DocumentMetadata>): Boolean {
        val metadata = currentItemsInIndex[mailId]

        if (metadata == null) { // a new mail
            log.debug("New mail discovered: {}", mail)

            return true
        }

        val checksum = calculateMailChecksum(mail)

        val isUpdated = mail.size != metadata.fileSize
                // TODO: check if attachments equal
                || metadata.checksum != checksum

        if (isUpdated) {
            log.debug("Updated mail discovered: {}\n" +
                    "Size changed: {}\n" +
                    "Checksum changed: {}",
                    mail, mail.size != metadata.fileSize,
                    metadata.checksum != checksum)
        }

        return isUpdated
    }


    open suspend fun extractContentAndIndex(indexPart: IndexedMailAccountConfig, mailMetadata: Email, mailId: String, indexer: IDocumentsIndexer) {
        mailMetadata.messageId?.let { messageId ->
            mailFetcher.fetchMails(FetchEmailOptions(mapToMailAccount(indexPart), null, listOf(messageId), false, true, true, false, true)) { fetchResult ->
                for (mailContent in fetchResult.allRetrievedMails) {
                    extractAttachmentsContentsAndIndex(indexPart, mailMetadata, mailId, mailContent, indexer)
                }
            }
        }
    }

    protected open fun extractAttachmentsContentsAndIndex(indexPart: IndexedMailAccountConfig, mailMetadata: Email, mailId: String, mailContent: Email, indexer: IDocumentsIndexer) {
        try {
            // TODO: attach attachments info and content to Document
            val attachmentContents = mailContent.attachments.mapNotNull { attachment ->
                try {
                    val attachmentFilename = File(attachment.name)
                    val attachmentTempFile = File.createTempFile(attachmentFilename.nameWithoutExtension + "_", "." + attachmentFilename.extension)

                    attachmentTempFile.writeBytes(attachment.content) // TODO: write non-blocking

                    val result = contentExtractor.extractContent(attachmentTempFile).content // TODO: use extractContentSuspendable()

                    attachmentTempFile.delete()

                    return@mapNotNull result
                } catch (e: Exception) {
                    log.error("Could not extract content of attachment $attachment", e)
                }

                null
            }

            val document = createDocument(mailMetadata, mailId, mailContent.plainTextBody ?: Jsoup.parse(mailContent.htmlBody ?: "").toPlainText(), attachmentContents)

            indexer.index(document)
        } catch (e: Exception) {
            log.error("Could not extract mail '$mailMetadata'", e)
        }
    }

    protected open fun createDocument(mailMetadata: Email, mailId: String, content: String?, attachmentContents: List<String>): Document {

        return Document(
                mailId,
                "" + (mailMetadata.messageId ?: ""),
                (content ?: "") + attachmentContents.map { "\r\n\r\n$it" }, // TODO: add an extra field for attachments; also include their name, size and contentType there
                mailMetadata.size ?: -1,
                calculateMailChecksum(mailMetadata),
                Date(0),
                mailMetadata.sentDate ?: mailMetadata.receivedDate,
                Date(0),
                mailMetadata.contentType,
                mailMetadata.subject, mailMetadata.sender, -1, "",
                "", // TODO: use languageDetector?
                "", listOf()
                // TODO: what about receivers?
        )
    }


    override fun listenForChangesToIndexedItems(index: IndexConfig, indexPart: IndexedMailAccountConfig, indexer: IDocumentsIndexer) {
    }


    protected open fun getIdForMail(indexPart: IndexedMailAccountConfig, mail: Email): String {
        return indexPart.mailAddress + "_" + (mail.messageId ?: mail.sentDate?.time ?: mail.receivedDate.time)
    }

    protected open fun mapToMailAccount(indexPart: IndexedMailAccountConfig): MailAccount {
        return MailAccount(indexPart.username, indexPart.password, indexPart.imapServerAddress, indexPart.imapServerPort)
    }

    protected open fun calculateMailChecksum(mail: Email): String {
        // we cannot use mail body for calculating hash as mail metadata doesn't contain mail body

        try {
            val mailInfoToHash = mail.sender + "_" + mail.subject +
                    "_" + mail.sentDate?.time + "_" + mail.receivedDate.time +
                    mail.attachments.map { "_" + it.name + "_" + it.size + "_" + it.mimeType }

            return hashService.hashString(HashAlgorithm.SHA512, mailInfoToHash)
        } catch (e: Exception) {
            log.error("Could not create hash for mail $mail", e)
        }

        val mailInfoToHash = mail.sender + "_" + mail.subject +
                mail.attachments.map { "_" + it.name + "_" + it.size + "_" + it.mimeType }

        return hashService.hashString(HashAlgorithm.SHA512, mailInfoToHash)
    }

}