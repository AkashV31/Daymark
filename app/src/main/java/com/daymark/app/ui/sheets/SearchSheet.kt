package com.daymark.app.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.daymark.app.data.SearchResult
import com.daymark.app.ui.components.EmptyState
import com.daymark.app.ui.components.SoftCard
import kotlinx.coroutines.delay

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun SearchSheet(
    onDismiss: () -> Unit,
    onSearch: suspend (String) -> List<SearchResult>,
    onOpenResult: (SearchResult) -> Unit
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<SearchResult>()) }
    var loading by remember { mutableStateOf(false) }
    LaunchedEffect(query) {
        if (query.trim().length < 2) {
            results = emptyList()
            loading = false
        } else {
            delay(180)
            loading = true
            results = runCatching { onSearch(query) }.getOrDefault(emptyList())
            loading = false
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 26.dp)) {
            Text("Search Daymark", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                placeholder = { Text("Tasks, notes, courses, goals…") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.height(11.dp))
            when {
                query.trim().length < 2 -> EmptyState("Search your local workspace.", "Results stay on this device and appear as you type.", modifier = Modifier.padding(vertical = 6.dp), symbol = "⌕")
                loading -> Text("Searching…", modifier = Modifier.padding(18.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                results.isEmpty() -> EmptyState("No results found.", "Try a different word or phrase.", modifier = Modifier.padding(vertical = 6.dp), symbol = "⌕")
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(results, key = { "${it.type}:${it.id}" }) { result ->
                        SearchResultRow(result) { onOpenResult(result) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(result: SearchResult, onClick: () -> Unit) {
    val (icon, color) = when (result.type) {
        "TASK" -> Icons.Rounded.Lightbulb to MaterialTheme.colorScheme.primary
        "EVENT" -> Icons.Rounded.CalendarMonth to MaterialTheme.colorScheme.secondary
        "DEADLINE" -> Icons.Rounded.Flag to MaterialTheme.colorScheme.error
        "NOTE" -> Icons.Rounded.Notes to MaterialTheme.colorScheme.primary
        "COURSE" -> Icons.Rounded.MenuBook to MaterialTheme.colorScheme.tertiary
        "PROJECT" -> Icons.Rounded.WorkOutline to MaterialTheme.colorScheme.secondary
        else -> Icons.Rounded.Flag to MaterialTheme.colorScheme.primary
    }
    SoftCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp), onClick = onClick) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.layout.Box(Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = .11f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.size(11.dp))
            Column(Modifier.weight(1f)) {
                Text(result.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(result.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
