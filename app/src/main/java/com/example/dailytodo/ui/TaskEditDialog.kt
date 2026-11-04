package com.example.dailytodo.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.dailytodo.data.RepeatType

/**
 * 新建 / 编辑待办。
 *
 * 交互设计上的两个决定：
 * 1. 周期参数全部用点选和加减，不用键盘输入 —— 手机上打字最烦，
 *    而「每 3 天」「每月 15 日」这种值范围有限，点选更快也更不容易输入非法值。
 * 2. 只有标题需要键盘。
 */
@Composable
fun TaskEditDialog(
    draft: TaskDraft,
    onDismiss: () -> Unit,
    onSave: (TaskDraft) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var current by remember(draft) { mutableStateOf(draft) }
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = if (draft.id == 0L) "新建待办" else "编辑待办",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = current.title,
                    onValueChange = { current = current.copy(title = it) },
                    label = { Text("待办内容") },
                    placeholder = { Text("例如：吃降压药") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(20.dp))
                Label("刷新周期")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RepeatType.entries.forEach { type ->
                        FilterChip(
                            selected = current.repeatType == type,
                            onClick = { current = current.copy(repeatType = type) },
                            label = { Text(typeLabel(type)) }
                        )
                    }
                }

                // 不同周期只显示相关的参数，避免一屏都是用不到的选项
                when (current.repeatType) {
                    RepeatType.WEEKLY -> {
                        Spacer(Modifier.height(14.dp))
                        Label("星期几刷新（可多选）")
                        WeekdayPicker(
                            mask = current.weekdays,
                            onToggle = { bit ->
                                val next = current.weekdays xor bit
                                // 至少保留一天，否则这条规则就没有意义了
                                current = current.copy(weekdays = if (next == 0) bit else next)
                            }
                        )
                    }

                    RepeatType.MONTHLY -> {
                        Spacer(Modifier.height(14.dp))
                        Label("每月哪一天")
                        Stepper(
                            value = current.monthDay,
                            min = 1,
                            max = 31,
                            suffix = "日",
                            onChange = { current = current.copy(monthDay = it) }
                        )
                        Text(
                            text = "遇到小月会自动落到月末（如 31 日 → 2 月 28 日）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    RepeatType.EVERY_N_DAYS -> {
                        Spacer(Modifier.height(14.dp))
                        Label("间隔天数")
                        Stepper(
                            value = current.intervalDays,
                            min = 1,
                            max = 60,
                            suffix = "天",
                            onChange = { current = current.copy(intervalDays = it) }
                        )
                    }

                    else -> Unit
                }

                Spacer(Modifier.height(18.dp))
                Label("刷新时刻")
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    current = current.copy(refreshHour = hour, refreshMinute = minute)
                                },
                                current.refreshHour,
                                current.refreshMinute,
                                true
                            ).show()
                        },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "%02d:%02d".format(current.refreshHour, current.refreshMinute),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "点此修改",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (draft.repeatType != RepeatType.ONCE) {
                    Text(
                        text = "到期后会在该时刻重新出现在桌面小部件上",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onDelete != null) {
                        TextButton(onClick = onDelete) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(current) },
                        enabled = current.title.isNotBlank()
                    ) { Text("保存") }
                }
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun WeekdayPicker(mask: Int, onToggle: (Int) -> Unit) {
    val names = listOf("一", "二", "三", "四", "五", "六", "日")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        names.forEachIndexed { index, name ->
            val bit = 1 shl index
            val selected = mask and bit != 0
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clickable { onToggle(bit) },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Stepper(value: Int, min: Int, max: Int, suffix: String, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepButton("-") { onChange((value - 1).coerceAtLeast(min)) }
        Text(
            text = "$value $suffix",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .width(76.dp)
                .padding(horizontal = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        StepButton("+") { onChange((value + 1).coerceAtMost(max)) }
    }
}

@Composable
private fun StepButton(symbol: String, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier
            .size(38.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(symbol, style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun typeLabel(type: RepeatType): String = when (type) {
    RepeatType.ONCE -> "一次性"
    RepeatType.DAILY -> "每天"
    RepeatType.WEEKLY -> "每周"
    RepeatType.MONTHLY -> "每月"
    RepeatType.EVERY_N_DAYS -> "隔几天"
}
