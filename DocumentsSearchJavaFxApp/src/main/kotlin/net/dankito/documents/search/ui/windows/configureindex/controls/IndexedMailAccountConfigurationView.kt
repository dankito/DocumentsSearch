package net.dankito.documents.search.ui.windows.configureindex.controls

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import net.dankito.documents.search.model.IndexedMailAccountConfig
import net.dankito.mail.EmailFetcher
import net.dankito.mail.model.CheckCredentialsResult
import net.dankito.mail.model.MailAccount
import net.dankito.utils.javafx.ui.controls.ProcessingIndicatorButton
import net.dankito.utils.javafx.ui.controls.processingIndicatorButton
import net.dankito.utils.javafx.ui.extensions.fixedHeight
import tornadofx.*


class IndexedMailAccountConfigurationView(
        private val mailFetcher: EmailFetcher,
        private val accountCredentialsChecked: (validDataEntered: Boolean) -> Unit,
        mailAddressChanged: ((newValue: String, oldValue: String) -> Unit)? = null
) : View() {

    companion object {
        private const val CheckCredentialsButtonHeight = 36.0

        private const val CheckCredentialsButtonWidth = 120.0
    }


    private var currentMailAccountConfig: IndexedMailAccountConfig? = null


    private val mailAddress = SimpleStringProperty("")

    private val username = SimpleStringProperty("")

    private val password = SimpleStringProperty("")

    private val imapServerAddress = SimpleStringProperty("")

    private val imapServerPort = SimpleIntegerProperty(993)

    private val checkCredentialsResult = SimpleStringProperty("")


    private var checkCredentialsButton: ProcessingIndicatorButton by singleAssign()


    var validDataEntered: Boolean = false
        private set


    init {
        mailAddress.addListener { _, oldValue, newValue ->
            currentMailAccountConfig?.mailAddress = newValue
            mailAddressChanged?.invoke(newValue, oldValue)
        }

        username.addListener { _, _, newValue -> currentMailAccountConfig?.username = newValue }

        password.addListener { _, _, newValue -> currentMailAccountConfig?.password = newValue }

        imapServerAddress.addListener { _, _, newValue -> currentMailAccountConfig?.imapServerAddress = newValue }

        imapServerPort.addListener { _, _, newValue -> currentMailAccountConfig?.imapServerPort = newValue.toInt() }
    }


    override val root = vbox {

        form {
            fieldset {
                field(messages["configure.index.window.mail.account.mail.address.label"]) {
                    textfield(mailAddress) {
                        action { checkCredentials() }
                    }
                }

                field(messages["configure.index.window.mail.account.username.label"]) {
                    textfield(username) {
                        action { checkCredentials() }
                    }
                }

                field(messages["configure.index.window.mail.account.password.label"]) {
                    passwordfield(password) {
                        action { checkCredentials() }
                    }
                }

                field(messages["configure.index.window.mail.account.imap.server.address.label"]) {
                    textfield(imapServerAddress) {
                        action { checkCredentials() }
                    }
                }

                field(messages["configure.index.window.mail.account.imap.server.port.label"]) {
                    textfield(imapServerPort) {
                        action { checkCredentials() }
                    }
                }
            }

            anchorpane {
                fixedHeight = CheckCredentialsButtonHeight

                label(checkCredentialsResult) {
                    anchorpaneConstraints {
                        topAnchor = 0.0
                        leftAnchor = 0.0
                        rightAnchor = CheckCredentialsButtonWidth + 12.0
                        bottomAnchor = 0.0
                    }
                }

                checkCredentialsButton = processingIndicatorButton(messages["configure.index.window.mail.account.check.credentials"]) {
                    prefWidth = CheckCredentialsButtonWidth

                    action { checkCredentials() }

                    anchorpaneConstraints {
                        topAnchor = 0.0
                        rightAnchor = 0.0
                        bottomAnchor = 0.0
                    }
                }
            }
        }
    }


    private fun checkCredentials() {
        checkCredentialsResult.value = ""
        checkCredentialsButton.setIsProcessing()

        mailFetcher.checkAreCredentialsCorrectAsync(mapToMailAccount()) { result ->
            runLater {
                checkCredentialsResult.value = messages[getCheckCredentialsResultTranslationKey(result)]

                checkCredentialsButton.resetIsProcessing()

                validDataEntered = result == CheckCredentialsResult.Ok

                accountCredentialsChecked(validDataEntered)
            }
        }
    }

    private fun getCheckCredentialsResultTranslationKey(result: CheckCredentialsResult): String {
        return when (result) {
            CheckCredentialsResult.Ok -> "configure.index.window.mail.account.check.credentials.result.ok"
            CheckCredentialsResult.WrongUsername -> "configure.index.window.mail.account.check.credentials.result.wrong.username"
            CheckCredentialsResult.WrongPassword -> "configure.index.window.mail.account.check.credentials.result.wrong.password"
            CheckCredentialsResult.InvalidImapServerAddress -> "configure.index.window.mail.account.check.credentials.result.invalid.imap.server.address"
            CheckCredentialsResult.InvalidImapServerPort -> "configure.index.window.mail.account.check.credentials.result.invalid.imap.server.port"
            CheckCredentialsResult.UnknownError -> "configure.index.window.mail.account.check.credentials.result.unknown.error"
        }
    }


    fun setCurrentMailAccount(config: IndexedMailAccountConfig?) {
        this.currentMailAccountConfig = config

        config?.let {
            mailAddress.value = config.mailAddress
            username.value = config.username
            password.value = config.password
            imapServerAddress.value = config.imapServerAddress
            imapServerPort.value = config.imapServerPort
        }
    }

    fun mapToMailAccount(): MailAccount {
        return MailAccount(
                username.value,
                password.value,
                imapServerAddress.value,
                imapServerPort.value
        )
    }

}