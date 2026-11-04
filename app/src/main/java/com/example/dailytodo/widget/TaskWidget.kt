package com.example.dailytodo.widget

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.dailytodo.R
import com.example.dailytodo.data.RepeatType
import com.example.dailytodo.data.Task
import com.example.dailytodo.domain.RecurrenceText
import com.example.dailytodo.domain.TaskSync

/**
 * 桌面小部件 —— 可滚动列表形态（方案 B）。
 *
 * ## 为什么整个小部件就是一个 LazyColumn
 *
 * 项目锁定的 Glance 1.1.1 没有 `defaultWeight()`，也就是说没法在 Column 里
 * 让列表「吃掉剩余高度」。所以这里换了个更简单也更稳的结构：把 Header、
 * 撤销条都作为 LazyColumn 的列表项，整个小部件就是一张可滚动的列表。
 *
 * 代价是下拉时标题会跟着滚走；换来的是列表高度天然确定，不需要猜
 * Header 有多高，也不会出现「只显示一行」的退化。
 *
 * ## 滚动由谁负责
 *
 * Glance 的 LazyColumn 最终会被编译成 RemoteViews 的滚动集合，列表的
 * 上下滑动由桌面（Launcher）原生提供，不是我们在 RemoteViews 里做手势。
 * 所以它跟手、省电，条目再多也不会一次性全部渲染。
 *
 * ## 为什么渲染前要重新读数据
 *
 * provideGlance 每次被系统调用（添加、拉伸、定时刷新、我们自己 updateAll）
 * 都会执行一次 `TaskSync.loadVisible`，也就是每次渲染都先补算一遍。
 * 这让显示永远是对的 —— 闹钟万一没响，用户滑回桌面时也能看到最新状态。
 */
class TaskWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: android.content.Context, id: androidx.glance.GlanceId) {
        val tasks = TaskSync.loadVisible(context)
        val undo = WidgetStateStore.readUndo(context)
        provideContent {
            WidgetContent(tasks = tasks, undo = undo)
        }
    }
}

@Composable
private fun WidgetContent(tasks: List<Task>, undo: UndoSnapshot?) {
    val palette = if (isNightMode()) DarkPalette else LightPalette

    LazyColumn(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(palette.background))
            .cornerRadiusCompat(20.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        item {
            Header(count = tasks.size, palette = palette)
            Spacer(GlanceModifier.height(8.dp))
        }

        // 撤销条作为列表项，位置紧跟标题，点完那一下最容易看到
        if (undo != null) {
            item {
                UndoBar(undo = undo, palette = palette)
                Spacer(GlanceModifier.height(6.dp))
            }
        }

        if (tasks.isEmpty() && undo == null) {
            item { EmptyHint(palette = palette) }
        } else {
            items(tasks) { task ->
                TaskRow(task = task, palette = palette)
            }
        }
    }
}

@Composable
private fun Header(count: Int, palette: WidgetPalette) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "待办清单",
            style = TextStyle(
                color = ColorProvider(palette.title),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1
        )
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = if (count == 0) "已完成" else "$count 条",
            style = TextStyle(color = ColorProvider(palette.muted), fontSize = 12.sp),
            maxLines = 1
        )
    }
    Spacer(GlanceModifier.height(6.dp))
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(1.dp)
            .background(ColorProvider(palette.divider))
    ) {}
}

