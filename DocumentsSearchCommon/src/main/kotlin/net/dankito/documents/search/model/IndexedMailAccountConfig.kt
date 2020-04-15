package net.dankito.documents.search.model


open class IndexedMailAccountConfig(
        var mailAddress: String,
        var username: String,
        var password: String,
        var imapUrl: String,
        var port: Int
) : IndexPartConfig() {

    internal constructor() : this("", "", "", "", -1) // for object deserializers


    override fun toString(): String {
        return mailAddress
    }

}