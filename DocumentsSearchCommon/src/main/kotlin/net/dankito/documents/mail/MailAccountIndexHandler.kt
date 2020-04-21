package net.dankito.documents.mail

import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import net.dankito.documents.IIndexHandler
import net.dankito.documents.contentextractor.IFileContentExtractor
import net.dankito.documents.search.index.IDocumentsIndexer
import net.dankito.documents.search.model.*
import net.dankito.documents.search.model.Attachment
import net.dankito.mail.EmailFetcher
import net.dankito.mail.model.*
import net.dankito.utils.extensions.htmlToPlainText
import net.dankito.utils.hashing.HashAlgorithm
import net.dankito.utils.hashing.HashService
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

        val isUpdated = mail.size != metadata.size
                // TODO: check if attachments equal
                || metadata.checksum != checksum

        if (isUpdated) {
            log.debug("Updated mail discovered: {}\n" +
                    "Size changed: {}\n" +
                    "Checksum changed: {}",
                    mail, mail.size != metadata.size,
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
            val attachments = mailContent.attachments.mapNotNull { attachment ->
                try {
                    val attachmentFilename = File(attachment.name)
                    val attachmentTempFile = File.createTempFile(attachmentFilename.nameWithoutExtension + "_", "." + attachmentFilename.extension)

                    attachmentTempFile.writeBytes(attachment.content) // TODO: write non-blocking

                    val content = contentExtractor.extractContent(attachmentTempFile).content // TODO: use extractContentSuspendable()

                    attachmentTempFile.delete()

                    return@mapNotNull Attachment(attachment.name, attachment.size, attachment.mimeType, content ?: "")
                } catch (e: Exception) {
                    log.error("Could not extract content of attachment $attachment", e)
                }

                null
            }

            val document = createDocument(mailMetadata, mailId, mailContent.plainTextBody ?: (mailContent.htmlBody ?: "").htmlToPlainText(), attachments)

            indexer.index(document)
        } catch (e: Exception) {
            log.error("Could not extract mail '$mailMetadata'", e)
        }
    }

    protected open fun createDocument(mailMetadata: Email, mailId: String, content: String?, attachments: List<Attachment>): Document {

        return Document(
                mailId,
                "" + (mailMetadata.messageId ?: ""),
                content ?: "",
                mailMetadata.size ?: -1,
                calculateMailChecksum(mailMetadata),
                mailMetadata.sentDate ?: mailMetadata.receivedDate,
                mailMetadata.contentType,
                mailMetadata.subject, mailMetadata.sender, -1, null, null, // TODO: use languageDetector?
                mailMetadata.recipients,
                attachments
        )
    }


    override fun listenForChangesToIndexedItems(index: IndexConfig, indexPart: IndexedMailAccountConfig, indexer: IDocumentsIndexer) {
        mailFetcher.addMessageListener(MessageChangedListenerOptions(mapToMailAccount(indexPart))) { type, mail ->
            if (type == MessageChangeType.Added || type == MessageChangeType.Modified) {
                extractAttachmentsContentsAndIndex(indexPart, mail, getIdForMail(indexPart, mail), mail, indexer)
            }
            else if (type == MessageChangeType.Deleted) {
                indexer.remove(getIdForMail(indexPart, mail))
            }

            indexUpdatedEventBus.onNext(index)
        }
    }


    protected open fun getIdForMail(indexPart: IndexedMailAccountConfig, mail: Email): String {
        return indexPart.imapServerAddress + "_" + indexPart.username + "_" + (mail.messageId ?: mail.sentDate?.time ?: mail.receivedDate.time)
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