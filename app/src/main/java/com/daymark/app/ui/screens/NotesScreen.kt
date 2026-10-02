package com.daymark.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daymark.app.data.NoteEntity
import com.daymark.app.ui.components.EmptyState
import com.daymark.app.ui.components.ScreenHeader
import com.daymark.app.ui.components.SoftCard
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun NotesScreen(
    notes: List<NoteEntity>,
    onSearch: () -> Unit,
    onCreate: () -> Unit,
    onOpen: (NoteEntity) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = notes.filter {
        query.isBlank() || it.title.contains(query, true) || it.content.contains(query, true)
    }.sortedWith(compareByDescending<NoteEntity> { it.isPinned }.thenByDescending { it.updatedAtMillis })
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Notes", "Ideas, details and things to keep", onSearch = onSearch)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp),
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            placeholder = { Text("Find a note") },
            singleLine = true,
            shape = RoundedCornerShape(17.dp)
        )
        if (filtered.isEmpty()) {
            EmptyState(
                title = if (query.isBlank()) "Capture an idea before it disappears." else "No matching notes.",
                message = if (query.isBlank()) "Keep a quick thought, course note or project detail close by." else "Try another word or phrase.",
                actionLabel = if (query.isBlank()) "Write a note" else null,
                onAction = if (query.isBlank()) onCreate else null,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 26.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val pinned = filtered.filter { it.isPinned }
                val recent = filtered.filterNot { it.isPinned }
                if (pinned.isNotEmpty()) {
                    item("pinned-heading") { SectionLabel("PINNED") }
                    items(pinned, key = { it.id }) { note -> NoteCard(note, onClick = { onOpen(note) }) }
                }
                if (recent.isNotEmpty()) {
                    item("recent-heading") { SectionLabel(if (pinned.isNotEmpty()) "RECENT" else "ALL NOTES") }
                    items(recent, key = { it.id }) { note -> NoteCard(note, onClick = { onOpen(note) }) }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, modifier = Modifier.padding(top = 7.dp, bottom = 1.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = .8f.sp)
}

@Composable
private fun NoteCard(note: NoteEntity, onClick: () -> Unit) {
    SoftCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(21.dp), onClick = onClick) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    note.title.ifBlank { "Untitled note" },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (note.isPinned) {
                    Icon(Icons.Rounded.PushPin, contentDescription = "Pinned", modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            if (note.content.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(note.content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(11.dp))
            Text(formatUpdated(note.updatedAtMillis), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatUpdated(epochMillis: Long): String = runCatching {
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMM · h:mm a", Locale.getDefault()))
}.getOrDefault("Recently updated")
