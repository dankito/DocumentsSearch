package net.dankito.documents.search.model


open class CombinedCancellable(protected val cancellables: List<Cancellable>) : Cancellable {

    override fun cancel() {
        cancellables.forEach {
            it.cancel()
        }
    }

}