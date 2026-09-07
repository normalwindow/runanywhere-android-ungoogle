package xyz.normalwindow.runanywhere.ui.screens.tools

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import xyz.normalwindow.runanywhere.data.settings.SettingsRepository
import xyz.normalwindow.runanywhere.data.settings.WebSearchConsentPolicy
import xyz.normalwindow.runanywhere.data.settings.WebSearchConsentState
import xyz.normalwindow.runanywhere.data.settings.WebSearchEngine
import xyz.normalwindow.runanywhere.util.RACLog
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.public.api.llm
import com.runanywhere.sdk.public.types.RAToolDefinition
import kotlinx.coroutines.launch

// Mirrors iOS ToolSettingsViewModel: a persisted master toggle plus the list
// of tools registered with the SDK. Tools themselves are registered at app
// boot (BuiltInTools), so this screen only reads the registry.
class ToolsViewModel : ViewModel() {

    val toolCallingEnabled: Boolean
        get() = WebSearchConsentPolicy.permitsTransfer(
            WebSearchConsentState(
                toolsEnabled = SettingsRepository.settings.toolCallingEnabled,
                acceptedScope = SettingsRepository.settings.webSearchConsentScope,
                currentScope = SettingsRepository.currentWebSearchRoute()?.scope,
            ),
        )

    val webSearchEngine: WebSearchEngine
        get() = SettingsRepository.settings.webSearchEngine

    var showWebSearchDisclosure by mutableStateOf(false)
        private set

    var tools by mutableStateOf<List<RAToolDefinition>>(emptyList())
        private set

    init {
        viewModelScope.launch {
            tools = runCatching { RunAnywhere.llm.tools.list() }
                .onFailure { RACLog.w("failed to load registered tools: ${it.message}") }
                .getOrDefault(emptyList())
        }
    }

    fun setEnabled(value: Boolean) {
        if (value) {
            showWebSearchDisclosure = true
        } else {
            SettingsRepository.setWebToolsTransferEnabled(false)
        }
    }

    fun setWebSearchEngine(value: WebSearchEngine) {
        val wasEnabled = toolCallingEnabled
        SettingsRepository.setWebSearchEngine(value)
        if (wasEnabled && !toolCallingEnabled) {
            showWebSearchDisclosure = true
        }
    }

    fun acceptWebSearchDisclosure() {
        SettingsRepository.setWebToolsTransferEnabled(true)
        showWebSearchDisclosure = false
    }

    fun dismissWebSearchDisclosure() {
        showWebSearchDisclosure = false
    }
}
