package xyz.normalwindow.runanywhere.ui.screens.ocr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.normalwindow.runanywhere.ui.components.EmptyState
import xyz.normalwindow.runanywhere.ui.components.EmptyStateAction
import xyz.normalwindow.runanywhere.ui.components.ScreenLede
import xyz.normalwindow.runanywhere.ui.components.SectionCard
import xyz.normalwindow.runanywhere.ui.components.SectionHeader
import xyz.normalwindow.runanywhere.ui.components.StatusNote
import xyz.normalwindow.runanywhere.ui.components.StatusTone
import xyz.normalwindow.runanywhere.ui.screens.models.ModelPickerCard
import xyz.normalwindow.runanywhere.ui.screens.models.ModelSelectionContext
import xyz.normalwindow.runanywhere.ui.screens.models.ModelSelectionSheet
import xyz.normalwindow.runanywhere.ui.screens.models.ModelSelectionViewModel
import xyz.normalwindow.runanywhere.ui.theme.AppMotion
import xyz.normalwindow.runanywhere.ui.theme.LocalDimens
import xyz.normalwindow.runanywhere.ui.theme.icons.RACIcons
import xyz.normalwindow.runanywhere.data.settings.AppLocale
import java.util.Locale

/**
 * Document OCR experience for Nemotron OCR / Parse. Model download/load uses
 * the shared catalog picker; the file picker is only for the document photo.
 */
@Composable
fun OcrScreen(viewModel: OcrViewModel = viewModel()) {
    val dimens = LocalDimens.current
    val context = LocalContext.current
    val modelVm: ModelSelectionViewModel =
        viewModel(factory = ModelSelectionViewModel.Factory(ModelSelectionContext.OCR))
    var showSheet by remember { mutableStateOf(false) }

    val model = modelVm.state.models.firstOrNull { it.id == modelVm.state.currentModelId }
    val modelLoaded = model != null
    val busy = modelVm.state.busyModelId != null
    val sheetLocked = busy || viewModel.isExtracting

    // The Android photo picker rather than ACTION_GET_CONTENT: it needs no storage permission and
    // offers only images, so the user can never pick something this screen cannot decode.
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }.onSuccess { bitmap ->
                if (bitmap != null) viewModel.onImagePicked(bitmap)
                else viewModel.reportError("Could not decode the selected image.")
            }.onFailure {
                viewModel.reportError("Could not read the selected image: ${it.message}")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(dimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingLg),
    ) {
        ScreenLede(
            "Extract text from invoices, receipts, and scans with Nemotron OCR on the NPU.",
        )

        ModelPickerCard(
            label = AppLocale.text("OCR model"),
            model = model,
            icon = RACIcons.Outline.ScanText,
            busy = busy,
            enabled = !sheetLocked,
            placeholder = AppLocale.text("Select an OCR model"),
            onClick = { showSheet = true },
        )
        DocumentCard(
            viewModel = viewModel,
            modelLoaded = modelLoaded,
            busy = busy,
            onPickImage = {
                imagePicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
        )

        AnimatedVisibility(
            visible = viewModel.extractedText.isNotBlank(),
            enter = fadeIn(AppMotion.standard()),
            exit = fadeOut(AppMotion.exit()),
        ) {
            ResultCard(
                text = viewModel.extractedText,
                latencyMs = viewModel.latencyMs,
                onCopy = {
                    val clipboard =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(
                        ClipData.newPlainText("OCR text", viewModel.extractedText),
                    )
                    Toast.makeText(context, AppLocale.text("Copied to clipboard"), Toast.LENGTH_SHORT).show()
                },
            )
        }

        viewModel.error?.let { StatusNote(it, StatusTone.ERROR) }
        if (viewModel.status.isNotEmpty()) StatusNote(viewModel.status, StatusTone.NEUTRAL)
    }

    if (showSheet) {
        ModelSelectionSheet(viewModel = modelVm, onDismiss = { showSheet = false })
    }
}

@Composable
private fun DocumentCard(
    viewModel: OcrViewModel,
    modelLoaded: Boolean,
    busy: Boolean,
    onPickImage: () -> Unit,
) {
    val dimens = LocalDimens.current
    SectionCard {
        SectionHeader(title = AppLocale.text("Document"))

        val source = viewModel.image
        if (source != null) {
            Image(
                bitmap = source.asImageBitmap(),
                contentDescription = AppLocale.text("Document image"),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = PREVIEW_MAX_HEIGHT)
                    .clip(RoundedCornerShape(dimens.radiusMd)),
            )
        } else {
            // The empty slot is where a first-time user actually is, so it carries the
            // action and the explanation rather than the words "no document selected".
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(dimens.radiusMd),
                modifier = Modifier.fillMaxWidth(),
            ) {
                EmptyState(
                    icon = RACIcons.Outline.ScanText,
                    title = AppLocale.text("No document yet"),
                    body = if (modelLoaded) {
                        "Pick a photo of an invoice, receipt, or scan. The text comes back as " +
                            "characters you can select and copy."
                    } else {
                        "Choose an OCR model above first, then pick a photo of an invoice, " +
                            "receipt, or scan."
                    },
                    primaryAction = EmptyStateAction(
                        label = AppLocale.text("Choose an image"),
                        enabled = !viewModel.isExtracting,
                        onClick = onPickImage,
                    ),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spacingMd)) {
            if (viewModel.image != null) {
                OutlinedButton(onClick = onPickImage, enabled = !viewModel.isExtracting) {
                    Text(AppLocale.text("Change document…"))
                }
            }
            Button(
                onClick = { viewModel.extract() },
                enabled = modelLoaded &&
                    !busy &&
                    viewModel.image != null &&
                    !viewModel.isExtracting,
            ) {
                if (viewModel.isExtracting) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(dimens.iconSm),
                            strokeWidth = 2.dp,
                        )
                        Text(AppLocale.text("Reading…"))
                    }
                } else {
                    Text(AppLocale.text("Extract text"))
                }
            }
        }
        // Only ever the one blocker that is actually in the way, and only once the user has
        // done their part — a warning about a missing model above a card they have not filled
        // in yet is noise.
        if (!modelLoaded && viewModel.image != null) {
            StatusNote("Choose an OCR model above to read this document.", StatusTone.NEUTRAL)
        }
    }
}

@Composable
private fun ResultCard(text: String, latencyMs: Long?, onCopy: () -> Unit) {
    SectionCard {
        SectionHeader(
            title = AppLocale.text("Extracted text"),
            status = latencyMs?.let { String.format(Locale.US, "%.1f s", it / 1000.0) },
        )
        SelectionContainer {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                ),
            )
        }
        OutlinedButton(onClick = onCopy) { Text(AppLocale.text("Copy")) }
    }
}

/** Tall enough to judge a scan, short enough that the actions stay on screen. */
private val PREVIEW_MAX_HEIGHT = 280.dp
