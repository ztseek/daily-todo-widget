plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") // Kotlin 2.0 起 Compose 编译器独立成插件
    id("com.google.devtools.ksp")             // Room 用 KSP 生成代码（比 kapt 快很多）
}

android {
    namespace = "com.example.dailytodo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.dailytodo"
        // minSdk 26 的理由：可以直接用 java.time（LocalDate/ZonedDateTime），
        // 不必引入 core-library-desugaring，周期计算代码会干净很多。
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // 桌面小部件：Glance 是 Compose 风格的 AppWidget DSL，
    // 最终仍编译成 RemoteViews，所以它只能用小部件支持的组件子集。
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")

    // 本地数据库
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // 兜底刷新：闹钟被系统杀掉时，每 6 小时兜一次
    implementation("androidx.work:work-runtime-ktx:2.9.1")
}
