package com.example.wordlength.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wordlength.data.model.ApostropheMode
import com.example.wordlength.data.model.AppTheme
import com.example.wordlength.data.model.CaseSensitivity
import com.example.wordlength.data.model.HyphenMode
import com.example.wordlength.data.model.LengthUnit
import com.example.wordlength.data.model.NumberMode
import com.example.wordlength.data.model.StopWordsMode
import com.example.wordlength.ui.viewmodel.WordLengthViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: WordLengthViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    var newExclusionText by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentPadding = PaddingValues(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Configure word measurement, tokenization, and display.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Appearance
        item {
            SettingsCategory(title = "APPEARANCE") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Theme", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppTheme.entries.forEach { themeOption ->
                            FilterChip(
                                selected = settings.theme == themeOption,
                                onClick = { viewModel.updateTheme(themeOption) },
                                label = { Text(themeOption.label) }
                            )
                        }
                    }

                    SettingsSwitchRow(
                        label = "Dynamic Color",
                        description = "Use system wallpaper colors on Android 12+",
                        checked = settings.dynamicColor,
                        onCheckedChange = viewModel::updateDynamicColor
                    )
                }
            }
        }

        // Length Unit
        item {
            SettingsCategory(title = "LENGTH MEASUREMENT UNIT") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LengthUnit.entries.forEach { unit ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.updateLengthUnit(unit) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.lengthUnit == unit,
                                onClick = { viewModel.updateLengthUnit(unit) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = unit.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = unit.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Tokenization & Filtering
        item {
            SettingsCategory(title = "ANALYSIS & TOKENIZATION") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Case
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Case Sensitivity", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CaseSensitivity.entries.forEach { opt ->
                                FilterChip(
                                    selected = settings.caseSensitivity == opt,
                                    onClick = { viewModel.updateCaseSensitivity(opt) },
                                    label = { Text(opt.label) }
                                )
                            }
                        }
                    }

                    // Numbers
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Count Numbers", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberMode.entries.forEach { opt ->
                                FilterChip(
                                    selected = settings.numberMode == opt,
                                    onClick = { viewModel.updateNumberMode(opt) },
                                    label = { Text(opt.label) }
                                )
                            }
                        }
                    }

                    // Apostrophes
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Apostrophe Handling", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ApostropheMode.entries.forEach { opt ->
                                FilterChip(
                                    selected = settings.apostropheMode == opt,
                                    onClick = { viewModel.updateApostropheMode(opt) },
                                    label = { Text(opt.label) }
                                )
                            }
                        }
                    }

                    // Hyphens
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Hyphen Handling", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HyphenMode.entries.forEach { opt ->
                                FilterChip(
                                    selected = settings.hyphenMode == opt,
                                    onClick = { viewModel.updateHyphenMode(opt) },
                                    label = { Text(opt.label) }
                                )
                            }
                        }
                    }

                    // Stop words
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Stop Words", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StopWordsMode.entries.forEach { opt ->
                                FilterChip(
                                    selected = settings.stopWordsMode == opt,
                                    onClick = { viewModel.updateStopWordsMode(opt) },
                                    label = { Text(opt.label) }
                                )
                            }
                        }
                    }

                    // Minimum Word Length
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Minimum Word Length: ${settings.minimumLength}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(1, 2, 3, 4, 5, 6).forEach { len ->
                                FilterChip(
                                    selected = settings.minimumLength == len,
                                    onClick = { viewModel.updateMinLength(len) },
                                    label = { Text("$len") }
                                )
                            }
                        }
                    }

                    // Maximum Word Length
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Maximum Word Length", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = settings.maximumLength == null,
                                onClick = { viewModel.updateMaxLength(null) },
                                label = { Text("Unlimited") }
                            )
                            listOf(10, 15, 20, 25, 30).forEach { maxLen ->
                                FilterChip(
                                    selected = settings.maximumLength == maxLen,
                                    onClick = { viewModel.updateMaxLength(maxLen) },
                                    label = { Text("$maxLen") }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Custom Exclusions
        item {
            SettingsCategory(title = "CUSTOM EXCLUSIONS") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Exclude specific words from analysis results.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newExclusionText,
                            onValueChange = { newExclusionText = it },
                            placeholder = { Text("Word to exclude") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedButton(
                            onClick = {
                                viewModel.addCustomExclusion(newExclusionText)
                                newExclusionText = ""
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ADD")
                        }
                    }

                    if (settings.customExclusions.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            settings.customExclusions.forEach { word ->
                                FilterChip(
                                    selected = true,
                                    onClick = { viewModel.removeCustomExclusion(word) },
                                    label = { Text(word) },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Result Display Settings
        item {
            SettingsCategory(title = "RESULT CARDS") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SettingsSwitchRow("Show Median", "Calculate and display median word length", settings.showMedian, viewModel::updateShowMedian)
                    SettingsSwitchRow("Show Mode", "Display most common word length(s)", settings.showMode, viewModel::updateShowMode)
                    SettingsSwitchRow("Show Distribution", "Display horizontal length frequency bars", settings.showDistribution, viewModel::updateShowDistribution)
                    SettingsSwitchRow("Show Chart", "Display vertical length histogram", settings.showChart, viewModel::updateShowChart)
                    SettingsSwitchRow("Show Unique Word Analysis", "Analyze average unique word length separately", settings.showUniqueAnalysis, viewModel::updateShowUniqueAnalysis)
                }
            }
        }

        // Behavior Settings
        item {
            SettingsCategory(title = "BEHAVIOR") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SettingsSwitchRow("Live Analysis", "Analyze text dynamically as you type (300ms debounce)", settings.liveAnalysis, viewModel::updateLiveAnalysis)
                    SettingsSwitchRow("Haptics", "Provide tactile feedback on actions", settings.haptics, viewModel::updateHaptics)
                    SettingsSwitchRow("Animations", "Enable smooth bar and state transitions", settings.animations, viewModel::updateAnimations)
                    SettingsSwitchRow("Confirm Delete", "Prompt before clearing history entries", settings.confirmDelete, viewModel::updateConfirmDelete)
                    SettingsSwitchRow("Restore Last Text", "Save and restore editor text across app launches", settings.restoreLastText, viewModel::updateRestoreLastText)
                }
            }
        }

        // Reset Settings Button
        item {
            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("RESET SETTINGS TO DEFAULT")
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset All Settings?") },
            text = { Text("This will revert all configuration options to their default values. Your saved history will not be deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetSettings()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsCategory(
    title: String,
    content: @Composable () -> Unit
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
                text = title,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
