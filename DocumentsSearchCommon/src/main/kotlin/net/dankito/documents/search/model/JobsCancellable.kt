package net.dankito.documents.search.model

import kotlinx.coroutines.Job


open class JobsCancellable(protected val jobs: List<Job>): Cancellable {

    override fun cancel() {
        jobs.forEach {
            it.cancel()
        }
    }

}