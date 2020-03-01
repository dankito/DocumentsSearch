package net.dankito.documents.search.model


open class NoOpCancellable : Cancellable {

	override fun cancel() {
		// nothing to do
	}

}