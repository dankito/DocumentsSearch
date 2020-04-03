package net.dankito.documents.search.index

import org.apache.lucene.document.*
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexableField
import org.apache.lucene.index.Term
import org.apache.lucene.util.BytesRef
import org.apache.lucene.util.NumericUtils
import java.util.*


open class FieldBuilder {

	open fun createDocument(fields: List<IndexableField>): Document {
		val document = Document()

		fields.forEach { field ->
			document.add(field)
		}

		return document
	}

	open fun saveDocument(writer: IndexWriter, vararg fields: IndexableField): Document {
		val document = createDocument(fields.toList())

		writer.addDocument(document)

		writer.commit()

		return document
	}

	open fun updateDocument(writer: IndexWriter, idFieldName: String, idFieldValue: String, vararg fields: IndexableField): Document {
		val fieldsIncludingIdField = mutableListOf<IndexableField>(keywordField(idFieldName, idFieldValue, true))
		fieldsIncludingIdField.addAll(fields.toList())

		val document = createDocument(fieldsIncludingIdField)

		val findExistingDocumentTerm = Term(idFieldName, idFieldValue)

		writer.updateDocument(findExistingDocumentTerm, document)

		writer.commit()

		return document
	}

	open fun updateDocumentForNonNullFields(writer: IndexWriter, idFieldName: String, idFieldValue: String, vararg fields: IndexableField?): Document {
		return updateDocument(writer, idFieldName, idFieldValue, *fields.filterNotNull().toTypedArray())
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

	@JvmOverloads
	open fun nullableFullTextSearchField(name: String, value: String?, store: Boolean = false): TextField? {
		return if (value == null) null else fullTextSearchField(name, value, store)
	}

	open fun storedTextField(name: String, value: String): TextField {
		return TextField(name, value, Field.Store.YES)
	}

	open fun unstoredTextField(name: String, value: String): TextField {
		return TextField(name, value, Field.Store.NO)
	}

	@JvmOverloads
	open fun keywordField(name: String, value: String, store: Boolean = true): StringField {
		return stringField(name, value, store)
	}

	@JvmOverloads
	open fun nullableKeywordField(name: String, value: String?, store: Boolean = true): StringField? {
		return if (value == null) null else keywordField(name, value, store)
	}

	@JvmOverloads
	open fun stringField(name: String, value: String, store: Boolean = true): StringField {
		return if (store) {
			storedStringField(name, value)
		}
		else {
			unstoredStringField(name, value)
		}
	}

	@JvmOverloads
	open fun nullableStringField(name: String, value: String?, store: Boolean = true): StringField? {
		return if (value == null) null else stringField(name, value, store)
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

	open fun nullableIntField(name: String, value: Int?): IntPoint? {
		return if (value == null) null else intField(name, value)
	}

	open fun longField(name: String, value: Long): LongPoint {
		return LongPoint(name, value)
	}

	open fun nullableLongField(name: String, value: Long?): LongPoint? {
		return if (value == null) null else LongPoint(name, value)
	}

	open fun floatField(name: String, value: Float): FloatPoint {
		return FloatPoint(name, value)
	}

	open fun nullableFloatField(name: String, value: Float?): FloatPoint? {
		return if (value == null) null else floatField(name, value)
	}

	open fun doubleField(name: String, value: Double): DoublePoint {
		return DoublePoint(name, value)
	}

	open fun nullableDoubleField(name: String, value: Double?): DoublePoint? {
		return if (value == null) null else doubleField(name, value)
	}


	open fun dateTimeField(name: String, value: Date): LongPoint {
		return longField(name, value.time)
	}

	open fun nullableDateTimeField(name: String, value: Date?): LongPoint? {
		return if (value == null) null else dateTimeField(name, value)
	}


	open fun storedField(name: String, value: String): StoredField {
		return StoredField(name, value)
	}

	open fun nullableStoredField(name: String, value: String?): StoredField? {
		return if (value == null) null else storedField(name, value)
	}

	open fun storedField(name: String, value: ByteArray): StoredField {
		return StoredField(name, value)
	}

	open fun nullableStoredField(name: String, value: ByteArray?): StoredField? {
		return if (value == null) null else storedField(name, value)
	}

	open fun storedField(name: String, value: Int): StoredField {
		return StoredField(name, value)
	}

	open fun nullableStoredField(name: String, value: Int?): StoredField? {
		return if (value == null) null else storedField(name, value)
	}

	open fun storedField(name: String, value: Long): StoredField {
		return StoredField(name, value)
	}

	open fun nullableStoredField(name: String, value: Long?): StoredField? {
		return if (value == null) null else storedField(name, value)
	}

	open fun storedField(name: String, value: Float): StoredField {
		return StoredField(name, value)
	}

	open fun nullableStoredField(name: String, value: Float?): StoredField? {
		return if (value == null) null else storedField(name, value)
	}

	open fun storedField(name: String, value: Double): StoredField {
		return StoredField(name, value)
	}

	open fun nullableStoredField(name: String, value: Double?): StoredField? {
		return if (value == null) null else storedField(name, value)
	}

	open fun storedField(name: String, value: Date): StoredField {
		return storedField(name, value.time)
	}

	open fun nullableStoredField(name: String, value: Date?): StoredField? {
		return if (value == null) null else storedField(name, value)
	}


	@JvmOverloads
	open fun sortField(name: String, value: String, caseInsensitive: Boolean = true): SortedDocValuesField {
		val adjustedValue = if (caseInsensitive) value.toLowerCase() else value

		return SortedDocValuesField(name, BytesRef(adjustedValue))
	}

	open fun sortField(name: String, value: Int): SortedNumericDocValuesField {
		return sortField(name, value.toLong())
	}

	open fun sortField(name: String, value: Long): SortedNumericDocValuesField {
		return SortedNumericDocValuesField(name, value)
	}

	open fun sortField(name: String, value: Float): SortedNumericDocValuesField {
		return sortField(name, NumericUtils.floatToSortableInt(value))
	}

	open fun sortField(name: String, value: Double): SortedNumericDocValuesField {
		return sortField(name, NumericUtils.doubleToSortableLong(value))
	}

}