# 待办小部件（DailyTodoWidget）

一个把「游戏日常任务」的思路搬到待办清单上的 Android 应用：

- 在应用里写待办，桌面上有个小部件直接列出来
- 点小部件上的任意一条 → 它立刻消失，代表这次做完了
- 每条待办可以设刷新周期（每天 / 每周几 / 每月几号 / 每隔 N 天）和一个刷新时刻
- 到了刷新时刻，它自动回到桌面小部件上

---

## 一、先决条件

| 项目 | 要求 |
|---|---|
| 构建工具 | Android Studio（Ladybug 2024.2 或更新版本） |
| JDK | 17（Android Studio 自带的 JBR 就是 17，无需另装） |
| 手机 | Android 8.0（API 26）及以上 |

工程已附带完整的 Gradle Wrapper（`gradle/wrapper/gradle-wrapper.jar` + `.properties`，
指定 Gradle 8.9）。两种方式都能编译，二选一：

**方式 A：用 Gradle Wrapper（推荐，无需本机装 Gradle）**

```bash
./gradlew assembleDebug        # Linux / macOS
gradlew.bat assembleDebug      # Windows
```

产物在 `app/build/outputs/apk/debug/app-debug.apk`。

**方式 B：用 Android Studio**

1. Android Studio → `File > Open`，选中本工程目录
2. 等右下角 Gradle Sync 跑完
3. `Build > Build Bundle(s) / APK(s) > Build APK(s)`

若 Sync 报「Gradle distribution not found」，在
`Settings > Build, Execution, Deployment > Build Tools > Gradle`
里把 Gradle 版本手动指定为 8.9 即可。

## 二、编译安装

```
Build > Build Bundle(s) / APK(s) > Build APK(s)
```

产物在 `app/build/outputs/apk/debug/app-debug.apk`，直接拷到手机上装。
或者手机开 USB 调试，点 Android Studio 的绿色三角直接跑。

## 三、装完之后必须做的三件事

1. **打开一次应用**。首次启动会建库、注册兜底任务、排第一个闹钟。
2. **把「允许精确闹钟」打开**（应用内 → 设置 → 允许精确闹钟 → 去开启）。
   不开也能用，只是刷新可能晚十几分钟。
3. **把电池优化关掉**（应用内 → 设置 → 关闭电池优化）。
   小米 / 华为 / OPPO / vivo 上这一步尤其重要，否则后台会被清掉。
   部分机型还要在系统设置里额外开「自启动」「后台弹窗」。

## 四、添加小部件

长按桌面空白处 → 小部件 → 找到「待办清单」→ 拖到桌面。

列表是**可滚动**的（方案 B）：小部件拉多高就显示多少行，超出部分
上下滑动查看，不再有「还有 N 条」的折叠提示。默认尺寸 4×3，
纵向、横向都可以继续拉伸。

## 五、代码结构导览

```
app/src/main/java/com/example/dailytodo/
├── TodoApp.kt                    应用入口：注册兜底刷新 + 冷启动补算
├── data/
│   ├── Task.kt                   实体 + 周期枚举 + 星期位掩码工具
│   ├── TaskDao.kt                查询
│   ├── AppDatabase.kt            Room 数据库
│   ├── TaskRepository.kt         数据门面（手写单例，没用 Hilt）
│   └── SettingsStore.kt          设置项
├── domain/
│   ├── RecurrenceEngine.kt       ★ 周期引擎（整个 app 的核心）
│   ├── RecurrenceText.kt         规则 → 中文文案
│   └── TaskSync.kt               ★ 同步编排（补算→落库→重绘→排闹钟）
├── schedule/
│   ├── AlarmScheduler.kt         只排"最近一个"闹钟
│   ├── RefreshReceiver.kt        闹钟到点回调
│   ├── BootReceiver.kt           开机 / 升级后重建
│   └── WidgetRefreshWorker.kt    每 6 小时兜底
├── widget/
│   ├── TaskWidget.kt             ★ Glance 小部件 + 尺寸自适应
│   ├── TaskWidgetReceiver.kt     系统与 Glance 之间的桥
│   ├── WidgetActions.kt          点击完成 / 撤销
│   └── WidgetStateStore.kt       5 秒撤销的临时状态
└── ui/
    ├── MainActivity.kt
    ├── TaskListScreen.kt         列表 + 已完成分区
    ├── TaskEditDialog.kt         新建/编辑 + 周期选择器
    ├── SettingsDialog.kt         设置（含两个系统限制的修复入口）
    ├── TaskViewModel.kt
    └── theme/Theme.kt
```

带 ★ 的三个文件是重点，`DESIGN.md` 专门讲它们为什么这么写。

## 六、几个你可能会踩的坑

**Q：小部件上点了没反应？**
A：检查应用是否被系统冻结。国产 ROM 上如果把应用"强行停止"，所有广播都收不到，
   直到用户重新打开应用。

**Q：到了时间任务没回来？**
A：按这个顺序查：
1. 设置里精确闹钟开了没
2. 电池优化关了没
3. 手动点一下小部件里任意一条待办 —— 这会强制触发一次补算，
   如果这时候任务回来了，说明是闹钟被系统杀了，不是逻辑错

**Q：改手机时间会不会乱？**
A：不会。所有判断都基于绝对时间戳和 `ZonedDateTime`，
   但如果你把手表调回过去，某些任务的 `nextDueAt` 会显得"在未来"，
   表现是暂时不刷新，等时间走回来就正常。

**Q：数据存在哪？**
A：只在手机本地的 SQLite（`dailytodo.db`），没有任何联网代码，没有权限之外的采集。

**Q：编译报 `Unresolved reference: cornerRadius`？**
A：`cornerRadius` 在 Glance 不同版本里的包位置有过变动。
   把 `widget/TaskWidget.kt` 里 `.cornerRadiusCompat(20.dp)` 和 `.cornerRadiusCompat(9.dp)`
   这两类调用删掉，即可通过编译 —— 代价只是小部件变直角，不影响任何逻辑。
   这也是代码里把它包成 `cornerRadiusCompat` 的原因：要改只改一处。

**Q：编译报 Room 的 `Cannot figure out how to save this field`？**
A：检查 `AppDatabase` 上的 `@TypeConverters(Converters::class)` 有没有被误删。
   `RepeatType` 这个枚举靠它转成字符串存库。
