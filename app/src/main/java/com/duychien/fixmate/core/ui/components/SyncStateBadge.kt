package com.duychien.fixmate.core.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.duychien.fixmate.R
import com.duychien.fixmate.core.ui.theme.FixMateTheme
import com.duychien.fixmate.domain.model.SyncState

@Composable
fun SyncStateBadge(
    syncState: SyncState,
    modifier: Modifier = Modifier,
) {
    val (icon, label, tint) = syncState.visuals()
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = tint,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
        )
    }
}

private data class SyncVisuals(val icon: ImageVector, val label: String, val tint: Color)

@Composable
private fun SyncState.visuals(): SyncVisuals = when (this) {
    SyncState.SYNCED -> SyncVisuals(
        icon = Icons.Default.CheckCircle,
        label = stringResource(R.string.sync_synced),
        tint = FixMateTheme.colors.synced,
    )
    SyncState.PENDING_CREATE -> SyncVisuals(
        icon = Icons.Default.Warning,
        label = stringResource(R.string.sync_pending_create),
        tint = FixMateTheme.colors.pending,
    )
    SyncState.PENDING_UPDATE -> SyncVisuals(
        icon = Icons.Default.Refresh,
        label = stringResource(R.string.sync_pending_update),
        tint = FixMateTheme.colors.pending,
    )
    SyncState.PENDING_DELETE -> SyncVisuals(
        icon = Icons.Default.Delete,
        label = stringResource(R.string.sync_pending_delete),
        tint = MaterialTheme.colorScheme.error,
    )
}
