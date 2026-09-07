package xyz.normalwindow.runanywhere.ui.screens.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.normalwindow.runanywhere.ui.components.WebSearchDisclosureDialog
import xyz.normalwindow.runanywhere.ui.theme.LocalDimens
import xyz.normalwindow.runanywhere.ui.theme.RACTextStyles
import xyz.normalwindow.runanywhere.ui.theme.icons.RACIcons
import xyz.normalwindow.runanywhere.data.settings.AppLocale
import xyz.normalwindow.runanywhere.data.settings.WebSearchEngine
import xyz.normalwindow.runanywhere.util.readableWidth
import com.runanywhere.sdk.public.types.RAToolDefinition
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

@Composable
fun ToolsScreen(viewModel: ToolsViewModel = viewModel()) {
    val dimens = LocalDimens.current
    var engineDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .readableWidth()
            .verticalScroll(rememberScrollState())
            .padding(dimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingLg),
    ) {
        Section(AppLocale.text("Web & Tools")) {
            ToggleRow(
                label = AppLocale.text("Enable web and tools"),
                description = AppLocale.text("Allow the assistant to use on-device utilities and send a query to the configured web-search service when it chooses the search tool."),
                checked = viewModel.toolCallingEnabled,
                onCheckedChange = viewModel::setEnabled,
            )
            EngineRow(
                label = AppLocale.text("Search engine"),
                description = AppLocale.text(viewModel.webSearchEngine.label),
                onClick = { engineDialog = true },
            )
            if (viewModel.toolCallingEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(AppLocale.text("Registered tools"), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = viewModel.tools.size.toString(),
                        style = RACTextStyles.Metric,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (viewModel.tools.isEmpty()) {
                    Text(
                        text = AppLocale.text("No tools registered"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    viewModel.tools.forEach { tool -> ToolRow(tool) }
                }
            }
        }
    }

    if (viewModel.showWebSearchDisclosure) {
        WebSearchDisclosureDialog(
            onAllow = viewModel::acceptWebSearchDisclosure,
            onDismiss = viewModel::dismissWebSearchDisclosure,
        )
    }

    if (engineDialog) {
        AlertDialog(
            onDismissRequest = { engineDialog = false },
            title = { Text(AppLocale.text("Search engine")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingXs)) {
                    WebSearchEngine.entries.forEach { engine ->
                        TextButton(
                            onClick = {
                                engineDialog = false
                                viewModel.setWebSearchEngine(engine)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(AppLocale.text(engine.label)) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { engineDialog = false }) { Text(AppLocale.text("Cancel")) }
            },
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
private fun EngineRow(label: String, description: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ToggleRow(label: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val dimens = LocalDimens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spacingMd),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ToolRow(tool: RAToolDefinition) {
    val dimens = LocalDimens.current
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(dimens.radiusMd),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(dimens.spacingMd),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingXs),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm),
            ) {
                Icon(
                    imageVector = RACIcons.Outline.Tool,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(dimens.iconSm),
                )
                Text(tool.name, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val parameterNames = tool.parameterNames()
            if (parameterNames.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimens.spacingXs),
                ) {
                    Text(
                        text = AppLocale.text("Params:"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    parameterNames.forEach { name ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = RoundedCornerShape(dimens.radiusSm),
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = dimens.spacingSm, vertical = dimens.spacingXs),
                            )
                        }
                    }
                }
            }
        }
    }
}

private val toolParametersJson = Json { ignoreUnknownKeys = true }

/**
 * `ToolDefinition.parameters` is now a single raw JSON-Schema object string
 * (idl/tool_calling.proto) rather than a typed parameter list -- read the
 * `properties` object's keys for display, same as the JSON Schema every
 * OpenAI/Anthropic/MCP tool definition already publishes.
 */
private fun RAToolDefinition.parameterNames(): List<String> {
    if (parameters.isBlank()) return emptyList()
    val root = runCatching { toolParametersJson.parseToJsonElement(parameters).jsonObject }.getOrNull()
        ?: return emptyList()
    val properties = (root["properties"] as? JsonObject) ?: return emptyList()
    return properties.keys.toList()
}
