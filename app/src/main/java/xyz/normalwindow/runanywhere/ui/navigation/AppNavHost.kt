package xyz.normalwindow.runanywhere.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import xyz.normalwindow.runanywhere.ui.screens.diffusion.DiffusionScreen
import xyz.normalwindow.runanywhere.ui.screens.chat.ChatScreen
import xyz.normalwindow.runanywhere.ui.screens.chat.ChatViewModel
import xyz.normalwindow.runanywhere.ui.screens.benchmark.BenchmarkDetailScreen
import xyz.normalwindow.runanywhere.ui.screens.benchmark.BenchmarkScreen
import xyz.normalwindow.runanywhere.ui.screens.cloud.CloudProvidersScreen
import xyz.normalwindow.runanywhere.ui.screens.diarization.DiarizationScreen
import xyz.normalwindow.runanywhere.ui.screens.more.MoreScreen
import xyz.normalwindow.runanywhere.ui.screens.ocr.OcrScreen
import xyz.normalwindow.runanywhere.ui.screens.rag.RagScreen
import xyz.normalwindow.runanywhere.ui.screens.segmentation.SegmentationScreen
import xyz.normalwindow.runanywhere.ui.screens.settings.SettingsScreen
import xyz.normalwindow.runanywhere.ui.screens.solutions.SolutionsScreen
import xyz.normalwindow.runanywhere.ui.screens.stt.SttScreen
import xyz.normalwindow.runanywhere.ui.screens.tools.ToolsScreen
import xyz.normalwindow.runanywhere.ui.screens.tts.TtsScreen
import xyz.normalwindow.runanywhere.ui.screens.vad.VadScreen
import xyz.normalwindow.runanywhere.ui.screens.vision.VisionScreen
import xyz.normalwindow.runanywhere.ui.screens.voice.VoiceScreen
import xyz.normalwindow.runanywhere.ui.theme.AppMotion

@Composable
fun AppNavHost(
    navController: NavHostController,
    chatViewModel: ChatViewModel,
    onOpenModels: () -> Unit,
    isModelSheetVisible: Boolean,
    onOpenVision: () -> Unit,
    onOpenVoice: () -> Unit,
    onOpenAdvanced: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Chat,
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(AppMotion.emphasis()) { it / 4 } +
                fadeIn(AppMotion.standard())
        },
        exitTransition = {
            slideOutHorizontally(AppMotion.exit()) { -it / 4 } +
                fadeOut(AppMotion.exit())
        },
        popEnterTransition = {
            slideInHorizontally(AppMotion.emphasis()) { -it / 4 } +
                fadeIn(AppMotion.standard())
        },
        popExitTransition = {
            slideOutHorizontally(AppMotion.exit()) { it / 4 } +
                fadeOut(AppMotion.exit())
        },
    ) {
        composable<Chat> {
            ChatScreen(
                viewModel = chatViewModel,
                onOpenModels = onOpenModels,
                onOpenVision = onOpenVision,
                onOpenVoice = onOpenVoice,
                onOpenAdvanced = onOpenAdvanced,
            )
        }
        composable<Voice> { VoiceScreen() }
        composable<More> { MoreScreen(onNavigate = { navController.navigate(it) }) }
        composable<Settings> {
            SettingsScreen(
                onOpenModels = onOpenModels,
                onOpenAdvanced = onOpenAdvanced,
            )
        }
        composable<Tools> { ToolsScreen() }
        composable<Tts> { TtsScreen() }
        composable<Diffusion> { DiffusionScreen() }
        composable<Stt> { SttScreen() }
        composable<Vad> { VadScreen() }
        composable<Vision> { entry ->
            VisionScreen(openLiveCamera = entry.toRoute<Vision>().openLiveCamera)
        }
        composable<Segmentation> { SegmentationScreen() }
        composable<Ocr> { OcrScreen() }
        composable<Diarization> { DiarizationScreen() }
        composable<Documents> { RagScreen() }
        composable<Solutions> { SolutionsScreen() }
        composable<CloudProviders> { CloudProvidersScreen() }
        composable<Benchmarks> {
            BenchmarkScreen(
                onOpenDetail = { navController.navigate(BenchmarkDetail(it)) },
                onOpenModels = onOpenModels,
                isModelSheetVisible = isModelSheetVisible,
            )
        }
        composable<BenchmarkDetail> { entry ->
            BenchmarkDetailScreen(runId = entry.toRoute<BenchmarkDetail>().runId)
        }
    }
}
