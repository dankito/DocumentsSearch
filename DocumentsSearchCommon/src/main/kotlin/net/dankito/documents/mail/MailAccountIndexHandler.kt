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
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


open class MailAccountIndexHandler(
        protected val contentExtractor: IFileContentExtractor,
        protected val hashService: HashService = HashService(),
        protected val indexUpdatedEventBus: PublishSubject<IndexConfig>
) : IIndexHandler<IndexedMailAccountConfig> {

    companion object {
        private val DateFormatForSentAndReceiveDate = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss")

        private val log = LoggerFactory.getLogger(MailAccountIndexHandler::class.java)
    }


    protected val mailFetcher = EmailFetcher()


    override suspend fun updateIndexPartElements(index: IndexConfig, indexPart: IndexedMailAccountConfig,
                                                 currentItemsInIndex: MutableMap<String, DocumentMetadata>, indexer: IDocumentsIndexer) {

        withContext(Dispatchers.IO) {
            val account = mapToMailAccount(indexPart)
            val messageIds = currentItemsInIndex.values.mapNotNull { it.url.toLongOrNull() }
            val lastRetrievedMessageId = messageIds.sortedDescending().firstOrNull()
            // we simply assume that messages never change, what is true in most cases, and retrieve only messages that
            // haven't been retrieved yet but are not looking for message updates
            val startMessageId = if (lastRetrievedMessageId != null) lastRetrievedMessageId + 1 else null

            mailFetcher.fetchMails(FetchEmailOptions(account, startMessageId, null, true, true, true, false, true, 10)) { fetchResult ->
                for (mail in fetchResult.allRetrievedMails) { // TODO: re-enable retrieving in chunks
                    async(Dispatchers.IO) {
                        val mailId = getIdForMail(indexPart, mail)

                        if (isNewOrUpdatedMail(indexPart, mail, mailId, currentItemsInIndex)) {
                            extractAttachmentsContentsAndIndex(indexPart, mail, mailId, mail, indexer)

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
                    attachmentTempFile.deleteOnExit()

                    attachmentTempFile.writeBytes(attachment.content) // TODO: write non-blocking

                    return@mapNotNull contentExtractor.extractContent(attachmentTempFile).content // TODO: use extractContentSuspendable()
                } catch (e: Exception) {
                    log.error("Could not extract content of attachment $attachment", e)
                }

                null
            }

            val document = createDocument(mailMetadata, mailId, mailContent.plainTextBody ?: Jsoup.parse(mailContent.htmlBody ?: "").text(), attachmentContents)

            indexer.index(document)
        } catch (e: Exception) {
            log.error("Could not extract mail '$mailMetadata'", e)
        }
    }

    protected open fun createDocument(mailMetadata: Email, mailId: String, content: String?, attachmentContents: List<String>): Document {

        return Document(
                mailId,
                mailId,
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
        return indexPart.mailAddress + "_" + (mail.messageId ?: DateFormatForSentAndReceiveDate.format(mail.sentDate ?: mail.receivedDate))
    }

    protected open fun mapToMailAccount(indexPart: IndexedMailAccountConfig): MailAccount {
        return MailAccount(indexPart.username, indexPart.password, indexPart.imapUrl, indexPart.port)
    }

    protected open fun calculateMailChecksum(mail: Email): String {
        // we cannot use mail body for calculating hash as mail metadata doesn't contain mail body

        // TODO: really bad code
        try {
            val mailInfoToHash = mail.sender + "_" + mail.subject +
                    "_" + DateFormatForSentAndReceiveDate.format(mail.sentDate ?: Date()) +
                    "_" + DateFormatForSentAndReceiveDate.format(mail.receivedDate) +
                    mail.attachments.map { "_" + it.name + "_" + it.size + "_" + it.mimeType }

            return hashService.hashString(HashAlgorithm.SHA512, mailInfoToHash)
        } catch (e: Exception) {
            log.error("Could not create hash for dates ${mail.sentDate ?: Date()}", e)
        }

        val mailInfoToHash = mail.sender + "_" + mail.subject +
                "_" + DateFormatForSentAndReceiveDate.format(mail.receivedDate) +
                mail.attachments.map { "_" + it.name + "_" + it.size + "_" + it.mimeType }

        return hashService.hashString(HashAlgorithm.SHA512, mailInfoToHash)
    }

}