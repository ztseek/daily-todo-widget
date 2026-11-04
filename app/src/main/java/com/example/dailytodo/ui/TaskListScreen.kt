package com.example.dailytodo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dailytodo.data.RepeatType
import com.example.dailytodo.data.Task
import com.example.dailytodo.domain.RecurrenceText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(vm: TaskViewModel = viewModel()) {
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val countOverdue by vm.countOverdue.collectAsStateWithLifecycle()

    // null = 关闭；非 null = 正在编辑（id==0 表示新建）
    var editing by remember { mutableStateOf<TaskDraft?>(null) }
    var editingOriginal by remember { mutableStateOf<Task?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    val pending = tasks.filter { it.completedAt == null }
    val done = tasks.filter { it.completedAt != null }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingOriginal = null
                    editing = TaskDraft(repeatType = RepeatType.DAILY)
                },
                text = { Text("新建") },
                icon = { Text("+", style = MaterialTheme.typography.titleLarge) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 自己画标题栏，避免依赖 ExperimentalMaterial3Api 的 TopAppBar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("待办清单", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "点小部件上的条目即完成，到期自动回来",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { showSettings = true }) { Text("设置") }
            }

            if (tasks.isEmpty()) {
                EmptyHint()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp
                    )
                ) {
                    items(pending, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onToggle = { vm.toggleComplete(task) },
                            onClick = {
                                editingOriginal = task
                                editing = task.toDraft()
                            }
                        )
                    }

                    if (done.isNotEmpty()) {
                        item {
                            Text(
                                text = "已完成 · 等待下次刷新",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp, top = 20.dp, bottom = 6.dp)
                            )
                        }
                        items(done, key = { it.id }) { task ->
                            TaskCard(
                                task = task,
                                onToggle = { vm.toggleComplete(task) },
                                onClick = {
                                    editingOriginal = task
                                    editing = task.toDraft()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    editing?.let { draft ->
        TaskEditDialog(
            draft = draft,
            onDismiss = { editing = null },
            onSave = { updated ->
                vm.saveDraft(updated, editingOriginal)
                editing = null
            },
            onDelete = editingOriginal?.let { original ->
                {
                    vm.delete(original)
                    editing = null
                }
            }
        )
    }

    if (showSettings) {
        SettingsDialog(
            countOverdue = countOverdue,
            onCountOverdueChange = vm::setCountOverdue,
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
private fun TaskCard(task: Task, onToggle: () -> Unit, onClick: () -> Unit) {
    val completed = task.completedAt != null
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 勾选圈
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (completed) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center
            ) {
                if (completed) {
                    Text(
                        "✓",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (completed) TextDecoration.LineThrough else null,
                    color = if (completed) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = RecurrenceText.describe(task),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (task.overdueCount > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "逾期 ${task.overdueCount} 次",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHint() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("还没有待办", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "点右下角新建一条，再把「待办清单」小部件放到桌面",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
