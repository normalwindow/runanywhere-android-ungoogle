package xyz.normalwindow.runanywhere.data.settings

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object ModelDownloadSourceResolver {
    fun resolve(url: String): String {
        val parsed = url.toHttpUrlOrNull() ?: return url
        if (parsed.host != "huggingface.co" && parsed.host != "www.huggingface.co") return url
        val settings = SettingsRepository.settings
        val customBase = settings.customDownloadBaseUrl.toHttpUrlOrNull()
        if (settings.modelDownloadSource == ModelDownloadSource.CUSTOM &&
            (customBase == null || customBase.scheme != "https")
        ) return url
        val host = when (settings.modelDownloadSource) {
            ModelDownloadSource.HUGGING_FACE -> return url
            ModelDownloadSource.CHINA_MIRROR -> "hf-mirror.com"
            ModelDownloadSource.CUSTOM -> customBase?.host ?: return url
        }
        val prefix = customBase?.encodedPath?.trimEnd('/') ?: ""
        val builder = (customBase ?: parsed).newBuilder()
            .encodedPath(prefix + parsed.encodedPath)
            .encodedQuery(parsed.encodedQuery)
        return builder.host(host).build().toString()
    }
}