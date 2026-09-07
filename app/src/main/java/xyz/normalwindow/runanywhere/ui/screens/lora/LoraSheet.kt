package xyz.normalwindow.runanywhere.ui.screens.lora

import ai.runanywhere.proto.v1.LoraAdapterCatalogEntry
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import xyz.normalwindow.runanywhere.download.DownloadProgressInfo
import xyz.normalwindow.runanywhere.ui.screens.models.DownloadProgressBlock
import xyz.normalwindow.runanywhere.data.settings.AppLocale
import xyz.normalwindow.runanywhere.ui.theme.LocalDimens
import xyz.normalwindow.runanywhere.ui.theme.icons.RACIcons
import xyz.normalwindow.runanywhere.ui.theme.primaryGreen
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoraSheet(viewModel: LoraViewModel, onDismiss: () -> Unit) {
    val dimens = LocalDimens.current
    val state = viewModel.state
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) { viewModel.refresh() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = dimens.radiusLg, topEnd = dimens.radiusLg),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        dragHandle = null,
        contentWindowInsets = { WindowInsets.systemBars },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.spacingLg)
                .padding(bottom = dimens.spacingXl),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingSm),
        ) {
            Column(modifier = Modifier.padding(vertical = dimens.spacingMd)) {
                Text(AppLocale.text("LoRA Adapters"), style = MaterialTheme.typography.titleMedium)
                viewModel.modelName?.let {
                    Text(
                        text = AppLocale.format("for %s", it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            when {
                state.isLoading -> CenterNote("Loading adapters…", showSpinner = true)
                state.adapters.isEmpty() -> CenterNote("No adapters for this model")
                else -> state.adapters.forEach { entry ->
                    LoraRow(
                        entry = entry,
                        isActive = state.activeId == entry.id,
                        isDownloaded = viewModel.isDownloaded(entry),
                        isBusy = state.busyId == entry.id,
                        progress = if (state.busyId == entry.id) state.progress else null,
                        onDownload = { viewModel.download(entry) },
                        onApply = { scale -> viewModel.apply(entry, scale) }, // null → commons resolves
                        onRemove = viewModel::clear,
                    )
                }
            }

            Text(
                text = AppLocale.text("Adapters fine-tune the loaded model's responses. Applying one replaces any active adapter."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = dimens.spacingSm),
            )
        }
    }

    state.error?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            confirmButton = { TextButton(onClick = viewModel::clearError) { Text(AppLocale.text("OK")) } },
            title = { Text(AppLocale.text("Error")) },
            text = { Text(message) },
        )
    }
}

@Composable
private fun LoraRow(
    entry: LoraAdapterCatalogEntry,
    isActive: Boolean,
    isDownloaded: Boolean,
    isBusy: Boolean,
    progress: DownloadProgressInfo?,
    onDownload: () -> Unit,
    onApply: (Float?) -> Unit,
    onRemove: () -> Unit,
) {
    val dimens = LocalDimens.current
    // Catalog default when present (incl. 0.0); null means unset — Apply passes
    // null so commons resolve_effective_lora_scale owns the 1.0 fallback.
    // Never invent an effective scale in the example app.
    var scale by rememberSaveable(entry.id) {
        mutableStateOf(entry.default_scale)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.radiusLg))
            .background(MaterialTheme.colorScheme.surface)
            .padding(dimens.spacingMd),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingSm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.name,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // LoraAdapterCatalogEntry.description/size_bytes were deleted outright
                    // (idl/lora_options.proto: "everything generic about the artifact ...
                    // lives on the ModelInfo record for this adapter" now) -- the catalog
                    // entry itself no longer carries display/size metadata to show here.
                }

                Spacer(modifier = Modifier.size(dimens.spacingMd))

                LoraAction(
                    isActive = isActive,
                    isDownloaded = isDownloaded,
                    isBusy = isBusy,
                    onDownload = onDownload,
                    onApply = { onApply(scale) },
                    onRemove = onRemove,
                )
            }

            // An adapter is a smaller file than a model but the same kind of wait, so it reports the
            // same way: one bar, one line of real numbers.
            if (isBusy) DownloadProgressBlock(progress)

            if (isDownloaded && !isActive) {
                StrengthControl(scale = scale, onScaleChange = { scale = it })
            } else if (isActive) {
                Text(
                    text = scale?.let { "Strength ${String.format(Locale.US, "%.2fx", it)}" }
                        ?: "Strength (SDK default)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StrengthControl(scale: Float?, onScaleChange: (Float) -> Unit) {
    val dimens = LocalDimens.current
    // Slider requires a concrete thumb position; 1f here is chrome only when
    // scale is unset — Apply still sends null so commons resolves.
    val sliderValue = (scale ?: 1f).coerceIn(0.1f, 2.0f)
    Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingXs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = AppLocale.text("Strength"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = scale?.let { String.format(Locale.US, "%.2fx", it) } ?: AppLocale.text("SDK default"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = onScaleChange,
            valueRange = 0.1f..2.0f,
            steps = 18,
        )
    }
}

@Composable
private fun LoraAction(
    isActive: Boolean,
    isDownloaded: Boolean,
    isBusy: Boolean,
    onDownload: () -> Unit,
    onApply: () -> Unit,
    onRemove: () -> Unit,
) {
    val dimens = LocalDimens.current
    when {
        // The percentage lives in the row's progress block; the trailing slot stays a plain spinner
        // so the same number is never printed twice.
        isBusy -> CircularProgressIndicator(modifier = Modifier.size(dimens.iconSm), strokeWidth = 2.dp)
        isActive -> Row(verticalAlignment = Alignment.CenterVertically) {
            Pill(AppLocale.text("Active"), primaryGreen)
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = RACIcons.Outline.Close,
                    contentDescription = AppLocale.text("Remove adapter"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(dimens.iconSm),
                )
            }
        }
        isDownloaded -> Pill(AppLocale.text("Apply"), MaterialTheme.colorScheme.primary, onClick = onApply)
        else -> IconButton(onClick = onDownload) {
            Icon(
                imageVector = RACIcons.Outline.Download,
                contentDescription = AppLocale.text("Download adapter"),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(dimens.iconMd),
            )
        }
    }
}

@Composable
private fun Pill(text: String, color: androidx.compose.ui.graphics.Color, onClick: (() -> Unit)? = null) {
    val dimens = LocalDimens.current
    val base = Modifier
        .clip(RoundedCornerShape(dimens.radiusFull))
        .background(color.copy(alpha = 0.15f))
    Box(
        modifier = if (onClick != null) base.clickable(onClick = onClick) else base,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            modifier = Modifier.padding(horizontal = dimens.spacingMd, vertical = dimens.spacingXs),
        )
    }
}

@Composable
private fun CenterNote(text: String, showSpinner: Boolean = false) {
    val dimens = LocalDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimens.spacingLg),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showSpinner) {
            CircularProgressIndicator(modifier = Modifier.size(dimens.iconSm), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.height(dimens.spacingSm))
        }
        Text(
            text = AppLocale.text(text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = if (showSpinner) dimens.spacingSm else 0.dp),
        )
    }
}
