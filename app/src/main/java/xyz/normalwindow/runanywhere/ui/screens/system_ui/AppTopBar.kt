package xyz.normalwindow.runanywhere.ui.screens.system_ui

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import xyz.normalwindow.runanywhere.ui.navigation.BenchmarkDetail
import xyz.normalwindow.runanywhere.ui.navigation.Benchmarks
import xyz.normalwindow.runanywhere.ui.navigation.Chat
import xyz.normalwindow.runanywhere.ui.navigation.CloudProviders
import xyz.normalwindow.runanywhere.ui.navigation.Diarization
import xyz.normalwindow.runanywhere.ui.navigation.Diffusion
import xyz.normalwindow.runanywhere.ui.navigation.Documents
import xyz.normalwindow.runanywhere.ui.navigation.More
import xyz.normalwindow.runanywhere.ui.navigation.Ocr
import xyz.normalwindow.runanywhere.ui.navigation.Segmentation
import xyz.normalwindow.runanywhere.ui.navigation.Settings
import xyz.normalwindow.runanywhere.ui.navigation.Solutions
import xyz.normalwindow.runanywhere.ui.navigation.Stt
import xyz.normalwindow.runanywhere.ui.navigation.Tools
import xyz.normalwindow.runanywhere.ui.navigation.Tts
import xyz.normalwindow.runanywhere.ui.navigation.Vad
import xyz.normalwindow.runanywhere.ui.navigation.Vision
import xyz.normalwindow.runanywhere.ui.navigation.Voice
import xyz.normalwindow.runanywhere.ui.screens.chat.ChatTopBar
import xyz.normalwindow.runanywhere.ui.theme.icons.RACIcons
import xyz.normalwindow.runanywhere.data.settings.AppLocale
import com.runanywhere.sdk.public.types.RAModelInfo
import com.runanywhere.sdk.public.connect.ConnectModel

// Pure route dispatcher: picks each screen's own top bar. No UI defined here.
@Composable
fun AppTopBar(
    destination: NavDestination?,
    model: RAModelInfo?,
    hostedModel: ConnectModel?,
    conversationModelName: String?,
    generating: Boolean,
    loraActive: Boolean,
    hasMessages: Boolean,
    onModelClick: () -> Unit,
    onNewChat: () -> Unit,
    onHistory: () -> Unit,
    onLora: () -> Unit,
    onDetails: () -> Unit,
    onMenu: () -> Unit,
    onNavigateBack: () -> Unit,
    showMenu: Boolean,
    canNavigateBack: Boolean,
) {
    when {
        destination == null -> Unit
        destination.hasRoute<Chat>() -> ChatTopBar(
            model = model,
            hostedModel = hostedModel,
            conversationModelName = conversationModelName,
            generating = generating,
            loraActive = loraActive,
            hasMessages = hasMessages,
            onModelClick = onModelClick,
            onNewChat = onNewChat,
            onHistory = onHistory,
            onLora = onLora,
            onDetails = onDetails,
            onMenu = onMenu,
            showMenu = showMenu,
        )
        destination.hasRoute<Voice>() -> StandardTopBar(AppLocale.text("Talk"), showMenu, onMenu, canNavigateBack, onNavigateBack)
        destination.hasRoute<More>() -> StandardTopBar(AppLocale.text("Advanced"), showMenu, onMenu, canNavigateBack, onNavigateBack)
        destination.hasRoute<Settings>() -> StandardTopBar(AppLocale.text("Settings"), showMenu, onMenu, canNavigateBack, onNavigateBack)
        destination.hasRoute<Tools>() -> StandardTopBar(AppLocale.text("Web & tools"), showMenu, onMenu, canNavigateBack, onNavigateBack)
        destination.hasRoute<Tts>() -> StandardTopBar(AppLocale.text("Read aloud"), showMenu, onMenu, canNavigateBack, onNavigateBack)
        destination.hasRoute<Stt>() -> StandardTopBar(AppLocale.text("Transcription"), showMenu, onMenu, canNavigateBack, onNavigateBack)
        destination.hasRoute<Vad>() -> StandardTopBar(AppLocale.text("Voice activity"), showMenu, onMenu, canNavigateBack, onNavigateBack)
        destination.hasRoute<Vision>() -> StandardTopBar(AppLocale.text("Images & live"), showMenu, onMenu, canNavigateBack, onNavigateBack)
        destination.hasRoute<Documents>() -> StandardTopBar(AppLocale.text("Documents"), showMenu, onMenu, canNavigateBack, onNavigateBack)
        destination.hasRoute<Ocr>() -> StandardTopBar(AppLocale.text("Document OCR"), showMenu, onMenu, canNavigateBack, onNavigateBack)
        destination.hasRoute<Segmentation>() -> StandardTopBar(AppLocale.text("Segmentation"), showMenu, onMenu, canNavigateBack, onNavigateBack)
        destination.hasRoute<Diffusion>() -> StandardTopBar(AppLocale.text("Image generation"), showMenu, onMenu, canNavigateBack, onNavigateBack)
        destination.hasRoute<Diarization>() -> StandardTopBar(AppLocale.text("Diarization"), showMenu, onMenu, canNavigateBack, onNavigateBack)
        destination.hasRoute<Solutions>() -> StandardTopBar(AppLocale.text("Solutions"), showMenu, onMenu, canNavigateBack, onNavigateBack)
        destination.hasRoute<CloudProviders>() -> StandardTopBar(AppLocale.text("Cloud providers"), showMenu, onMenu, canNavigateBack, onNavigateBack)
        destination.hasRoute<Benchmarks>() -> StandardTopBar(AppLocale.text("Benchmarks"), showMenu, onMenu, canNavigateBack, onNavigateBack)
        destination.hasRoute<BenchmarkDetail>() -> StandardTopBar(AppLocale.text("Run details"), showMenu, onMenu, canNavigateBack, onNavigateBack)
        else -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StandardTopBar(
    title: String,
    showMenu: Boolean = false,
    onMenu: () -> Unit = {},
    canNavigateBack: Boolean = false,
    onNavigateBack: () -> Unit = {},
) {
    CenterAlignedTopAppBar(
        title = { Text(AppLocale.text(title)) },
        navigationIcon = {
            when {
                canNavigateBack -> {
                    IconButton(onClick = onNavigateBack) {
                        Icon(RACIcons.Outline.ChevronLeft, contentDescription = AppLocale.text("Go back"))
                    }
                }
                showMenu -> {
                    IconButton(onClick = onMenu) {
                        Icon(RACIcons.Outline.Menu, contentDescription = AppLocale.text("Open menu"))
                    }
                }
            }
        },
    )
}
