package xyz.normalwindow.runanywhere

import androidx.test.ext.junit.runners.AndroidJUnit4
import xyz.normalwindow.runanywhere.data.settings.WebSearchEngine
import xyz.normalwindow.runanywhere.state.GlobalState
import xyz.normalwindow.runanywhere.tools.WebSearchTool
import com.runanywhere.sdk.public.extensions.LLM.string
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WebSearchToolE2ETest {
    @Test
    fun developerKeylessPublicFallbackReturnsRealHttpSource() = runBlocking {
        val result = WebSearchTool.search(
            query = "Qualcomm Hexagon v81",
            backendUrl = "",
            engine = WebSearchEngine.BING,
        )

        assertFalse(result.containsKey("error"))
        val resultCount = result["result_count"]?.string?.toIntOrNull() ?: 0
        assertTrue("Developer fallback must return at least one real web result", resultCount > 0)
        val source = result["source_url"]?.string.orEmpty()
        assertTrue(source.startsWith("https://") || source.startsWith("http://"))
    }

    @Test
    fun configuredSearchProxyReturnsRealSource() = runBlocking {
        assumeTrue(
            "RUNANYWHERE_WEB_SEARCH_URL is required for the production web-search E2E",
            BuildConfig.WEB_SEARCH_URL.isNotBlank(),
        )
        GlobalState.awaitBootstrapComplete()
        val result = WebSearchTool.search("Qualcomm Hexagon v81")
        assertFalse(result.containsKey("error"))
        val resultCount = result["result_count"]?.string?.toIntOrNull() ?: 0
        assertTrue("Search must return at least one real web result", resultCount > 0)
        val source = result["source_url"]?.string.orEmpty()
        assertTrue(source.startsWith("https://") || source.startsWith("http://"))
    }
}
