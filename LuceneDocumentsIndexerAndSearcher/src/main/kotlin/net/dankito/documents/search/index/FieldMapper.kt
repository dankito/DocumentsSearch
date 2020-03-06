package net.dankito.documents.search.index

import org.apache.lucene.document.Document
import java.util.Date


open class FieldMapper {

	open fun string(document: Document, fieldName: String): String {
		return document.getField(fieldName).stringValue()
	}

	open fun int(document: Document, fieldName: String): Int {
		return document.getField(fieldName).numericValue().toInt()
	}

	open fun long(document: Document, fieldName: String): Long {
		return document.getField(fieldName).numericValue().toLong()
	}

	open fun float(document: Document, fieldName: String): Float {
		return document.getField(fieldName).numericValue().toFloat()
	}

	open fun double(document: Document, fieldName: String): Double {
		return document.getField(fieldName).numericValue().toDouble()
	}

	open fun date(document: Document, fieldName: String): Date {
		return Date(document.getField(fieldName).numericValue().toLong())
	}

}