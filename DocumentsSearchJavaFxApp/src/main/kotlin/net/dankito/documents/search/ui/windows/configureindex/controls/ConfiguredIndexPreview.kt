package net.dankito.documents.search.ui.windows.configureindex.controls

import javafx.beans.property.SimpleIntegerProperty
import javafx.collections.FXCollections
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import net.dankito.documents.filesystem.ExcludeReason
import net.dankito.documents.filesystem.ExcludedFile
import net.dankito.documents.search.ui.windows.configureindex.model.ExcludedFileViewModel
import net.dankito.documents.search.ui.windows.configureindex.model.IncludedFileViewModel
import net.dankito.utils.FormatUtils
import net.dankito.utils.javafx.ui.extensions.fixedHeight
import net.dankito.utils.javafx.ui.extensions.initiallyUseRemainingSpace
import tornadofx.*
import java.nio.file.Path


class ConfiguredIndexPreview : View() {

    companion object {
        private const val LabelPaneHeight = 30.0

        private const val FileSizeColumnWidth = 70.0
    }


    private val includedFiles = FXCollections.observableArrayList<IncludedFileViewModel>()

    private val excludedFiles = FXCollections.observableArrayList<ExcludedFileViewModel>()

    private val countIncludedFiles = SimpleIntegerProperty(0)

    private val countExcludedFiles = SimpleIntegerProperty(0)


    private val formatUtils = FormatUtils()


    override val root = splitpane(Orientation.VERTICAL) {

        vbox {
            anchorpane {
                fixedHeight = LabelPaneHeight
                alignment = Pos.CENTER_LEFT

                label(messages["configure.index.window.included.files.preview"]) {

                    anchorpaneConstraints {
                        topAnchor = 0.0
                        leftAnchor = 0.0
                        bottomAnchor = 0.0
                    }
                }

                hbox {
                    alignment = Pos.CENTER_LEFT

                    label(countIncludedFiles)

                    label(messages["configure.index.window.files"]) {
                        hboxConstraints {
                            marginLeft = 6.0
                        }
                    }

                    anchorpaneConstraints {
                        topAnchor = 0.0
                        rightAnchor = 0.0
                        bottomAnchor = 0.0
                    }
                }

                vboxConstraints {
                    marginTop = 4.0
                }
            }

            tableview<IncludedFileViewModel>(includedFiles) {
                column<IncludedFileViewModel, String>(messages["configure.index.window.included.excluded.file.path.column.name"], IncludedFileViewModel::path) {
                    this.initiallyUseRemainingSpace(this@tableview)
                }

                column<IncludedFileViewModel, Number>(messages["configure.index.window.included.excluded.file.path.column.size"], IncludedFileViewModel::size) {
                    this.cellFormat { size ->
                        this.text = formatUtils.formatFileSize(size.toLong())
                    }

                    prefWidth = FileSizeColumnWidth
                }

                smartResize()

                vboxConstraints {
                    vGrow = Priority.ALWAYS
                }
            }
        }

        vbox {
            anchorpane {
                fixedHeight = LabelPaneHeight
                alignment = Pos.CENTER_LEFT

                label(messages["configure.index.window.excluded.files.preview"]) {

                    anchorpaneConstraints {
                        topAnchor = 0.0
                        leftAnchor = 0.0
                        bottomAnchor = 0.0
                    }
                }

                hbox {
                    alignment = Pos.CENTER_LEFT

                    label(countExcludedFiles)

                    label(messages["configure.index.window.files"]) {
                        hboxConstraints {
                            marginLeft = 6.0
                        }
                    }

                    anchorpaneConstraints {
                        topAnchor = 0.0
                        rightAnchor = 0.0
                        bottomAnchor = 0.0
                    }
                }

                vboxConstraints {
                    marginTop = 2.0
                }
            }

            tableview<ExcludedFileViewModel>(excludedFiles) {
                column<ExcludedFileViewModel, String>(messages["configure.index.window.included.excluded.file.path.column.name"], ExcludedFileViewModel::path) {
                    this.initiallyUseRemainingSpace(this@tableview)
                }

                column<ExcludedFileViewModel, Number>(messages["configure.index.window.included.excluded.file.path.column.size"], ExcludedFileViewModel::size) {
                    this.cellFormat { size ->
                        this.text = formatUtils.formatFileSize(size.toLong())
                    }

                    prefWidth = FileSizeColumnWidth
                }

                column<ExcludedFileViewModel, ExcludeReason>(messages["configure.index.window.excluded.file.reason.column.name"], ExcludedFileViewModel::reason) {
                    this.cellFormat { reason ->
                        this.text = translateExcludeReason(reason)
                    }

                    prefWidth = 200.0
                }

                smartResize()

                vboxConstraints {
                    vGrow = Priority.ALWAYS
                }
            }
        }
    }

    private fun translateExcludeReason(reason: ExcludeReason): String {
        return when (reason) {
            ExcludeReason.ExcludePatternMatches -> messages["configure.index.window.excluded.file.reason.exclude.pattern.matches"]
            ExcludeReason.ExcludedParentDirectory -> messages["configure.index.window.excluded.file.reason.excluded.parent.directory"]
            ExcludeReason.FileSmallerThanMinFileSize -> messages["configure.index.window.excluded.file.reason.smaller.than.min.file.size"]
            ExcludeReason.FileLargerThanMaxFileSize -> messages["configure.index.window.excluded.file.reason.larger.than.max.file.size"]
            ExcludeReason.ErrorOccurred -> messages["configure.index.window.excluded.file.reason.error.occurred"]
        }
    }


    fun update(includes: List<Path>, excludes: List<ExcludedFile>) {
        includedFiles.setAll(includes.map { IncludedFileViewModel(it) })
        countIncludedFiles.set(includes.size)

        excludedFiles.setAll(excludes.map { ExcludedFileViewModel(it) })
        countExcludedFiles.set(excludes.size)
    }

}