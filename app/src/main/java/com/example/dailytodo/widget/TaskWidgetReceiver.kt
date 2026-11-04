package com.example.dailytodo.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * 系统与 Glance 之间的桥。
 *
 * 这里刻意没有 override onUpdate 去做"补算 + 重绘"，因为 provideGlance
 * 本身就会先 loadVisible 再渲染。如果在 onUpdate 里再调 updateAll，
 * 就会形成 onUpdate → updateAll → onUpdate 的死循环。
 */
class TaskWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TaskWidget()
}
