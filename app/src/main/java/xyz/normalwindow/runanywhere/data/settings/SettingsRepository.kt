package xyz.normalwindow.runanywhere.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import xyz.normalwindow.runanywhere.BuildConfig
import xyz.normalwindow.runanywhere.data.security.SecureStringPreferences
import xyz.normalwindow.runanywhere.data.security.migrateSensitiveString
import xyz.normalwindow.runanywhere.data.security.securePreferences
import xyz.normalwindow.runanywhere.util.RACLog

object SettingsRepository {
    private const val PREFS = "app_settings"
    private const val SECURE_PREFS = "app_secure_settings"
    private const val KEY_TEMPERATURE = "temperature"
    private const val KEY_MAX_TOKENS = "max_tokens"
    private const val KEY_SYSTEM_PROMPT = "system_prompt"
    private const val KEY_STREAMING = "streaming"
    private const val KEY_DISABLE_THINKING = "disable_thinking"
    private const val KEY_TOOL_CALLING = "tool_calling_enabled"
    private const val LEGACY_KEY_WEB_SEARCH_DISCLOSURE = "web_search_disclosure_accepted"
    private const val KEY_WEB_SEARCH_CONSENT_SCOPE = "web_search_consent_scope_v2"
    private const val KEY_HF_TOKEN = "hf_token"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_MODEL_DOWNLOAD_SOURCE = "model_download_source"
    private const val KEY_CUSTOM_DOWNLOAD_BASE_URL = "custom_download_base_url"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_THEME_COLOR = "theme_color"
    private const val KEY_WEB_SEARCH_ENGINE = "web_search_engine"

    private var prefs: SharedPreferences? = null
    private var securePrefs: SecureStringPreferences? = null

    var settings: AppSettings by mutableStateOf(AppSettings())
        private set

    fun initialize(context: Context) = initialize(context, ::securePreferences)

    internal fun initialize(
        context: Context,
        securePreferencesFactory: (Context, String) -> SecureStringPreferences,
    ) {
        if (prefs != null) return
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val secure = runCatching { securePreferencesFactory(context, SECURE_PREFS) }
            .onFailure { RACLog.w("Secure settings unavailable; credentials cannot be loaded or saved") }
            .getOrNull()
        initializePreferences(p, secure)
    }

    internal fun initializeForTesting(
        legacyPreferences: SharedPreferences,
        securePreferences: SecureStringPreferences?,
    ) {
        if (prefs != null) return
        initializePreferences(legacyPreferences, securePreferences)
    }

