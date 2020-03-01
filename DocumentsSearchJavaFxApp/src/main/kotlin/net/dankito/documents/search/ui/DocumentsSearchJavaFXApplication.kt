package net.dankito.banking.ui.javafx

import javafx.application.Application
import net.dankito.documents.search.ui.windows.mainwindow.MainWindow
import net.dankito.utils.javafx.ui.Utf8App


open class DocumentsSearchJavaFXApplication : Utf8App("Messages", MainWindow::class) {


    @Throws(Exception::class)
    override fun stop() {
        super.stop()
        System.exit(0) // otherwise Window would be closed but application still running in background
    }

}



fun main(args: Array<String>) {
    Application.launch(DocumentsSearchJavaFXApplication::class.java, *args)
}