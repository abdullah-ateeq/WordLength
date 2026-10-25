package com.example.wordlength.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.draw.clip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wordlength.data.model.SearchMatchMode
import com.example.wordlength.data.model.WordLengthResult
import com.example.wordlength.data.model.WordSortOrder
import com.example.wordlength.data.preferences.AppSettings
import com.example.wordlength.ui.components.DistributionBarChart
import com.example.wordlength.ui.components.LengthHistogramChart
import com.example.wordlength.ui.components.StatCard
import com.example.wordlength.ui.components.WordDetailBottomSheet
import com.example.wordlength.ui.viewmodel.WordLengthViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: WordLengthViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.saveSuccessMessage) {
        uiState.saveSuccessMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSaveSuccessMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            item {
                HeaderSection()
            }

            // Input section
            item {
                InputSection(
                    text = uiState.text,
                    liveChars = uiState.liveTextCharacters,
                    liveWords = uiState.liveTextWords,
                    liveLines = uiState.liveTextLines,
                    isAnalyzing = uiState.isAnalyzing,
                    onTextChanged = viewModel::onTextChanged,
                    onClearText = viewModel::clearText,
                    onAnalyze = viewModel::analyzeExplicitly
                )
            }

            // Error or empty state message
            if (uiState.errorMessage != null) {
                item {
                    ErrorMessageCard(uiState.errorMessage!!)
                }
            }

            // Filter Summary (Expandable)
            item {
                FilterSummaryCard(
                    settings = settings,
                    isExpanded = uiState.filterSummaryExpanded,
                    onToggle = viewModel::toggleFilterSummaryExpanded
                )
            }

            // Results Section (Visible only when words exist)
            if (uiState.totalWords > 0) {
                // Action Buttons
                item {
                    ActionButtonsSection(
                        context = context,
                        viewModel = viewModel,
                        onShowSnackbar = { msg ->
                            coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    )
                }

                // Main Stats Grid
                item {
                    MainStatsGrid(
                        totalWords = uiState.totalWords,
                        uniqueWords = uiState.uniqueWords,
                        averageLength = uiState.averageLength,
                        medianLength = uiState.medianLength,
                        showMedian = settings.showMedian
                    )
                }

                // Shortest & Longest Word Cards
                item {
                    ShortestLongestSection(
                        shortestWords = uiState.shortestWords,
                        minimumLength = uiState.minimumLength,
                        longestWords = uiState.longestWords,
                        maximumLength = uiState.maximumLength,
                        showAllShortest = uiState.showAllShortest,
                        showAllLongest = uiState.showAllLongest,
                        onToggleShortest = viewModel::toggleShowAllShortest,
                        onToggleLongest = viewModel::toggleShowAllLongest,
                        onCopy = { txt ->
                            viewModel.copyToClipboard(context, txt)
                            coroutineScope.launch { snackbarHostState.showSnackbar("Copied: $txt") }
                        }
                    )
                }

                // Mode / Most Common Length
                if (settings.showMode && uiState.modeLengths.isNotEmpty()) {
                    item {
                        ModeCard(
                            modeLengths = uiState.modeLengths,
                            distribution = uiState.distribution
                        )
                    }
                }

                // Range and Character Totals
                item {
                    RangeAndCharactersCard(
                        totalCharacters = uiState.totalCharacters,
                        averageLength = uiState.averageLength,
                        minimumLength = uiState.minimumLength,
                        maximumLength = uiState.maximumLength,
                        range = uiState.range
                    )
                }

                // Unique Word Analysis (Optional)
                if (settings.showUniqueAnalysis && uiState.averageUniqueLength != null) {
                    item {
                        UniqueAnalysisCard(
                            uniqueWords = uiState.uniqueWords,
                            averageUniqueLength = uiState.averageUniqueLength!!
                        )
                    }
                }

                // Word Length Distribution
                if (settings.showDistribution && uiState.distribution.isNotEmpty()) {
                    item {
                        DistributionCard(
                            distribution = uiState.distribution,
                            totalWords = uiState.totalWords,
                            onLengthSelected = { len ->
                                viewModel.onSearchQueryChanged("")
                                viewModel.setSortOrder(WordSortOrder.LENGTH_ASC)
                            }
                        )
                    }
                }

                // Word Length Chart / Histogram
                if (settings.showChart && uiState.distribution.isNotEmpty()) {
                    item {
                        HistogramCard(
                            distribution = uiState.distribution,
                            isExpanded = uiState.chartExpanded,
                            onToggle = viewModel::toggleChartExpanded
                        )
                    }
                }

                // Length Groups Summary (Expandable)
                if (uiState.lengthGroups.isNotEmpty()) {
                    item {
                        LengthGroupsCard(
                            groups = uiState.lengthGroups,
                            isExpanded = uiState.lengthGroupsExpanded,
                            onToggle = viewModel::toggleLengthGroupsExpanded
                        )
                    }
                }

                // Length Summary Table (Expandable)
                item {
                    LengthSummaryTableCard(
                        distribution = uiState.distribution,
                        totalWords = uiState.totalWords,
                        isExpanded = uiState.lengthSummaryExpanded,
                        onToggle = viewModel::toggleLengthSummaryExpanded
                    )
                }

                // Search & Word List Header
                item {
                    WordListHeader(
                        searchQuery = uiState.searchQuery,
                        searchMatchMode = uiState.searchMatchMode,
                        sortOrder = uiState.sortOrder,
                        resultCount = uiState.results.size,
                        onQueryChanged = viewModel::onSearchQueryChanged,
                        onMatchModeChanged = viewModel::setSearchMatchMode,
                        onSortOrderChanged = viewModel::setSortOrder
                    )
                }

                // Filtered Words List
                items(
                    items = uiState.results,
                    key = { it.word + "_" + it.length }
                ) { result ->
                    WordItemRow(
                        result = result,
                        onClick = { viewModel.selectWordDetail(result) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )

        // Bottom sheet for Word Detail
        WordDetailBottomSheet(
            result = uiState.selectedWordDetail,
            minimumLength = uiState.minimumLength,
            maximumLength = uiState.maximumLength,
            shortestCount = uiState.shortestWords.size,
            longestCount = uiState.longestWords.size,
            onDismiss = { viewModel.selectWordDetail(null) },
            onCopyWord = { word ->
                viewModel.copyToClipboard(context, word)
                viewModel.selectWordDetail(null)
                coroutineScope.launch { snackbarHostState.showSnackbar("Copied: $word") }
            }
        )
    }
}

@Composable
private fun HeaderSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "WordLength",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Analyze shortest, longest, and average word length.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InputSection(
    text: String,
    liveChars: Int,
    liveWords: Int,
    liveLines: Int,
    isAnalyzing: Boolean,
    onTextChanged: (String) -> Unit,
    onClearText: () -> Unit,
    onAnalyze: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TEXT",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )

            if (text.isNotEmpty()) {
                TextButton(
                    onClick = onClearText,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear Text",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        OutlinedTextField(
            value = text,
            onValueChange = onTextChanged,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            placeholder = {
                Text(
                    text = "Paste or type your text here...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )

        // Live stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LiveStatBadge("Characters", String.format(Locale.US, "%,d", liveChars))
                LiveStatBadge("Words", String.format(Locale.US, "%,d", liveWords))
                LiveStatBadge("Lines", String.format(Locale.US, "%,d", liveLines))
            }

            if (isAnalyzing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Button(
            onClick = onAnalyze,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "ANALYZE TEXT",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun LiveStatBadge(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ErrorMessageCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun FilterSummaryCard(
    settings: AppSettings,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onToggle() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTIVE FILTERS & SETTINGS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle Filters",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterItemRow("Length Unit", settings.lengthUnit.label)
                    FilterItemRow("Case", settings.caseSensitivity.label)
                    FilterItemRow("Punctuation", "Ignored around words")
                    FilterItemRow("Numbers", settings.numberMode.label)
                    FilterItemRow("Apostrophes", settings.apostropheMode.label)
                    FilterItemRow("Hyphens", settings.hyphenMode.label)
                    FilterItemRow("Stop Words", settings.stopWordsMode.label)
                    FilterItemRow("Minimum Length", "${settings.minimumLength} characters")
                    FilterItemRow("Maximum Length", settings.maximumLength?.let { "$it characters" } ?: "Unlimited")
                    if (settings.customExclusions.isNotEmpty()) {
                        FilterItemRow("Custom Exclusions", "${settings.customExclusions.size} words")
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterItemRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionButtonsSection(
    context: Context,
    viewModel: WordLengthViewModel,
    onShowSnackbar: (String) -> Unit
) {
    var showCsvMenu by remember { mutableStateOf(false) }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = {
                viewModel.copyToClipboard(context, viewModel.buildSummaryText())
                onShowSnackbar("Summary copied to clipboard")
            },
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("COPY SUMMARY", style = MaterialTheme.typography.labelSmall)
        }

        OutlinedButton(
            onClick = {
                viewModel.copyToClipboard(context, viewModel.buildAllWordsText())
                onShowSnackbar("All word results copied")
            },
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("COPY ALL", style = MaterialTheme.typography.labelSmall)
        }

        Box {
            OutlinedButton(
                onClick = { showCsvMenu = true },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("CSV", style = MaterialTheme.typography.labelSmall)
            }

            DropdownMenu(
                expanded = showCsvMenu,
                onDismissRequest = { showCsvMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Copy Words CSV") },
                    onClick = {
                        showCsvMenu = false
                        viewModel.copyToClipboard(context, viewModel.generateWordsCsv())
                        onShowSnackbar("Words CSV copied to clipboard")
                    }
                )
                DropdownMenuItem(
                    text = { Text("Copy Distribution CSV") },
                    onClick = {
                        showCsvMenu = false
                        viewModel.copyToClipboard(context, viewModel.generateDistributionCsv())
                        onShowSnackbar("Distribution CSV copied to clipboard")
                    }
                )
            }
        }

        OutlinedButton(
            onClick = { viewModel.shareSummary(context) },
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("SHARE", style = MaterialTheme.typography.labelSmall)
        }

        OutlinedButton(
            onClick = { viewModel.saveCurrentAnalysis() },
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("SAVE", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun MainStatsGrid(
    totalWords: Int,
    uniqueWords: Int,
    averageLength: Double?,
    medianLength: Double?,
    showMedian: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Total Words",
                value = String.format(Locale.US, "%,d", totalWords),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Unique Words",
                value = String.format(Locale.US, "%,d", uniqueWords),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Average Length",
                value = averageLength?.let { String.format(Locale.US, "%.2f", it) } ?: "—",
                unit = "characters",
                modifier = Modifier.weight(1f)
            )
            if (showMedian) {
                StatCard(
                    title = "Median Length",
                    value = medianLength?.let {
                        if (it % 1.0 == 0.0) it.toInt().toString() else String.format(Locale.US, "%.1f", it)
                    } ?: "—",
                    unit = "characters",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ShortestLongestSection(
    shortestWords: List<String>,
    minimumLength: Int?,
    longestWords: List<String>,
    maximumLength: Int?,
    showAllShortest: Boolean,
    showAllLongest: Boolean,
    onToggleShortest: () -> Unit,
    onToggleLongest: () -> Unit,
    onCopy: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ExtremumCard(
            title = if (shortestWords.size > 1) "SHORTEST WORDS (${shortestWords.size} TIED)" else "SHORTEST WORD",
            words = shortestWords,
            length = minimumLength ?: 0,
            showAll = showAllShortest,
            onToggleShowAll = onToggleShortest,
            onCopy = onCopy
        )

        ExtremumCard(
            title = if (longestWords.size > 1) "LONGEST WORDS (${longestWords.size} TIED)" else "LONGEST WORD",
            words = longestWords,
            length = maximumLength ?: 0,
            showAll = showAllLongest,
            onToggleShowAll = onToggleLongest,
            onCopy = onCopy
        )
    }
}

@Composable
private fun ExtremumCard(
    title: String,
    words: List<String>,
    length: Int,
    showAll: Boolean,
    onToggleShowAll: () -> Unit,
    onCopy: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                if (words.isNotEmpty()) {
                    IconButton(
                        onClick = { onCopy(words.joinToString(", ")) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy words",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            if (words.isEmpty()) {
                Text(text = "—", style = MaterialTheme.typography.headlineSmall)
            } else {
                val displayWords = if (showAll || words.size <= 3) words else words.take(3)

                displayWords.forEach { w ->
                    Text(
                        text = w,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (words.size > 3) {
                    TextButton(
                        onClick = onToggleShowAll,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(if (showAll) "Show Less" else "View All (${words.size})")
                    }
                }

                val unitText = if (words.size > 1) "$length characters each" else "$length character${if (length != 1) "s" else ""}"
                Text(
                    text = unitText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ModeCard(
    modeLengths: List<Int>,
    distribution: Map<Int, Int>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "MOST COMMON LENGTH",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )

            modeLengths.forEach { len ->
                val count = distribution[len] ?: 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$len character${if (len != 1) "s" else ""}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$count word${if (count != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun RangeAndCharactersCard(
    totalCharacters: Int,
    averageLength: Double?,
    minimumLength: Int?,
    maximumLength: Int?,
    range: Int?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "TOTAL WORD CHARACTERS",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format(Locale.US, "%,d", totalCharacters),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "AVG CHARACTERS/WORD",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = averageLength?.let { String.format(Locale.US, "%.2f", it) } ?: "—",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .padding(vertical = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WORD LENGTH RANGE",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${minimumLength ?: 0} – ${maximumLength ?: 0} chars (Range: ${range ?: 0})",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun UniqueAnalysisCard(
    uniqueWords: Int,
    averageUniqueLength: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "UNIQUE WORD LENGTH ANALYSIS",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Unique Words: $uniqueWords", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Average: ${String.format(Locale.US, "%.2f", averageUniqueLength)} chars",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DistributionCard(
    distribution: Map<Int, Int>,
    totalWords: Int,
    onLengthSelected: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "WORD LENGTH DISTRIBUTION",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )

            DistributionBarChart(
                distribution = distribution,
                totalWords = totalWords,
                onLengthSelected = onLengthSelected
            )
        }
    }
}

@Composable
private fun HistogramCard(
    distribution: Map<Int, Int>,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LENGTH HISTOGRAM",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle Histogram",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    LengthHistogramChart(distribution = distribution)
                }
            }
        }
    }
}

@Composable
private fun LengthGroupsCard(
    groups: List<com.example.wordlength.data.model.LengthGroup>,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LENGTH GROUPS",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle Groups",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    groups.forEach { g ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = g.label, style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = "${g.count} words (${String.format(Locale.US, "%.1f", g.percentage)}%)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LengthSummaryTableCard(
    distribution: Map<Int, Int>,
    totalWords: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LENGTH SUMMARY TABLE",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle Summary Table",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Length", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("Words", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                        Text("%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                    }

                    distribution.forEach { (len, count) ->
                        val pct = if (totalWords > 0) (count.toDouble() / totalWords) * 100.0 else 0.0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("$len", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                            Text("$count", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                            Text("${String.format(Locale.US, "%.2f", pct)}%", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WordListHeader(
    searchQuery: String,
    searchMatchMode: SearchMatchMode,
    sortOrder: WordSortOrder,
    resultCount: Int,
    onQueryChanged: (String) -> Unit,
    onMatchModeChanged: (SearchMatchMode) -> Unit,
    onSortOrderChanged: (WordSortOrder) -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "WORDS ($resultCount)",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )

            Box {
                TextButton(
                    onClick = { showSortMenu = true },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(sortOrder.label, style = MaterialTheme.typography.labelSmall)
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    WordSortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = { Text(order.label) },
                            onClick = {
                                onSortOrderChanged(order)
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search words...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onQueryChanged("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
        )

        // Search match mode options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SearchMatchMode.entries.forEach { mode ->
                FilterChip(
                    selected = searchMatchMode == mode,
                    onClick = { onMatchModeChanged(mode) },
                    label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}

@Composable
private fun WordItemRow(
    result: WordLengthResult,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.word,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${result.frequency} occurrence${if (result.frequency != 1) "s" else ""} • ${String.format(Locale.US, "%.1f", result.percentage)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "${result.length} chars",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
