// 根构建脚本：只声明插件版本，不在这里 apply。
// 版本组合说明（这套是已验证互相兼容的组合，升级时请成组升级）：
//   AGP 8.7.3  <->  Gradle 8.9  <->  Kotlin 2.0.21  <->  KSP 2.0.21-1.0.28
//   Compose BOM 2024.10.01  <->  Glance 1.1.1  <->  Room 2.6.1
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
}
