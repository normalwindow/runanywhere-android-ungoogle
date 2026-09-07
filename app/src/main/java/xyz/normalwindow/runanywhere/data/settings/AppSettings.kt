package xyz.normalwindow.runanywhere.data.settings

// Default assistant persona. The small on-device instruct models (LFM2.5-230M, Qwen3.5-0.8B) default
// to a defensive/confused persona with NO system prompt — they refuse to use the user's own name
// ("I don't have personal information like names") and misread simple statements. A short, explicit
// system prompt fixes context use on device (single-turn recall goes from a refusal to "Bob is your
// name"). Kept concise to spare the tight 1024-token context. Users can override in Settings.
const val DEFAULT_SYSTEM_PROMPT =
    "You are a helpful assistant. Give concise, direct answers. Remember what the user tells you " +
        "in the conversation, such as their name, and use it. Address the user as \"you\" and refer " +
        "to yourself as \"I\"."

enum class AppLanguage(val tag: String, val label: String) {
    SYSTEM("", "System default"),
    ENGLISH("en", "English"),
    CHINESE("zh-CN", "Simplified Chinese"),
}

enum class ModelDownloadSource(val label: String) {
    HUGGING_FACE("Hugging Face"),
    CHINA_MIRROR("China mirror (hf-mirror.com)"),
    CUSTOM("Custom source"),
}

enum class AppThemeMode(val label: String) {
    SYSTEM("System default"),
    LIGHT("Light"),
    DARK("Dark"),
}

enum class AppThemeColor(val label: String) {
    TEAL("Teal"),
    ORANGE("Orange"),
}

enum class WebSearchEngine(val label: String) {
    BING("Bing"),
    DUCKDUCKGO("DuckDuckGo"),
}

data class AppSettings(
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1024,
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val streaming: Boolean = true,
    // Show model-emitted reasoning by default. Users can still disable it in Settings for smaller
    // context windows or faster answers.
    val disableThinking: Boolean = false,
    val toolCallingEnabled: Boolean = false,
    val webSearchConsentScope: String = "",
    val hfToken: String = "",
    val language: AppLanguage = AppLanguage.SYSTEM,
    val modelDownloadSource: ModelDownloadSource = ModelDownloadSource.HUGGING_FACE,
    val customDownloadBaseUrl: String = "",
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val themeColor: AppThemeColor = AppThemeColor.TEAL,
    val webSearchEngine: WebSearchEngine = WebSearchEngine.BING,
)
