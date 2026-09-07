package xyz.normalwindow.runanywhere.ui.screens.rag

import xyz.normalwindow.runanywhere.data.settings.AppLanguage
import xyz.normalwindow.runanywhere.data.settings.AppLocale
import xyz.normalwindow.runanywhere.data.settings.SettingsRepository

internal fun formatDocumentChunkSummary(
    documentCount: Int,
    chunkCount: Int,
    language: AppLanguage = SettingsRepository.settings.language,
): String {
    val documentLabel = AppLocale.text(
        if (documentCount == 1) "document" else "documents",
        language,
    )
    val chunkLabel = AppLocale.text(
        if (chunkCount == 1) "chunk" else "chunks",
        language,
    )
    return "$documentCount $documentLabel \u00b7 $chunkCount $chunkLabel"
}
