package net.dankito.documents.search.ui.windows.configureindex.controls

import javafx.beans.property.SimpleIntegerProperty
import javafx.collections.FXCollections
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import net.dankito.documents.filesystem.ExcludeReason
import net.dankito.documents.search.ui.windows.configureindex.model.ExcludedFileViewModel
import net.dankito.documents.search.ui.windows.configureindex.model.IncludedIndexPartItemViewModel
import net.dankito.utils.FormatUtils
import net.dankito.utils.javafx.ui.extensions.fixedHeight
import net.dankito.utils.javafx.ui.extensions.initiallyUseRemainingSpace
import tornadofx.*


class ConfiguredIndexPreview : View() {

    companion object {
        private const val LabelPaneHeight = 30.0

        private const val FileSizeColumnWidth = 70.0
    }


    private val includedItems = FXCollections.observableArrayList<IncludedIndexPartItemViewModel<*>>()

    private val excludedItems = FXCollections.observableArrayList<ExcludedFileViewModel>()

    private val countIncludedItems = SimpleIntegerProperty(0)

    private val countExcludedItems = SimpleIntegerProperty(0)


    private val formatUtils = FormatUtils()


    override val root = splitpane(Orientation.VERTICAL) {

        vbox {
            anchorpane {
                fixedHeight = LabelPaneHeight
                alignment = Pos.CENTER_LEFT

                label(messages["configure.index.window.included.items.preview"]) {

                    anchorpaneConstraints {
                        topAnchor = 0.0
                        leftAnchor = 0.0
                        bottomAnchor = 0.0
                    }
                }

                hbox {
                    alignment = Pos.CENTER_LEFT

                    label(countIncludedItems)

                    label(messages["configure.index.window.items"]) {
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

            tableview<IncludedIndexPartItemViewModel<*>>(includedItems) {
                column<IncludedIndexPartItemViewModel<*>, String>(messages["configure.index.window.included.excluded.item.display.name.column.name"], IncludedIndexPartItemViewModel<*>::displayName) {
                    this.initiallyUseRemainingSpace(this@tableview)
                }

                column<IncludedIndexPartItemViewModel<*>, Number>(messages["configure.index.window.included.excluded.item.size.column.name"], IncludedIndexPartItemViewModel<*>::size) {
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

                label(messages["configure.index.window.excluded.items.preview"]) {

                    anchorpaneConstraints {
                        topAnchor = 0.0
                        leftAnchor = 0.0
                        bottomAnchor = 0.0
                    }
                }

                hbox {
                    alignment = Pos.CENTER_LEFT

                    label(countExcludedItems)

                    label(messages["configure.index.window.items"]) {
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

            tableview<ExcludedFileViewModel>(excludedItems) {
                column<ExcludedFileViewModel, String>(messages["configure.index.window.included.excluded.item.display.name.column.name"], ExcludedFileViewModel::path) {
                    this.initiallyUseRemainingSpace(this@tableview)
                }

                column<ExcludedFileViewModel, Number>(messages["configure.index.window.included.excluded.item.size.column.name"], ExcludedFileViewModel::size) {
                    this.cellFormat { size ->
                        this.text = formatUtils.formatFileSize(size.toLong())
                    }

                    prefWidth = FileSizeColumnWidth
                }

                column<ExcludedFileViewModel, ExcludeReason>(messages["configure.index.window.excluded.item.reason.column.name"], ExcludedFileViewModel::reason) {
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
            ExcludeReason.ExcludePatternMatches -> messages["configure.index.window.excluded.item.reason.exclude.pattern.matches"]
            ExcludeReason.ExcludedParentDirectory -> messages["configure.index.window.excluded.item.reason.excluded.parent.directory"]
            ExcludeReason.FileSmallerThanMinFileSize -> messages["configure.index.window.excluded.item.reason.smaller.than.min.size"]
            ExcludeReason.FileLargerThanMaxFileSize -> messages["configure.index.window.excluded.item.reason.larger.than.max.size"]
            ExcludeReason.ErrorOccurred -> messages["configure.index.window.excluded.item.reason.error.occurred"]
        }
    }


    fun update(includes: List<IncludedIndexPartItemViewModel<*>>, excludes: List<ExcludedFileViewModel>) {
        includedItems.setAll(includes)
        countIncludedItems.set(includes.size)

        excludedItems.setAll(excludes)
        countExcludedItems.set(excludes.size)
    }

}