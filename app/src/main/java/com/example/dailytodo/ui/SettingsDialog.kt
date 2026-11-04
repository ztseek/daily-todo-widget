package com.example.dailytodo.ui

import android.app.AlarmManager
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.dailytodo.schedule.AlarmScheduler

/**
 * 设置。
 *
 * 后两项不是"功能"，而是**修复系统限制的入口**：
 * 精确闹钟和电池优化是这类定时类 app 在真机上失效的第一、第二原因，
 * 与其让用户自己摸索，不如把开关摆出来。
 */
@Composable
fun SettingsDialog(
    countOverdue: Boolean,
    onCountOverdueChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    val exactAlarmGranted = remember {
        val am = context.getSystemService(AlarmManager::class.java)
        am != null && AlarmScheduler.canScheduleExact(am)
    }
    val batteryUnrestricted = remember {
        val pm = context.getSystemService(PowerManager::class.java)
        pm?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("设置", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))

                SettingSwitchRow(
                    title = "记录逾期次数",
                    desc = "关闭时：到期没点掉的任务原样留着。打开后：每越过一个刷新点仍未完成，" +
                            "逾期次数 +1，并在小部件上标红。",
                    checked = countOverdue,
                    onCheckedChange = onCountOverdueChange
                )

                Spacer(Modifier.height(18.dp))

                SettingActionRow(
                    title = "允许精确闹钟",
                    desc = if (exactAlarmGranted) "已允许，待办会准时回到桌面"
                    else "未允许，刷新可能延迟十几分钟",
                    actionText = if (exactAlarmGranted) "已开启" else "去开启",
                    enabled = !exactAlarmGranted,
                    onClick = {
                        AlarmScheduler.exactAlarmSettingsIntent()?.let { intent ->
                            runCatching { context.startActivity(intent) }
                        }
                    }
                )

                Spacer(Modifier.height(18.dp))

                SettingActionRow(
                    title = "关闭电池优化",
                    desc = if (batteryUnrestricted) "已加入不受限制名单"
                    else "部分国产系统会清理后台，导致定时刷新失效",
                    actionText = if (batteryUnrestricted) "已设置" else "去设置",
                    enabled = !batteryUnrestricted,
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            )
                        }
                    }
                )

                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("关闭") }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingActionRow(
    title: String,
    desc: String,
    actionText: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(onClick = onClick, enabled = enabled) { Text(actionText) }
    }
}
