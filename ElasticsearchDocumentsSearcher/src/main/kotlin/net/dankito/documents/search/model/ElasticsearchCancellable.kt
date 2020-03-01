package net.dankito.documents.search.model


open class ElasticsearchCancellable(protected val esCancellable: org.elasticsearch.client.Cancellable) : Cancellable {

	override fun cancel() {
		esCancellable.cancel()
	}

}