    private fun initializePreferences(
        p: SharedPreferences,
        secure: SecureStringPreferences?,
    ) {
        val hfToken = if (secure == null) {
            ""
        } else {
            val migration = migrateSensitiveString(
                readSecure = { secure.getString(KEY_HF_TOKEN) },
                readLegacy = { p.getString(KEY_HF_TOKEN, null) },
                writeSecure = { secure.putString(KEY_HF_TOKEN, it) },
                removeLegacy = { p.edit().remove(KEY_HF_TOKEN).commit() },
                normalize = { it.trim() },
                allowBlankSecureValue = true,
            )
            if (migration.failure != null) {
                RACLog.w("hf_token_migration_${migration.failure.name.lowercase()}")
            }
            securePrefs = secure
            migration.value.orEmpty()
        }
        prefs = p
        val webSearchEngine = p.getString(KEY_WEB_SEARCH_ENGINE, WebSearchEngine.BING.name)?.let { value ->
            runCatching { WebSearchEngine.valueOf(value) }.getOrDefault(WebSearchEngine.BING)
        } ?: WebSearchEngine.BING
        val acceptedWebSearchScope = p.getString(KEY_WEB_SEARCH_CONSENT_SCOPE, "").orEmpty()
        val currentWebSearchScope =
            WebSearchConsentPolicy.routeFor(BuildConfig.WEB_SEARCH_URL, webSearchEngine)?.scope
        val toolsEnabled = WebSearchConsentPolicy.permitsTransfer(
            WebSearchConsentState(
                toolsEnabled = p.getBoolean(KEY_TOOL_CALLING, false),
                acceptedScope = acceptedWebSearchScope,
                currentScope = currentWebSearchScope,
            ),
        )
        // A boolean accepted under the older generic disclosure is not enough
        // to authorize a new proxy/device-ID route. Route or wording changes
        // fail closed and require a fresh affirmative choice.
        p.edit()
            .remove(LEGACY_KEY_WEB_SEARCH_DISCLOSURE)
            .putBoolean(KEY_TOOL_CALLING, toolsEnabled)
            .apply()

        settings = AppSettings(
            temperature = p.getFloat(KEY_TEMPERATURE, AppSettings().temperature),
            maxTokens = p.getInt(KEY_MAX_TOKENS, AppSettings().maxTokens),
            systemPrompt = p.getString(KEY_SYSTEM_PROMPT, AppSettings().systemPrompt).orEmpty(),
            streaming = p.getBoolean(KEY_STREAMING, true),
            disableThinking = p.getBoolean(KEY_DISABLE_THINKING, AppSettings().disableThinking),
            toolCallingEnabled = toolsEnabled,
            webSearchConsentScope = acceptedWebSearchScope,
            hfToken = hfToken,
            language = p.getString(KEY_LANGUAGE, AppLanguage.SYSTEM.name)?.let { value ->
                runCatching { AppLanguage.valueOf(value) }.getOrDefault(AppLanguage.SYSTEM)
            } ?: AppLanguage.SYSTEM,
            modelDownloadSource = p.getString(KEY_MODEL_DOWNLOAD_SOURCE, ModelDownloadSource.HUGGING_FACE.name)
                ?.let { value -> runCatching { ModelDownloadSource.valueOf(value) }.getOrDefault(ModelDownloadSource.HUGGING_FACE) }
                ?: ModelDownloadSource.HUGGING_FACE,
            customDownloadBaseUrl = p.getString(KEY_CUSTOM_DOWNLOAD_BASE_URL, "").orEmpty(),
            themeMode = p.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)?.let { value ->
                runCatching { AppThemeMode.valueOf(value) }.getOrDefault(AppThemeMode.SYSTEM)
            } ?: AppThemeMode.SYSTEM,
            themeColor = p.getString(KEY_THEME_COLOR, AppThemeColor.TEAL.name)?.let { value ->
                runCatching { AppThemeColor.valueOf(value) }.getOrDefault(AppThemeColor.TEAL)
            } ?: AppThemeColor.TEAL,
            webSearchEngine = webSearchEngine,
        )
    }

    internal fun resetForTesting() {
        prefs = null
        securePrefs = null
        settings = AppSettings()
    }

    fun setTemperature(value: Float) {
        settings = settings.copy(temperature = value)
        prefs?.edit()?.putFloat(KEY_TEMPERATURE, value)?.apply()
    }

    fun setMaxTokens(value: Int) {
        settings = settings.copy(maxTokens = value)
        prefs?.edit()?.putInt(KEY_MAX_TOKENS, value)?.apply()
    }

    fun setSystemPrompt(value: String) {
        settings = settings.copy(systemPrompt = value)
        prefs?.edit()?.putString(KEY_SYSTEM_PROMPT, value)?.apply()
    }

    fun setStreaming(value: Boolean) {
        settings = settings.copy(streaming = value)
        prefs?.edit()?.putBoolean(KEY_STREAMING, value)?.apply()
    }

    fun setDisableThinking(value: Boolean) {
        settings = settings.copy(disableThinking = value)
        prefs?.edit()?.putBoolean(KEY_DISABLE_THINKING, value)?.apply()
    }

    fun setLanguage(value: AppLanguage) {
        settings = settings.copy(language = value)
        prefs?.edit()?.putString(KEY_LANGUAGE, value.name)?.apply()
        AppLocale.apply(value)
    }

    fun setModelDownloadSource(value: ModelDownloadSource) {
        settings = settings.copy(modelDownloadSource = value)
        prefs?.edit()?.putString(KEY_MODEL_DOWNLOAD_SOURCE, value.name)?.apply()
    }

    fun setCustomDownloadBaseUrl(value: String) {
        val normalized = value.trim().trimEnd('/')
        settings = settings.copy(customDownloadBaseUrl = normalized)
        prefs?.edit()?.putString(KEY_CUSTOM_DOWNLOAD_BASE_URL, normalized)?.apply()
    }

    fun setThemeMode(value: AppThemeMode) {
        settings = settings.copy(themeMode = value)
        prefs?.edit()?.putString(KEY_THEME_MODE, value.name)?.apply()
    }

    internal fun currentWebSearchRoute() =
        WebSearchConsentPolicy.routeFor(BuildConfig.WEB_SEARCH_URL, settings.webSearchEngine)

    fun setThemeColor(value: AppThemeColor) {
        settings = settings.copy(themeColor = value)
        prefs?.edit()?.putString(KEY_THEME_COLOR, value.name)?.apply()
    }

    /**
     * Changing engines also changes the consent scope. If tools are currently
     * enabled, fail closed until the user re-allows the new destination.
     */
    fun setWebSearchEngine(value: WebSearchEngine) {
        if (settings.webSearchEngine == value) return
        val nextScope = WebSearchConsentPolicy.routeFor(BuildConfig.WEB_SEARCH_URL, value)?.scope
        val keepTransfer = WebSearchConsentPolicy.permitsTransfer(
            WebSearchConsentState(
                toolsEnabled = settings.toolCallingEnabled,
                acceptedScope = settings.webSearchConsentScope,
                currentScope = nextScope,
            ),
        )
        val acceptedScope = if (keepTransfer) settings.webSearchConsentScope else ""
        settings = settings.copy(
            webSearchEngine = value,
            toolCallingEnabled = keepTransfer,
            webSearchConsentScope = acceptedScope,
        )
        prefs?.edit()?.also { editor ->
            editor.putString(KEY_WEB_SEARCH_ENGINE, value.name)
            editor.putBoolean(KEY_TOOL_CALLING, keepTransfer)
            if (acceptedScope.isBlank()) {
                editor.remove(KEY_WEB_SEARCH_CONSENT_SCOPE)
            } else {
                editor.putString(KEY_WEB_SEARCH_CONSENT_SCOPE, acceptedScope)
            }
        }?.apply()
    }

    /** Enables transfer only after disclosure acceptance; disabling also revokes that acceptance. */
    fun setWebToolsTransferEnabled(value: Boolean) {
        val currentScope = WebSearchConsentPolicy.routeFor(
            BuildConfig.WEB_SEARCH_URL,
            settings.webSearchEngine,
        )?.scope
        val next = if (value) {
            WebSearchConsentPolicy.accept(currentScope)
        } else {
            WebSearchConsentPolicy.revoke()
        }
        settings = settings.copy(
            toolCallingEnabled = next.toolsEnabled,
            webSearchConsentScope = next.acceptedScope,
        )
        prefs?.edit()?.also { editor ->
            editor.putBoolean(KEY_TOOL_CALLING, next.toolsEnabled)
            editor.remove(LEGACY_KEY_WEB_SEARCH_DISCLOSURE)
            if (next.acceptedScope.isBlank()) {
                editor.remove(KEY_WEB_SEARCH_CONSENT_SCOPE)
            } else {
                editor.putString(KEY_WEB_SEARCH_CONSENT_SCOPE, next.acceptedScope)
            }
        }?.apply()
    }

    /**
     * Durably replaces the Hugging Face token in encrypted storage.
     *
     * The observable settings state changes only after the synchronous commit
     * succeeds and its value is read back. A blank token is stored as an
     * authoritative encrypted tombstone so a stale plaintext migration source
     * cannot resurrect a cleared credential. Legacy plaintext cleanup happens
     * only after that verified secure write.
     */
    fun setHfToken(value: String): Result<Unit> {
        val secure = securePrefs
            ?: return Result.failure(IllegalStateException("Secure credential storage is unavailable"))
        val normalized = value.trim()
        val committed = runCatching {
            secure.putString(KEY_HF_TOKEN, normalized)
        }.getOrElse {
            return Result.failure(IllegalStateException("Could not write secure credential storage", it))
        }
        if (!committed) {
            return Result.failure(IllegalStateException("Could not commit secure credential storage"))
        }
        val verified = runCatching { secure.getString(KEY_HF_TOKEN) }
            .getOrElse {
                return Result.failure(
                    IllegalStateException("Could not verify secure credential storage", it),
                )
            }
        if (verified != normalized) {
            return Result.failure(IllegalStateException("Could not verify secure credential storage"))
        }

        settings = settings.copy(hfToken = normalized)
        val legacy = prefs
            ?: return Result.failure(IllegalStateException("Legacy credential storage is unavailable"))
        val legacyRemoved = runCatching {
            legacy.edit().remove(KEY_HF_TOKEN).commit()
        }.getOrElse {
            return Result.failure(IllegalStateException("Could not remove legacy credential storage", it))
        }
        if (!legacyRemoved) {
            return Result.failure(IllegalStateException("Could not commit legacy credential cleanup"))
        }
        return Result.success(Unit)
    }

    private inline fun pSafeEdit(block: SharedPreferences.Editor.() -> Unit) {
        prefs?.edit()?.apply(block)?.apply()
    }
}
