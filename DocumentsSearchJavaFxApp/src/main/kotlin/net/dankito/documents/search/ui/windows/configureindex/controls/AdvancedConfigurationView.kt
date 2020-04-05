package net.dankito.documents.search.ui.windows.configureindex.controls

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.geometry.Pos
import net.dankito.documents.search.model.IndexConfig
import net.dankito.utils.javafx.ui.controls.doubleTextfield
import net.dankito.utils.javafx.ui.extensions.fixedHeight
import net.dankito.utils.javafx.ui.extensions.fixedWidth
import tornadofx.*


class AdvancedConfigurationView(indexConfig: IndexConfig, private val updateIndexConfigPreview: () -> Unit) : View() {

    companion object {
        private const val ConfigureFileSizeHeight = 30.0
        private const val ConfigureFileSizeCheckboxWidth = 100.0
        private const val ConfigureFileSizeTextLeftAnchor = 150.0
        private const val ConfigureFileSizeTextFieldWidth = 100.0
    }


    private val isMaxFileSizeEnabled = SimpleBooleanProperty(indexConfig.ignoreFilesLargerThanCountBytes != null)

    private val isMinFileSizeEnabled = SimpleBooleanProperty(indexConfig.ignoreFilesSmallerThanCountBytes != null)

    private val maxFileSize = SimpleDoubleProperty(indexConfig.ignoreFilesLargerThanCountBytes?.toDouble() ?: 100 * 1024 * 1024.0)

    private val minFileSize = SimpleDoubleProperty(indexConfig.ignoreFilesSmallerThanCountBytes?.toDouble() ?: 10 * 1024.0)


    var includeRules: List<String> = ArrayList(indexConfig.includeRules)
        private set

    var excludeRules: List<String> = ArrayList(indexConfig.excludeRules)
        private set

    val ignoreFilesLargerThanCountBytes: Long?
        get() {
            if (isMaxFileSizeEnabled.value) {
                return maxFileSize.value.toLong()
            }

            return null
        }

    val ignoreFilesSmallerThanCountBytes: Long?
        get() {
            if (isMinFileSizeEnabled.value) {
                return minFileSize.value.toLong()
            }

            return null
        }


    init {
        isMaxFileSizeEnabled.addListener { _, _, _ -> updateIndexConfigPreview() }
        isMinFileSizeEnabled.addListener { _, _, _ -> updateIndexConfigPreview() }
        maxFileSize.addListener { _, _, _ -> updateIndexConfigPreview() }
        minFileSize.addListener { _, _, _ -> updateIndexConfigPreview() }
    }


    override val root = vbox {

        anchorpane {
            label(messages["configure.index.window.ignore.files.label"]) {
                fixedHeight = ConfigureFileSizeHeight
                alignment = Pos.CENTER_LEFT
            }

            hbox {
                fixedHeight = ConfigureFileSizeHeight
                alignment = Pos.CENTER_LEFT

                checkbox(messages["configure.index.window.ignore.files.larger.than"], isMaxFileSizeEnabled) {
                    fixedWidth = ConfigureFileSizeCheckboxWidth
                }

                doubleTextfield(maxFileSize) {
                    fixedWidth = ConfigureFileSizeTextFieldWidth

                    enableWhen(isMaxFileSizeEnabled)

                    hboxConstraints {
                        marginLeft = 4.0
                    }
                }

                anchorpaneConstraints {
                    topAnchor = 0.0
                    leftAnchor = ConfigureFileSizeTextLeftAnchor
                }
            }

            hbox {
                fixedHeight = ConfigureFileSizeHeight
                alignment = Pos.CENTER_LEFT

                checkbox(messages["configure.index.window.ignore.files.smaller.than"], isMinFileSizeEnabled) {
                    fixedWidth = ConfigureFileSizeCheckboxWidth
                }

                doubleTextfield(minFileSize) {
                    fixedWidth = ConfigureFileSizeTextFieldWidth

                    enableWhen(isMinFileSizeEnabled)

                    hboxConstraints {
                        marginLeft = 4.0
                    }
                }

                anchorpaneConstraints {
                    topAnchor = ConfigureFileSizeHeight
                    leftAnchor = ConfigureFileSizeTextLeftAnchor
                }
            }

            vboxConstraints {
                marginTop = 6.0
                marginBottom = 6.0
            }
        }

        add(ConfigureIncludeExcludeRulesView("configure.index.window.configure.include.rules.label", includeRules) { includeRules ->
            updateIncludeRules(includeRules)
        })

        add(ConfigureIncludeExcludeRulesView("configure.index.window.configure.exclude.rules.label", excludeRules) { excludeRules ->
            updateExcludeRules(excludeRules)
        })

    }


    private fun updateIncludeRules(includeRules: List<String>) {
        this.includeRules = includeRules

        updateIndexConfigPreview()
    }

    private fun updateExcludeRules(excludeRules: List<String>) {
        this.excludeRules = excludeRules

        updateIndexConfigPreview()
    }

}