package net.dankito.documents.search.ui.windows.mainwindow

import javafx.beans.property.SimpleIntegerProperty
import javafx.geometry.Pos
import net.dankito.documents.search.ui.presenter.DocumentsSearchPresenter
import net.dankito.documents.search.ui.windows.mainwindow.controls.SearchDocumentsView
import net.dankito.utils.javafx.ui.extensions.fixedHeight
import tornadofx.*
import tornadofx.FX.Companion.messages


class MainWindow : View(messages["application.title"]) {

    private val presenter = DocumentsSearchPresenter()


    private val countSearchResults = SimpleIntegerProperty(0)



    override val root = borderpane {
        prefHeight = 620.0
        prefWidth = 1150.0

        center {
            add(SearchDocumentsView(presenter, countSearchResults))
        }

        bottom {
            anchorpane {
                fixedHeight = 32.0

                hbox {
                    alignment = Pos.CENTER_LEFT

                    label(countSearchResults)

                    label(messages["hits"]) {
                        hboxConstraints {
                            marginLeft = 4.0
                        }
                    }

                    anchorpaneConstraints {
                        topAnchor = 0.0
                        rightAnchor = 6.0
                        bottomAnchor = 0.0
                    }
                }
            }
        }
    }

}