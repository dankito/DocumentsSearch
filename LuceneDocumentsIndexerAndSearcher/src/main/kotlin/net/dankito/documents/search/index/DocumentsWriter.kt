package net.dankito.documents.search.index

import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexableField
import org.apache.lucene.index.Term


open class DocumentsWriter {

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
        val fieldsIncludingIdField = mutableListOf(createIdField(idFieldName, idFieldValue))
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


    open fun deleteDocument(writer: IndexWriter, idFieldName: String, idFieldValue: String) {
        writer.deleteDocuments(Term(idFieldName, idFieldValue))
    }


    protected open fun createIdField(fieldName: String, value: String): IndexableField {
        return StringField(fieldName, value, Field.Store.YES)
    }

}