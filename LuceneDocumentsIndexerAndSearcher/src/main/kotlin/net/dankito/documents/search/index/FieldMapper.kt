package net.dankito.documents.search.index

import org.apache.lucene.document.Document
import java.util.*


open class FieldMapper {

	open fun string(document: Document, fieldName: String): String {
		return document.getField(fieldName).stringValue()
	}

	open fun nullableString(document: Document, fieldName: String): String? {
		return document.getField(fieldName)?.stringValue()
	}

	open fun int(document: Document, fieldName: String): Int {
		return number(document, fieldName).toInt()
	}

	open fun nullableInt(document: Document, fieldName: String): Int? {
		return nullableNumber(document, fieldName)?.toInt()
	}

	open fun long(document: Document, fieldName: String): Long {
		return number(document, fieldName).toLong()
	}

	open fun nullableLong(document: Document, fieldName: String): Long? {
		return nullableNumber(document, fieldName)?.toLong()
	}

	open fun float(document: Document, fieldName: String): Float {
		return number(document, fieldName).toFloat()
	}

	open fun nullableFloat(document: Document, fieldName: String): Float? {
		return nullableNumber(document, fieldName)?.toFloat()
	}

	open fun double(document: Document, fieldName: String): Double {
		return number(document, fieldName).toDouble()
	}

	open fun nullableDouble(document: Document, fieldName: String): Double? {
		return nullableNumber(document, fieldName)?.toDouble()
	}

	open fun date(document: Document, fieldName: String): Date {
		return Date(long(document, fieldName))
	}

	open fun nullableDate(document: Document, fieldName: String): Date? {
		nullableLong(document, fieldName)?.let {
			return Date(it)
		}

		return null
	}

	open fun number(document: Document, fieldName: String): Number {
		return document.getField(fieldName).numericValue()
	}

	open fun nullableNumber(document: Document, fieldName: String): Number? {
		return document.getField(fieldName)?.numericValue()
	}

}