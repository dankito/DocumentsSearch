package net.dankito.documents.search.index

import org.apache.lucene.document.Document
import org.apache.lucene.document.DoublePoint
import org.apache.lucene.document.Field
import org.apache.lucene.document.FloatPoint
import org.apache.lucene.document.IntPoint
import org.apache.lucene.document.LongPoint
import org.apache.lucene.document.StoredField
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexableField
import org.apache.lucene.index.Term
import java.util.Date


open class FieldBuilder {

	open fun createDocument(vararg fields: IndexableField): Document {
		val document = Document()

		fields.forEach { field ->
			document.add(field)
		}

		return document
	}

	open fun saveDocument(writer: IndexWriter, vararg fields: IndexableField): Document {
		val document = createDocument(*fields)

		writer.addDocument(document)

		writer.commit()

		return document
	}

	open fun updateDocument(writer: IndexWriter, existingDocumentSearchTerm: Term, vararg fields: IndexableField): Document {
		val document = createDocument(*fields)

		writer.updateDocument(existingDocumentSearchTerm, document)

		writer.commit()

		return document
	}


	@JvmOverloads
	open fun fullTextSearchField(name: String, value: String, store: Boolean = false): TextField {
		return if (store) {
			storedTextField(name, value)
		}
		else {
			unstoredTextField(name, value)
		}
	}

	open fun storedTextField(name: String, value: String): TextField {
		return TextField(name, value, Field.Store.YES)
	}

	open fun unstoredTextField(name: String, value: String): TextField {
		return TextField(name, value, Field.Store.NO)
	}

	@JvmOverloads
	open fun keywordField(name: String, value: String, store: Boolean = true): StringField {
		return if (store) {
			storedStringField(name, value)
		}
		else {
			unstoredStringField(name, value)
		}
	}

	open fun storedStringField(name: String, value: String): StringField {
		return StringField(name, value, Field.Store.YES)
	}

	open fun unstoredStringField(name: String, value: String): StringField {
		return StringField(name, value, Field.Store.NO)
	}


	open fun intField(name: String, value: Int): IntPoint {
		return IntPoint(name, value)
	}

	open fun longField(name: String, value: Long): LongPoint {
		return LongPoint(name, value)
	}

	open fun floatField(name: String, value: Float): FloatPoint {
		return FloatPoint(name, value)
	}

	open fun doubleField(name: String, value: Double): DoublePoint {
		return DoublePoint(name, value)
	}


	open fun dateTimeField(name: String, value: Date): LongPoint {
		return longField(name, value.time)
	}


	open fun storedField(name: String, value: String): StoredField {
		return StoredField(name, value)
	}

	open fun storedField(name: String, value: ByteArray): StoredField {
		return StoredField(name, value)
	}

	open fun storedField(name: String, value: Int): StoredField {
		return StoredField(name, value)
	}

	open fun storedField(name: String, value: Long): StoredField {
		return StoredField(name, value)
	}

	open fun storedField(name: String, value: Float): StoredField {
		return StoredField(name, value)
	}

	open fun storedField(name: String, value: Double): StoredField {
		return StoredField(name, value)
	}

	open fun storedField(name: String, value: Date): StoredField {
		return storedField(name, value.time)
	}

}