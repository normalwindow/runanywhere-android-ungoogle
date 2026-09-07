package xyz.normalwindow.runanywhere.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.normalwindow.runanywhere.ui.components.ScreenLede
import xyz.normalwindow.runanywhere.BuildConfig
import xyz.normalwindow.runanywhere.data.settings.AppLanguage
import xyz.normalwindow.runanywhere.data.settings.AppLocale
import xyz.normalwindow.runanywhere.data.settings.ModelDownloadSource
import xyz.normalwindow.runanywhere.data.settings.AppThemeColor
import xyz.normalwindow.runanywhere.data.settings.AppThemeMode
import xyz.normalwindow.runanywhere.state.GlobalState
import xyz.normalwindow.runanywhere.ui.screens.chat.ChatGenerationBudgetPolicy
import xyz.normalwindow.runanywhere.ui.screens.models.formatModelSize
import xyz.normalwindow.runanywhere.ui.theme.LocalDimens
import xyz.normalwindow.runanywhere.ui.theme.RACTextStyles
import xyz.normalwindow.runanywhere.ui.theme.icons.RACIcons
import xyz.normalwindow.runanywhere.util.readableWidth
import java.util.Locale
import android.widget.Toast
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onOpenModels: () -> Unit = {},
    onOpenAdvanced: () -> Unit = {},
) {
    val dimens = LocalDimens.current
    val settings = viewModel.settings
    val storage = viewModel.storage
    val loadedChatModel = GlobalState.model.loaded
    val chatBudget = ChatGenerationBudgetPolicy.resolve(
        requestedMaxTokens = settings.maxTokens,
        modelContextTokens = loadedChatModel?.context_length ?: 0,
    )
    var deletingModel by remember { mutableStateOf<com.runanywhere.sdk.public.types.RAModelInfo?>(null) }
    var languageDialog by remember { mutableStateOf(false) }
    var sourceDialog by remember { mutableStateOf(false) }
    var themeModeDialog by remember { mutableStateOf(false) }
    var themeColorDialog by remember { mutableStateOf(false) }
    var customSourceDraft by remember(settings.customDownloadBaseUrl) { mutableStateOf(settings.customDownloadBaseUrl) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .readableWidth()
            .verticalScroll(rememberScrollState())
            .padding(dimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingLg),
    ) {
        ScreenLede(
            AppLocale.text("Personalize the assistant, manage local models, and keep downloads private."),
        )

        Section(AppLocale.text("App")) {
            SettingsLinkRow(
                label = AppLocale.text("Language"),
                description = AppLocale.text(settings.language.label),
                icon = RACIcons.Outline.Globe,
                onClick = { languageDialog = true },
            )
            SettingsLinkRow(
                label = AppLocale.text("Model download source"),
                description = AppLocale.text(settings.modelDownloadSource.label),
                icon = RACIcons.Outline.Download,
                onClick = { sourceDialog = true },
            )
            if (settings.modelDownloadSource == ModelDownloadSource.CUSTOM) {
                OutlinedTextField(
                    value = customSourceDraft,
                    onValueChange = {
                        customSourceDraft = it
                        viewModel.editCustomDownloadBaseUrl(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(AppLocale.text("Custom source base URL")) },
                    placeholder = { Text("https://your-mirror.example") },
                    supportingText = { Text(AppLocale.text("Must use HTTPS. The Hugging Face path is appended automatically.")) },
                    singleLine = true,
                )
                TextButton(
                    onClick = {
                        val valid = customSourceDraft.trim().toHttpUrlOrNull()?.scheme == "https"
                        if (valid) {
                            viewModel.applyCustomDownloadBaseUrl()
                        } else {
                            Toast.makeText(context, AppLocale.text("Enter a valid HTTPS source URL"), Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = customSourceDraft.isNotBlank(),
                ) { Text(AppLocale.text("Apply download source")) }
            }
            SettingsLinkRow(
                label = AppLocale.text("Theme mode"),
                description = AppLocale.text(settings.themeMode.label),
                icon = RACIcons.Outline.Moon,
                onClick = { themeModeDialog = true },
            )
            SettingsLinkRow(
                label = AppLocale.text("Theme color"),
                description = AppLocale.text(settings.themeColor.label),
                icon = RACIcons.Outline.Sliders,
                onClick = { themeColorDialog = true },
            )
            SettingsLinkRow(
                label = AppLocale.text("Choose chat model"),
                description = AppLocale.text("Download or switch the model used by Ask"),
                icon = RACIcons.Outline.Model,
                onClick = onOpenModels,
            )
            SettingsLinkRow(
                label = AppLocale.text("Advanced workbench"),
                description = AppLocale.text("Voice, documents, tools, and diagnostics"),
                icon = RACIcons.Outline.Sliders,
                onClick = onOpenAdvanced,
            )
        }

        Section(AppLocale.text("Assistant")) {
            SliderRow(
                label = AppLocale.text("Temperature"),
                valueText = String.format(Locale.US, "%.1f", settings.temperature),
                value = settings.temperature,
                valueRange = 0f..2f,
                steps = 19,
                onValueChange = viewModel::setTemperature,
            )
            SliderRow(
                label = AppLocale.text("Max tokens"),
                valueText = settings.maxTokens.toString(),
                value = settings.maxTokens.toFloat(),
                valueRange = 256f..4096f,
                steps = 14,
                onValueChange = { viewModel.setMaxTokens(it.roundToInt()) },
                description = chatBudget.explanation(loadedChatModel?.name),
            )
            Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingXs)) {
                Text(AppLocale.text("System prompt"), style = MaterialTheme.typography.bodyLarge)
                OutlinedTextField(
                    value = settings.systemPrompt,
                    onValueChange = viewModel::setSystemPrompt,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(AppLocale.text("Optional — sets the assistant's behavior")) },
                    minLines = 2,
                    maxLines = 5,
                )
            }
            ToggleRow(
                label = AppLocale.text("Streaming"),
                description = AppLocale.text("Show the reply token-by-token"),
                checked = settings.streaming,
                onCheckedChange = viewModel::setStreaming,
            )
            ToggleRow(
                label = AppLocale.text("Show reasoning when available"),
                description = AppLocale.text("Thinking models can show a collapsible reasoning trace before the answer."),
                checked = !settings.disableThinking,
                onCheckedChange = { viewModel.setDisableThinking(!it) },
            )
        }

        // "Downloads", the word the web app's nav row, Advanced-hub row and panel title all
        // use for the same content — what is on this device and what it costs. This heading
        // said "Storage", which names the resource rather than the thing the reader came
        // here to manage, so one concept had two names across the apps.
        Section(AppLocale.text("Downloads")) {
            Text(
                text = AppLocale.format(
                    "Models %s · %s free",
                    formatModelSize(storage.modelsBytes).ifBlank { "0 B" },
                    formatModelSize(storage.freeBytes),
                ),
                style = RACTextStyles.Metric,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when {
                storage.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.size(dimens.iconSm),
                    strokeWidth = 2.dp,
                )
                storage.downloaded.isEmpty() -> Text(
                    AppLocale.text("No downloaded models yet."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> storage.downloaded.sortedBy { it.name.lowercase() }.forEach { model ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(model.name.ifBlank { model.id }, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                listOfNotNull(
                                    model.framework.name.removePrefix("INFERENCE_FRAMEWORK_").takeIf { it.isNotBlank() },
                                    model.download_size_bytes.takeIf { it > 0 }?.let(::formatModelSize),
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(
                            onClick = { deletingModel = model },
                            enabled = storage.busyId == null,
                        ) {
                            Text(AppLocale.text(if (storage.busyId == model.id) "Deleting…" else "Delete"))
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm)) {
                TextButton(onClick = viewModel::clearCache) { Text(AppLocale.text("Clear cache")) }
                TextButton(onClick = viewModel::cleanTempFiles) { Text(AppLocale.text("Clean temp files")) }
            }
            storage.message?.let {
                Text(AppLocale.text(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }

        Section(AppLocale.text("Private Downloads")) {
            Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingXs)) {
                Text(AppLocale.text("Hugging Face token"), style = MaterialTheme.typography.bodyLarge)
                OutlinedTextField(
                    value = viewModel.hfTokenDraft,
                    onValueChange = viewModel::editHfToken,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("hf_…") },
                    supportingText = {
                        Text(AppLocale.text("Stored securely and used only for private Hugging Face repos, including HNPU/QHexRT bundles"))
                    },
                    singleLine = true,
                    enabled = !storage.hfTokenBusy,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { viewModel.commitHfToken() }),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = viewModel::commitHfToken, enabled = !storage.hfTokenBusy) {
                        Text(AppLocale.text("Save token"))
                    }
                    TextButton(onClick = viewModel::clearHfToken, enabled = !storage.hfTokenBusy) {
                        Text(AppLocale.text("Clear"))
                    }
                    if (storage.hfTokenBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(dimens.iconSm),
                            strokeWidth = 2.dp,
                        )
                    }
                }
                storage.hfTokenMessage?.let { message ->
                    Text(
                        AppLocale.text(message),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (storage.hfTokenMessageIsError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
            }
        }

        Section(AppLocale.text("About")) {
            InfoRow(AppLocale.text("SDK version"), viewModel.sdkVersion)
            InfoRow(AppLocale.text("App version"), BuildConfig.VERSION_NAME)
            val uriHandler = LocalUriHandler.current
            if (BuildConfig.PRIVACY_POLICY_URL.isNotBlank()) {
                Text(
                    text = AppLocale.text("Privacy policy"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri(BuildConfig.PRIVACY_POLICY_URL) }
                        .padding(vertical = dimens.spacingXs),
                )
            }
            Text(
                text = AppLocale.text("Documentation"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uriHandler.openUri("https://docs.runanywhere.ai") }
                    .padding(vertical = dimens.spacingXs),
            )
            Text(
                text = AppLocale.text("Follow on X"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uriHandler.openUri("https://x.com/RunanywhereAI") }
                    .padding(vertical = dimens.spacingXs),
            )
        }
    }

    deletingModel?.let { model ->
        AlertDialog(
            onDismissRequest = { deletingModel = null },
            title = { Text(AppLocale.text("Delete downloaded model?")) },
            text = { Text(AppLocale.format("Remove %s and its files from this device?", model.name.ifBlank { model.id })) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deletingModel = null
                        viewModel.deleteModel(model)
                    },
                ) { Text(AppLocale.text("Delete"), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deletingModel = null }) { Text(AppLocale.text("Cancel")) } },
        )
    }

    if (languageDialog) {
        AlertDialog(
            onDismissRequest = { languageDialog = false },
            title = { Text(AppLocale.text("Language")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingXs)) {
                    AppLanguage.entries.forEach { language ->
                        TextButton(
                            onClick = {
                                languageDialog = false
                                viewModel.setLanguage(language)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(AppLocale.text(language.label)) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { languageDialog = false }) { Text(AppLocale.text("Cancel")) } },
        )
    }

    if (sourceDialog) {
        AlertDialog(
            onDismissRequest = { sourceDialog = false },
            title = { Text(AppLocale.text("Model download source")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingXs)) {
                    ModelDownloadSource.entries.forEach { source ->
                        TextButton(
                            onClick = {
                                sourceDialog = false
                                viewModel.setModelDownloadSource(source)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(AppLocale.text(source.label)) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { sourceDialog = false }) { Text(AppLocale.text("Cancel")) } },
        )
    }

    if (themeModeDialog) {
        AlertDialog(
            onDismissRequest = { themeModeDialog = false },
            title = { Text(AppLocale.text("Theme mode")) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingXs)) {
                AppThemeMode.entries.forEach { mode ->
                    TextButton(onClick = { themeModeDialog = false; viewModel.setThemeMode(mode) }, modifier = Modifier.fillMaxWidth()) {
                        Text(AppLocale.text(mode.label))
                    }
                }
            } },
            confirmButton = { TextButton(onClick = { themeModeDialog = false }) { Text(AppLocale.text("Cancel")) } },
        )
    }

    if (themeColorDialog) {
        AlertDialog(
            onDismissRequest = { themeColorDialog = false },
            title = { Text(AppLocale.text("Theme color")) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingXs)) {
                AppThemeColor.entries.forEach { color ->
                    TextButton(onClick = { themeColorDialog = false; viewModel.setThemeColor(color) }, modifier = Modifier.fillMaxWidth()) {
                        Text(AppLocale.text(color.label))
                    }
                }
            } },
            confirmButton = { TextButton(onClick = { themeColorDialog = false }) { Text(AppLocale.text("Cancel")) } },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val dimens = LocalDimens.current
    Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingSm)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(dimens.radiusLg),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(dimens.spacingLg),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingMd),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    description: String? = null,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(valueText, style = RACTextStyles.Metric, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, steps = steps)
        description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ToggleRow(label: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun SettingsLinkRow(
    label: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val dimens = LocalDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = dimens.spacingXs),
        horizontalArrangement = Arrangement.spacedBy(dimens.spacingMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(dimens.iconMd),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = RACIcons.Outline.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(dimens.iconSm),
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = RACTextStyles.Metric, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
