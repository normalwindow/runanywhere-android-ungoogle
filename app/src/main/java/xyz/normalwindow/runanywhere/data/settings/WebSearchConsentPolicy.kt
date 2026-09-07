package xyz.normalwindow.runanywhere.data.settings

import java.net.URI

internal data class WebSearchConsentState(
    val toolsEnabled: Boolean,
    val acceptedScope: String,
    val currentScope: String?,
)

internal data class WebSearchTransferRoute(
    val scope: String,
    val destinationLabel: String,
    val sendsPersistentDeviceId: Boolean,
)

/** Fail-closed, versioned gate shared by every Web & tools entry point. */
internal object WebSearchConsentPolicy {
    private const val DISCLOSURE_VERSION = "v2"

    fun routeFor(
        backendUrl: String,
        engine: WebSearchEngine = WebSearchEngine.BING,
    ): WebSearchTransferRoute? {
        if (backendUrl.isBlank()) {
            return when (engine) {
                WebSearchEngine.BING -> WebSearchTransferRoute(
                    scope = "$DISCLOSURE_VERSION:direct:bing",
                    destinationLabel = "Bing (www.bing.com)",
                    sendsPersistentDeviceId = false,
                )
                WebSearchEngine.DUCKDUCKGO -> WebSearchTransferRoute(
                    scope = "$DISCLOSURE_VERSION:direct:duckduckgo",
                    destinationLabel = "DuckDuckGo (lite.duckduckgo.com and api.duckduckgo.com)",
                    sendsPersistentDeviceId = false,
                )
            }
        }

        val uri = runCatching { URI(backendUrl.trim()) }.getOrNull() ?: return null
        if (
            !uri.scheme.equals("https", ignoreCase = true) ||
            uri.host.isNullOrBlank() ||
            uri.userInfo != null
        ) {
            return null
        }
        val normalizedHost = uri.host.lowercase()
        val effectivePort = if (uri.port == -1) 443 else uri.port
        val destination = if (effectivePort == 443) normalizedHost else "$normalizedHost:$effectivePort"
        return WebSearchTransferRoute(
            scope = "$DISCLOSURE_VERSION:proxy:$destination",
            destinationLabel = destination,
            sendsPersistentDeviceId = true,
        )
    }

    fun permitsTransfer(state: WebSearchConsentState): Boolean =
        state.toolsEnabled &&
            !state.currentScope.isNullOrBlank() &&
            state.acceptedScope == state.currentScope

    fun accept(currentScope: String?): WebSearchConsentState =
        if (currentScope.isNullOrBlank()) {
            revoke()
        } else {
            WebSearchConsentState(
                toolsEnabled = true,
                acceptedScope = currentScope,
                currentScope = currentScope,
            )
        }

    fun revoke(): WebSearchConsentState =
        WebSearchConsentState(toolsEnabled = false, acceptedScope = "", currentScope = null)
}
