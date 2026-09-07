package xyz.normalwindow.runanywhere.ui.screens.diffusion

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.normalwindow.runanywhere.ui.components.EmptyState
import xyz.normalwindow.runanywhere.ui.components.GeneratingCanvas
import xyz.normalwindow.runanywhere.ui.components.ScreenLede
import xyz.normalwindow.runanywhere.ui.components.StatusNote
import xyz.normalwindow.runanywhere.ui.components.StatusTone
import xyz.normalwindow.runanywhere.ui.screens.models.ModelPickerCard
import xyz.normalwindow.runanywhere.ui.screens.models.ModelSelectionContext
import xyz.normalwindow.runanywhere.ui.screens.models.ModelSelectionSheet
import xyz.normalwindow.runanywhere.ui.screens.models.ModelSelectionViewModel
import xyz.normalwindow.runanywhere.data.settings.AppLocale
import xyz.normalwindow.runanywhere.ui.theme.AppMotion
import xyz.normalwindow.runanywhere.ui.theme.LocalDimens
import xyz.normalwindow.runanywhere.ui.theme.icons.RACIcons
import java.util.Locale

/**
 * Text-to-image UI. Model download/load uses the shared catalog
 * [ModelSelectionSheet] (same pattern as STT / Vision / Segmentation).
 */
@Composable
fun DiffusionScreen() {
    val dimens = LocalDimens.current
    val vm: DiffusionViewModel = viewModel()
    val modelVm: ModelSelectionViewModel =
        viewModel(factory = ModelSelectionViewModel.Factory(ModelSelectionContext.IMAGE_GENERATION))
    var showSheet by remember { mutableStateOf(false) }

    val model = modelVm.state.models.firstOrNull { it.id == modelVm.state.currentModelId }
    val modelLoaded = model != null
    val busy = modelVm.state.busyModelId != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(dimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingLg),
    ) {
        ScreenLede("Choose a text-to-image model from the catalog, then generate on-device.")

        // Lock the sheet during generation too: swapping the model under an
        // in-flight generateImage would pull native state out from under it.
        ModelPickerCard(
            label = "Image model",
            model = model,
            icon = RACIcons.Outline.Sparkles,
            busy = busy,
            enabled = !(busy || vm.isGenerating),
            onClick = { showSheet = true },
        )

        OutlinedTextField(
            value = vm.prompt,
            onValueChange = vm::onPromptChange,
            label = { Text(AppLocale.text("Prompt")) },
            placeholder = { Text(AppLocale.text("a lighthouse at dusk, long exposure")) },
            supportingText = {
                Text(
                    if (vm.prompt.isBlank()) {
                        AppLocale.text("Describe what you want to see — subject, then style.")
                    } else {
                        AppLocale.format("Generates at %d×%d on this device.", OUTPUT_EDGE, OUTPUT_EDGE)
                    },
                )
            },
            singleLine = false,
            enabled = !vm.isGenerating,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spacingMd)) {
            Button(
                onClick = vm::generate,
                enabled = modelLoaded && !vm.isGenerating && !busy && vm.prompt.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                Text(if (vm.image == null) AppLocale.text("Generate") else AppLocale.text("Generate again"))
            }
            // A long NPU generation with no way out is a trap. The engine call is not
            // interruptible, so this abandons the wait rather than claiming to stop the NPU.
            if (vm.isGenerating) {
                OutlinedButton(onClick = vm::cancel) { Text(AppLocale.text("Stop waiting")) }
            }
        }

        // Only the blocker that is actually in the way, phrased as the next step.
        if (!modelLoaded) {
            StatusNote("Choose an image model above to generate.", StatusTone.NEUTRAL)
        } else if (vm.prompt.isBlank()) {
            StatusNote("Describe an image to generate it.", StatusTone.NEUTRAL)
        }

        vm.error?.let { StatusNote(it, StatusTone.ERROR) }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(dimens.radiusLg),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(dimens.radiusLg)),
        ) {
            // Crossfaded and scaled in, so a finished picture arrives rather than replacing
            // the placeholder in one frame.
            AnimatedContent(
                targetState = when {
                    vm.isGenerating -> CanvasState.GENERATING
                    vm.image != null -> CanvasState.RESULT
                    else -> CanvasState.EMPTY
                },
                transitionSpec = {
                    (fadeIn(AppMotion.emphasis()) + scaleIn(AppMotion.emphasis(), initialScale = 0.96f))
                        .togetherWith(fadeOut(AppMotion.exit()))
                },
                label = "canvas",
            ) { state ->
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    when (state) {
                        CanvasState.GENERATING -> GeneratingCanvas(
                            label = AppLocale.text("Painting your image…"),
                            supporting = AppLocale.text("On-device diffusion runs a fixed number of steps; the first one is the slowest."),
                        )
                        CanvasState.RESULT -> vm.image?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = AppLocale.format("Generated image for: %s", vm.prompt),
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        CanvasState.EMPTY -> EmptyState(
                            icon = RACIcons.Outline.Sparkles,
                            title = AppLocale.text("Nothing generated yet"),
                            body = AppLocale.text("Your image appears here. Nothing is uploaded — the model paints it on this device."),
                        )
                    }
                }
            }
        }

        vm.lastLatencyMs?.let {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm),
            ) {
                Text(
                    text = AppLocale.format("Generated in %.1f s", it / 1000.0),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showSheet) {
        ModelSelectionSheet(viewModel = modelVm, onDismiss = { showSheet = false })
    }
}

/** What the square canvas is currently showing. */
private enum class CanvasState { EMPTY, GENERATING, RESULT }

/** Matches the `ImageOptions` the view model requests. */
private const val OUTPUT_EDGE = 256
