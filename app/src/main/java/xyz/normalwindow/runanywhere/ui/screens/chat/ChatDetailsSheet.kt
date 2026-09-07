package xyz.normalwindow.runanywhere.ui.screens.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import xyz.normalwindow.runanywhere.ui.theme.LocalDimens
import xyz.normalwindow.runanywhere.data.settings.AppLocale
import java.text.DateFormat
import java.util.Date
import java.util.Locale

// Conversation-level analytics rollup. Mirrors the Overview tab of iOS
// ChatDetailsView (opened from the chat toolbar's info button), condensed to a
// single bottom sheet.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailsSheet(
    messages: List<ChatMessage>,
    createdAt: Long?,
    onDismiss: () -> Unit,
) {
    val dimens = LocalDimens.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val assistantMessages = messages.filter { !it.isUser }
    val stats = assistantMessages.mapNotNull { it.stats }
    val repliesWithContent = assistantMessages.count { it.text.isNotEmpty() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.spacingLg)
                .padding(bottom = dimens.spacingXl),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingSm),
        ) {
            Text(
                text = AppLocale.text("Analytics"),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = dimens.spacingSm),
            )

            SectionHeader(AppLocale.text("Conversation"))
            DetailRow(AppLocale.text("Messages"), "${messages.size}")
            DetailRow(AppLocale.text("From You"), "${messages.count { it.isUser }}")
            DetailRow(AppLocale.text("From AI"), "$repliesWithContent")
            createdAt?.let {
                DetailRow(
                    AppLocale.text("Created"),
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it)),
                )
            }

            if (stats.isNotEmpty()) {
                SectionHeader(AppLocale.text("Performance"))
                val avgTimeSec = stats.map { it.totalTimeMs }.average() / 1000.0
                val avgSpeed = stats.map { it.tokensPerSecond }.average()
                val totalTokens = stats.sumOf { it.inputTokens + it.tokens }
                DetailRow(AppLocale.text("Avg Response"), String.format(Locale.US, "%.1fs", avgTimeSec))
                DetailRow(AppLocale.text("Token Speed"), "${avgSpeed.toInt()} tok/s")
                DetailRow(AppLocale.text("Total Tokens"), "$totalTokens")
                if (repliesWithContent > 0) {
                    DetailRow(AppLocale.text("Success Rate"), "${stats.size * 100 / repliesWithContent}%")
                }

                SectionHeader(AppLocale.text("Models"))
                stats.groupBy { it.modelName ?: AppLocale.text("Unknown") }.forEach { (model, items) ->
                    DetailRow(model, AppLocale.format("%d responses", items.size))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    val dimens = LocalDimens.current
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = dimens.spacingMd),
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