@Composable
private fun TaskRow(task: Task, palette: WidgetPalette) {
    // 逾期次数 > 0 时优先显示逾期标签，这是用户最需要的一眼信息
    val showOverdue = task.overdueCount > 0
    val labelText = if (showOverdue) "逾期${task.overdueCount}次" else RecurrenceText.shortLabel(task)
    val labelBg: Int
    val labelFg: Int
    if (showOverdue) {
        labelBg = palette.overdueBg
        labelFg = palette.overdueFg
    } else {
        val pair = tagColors(task.repeatType, palette)
        labelBg = pair.first
        labelFg = pair.second
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            // 整行可点，不是只有文字可点 —— 小部件上手指精度很差
            .clickable(
                actionRunCallback<CompleteAction>(actionParametersOf(TaskIdKey to task.id))
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_radio_unchecked),
            contentDescription = null,
            modifier = GlanceModifier.size(15.dp),
            colorFilter = ColorFilter.tint(ColorProvider(palette.ring))
        )
        Spacer(GlanceModifier.width(10.dp))
        Text(
            text = task.title,
            style = TextStyle(color = ColorProvider(palette.item), fontSize = 14.sp),
            maxLines = 1
        )
        Spacer(GlanceModifier.width(8.dp))
        Box(
            modifier = GlanceModifier
                .cornerRadiusCompat(9.dp)
                .background(ColorProvider(labelBg))
                .padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
            Text(
                text = labelText,
                style = TextStyle(color = ColorProvider(labelFg), fontSize = 10.sp),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun UndoBar(undo: UndoSnapshot, palette: WidgetPalette) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .cornerRadiusCompat(10.dp)
            .background(ColorProvider(palette.undoBg))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable(actionRunCallback<UndoAction>()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "「${undo.title}」已完成",
            style = TextStyle(color = ColorProvider(palette.undoFg), fontSize = 12.sp),
            maxLines = 1
        )
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = "撤销",
            style = TextStyle(
                color = ColorProvider(palette.undoFg),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun EmptyHint(palette: WidgetPalette) {
    Box(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "清单空了，休息一下",
            style = TextStyle(color = ColorProvider(palette.muted), fontSize = 13.sp)
        )
    }
}

@Composable
private fun isNightMode(): Boolean {
    val context = LocalContext.current
    return (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
}

private fun tagColors(type: RepeatType, p: WidgetPalette): Pair<Int, Int> = when (type) {
    RepeatType.DAILY -> p.tagDailyBg to p.tagDailyFg
    RepeatType.WEEKLY -> p.tagWeeklyBg to p.tagWeeklyFg
    RepeatType.MONTHLY -> p.tagMonthlyBg to p.tagMonthlyFg
    RepeatType.EVERY_N_DAYS -> p.tagIntervalBg to p.tagIntervalFg
    RepeatType.ONCE -> p.tagOnceBg to p.tagOnceFg
}

/**
 * Glance 的 cornerRadius 在不同版本里行为略有差异，
 * 这里包一层，将来要换实现只改一处。
 */
private fun GlanceModifier.cornerRadiusCompat(radius: Dp): GlanceModifier =
    this.cornerRadius(radius)

private data class WidgetPalette(
    val background: Int,
    val title: Int,
    val item: Int,
    val muted: Int,
    val hint: Int,
    val divider: Int,
    val ring: Int,
    val undoBg: Int,
    val undoFg: Int,
    val tagDailyBg: Int,
    val tagDailyFg: Int,
    val tagWeeklyBg: Int,
    val tagWeeklyFg: Int,
    val tagMonthlyBg: Int,
    val tagMonthlyFg: Int,
    val tagIntervalBg: Int,
    val tagIntervalFg: Int,
    val tagOnceBg: Int,
    val tagOnceFg: Int,
    val overdueBg: Int,
    val overdueFg: Int,
)

private val LightPalette = WidgetPalette(
    background = 0xFFF7F7F5.toInt(),
    title = 0xFF1B1B1F.toInt(),
    item = 0xFF2A2A30.toInt(),
    muted = 0xFF8A8A93.toInt(),
    hint = 0xFFB0B0B8.toInt(),
    divider = 0xFFE5E5E2.toInt(),
    ring = 0xFF9A9AA3.toInt(),
    undoBg = 0xFFE3F3EA.toInt(),
    undoFg = 0xFF0F6E56.toInt(),
    tagDailyBg = 0xFFE6F1FB.toInt(),
    tagDailyFg = 0xFF185FA5.toInt(),
    tagWeeklyBg = 0xFFEEEDFE.toInt(),
    tagWeeklyFg = 0xFF534AB7.toInt(),
    tagMonthlyBg = 0xFFFAEEDA.toInt(),
    tagMonthlyFg = 0xFF854F0B.toInt(),
    tagIntervalBg = 0xFFE1F5EE.toInt(),
    tagIntervalFg = 0xFF0F6E56.toInt(),
    tagOnceBg = 0xFFF1EFE8.toInt(),
    tagOnceFg = 0xFF5F5E5A.toInt(),
    overdueBg = 0xFFFCEBEB.toInt(),
    overdueFg = 0xFFA32D2D.toInt(),
)

private val DarkPalette = WidgetPalette(
    background = 0xFF232327.toInt(),
    title = 0xFFEDEDF0.toInt(),
    item = 0xFFDCDCE2.toInt(),
    muted = 0xFF8E8E96.toInt(),
    hint = 0xFF6E6E78.toInt(),
    divider = 0xFF2E2E35.toInt(),
    ring = 0xFF6E6E78.toInt(),
    undoBg = 0xFF1B332A.toInt(),
    undoFg = 0xFF9FE1CB.toInt(),
    tagDailyBg = 0xFF14304A.toInt(),
    tagDailyFg = 0xFF85B7EB.toInt(),
    tagWeeklyBg = 0xFF2A2647.toInt(),
    tagWeeklyFg = 0xFFAFA9EC.toInt(),
    tagMonthlyBg = 0xFF3A2C12.toInt(),
    tagMonthlyFg = 0xFFFAC775.toInt(),
    tagIntervalBg = 0xFF123A31.toInt(),
    tagIntervalFg = 0xFF5DCAA5.toInt(),
    tagOnceBg = 0xFF2C2C33.toInt(),
    tagOnceFg = 0xFFB4B2A9.toInt(),
    overdueBg = 0xFF3A1A1A.toInt(),
    overdueFg = 0xFFF09595.toInt(),
)
