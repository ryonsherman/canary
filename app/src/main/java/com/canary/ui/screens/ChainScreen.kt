package com.canary.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.canary.data.PreferencesManager
import com.canary.model.Canary
import com.canary.ui.theme.*
import com.canary.viewmodel.ChainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChainScreen(
    prefsManager: PreferencesManager,
) {
    val viewModel = remember { ChainViewModel(prefsManager) }
    var depth by remember { mutableIntStateOf(50) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadChain(depth)
    }

    val state = viewModel.chainState()
    val loading = viewModel.isLoading()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chain") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = loading,
            onRefresh = { scope.launch { viewModel.loadChain(depth) } },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (state != null && !loading) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (state.intact) "Chain Intact ✓" else "Chain Broken ✗",
                                style = MaterialTheme.typography.headlineMedium,
                                color = if (state.intact) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${state.totalCount} canaries verified",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            state.breakAtIndex?.let {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Break at canary #$it",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }

                if (loading && state == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (state != null) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.canaries) { canary ->
                            CanaryListItem(canary)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "No canaries found. Push one first.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CanaryListItem(canary: Canary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Canary #${canary.counter}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    canary.date,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Hash: ${canary.fileHash.take(16)}...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
