package xyz.normalwindow.runanywhere.ui.screens.chat

import xyz.normalwindow.runanywhere.data.settings.AppLanguage
import xyz.normalwindow.runanywhere.data.settings.AppLocale
import xyz.normalwindow.runanywhere.data.settings.SettingsRepository

/** Effective output budget for one normal (non-tool) chat response. */
internal data class ChatGenerationBudget(
    val requestedMaxTokens: Int,
    val effectiveMaxTokens: Int,
    val modelContextTokens: Int?,
) {
    val isCapped: Boolean get() = effectiveMaxTokens < requestedMaxTokens

    fun explanation(
        modelName: String?,
        language: AppLanguage = SettingsRepository.settings.language,
    ): String {
        val subject = modelName?.takeIf { it.isNotBlank() } ?: AppLocale.text("Chat", language)
        return when {
            !isCapped && modelContextTokens != null ->
                AppLocale.text(
                    "Chat reserves at least half of the model context for your prompt and system instructions.",
                    language,
                )
            !isCapped ->
                AppLocale.text(
                    "Chat uses your saved maximum and will reserve input space when the model reports its context size.",
                    language,
                )
            modelContextTokens != null ->
                AppLocale.format(
                    "%s will use up to %d output tokens with its %d-token context. Your %d-token preference stays saved.",
                    subject,
                    effectiveMaxTokens,
                    modelContextTokens,
                    requestedMaxTokens,
                    language = language,
                )
            else ->
                AppLocale.format(
                    "%s will use up to %d output tokens until it reports a context size. Your %d-token preference stays saved.",
                    subject,
                    effectiveMaxTokens,
                    requestedMaxTokens,
                    language = language,
                )
        }
    }
}

/**
 * Production response-budget policy for normal chat.
 *
 * The saved setting remains the user's preference. Each request independently
 * reserves half of a known context window for input/template overhead and caps
 * consumer chat output at 512 tokens. This prevents 512/1K QHexRT models from
 * inheriting a 4K-token multi-minute decode while leaving the setting intact for
 * other consumers and future larger-budget modes.
 */
internal object ChatGenerationBudgetPolicy {
    const val MAX_NORMAL_OUTPUT_TOKENS: Int = 512

    fun resolve(requestedMaxTokens: Int, modelContextTokens: Int): ChatGenerationBudget {
        val requested = requestedMaxTokens.coerceAtLeast(1)
        val context = modelContextTokens.takeIf { it > 0 }
        val contextOutputLimit = context?.let { (it / 2).coerceAtLeast(1) }
        val effective = minOf(
            requested,
            MAX_NORMAL_OUTPUT_TOKENS,
            contextOutputLimit ?: Int.MAX_VALUE,
        )
        return ChatGenerationBudget(
            requestedMaxTokens = requested,
            effectiveMaxTokens = effective,
            modelContextTokens = context,
        )
    }
}
