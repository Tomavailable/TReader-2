package com.example.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import java.util.Locale

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SplitMode
import com.example.data.TtsVoiceItem

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SettingsBottomSheet(
    viewModel: TtsReaderViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val voices by viewModel.ttsManager.availableVoices.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var editingSplitMode by remember { mutableStateOf<SplitMode?>(null) }
    var showSecondarySplitDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header with clear Close button on top right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "播放与语音设置",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_settings_btn")
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // 1. TTS 语言 (折叠卡片 1)
            // ==========================================
            ExpandableSettingSection(
                title = "TTS语言",
                subtitle = "目标发音、3人轮发音、语速与音调",
                icon = {
                    Icon(
                        Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                initiallyExpanded = false
            ) {
                // Target TTS Language
                Text(
                    text = "目标朗读语言",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                val languages = listOf(
                    "en-US" to "英语 (en-US)",
                    "all" to "全部可用声音"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    languages.forEach { (code, label) ->
                        FilterChip(
                            selected = uiState.selectedLanguage == code,
                            onClick = { viewModel.setLanguage(code) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("lang_chip_$code")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Multi-speaker rotation switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Groups,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "3位发音人轮流朗读",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "每句由发音人1、2、3依次朗读后再推进",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = uiState.multiSpeakerEnabled,
                        onCheckedChange = { viewModel.setMultiSpeakerEnabled(it) },
                        modifier = Modifier.testTag("multi_speaker_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 常用发音人 (默认展示 3 位发音人)
                VoiceSelectorItem(
                    speakerNumber = 1,
                    label = "发音人 1 (主音)",
                    selectedVoiceId = uiState.speaker1VoiceId,
                    rate = uiState.speaker1Rate,
                    pitch = uiState.speaker1Pitch,
                    allowAdjust = true,
                    voices = voices,
                    selectedLanguage = uiState.selectedLanguage,
                    onSelectVoice = { viewModel.setSpeakerVoice(0, it) },
                    onRateChange = { viewModel.setSpeakerRate(0, it) },
                    onPitchChange = { viewModel.setSpeakerPitch(0, it) },
                    onAudition = { viewModel.testVoice(0) }
                )

                Spacer(modifier = Modifier.height(8.dp))
                VoiceSelectorItem(
                    speakerNumber = 2,
                    label = "发音人 2",
                    selectedVoiceId = uiState.speaker2VoiceId,
                    rate = uiState.speaker2Rate,
                    pitch = uiState.speaker2Pitch,
                    allowAdjust = true,
                    voices = voices,
                    selectedLanguage = uiState.selectedLanguage,
                    onSelectVoice = { viewModel.setSpeakerVoice(1, it) },
                    onRateChange = { viewModel.setSpeakerRate(1, it) },
                    onPitchChange = { viewModel.setSpeakerPitch(1, it) },
                    onAudition = { viewModel.testVoice(1) }
                )

                Spacer(modifier = Modifier.height(8.dp))
                VoiceSelectorItem(
                    speakerNumber = 3,
                    label = "发音人 3",
                    selectedVoiceId = uiState.speaker3VoiceId,
                    rate = uiState.speaker3Rate,
                    pitch = uiState.speaker3Pitch,
                    allowAdjust = true,
                    voices = voices,
                    selectedLanguage = uiState.selectedLanguage,
                    onSelectVoice = { viewModel.setSpeakerVoice(2, it) },
                    onRateChange = { viewModel.setSpeakerRate(2, it) },
                    onPitchChange = { viewModel.setSpeakerPitch(2, it) },
                    onAudition = { viewModel.testVoice(2) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Loop repeat count
                Text(
                    text = "单句循环播放次数",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                val loopCounts = listOf(
                    1 to "1x",
                    2 to "2x",
                    3 to "3x",
                    5 to "5x",
                    999 to "∞"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    loopCounts.forEach { (count, label) ->
                        FilterChip(
                            selected = uiState.targetRepeatCount == count,
                            onClick = { viewModel.setTargetRepeatCount(count) },
                            label = { Text(label, fontSize = 13.sp) },
                            modifier = Modifier.testTag("loop_chip_$count")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // 2. 词典设置 (折叠卡片 2)
            // ==========================================
            ExpandableSettingSection(
                title = "词典设置",
                subtitle = "查词交互方案与外部词典小窗设置",
                icon = {
                    Icon(
                        Icons.Default.Translate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                initiallyExpanded = false
            ) {
                var isEudicExpanded by remember { mutableStateOf(false) }
                var showAppPickerDialog by remember { mutableStateOf(false) }
                var isScheme1Expanded by remember { mutableStateOf(true) }

                Text(
                    text = "查词响应模式：",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 方案一：优先调起词典软件（内部折叠 4 个外部词典应用选项）
                    val isDirectSelected = uiState.wordLookupMode == WordLookupMode.DIRECT_DICT
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDirectSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = if (isDirectSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        viewModel.setWordLookupMode(WordLookupMode.DIRECT_DICT)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    RadioButton(
                                        selected = isDirectSelected,
                                        onClick = { viewModel.setWordLookupMode(WordLookupMode.DIRECT_DICT) }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(WordLookupMode.DIRECT_DICT.title, fontWeight = if (isDirectSelected) FontWeight.Bold else FontWeight.Normal)
                                        Text(
                                            text = "当前默认应用: ${uiState.defaultDictApp.label}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { isScheme1Expanded = !isScheme1Expanded },
                                    modifier = Modifier.size(36.dp).testTag("scheme1_expand_btn")
                                ) {
                                    Icon(
                                        imageVector = if (isScheme1Expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = if (isScheme1Expanded) "折叠外部词典选项" else "展开外部词典选项",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            if (isScheme1Expanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(bottom = 6.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = "选择方案一默认调起的外部词典软件：",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )

                                    // 1. 欧路词典 (默认)
                                    val isEudicSelected = uiState.defaultDictApp == DictAppOption.EUDIC
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isEudicSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        border = if (isEudicSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { viewModel.setDefaultDictApp(DictAppOption.EUDIC) }
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    RadioButton(
                                                        selected = isEudicSelected,
                                                        onClick = { viewModel.setDefaultDictApp(DictAppOption.EUDIC) }
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Column {
                                                        Text("欧路词典 (默认)", fontWeight = if (isEudicSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                                                        Text(
                                                            text = "调起: ${uiState.eudicInvokeMode.title.substringBefore("：")}",
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }

                                                IconButton(
                                                    onClick = { isEudicExpanded = !isEudicExpanded },
                                                    modifier = Modifier.size(32.dp).testTag("eudic_expand_triangle_btn")
                                                ) {
                                                    Icon(
                                                        imageVector = if (isEudicExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }

                                            if (isEudicExpanded) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    EudicInvokeMode.entries.forEach { mode ->
                                                        FilterChip(
                                                            selected = uiState.eudicInvokeMode == mode,
                                                            onClick = { viewModel.setEudicInvokeMode(mode) },
                                                            label = {
                                                                Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                                                    Text(mode.title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                                    Text(mode.desc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                                }
                                                            },
                                                            modifier = Modifier.fillMaxWidth().testTag("eudic_mode_${mode.name}")
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // 2. 谷歌翻译
                                    val isGoogleSelected = uiState.defaultDictApp == DictAppOption.GOOGLE_TRANSLATE
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isGoogleSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        border = if (isGoogleSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { viewModel.setDefaultDictApp(DictAppOption.GOOGLE_TRANSLATE) }
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isGoogleSelected,
                                                onClick = { viewModel.setDefaultDictApp(DictAppOption.GOOGLE_TRANSLATE) }
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("谷歌翻译", fontSize = 13.sp, fontWeight = if (isGoogleSelected) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    }

                                    // 3. 自定义词典软件
                                    val isCustomSelected = uiState.defaultDictApp == DictAppOption.CUSTOM_APP
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isCustomSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        border = if (isCustomSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.setDefaultDictApp(DictAppOption.CUSTOM_APP)
                                                    if (uiState.customDictPackageName == null) {
                                                        showAppPickerDialog = true
                                                    }
                                                }
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                RadioButton(
                                                    selected = isCustomSelected,
                                                    onClick = {
                                                        viewModel.setDefaultDictApp(DictAppOption.CUSTOM_APP)
                                                        if (uiState.customDictPackageName == null) {
                                                            showAppPickerDialog = true
                                                        }
                                                    }
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Column {
                                                    Text("自定义词典软件", fontSize = 13.sp, fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Normal)
                                                    Text(
                                                        text = if (uiState.customDictAppName != null) "已选: ${uiState.customDictAppName}" else "选取已安装的任意词典/翻译应用",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1
                                                    )
                                                }
                                            }

                                            OutlinedButton(
                                                onClick = { showAppPickerDialog = true },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(30.dp).testTag("select_custom_app_btn")
                                            ) {
                                                Text(if (uiState.customDictAppName != null) "更换" else "选取软件", fontSize = 11.sp)
                                            }
                                        }
                                    }

                                    // 4. 系统通用划词
                                    val isSystemSelected = uiState.defaultDictApp == DictAppOption.SYSTEM_CHOOSER
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSystemSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        border = if (isSystemSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { viewModel.setDefaultDictApp(DictAppOption.SYSTEM_CHOOSER) }
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isSystemSelected,
                                                onClick = { viewModel.setDefaultDictApp(DictAppOption.SYSTEM_CHOOSER) }
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("系统通用划词 (每次询问)", fontSize = 13.sp, fontWeight = if (isSystemSelected) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 方案二：静读天下风格快捷菜单
                    val isPopoverSelected = uiState.wordLookupMode == WordLookupMode.POPOVER_MENU
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPopoverSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = if (isPopoverSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setWordLookupMode(WordLookupMode.POPOVER_MENU) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isPopoverSelected,
                                onClick = { viewModel.setWordLookupMode(WordLookupMode.POPOVER_MENU) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(WordLookupMode.POPOVER_MENU.title, fontWeight = if (isPopoverSelected) FontWeight.Bold else FontWeight.Normal)
                                Text(
                                    text = WordLookupMode.POPOVER_MENU.desc,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (showAppPickerDialog) {
                    InstalledAppPickerDialog(
                        onDismiss = { showAppPickerDialog = false },
                        onAppSelected = { pkg, name ->
                            viewModel.setCustomDictApp(pkg, name)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // 3. 睡眠模式 (折叠卡片 3)
            // ==========================================
            ExpandableSettingSection(
                title = "睡眠模式",
                subtitle = if (uiState.sleepTimerMinutes > 0) "将在 ${uiState.sleepTimerRemainingSeconds / 60} 分钟后暂停" else "定时停止播放",
                icon = {
                    Icon(
                        Icons.Default.Bedtime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                initiallyExpanded = false
            ) {
                Text(
                    text = "定时结束后自动暂停播放",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                val sleepOptions = listOf(
                    0 to "关闭",
                    15 to "15",
                    30 to "30",
                    45 to "45",
                    60 to "60"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sleepOptions.forEach { (mins, lbl) ->
                        FilterChip(
                            selected = uiState.sleepTimerMinutes == mins,
                            onClick = { viewModel.setSleepTimer(mins) },
                            label = { Text(lbl, fontSize = 12.sp) },
                            modifier = Modifier.testTag("sleep_chip_$mins")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // 4. 显示模式 (折叠卡片 4)
            // ==========================================
            ExpandableSettingSection(
                title = "显示模式",
                subtitle = "深浅色外观主题与界面动效",
                icon = {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                initiallyExpanded = false
            ) {
                Text(
                    text = "色彩主题：",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.themeMode == ThemeMode.SYSTEM,
                        onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                        label = { Text("自动", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.BrightnessAuto, contentDescription = null, modifier = Modifier.size(15.dp))
                        },
                        modifier = Modifier.weight(1f).testTag("theme_chip_system")
                    )
                    FilterChip(
                        selected = uiState.themeMode == ThemeMode.DARK,
                        onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                        label = { Text("夜间", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(15.dp))
                        },
                        modifier = Modifier.weight(1f).testTag("theme_chip_dark")
                    )
                    FilterChip(
                        selected = uiState.themeMode == ThemeMode.LIGHT,
                        onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                        label = { Text("日间", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(15.dp))
                        },
                        modifier = Modifier.weight(1f).testTag("theme_chip_light")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("界面动效与平滑滚动", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("默认关闭以降低耗电与重组开销；开启后朗读滚动使用平滑动效", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = uiState.isAnimationEnabled,
                        onCheckedChange = { viewModel.setAnimationEnabled(it) },
                        modifier = Modifier.testTag("animation_toggle_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // 5. 断句逻辑 (折叠卡片 5)
            // ==========================================
            ExpandableSettingSection(
                title = "断句逻辑",
                subtitle = "文本切分与分句模式",
                icon = {
                    Icon(
                        Icons.Default.Spellcheck,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                initiallyExpanded = false
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Split Modes
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SplitMode.values().forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (uiState.splitMode == mode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                                    .clickable { viewModel.setSplitMode(mode) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    RadioButton(
                                        selected = uiState.splitMode == mode,
                                        onClick = { viewModel.setSplitMode(mode) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = mode.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (uiState.splitMode == mode) FontWeight.Bold else FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = mode.shortDesc,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { editingSplitMode = mode },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("split_mode_settings_${mode.name}")
                                ) {
                                    Icon(
                                        imageVector = if (mode == SplitMode.PARAGRAPH_FLOW) Icons.Default.Tune else Icons.Default.Edit,
                                        contentDescription = if (mode == SplitMode.PARAGRAPH_FLOW) "排版与字体设置" else "编辑 ${mode.title} 规则",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "智能二次拆分长句",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (uiState.isSplitEnabled) "已开启：超过 ${uiState.secondarySplitMinLength} 字按标点拆分" else "未开启：长句保持完整",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { showSecondarySplitDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("secondary_split_edit_icon_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "编辑二次拆分规则",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            ExpandableSettingSection(
                title = "关于我 \\^O^/",
                subtitle = "应用特色与功能简介",
                icon = {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                initiallyExpanded = false
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "📖 智能朗读与多发音人",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "· 原生 TTS 语音合成，支持多语言、多发音人轮流朗读与单句循环；可即时微调语速与音高，轻松实现分角色朗读与语言听力训练。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Text(
                        text = "✂️ 智能断句与抗干扰",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "· 系统国际断句：准确按语言学小句分割，停顿自然。\n· 长句模式：保留整句完整，平滑清除软换行。\n· 智能规避：全模式避开小数点（3.14）、网址（open.ai）、英文缩写（Dr. Smith）及软换行。\n· 智能拆分：开启拆分后，依据次级标点在中点自动均衡拆分（150字以上最多拆3段）。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Text(
                        text = "📚 词典与生词划词查询",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "· 第三方词典调起：支持一键调起欧路词典等应用内查词小窗，或绑定手机自定义词典软件。\n· 查词响应模式：支持直接调起小窗或快捷图标工具栏。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Text(
                        text = "💾 本地离线与音频导出",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "· 历史文章自动存储，断点续读不丢失；支持将朗读合成的音频导出保存，方便离线听书与练习。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "祝您使用愉快！\\^O^/",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }

        if (editingSplitMode != null) {
            when (editingSplitMode) {
                SplitMode.SMART, SplitMode.SCHEME_A -> {
                    SchemeAEditDialog(
                        title = "${editingSplitMode?.title} 规则设置",
                        uiState = uiState,
                        viewModel = viewModel,
                        onDismiss = { editingSplitMode = null }
                    )
                }
                SplitMode.PARAGRAPH_FLOW -> {
                    ParagraphFlowEditDialog(
                        uiState = uiState,
                        viewModel = viewModel,
                        onDismiss = { editingSplitMode = null }
                    )
                }
                SplitMode.PUNCTUATION -> {
                    SchemeCEditDialog(
                        title = "${editingSplitMode?.title} 规则设置",
                        uiState = uiState,
                        viewModel = viewModel,
                        onDismiss = { editingSplitMode = null }
                    )
                }
                SplitMode.SIMPLE -> {
                    SimpleSchemeEditDialog(
                        uiState = uiState,
                        viewModel = viewModel,
                        onDismiss = { editingSplitMode = null }
                    )
                }
                null -> {}
            }
        }

        if (showSecondarySplitDialog) {
            SecondarySplitEditDialog(
                uiState = uiState,
                viewModel = viewModel,
                onDismiss = { showSecondarySplitDialog = false }
            )
        }
    }
}

@Composable
private fun VoiceSelectorItem(
    speakerNumber: Int,
    label: String,
    selectedVoiceId: String?,
    rate: Float,
    pitch: Float,
    allowAdjust: Boolean = true,
    voices: List<TtsVoiceItem>,
    selectedLanguage: String,
    onSelectVoice: (String) -> Unit,
    onRateChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onAudition: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showAdjustDialog by remember { mutableStateOf(false) }

    val filteredVoices = remember(voices, selectedLanguage) {
        if (selectedLanguage == "all") voices else {
            val prefix = selectedLanguage.substringBefore("-").lowercase()
            val list = voices.filter { it.locale.language.lowercase().startsWith(prefix) }
            if (list.isNotEmpty()) list else voices
        }
    }

    val currentVoice = remember(selectedVoiceId, filteredVoices, voices) {
        filteredVoices.firstOrNull { it.id == selectedVoiceId }
            ?: voices.firstOrNull { it.id == selectedVoiceId }
            ?: filteredVoices.getOrNull(speakerNumber - 1)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                if (allowAdjust) {
                    Text(
                        text = "语速 ${String.format("%.2f", rate)}x · 音调 ${String.format("%.2f", pitch)}x",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = true }
                            .testTag("voice_select_btn_$speakerNumber"),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currentVoice?.name ?: "默认发音人",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.75f)
                    ) {
                        if (filteredVoices.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("系统默认语音") },
                                onClick = { expanded = false }
                            )
                        } else {
                            filteredVoices.forEach { voice ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            voice.name,
                                            maxLines = 1,
                                            fontSize = 13.sp
                                        )
                                    },
                                    onClick = {
                                        onSelectVoice(voice.id)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 设置按钮：仅在允许多发音人调节时显示
                if (allowAdjust) {
                    FilledTonalIconButton(
                        onClick = { showAdjustDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("voice_tune_btn_$speakerNumber")
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "调节语速音调",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // 试听按钮
                Button(
                    onClick = onAudition,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("audition_btn_$speakerNumber")
                ) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = "试听",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("试听", fontSize = 12.sp)
                }
            }
        }
    }

    if (allowAdjust && showAdjustDialog) {
        AlertDialog(
            onDismissRequest = { showAdjustDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("发音人 $speakerNumber 音效微调", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 语速调节
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("朗读语速", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("${String.format("%.2f", rate)}x", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = rate,
                            onValueChange = onRateChange,
                            valueRange = 0.5f..2.5f,
                            steps = 19
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(0.85f to "0.85", 1.0f to "1.0", 1.1f to "1.1", 1.25f to "1.25", 1.5f to "1.5").forEach { (r, lbl) ->
                                val isSelected = kotlin.math.abs(rate - r) < 0.02f
                                OutlinedButton(
                                    onClick = { onRateChange(r) },
                                    colors = if (isSelected) ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ) else ButtonDefaults.outlinedButtonColors(),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text(lbl, fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    // 音调调节
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("朗读音调", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("${String.format("%.2f", pitch)}x", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = pitch,
                            onValueChange = onPitchChange,
                            valueRange = 0.5f..2.0f,
                            steps = 14
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(0.8f to "0.8低沉", 1.0f to "1.0标准", 1.2f to "1.2清脆", 1.4f to "1.4高亢").forEach { (p, lbl) ->
                                val isSelected = kotlin.math.abs(pitch - p) < 0.02f
                                OutlinedButton(
                                    onClick = { onPitchChange(p) },
                                    colors = if (isSelected) ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ) else ButtonDefaults.outlinedButtonColors(),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text(lbl, fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    // 试听按钮
                    Button(
                        onClick = onAudition,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("试听此音效")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showAdjustDialog = false }) {
                    Text("完成")
                }
            }
        )
    }
}

private data class InstalledAppItem(
    val name: String,
    val packageName: String,
    val icon: android.graphics.drawable.Drawable?
)

@Composable
private fun InstalledAppPickerDialog(
    onDismiss: () -> Unit,
    onAppSelected: (packageName: String, appName: String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val pm = remember { context.packageManager }
    var searchQuery by remember { mutableStateOf("") }

    val installedApps = remember {
        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val launcherApps = pm.queryIntentActivities(mainIntent, 0)

            val textIntent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
                type = "text/plain"
            }
            val textApps = pm.queryIntentActivities(textIntent, 0)

            val all = (launcherApps + textApps).distinctBy { it.activityInfo.packageName }
            all.filter { it.activityInfo.packageName != context.packageName }
                .map {
                    InstalledAppItem(
                        name = it.loadLabel(pm).toString(),
                        packageName = it.activityInfo.packageName,
                        icon = try { it.loadIcon(pm) } catch (e: Exception) { null }
                    )
                }.sortedBy { it.name.lowercase() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) installedApps
        else installedApps.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("选择已安装的词典/翻译软件", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("搜索软件名称或包名...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                if (filteredApps.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("未找到相关应用", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredApps.size) { index ->
                            val app = filteredApps[index]
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAppSelected(app.packageName, app.name)
                                        onDismiss()
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    DrawableAppIcon(
                                        drawable = app.icon,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(app.name, fontWeight = FontWeight.Bold, maxLines = 1, fontSize = 14.sp)
                                        Text(app.packageName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun DrawableAppIcon(
    drawable: android.graphics.drawable.Drawable?,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(drawable) {
        if (drawable == null) return@remember null
        try {
            if (drawable is android.graphics.drawable.BitmapDrawable) {
                drawable.bitmap.asImageBitmap()
            } else {
                val w = drawable.intrinsicWidth.coerceIn(48, 144)
                val h = drawable.intrinsicHeight.coerceIn(48, 144)
                val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bmp)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bmp.asImageBitmap()
            }
        } catch (e: Exception) {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier
        )
    } else {
        Icon(
            imageVector = Icons.Default.Apps,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = modifier
        )
    }
}

@Composable
private fun ExpandableSettingSection(
    title: String,
    subtitle: String? = null,
    icon: @Composable () -> Unit,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    icon()
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "折叠" else "展开",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                    content()
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun CustomPunctRuleSection(
    title: String,
    desc: String,
    puncts: Set<Char>,
    onAdd: (Char) -> Unit,
    onRemove: (Char) -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            puncts.forEach { ch ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = ch.toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "删除 $ch",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onRemove(ch) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("输入要添加的符号", fontSize = 11.sp) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            )
            Button(
                onClick = {
                    inputText.trim().forEach { ch ->
                        onAdd(ch)
                    }
                    inputText = ""
                },
                enabled = inputText.trim().isNotEmpty(),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier.height(44.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("添加", fontSize = 12.sp)
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun CustomWordsRuleSection(
    title: String,
    desc: String,
    words: Set<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            words.forEach { word ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = word,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "删除 $word",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onRemove(word) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("输入避让英文缩写(如 mr, dr)", fontSize = 11.sp) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            )
            Button(
                onClick = {
                    val clean = inputText.trim()
                    if (clean.isNotEmpty()) {
                        onAdd(clean)
                    }
                    inputText = ""
                },
                enabled = inputText.trim().isNotEmpty(),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier.height(44.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("添加", fontSize = 12.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SchemeAEditDialog(
    title: String = "智能断句 规则设置",
    uiState: ReaderUiState,
    viewModel: TtsReaderViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "基于国际标准语言学断句算法，结合主断句标点、闭合引号及英文缩写避让，断句自然精准。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                CustomPunctRuleSection(
                    title = "主断句终止标点",
                    desc = "触发分句的核心标点符号（如句号、问号、感叹号、省略号）",
                    puncts = uiState.terminatorPuncts,
                    onAdd = { viewModel.addTerminatorPunct(it) },
                    onRemove = { viewModel.removeTerminatorPunct(it) }
                )

                CustomPunctRuleSection(
                    title = "闭合规避标点",
                    desc = "紧随终止符后的闭合引号或括号，避免标点与引号被割裂",
                    puncts = uiState.closingPuncts,
                    onAdd = { viewModel.addClosingPunct(it) },
                    onRemove = { viewModel.removeClosingPunct(it) }
                )

                CustomWordsRuleSection(
                    title = "英文缩写避让词",
                    desc = "遇到列表中以句点结尾的缩写词时自动忽略句点，不触发断句",
                    words = uiState.abbreviations,
                    onAdd = { viewModel.addAbbreviation(it) },
                    onRemove = { viewModel.removeAbbreviation(it) }
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("完成")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { viewModel.resetSchemeARules() }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("恢复默认")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SchemeCEditDialog(
    title: String = "标准标点 规则设置",
    uiState: ReaderUiState,
    viewModel: TtsReaderViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "段落软换行将被自动平滑消除，仅在指定的终止标点及闭合符号处切分，适合连续长句阅读。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                CustomPunctRuleSection(
                    title = "主断句终止标点",
                    desc = "触发句子结束的核心标点符号",
                    puncts = uiState.terminatorPuncts,
                    onAdd = { viewModel.addTerminatorPunct(it) },
                    onRemove = { viewModel.removeTerminatorPunct(it) }
                )

                CustomPunctRuleSection(
                    title = "闭合规避标点",
                    desc = "紧随终止符后的闭合引号或括号",
                    puncts = uiState.closingPuncts,
                    onAdd = { viewModel.addClosingPunct(it) },
                    onRemove = { viewModel.removeClosingPunct(it) }
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("完成")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { viewModel.resetPunctuationRules() }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("恢复默认")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParagraphFlowEditDialog(
    uiState: ReaderUiState,
    viewModel: TtsReaderViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "原文排版流式朗读 设置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "自然段落原貌排版，TTS 朗读时在正文内平滑高亮，无任何外层边框遮挡，带来纯粹的书卷阅读体验。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                // 1. Font size
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "正文字体大小",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${uiState.flowFontSize.toInt()} pt",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Slider(
                        value = uiState.flowFontSize,
                        onValueChange = { viewModel.setFlowFontSize(it) },
                        valueRange = 12f..32f,
                        steps = 19,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("flow_font_size_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(14f to "14pt", 16f to "16pt", 18f to "18pt(推荐)", 22f to "22pt", 26f to "26pt").forEach { (sz, label) ->
                            FilterChip(
                                selected = uiState.flowFontSize.toInt() == sz.toInt(),
                                onClick = { viewModel.setFlowFontSize(sz) },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 2. Line spacing / multiplier
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "文本行距",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "%.2fx", uiState.flowLineHeightMultiplier),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Slider(
                        value = uiState.flowLineHeightMultiplier,
                        onValueChange = { viewModel.setFlowLineHeightMultiplier(it) },
                        valueRange = 1.1f..2.4f,
                        steps = 12,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("flow_line_height_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(1.3f to "1.3x", 1.5f to "1.5x", 1.6f to "1.6x(推荐)", 1.8f to "1.8x", 2.0f to "2.0x").forEach { (mult, label) ->
                            FilterChip(
                                selected = kotlin.math.abs(uiState.flowLineHeightMultiplier - mult) < 0.05f,
                                onClick = { viewModel.setFlowLineHeightMultiplier(mult) },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 3. Paragraph spacing
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "段落间距",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${uiState.flowParagraphSpacing.toInt()} dp",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Slider(
                        value = uiState.flowParagraphSpacing,
                        onValueChange = { viewModel.setFlowParagraphSpacing(it) },
                        valueRange = 4f..28f,
                        steps = 11,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("flow_paragraph_spacing_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(6f to "6dp", 12f to "12dp(推荐)", 18f to "18dp", 24f to "24dp").forEach { (sp, label) ->
                            FilterChip(
                                selected = uiState.flowParagraphSpacing.toInt() == sp.toInt(),
                                onClick = { viewModel.setFlowParagraphSpacing(sp) },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 4. Indent
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "段落首行缩进",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "在每个段落开头保留 2 字符空隙，更符合中文书卷排版习惯",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.flowIndentParagraphs,
                        onCheckedChange = { viewModel.setFlowIndentParagraphs(it) },
                        modifier = Modifier.testTag("flow_indent_switch")
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 5. Custom Terminator Punctuations for TTS sentence identification
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "TTS 朗读终止标点",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "TTS 识别句子并断句停顿的核心标点。可直接点击已有标点删除，或在输入框添加新标点。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    CustomPunctRuleSection(
                        title = "触发分句终止符",
                        desc = "在此集合内的标点将作为 TTS 识别单句的结束符",
                        puncts = uiState.terminatorPuncts,
                        onAdd = { viewModel.addTerminatorPunct(it) },
                        onRemove = { viewModel.removeTerminatorPunct(it) }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 6. Live Preview
                Text(
                    text = "排版与高亮实时预览（无边框自然流式）：",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                val previewFontSize = uiState.flowFontSize.sp
                val previewLineHeight = (uiState.flowFontSize * uiState.flowLineHeightMultiplier).sp
                val indentPrefix = if (uiState.flowIndentParagraphs) "　　" else ""

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(uiState.flowParagraphSpacing.dp)
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                append(indentPrefix)
                                append("在浩瀚无垠的宇宙星空中，每一个恒星都有其独特的轨迹。")
                                withStyle(
                                    SpanStyle(
                                        background = MaterialTheme.colorScheme.primaryContainer,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                ) {
                                    append("朗读时当前播放句将在此高亮突出显示。")
                                }
                                append("其他句子则以优雅的书卷排版自然呈现。")
                            },
                            fontSize = previewFontSize,
                            lineHeight = previewLineHeight,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "${indentPrefix}第二段落文字清晰明了，行距与字体均根据上方参数即时渲染调整。",
                            fontSize = previewFontSize,
                            lineHeight = previewLineHeight,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("完成")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    viewModel.resetFlowTypographyRules()
                    viewModel.resetSchemeARules()
                }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("恢复默认")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleSchemeEditDialog(
    uiState: ReaderUiState,
    viewModel: TtsReaderViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "简易断句 规则设置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "纯标点规则高效切分，文本只要遇到以下任一标点符号即立即完成分句。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                CustomPunctRuleSection(
                    title = "触发分句标点",
                    desc = "在此集合内的所有标点都将作为断句点",
                    puncts = uiState.simplePuncts,
                    onAdd = { viewModel.addSimplePunct(it) },
                    onRemove = { viewModel.removeSimplePunct(it) }
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("完成")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { viewModel.resetSimpleRules() }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("恢复默认")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecondarySplitEditDialog(
    uiState: ReaderUiState,
    viewModel: TtsReaderViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "智能2次拆分 规则设置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "启用二次拆分长句",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (uiState.isSplitEnabled) "已启用：长句超过阈值自动拆分" else "未启用：长句保持完整不拆分",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = uiState.isSplitEnabled,
                        onCheckedChange = { viewModel.toggleSplitEnabled() }
                    )
                }

                Text(
                    text = "长句超过字数阈值时，自动在句子中间靠近中顿标点处拆分为均衡的两段或三段短句。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "触发拆分最小句长",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "句子字符数达到此长度时触发二次拆分（当前：${uiState.secondarySplitMinLength} 字）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(20, 30, 50, 80).forEach { len ->
                            val isSel = uiState.secondarySplitMinLength == len
                            FilterChip(
                                selected = isSel,
                                onClick = { viewModel.setSecondarySplitMinLength(len) },
                                label = { Text("${len}字", fontSize = 12.sp) }
                            )
                        }
                    }
                }

                CustomPunctRuleSection(
                    title = "二次拆分中顿标点",
                    desc = "拆分时优先在接近中点的这些标点位置切分（如逗号、分号、顿号、冒号、破折号）",
                    puncts = uiState.secondaryPuncts,
                    onAdd = { viewModel.addSecondaryPunct(it) },
                    onRemove = { viewModel.removeSecondaryPunct(it) }
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("完成")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { viewModel.resetSecondarySplitRules() }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("恢复默认")
            }
        }
    )
}

