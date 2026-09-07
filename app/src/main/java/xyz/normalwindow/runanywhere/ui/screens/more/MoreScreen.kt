package xyz.normalwindow.runanywhere.ui.screens.more

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import xyz.normalwindow.runanywhere.ui.components.ScreenLede
import xyz.normalwindow.runanywhere.ui.navigation.Benchmarks
import xyz.normalwindow.runanywhere.ui.HybridBetaCopy
import xyz.normalwindow.runanywhere.ui.navigation.CloudProviders
import xyz.normalwindow.runanywhere.ui.navigation.Diarization
import xyz.normalwindow.runanywhere.ui.navigation.Diffusion
import xyz.normalwindow.runanywhere.ui.navigation.Documents
import xyz.normalwindow.runanywhere.ui.navigation.Ocr
import xyz.normalwindow.runanywhere.ui.navigation.Segmentation
import xyz.normalwindow.runanywhere.ui.navigation.Settings
import xyz.normalwindow.runanywhere.ui.navigation.Solutions
import xyz.normalwindow.runanywhere.ui.navigation.Stt
import xyz.normalwindow.runanywhere.ui.navigation.Tools
import xyz.normalwindow.runanywhere.ui.navigation.Tts
import xyz.normalwindow.runanywhere.ui.navigation.Vad
import xyz.normalwindow.runanywhere.ui.navigation.Vision
import xyz.normalwindow.runanywhere.ui.theme.LocalDimens
import xyz.normalwindow.runanywhere.ui.theme.icons.RACIcons
import xyz.normalwindow.runanywhere.data.settings.AppLocale

private enum class AdvancedGroup(val title: String) {
    ASSISTANT("Assistant add-ons"),
    SPEECH("Speech lab"),
    DEVELOPER("Developer diagnostics"),
}

private data class AdvancedEntry(
    val label: String,
    val description: String,
    val icon: ImageVector,
    val group: AdvancedGroup,
    val route: Any,
)

/**
 * The Advanced hub.
 *
 * Every description says what the reader gets, never which model does it. They used to read
 * like release notes — "(NVIDIA Sortformer)", "(SegFormer)", "(Cosmos3-Edge diffusion)", "Run
 * YAML-driven SDK pipelines" — and a parenthesised codename or a serialisation format is the
 * one thing a reader deciding whether to tap cannot use. The model names live on the screens
 * themselves, where a curious reader has already opted in. iOS `ConsumerAdvancedHubView` and
 * the web hub carry the same rewritten copy, so a row means the same thing in every app.
 */
@Composable
fun MoreScreen(onNavigate: (Any) -> Unit) {
    val dimens = LocalDimens.current
    val entries = listOf(
        AdvancedEntry("Settings", "Personalization, privacy, and account controls", RACIcons.Outline.Settings, AdvancedGroup.ASSISTANT, Settings),
        AdvancedEntry("Documents", "Inspect document Q&A setup and sources", RACIcons.Outline.FileText, AdvancedGroup.ASSISTANT, Documents),
        AdvancedEntry("Images & live", "Photo prompts, camera mode, and VLM metrics", RACIcons.Outline.Eye, AdvancedGroup.ASSISTANT, Vision()),
        AdvancedEntry("Document OCR", "Extract text from invoices and scans", RACIcons.Outline.ScanText, AdvancedGroup.ASSISTANT, Ocr),
        AdvancedEntry("Segmentation", "Split a photo into labelled regions", RACIcons.Outline.Layers, AdvancedGroup.ASSISTANT, Segmentation),
        AdvancedEntry("Diarization", "See who spoke when in a recording", RACIcons.Outline.Users, AdvancedGroup.SPEECH, Diarization),
        AdvancedEntry("Image generation", "Make a picture from a description", RACIcons.Outline.Sparkles, AdvancedGroup.ASSISTANT, Diffusion),
        AdvancedEntry("Read aloud", "Generate speech and preview voices", RACIcons.Outline.Volume, AdvancedGroup.SPEECH, Tts),
        AdvancedEntry("Transcription", HybridBetaCopy.TRANSCRIPTION_ENTRY_DESCRIPTION, RACIcons.Outline.Waveform, AdvancedGroup.SPEECH, Stt),
        AdvancedEntry("Voice activity", "Tune speech detection infrastructure", RACIcons.Outline.Pulse, AdvancedGroup.SPEECH, Vad),
        AdvancedEntry("Web & tools", "Inspect and control assistant tools", RACIcons.Outline.Tool, AdvancedGroup.DEVELOPER, Tools),
        AdvancedEntry("Solutions", "Run saved multi-step workflows", RACIcons.Outline.Route, AdvancedGroup.DEVELOPER, Solutions),
        AdvancedEntry("Cloud providers", HybridBetaCopy.CLOUD_PROVIDERS_ENTRY_DESCRIPTION, RACIcons.Outline.Cloud, AdvancedGroup.DEVELOPER, CloudProviders),
        AdvancedEntry("Benchmarks", "Measure speed, memory, and device behavior", RACIcons.Outline.Gauge, AdvancedGroup.DEVELOPER, Benchmarks),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(dimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingLg),
    ) {
        ScreenLede(
            AppLocale.text("SDK workbenches and diagnostics live here so the assistant stays simple."),
        )

        AdvancedGroup.entries.forEach { group ->
            AdvancedSection(group.title) {
                entries.filter { it.group == group }.forEach { entry ->
                    AdvancedRow(entry) { onNavigate(entry.route) }
                }
            }
        }
    }
}

@Composable
private fun AdvancedSection(title: String, content: @Composable () -> Unit) {
    val dimens = LocalDimens.current
    Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingSm)) {
        Text(
            text = AppLocale.text(title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = dimens.spacingXs),
        )
        content()
    }
}

@Composable
private fun AdvancedRow(entry: AdvancedEntry, onClick: () -> Unit) {
    val dimens = LocalDimens.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(dimens.radiusLg),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(dimens.radiusLg))
                .clickable(onClick = onClick)
                .padding(dimens.spacingLg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingMd),
        ) {
            Icon(
                imageVector = entry.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(dimens.iconMd),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(AppLocale.text(entry.label), style = MaterialTheme.typography.bodyLarge)
                Text(
                    AppLocale.text(entry.description),
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
}
