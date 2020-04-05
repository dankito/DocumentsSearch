package net.dankito.documents.search.ui

import net.dankito.documents.filesystem.ExcludedFile
import net.dankito.utils.extensions.absolutePath
import net.dankito.utils.extensions.size


val ExcludedFile.absolutePath: String
    get() = this.path.absolutePath

val ExcludedFile.size: Long
    get() = this.path.size