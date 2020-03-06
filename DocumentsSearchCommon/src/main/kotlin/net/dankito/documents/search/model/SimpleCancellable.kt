package net.dankito.documents.search.model


open class SimpleCancellable: Cancellable {

	open var isCancelled: Boolean = false
		protected set

	override fun cancel() {
		isCancelled = true
	}


	override fun toString(): String {
		return "isCancelled? $isCancelled"
	}

}