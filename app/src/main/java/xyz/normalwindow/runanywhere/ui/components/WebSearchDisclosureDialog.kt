package xyz.normalwindow.runanywhere.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import xyz.normalwindow.runanywhere.BuildConfig
import xyz.normalwindow.runanywhere.data.settings.AppLanguage
import xyz.normalwindow.runanywhere.data.settings.AppLocale
import xyz.normalwindow.runanywhere.data.settings.SettingsRepository
import xyz.normalwindow.runanywhere.data.settings.WebSearchConsentPolicy
import xyz.normalwindow.runanywhere.data.settings.WebSearchEngine

internal fun webSearchDisclosureText(
    backendUrl: String,
    engine: WebSearchEngine = SettingsRepository.settings.webSearchEngine,
    language: AppLanguage = SettingsRepository.settings.language,
): String {
    val route = WebSearchConsentPolicy.routeFor(backendUrl, engine)
        ?: return AppLocale.text(
            "Web search is unavailable because this build does not have a valid secure search destination.",
            language,
        )
    return if (!route.sendsPersistentDeviceId) {
        AppLocale.format(
            "When Web & tools is on, a model-generated search query based on your message is sent directly to %s to retrieve current results. As with any internet request, those services can see your device's IP address. No persistent device ID is sent. No query is sent until you choose Allow. Turn Web & tools off to revoke this choice.",
            route.destinationLabel,
            language = language,
        )
    } else {
        AppLocale.format(
            "When Web & tools is on, a model-generated search query based on your message and this app's persistent device ID are sent to %s to retrieve current results and prevent abuse. As with any internet request, that service can also see your device's IP address. No query is sent until you choose Allow. Turn Web & tools off to revoke this choice.",
            route.destinationLabel,
            language = language,
        )
    }
}

@Composable
internal fun WebSearchDisclosureDialog(
    onAllow: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppLocale.text("Allow web search?")) },
        text = {
            Text(webSearchDisclosureText(backendUrl = BuildConfig.WEB_SEARCH_URL))
        },
        confirmButton = {
            TextButton(onClick = onAllow) { Text(AppLocale.text("Allow")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(AppLocale.text("Not now")) }
        },
    )
}
