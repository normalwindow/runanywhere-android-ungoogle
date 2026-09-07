package xyz.normalwindow.runanywhere.ui.screens.rag

import xyz.normalwindow.runanywhere.data.settings.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class RagDocumentSummaryTest {
    @Test
    fun `summary pluralizes document and chunk counts independently`() {
        assertEquals("1 document · 1 chunk", formatDocumentChunkSummary(1, 1, AppLanguage.ENGLISH))
        assertEquals("1 document · 2 chunks", formatDocumentChunkSummary(1, 2, AppLanguage.ENGLISH))
        assertEquals("2 documents · 1 chunk", formatDocumentChunkSummary(2, 1, AppLanguage.ENGLISH))
        assertEquals("0 documents · 0 chunks", formatDocumentChunkSummary(0, 0, AppLanguage.ENGLISH))
    }
}
