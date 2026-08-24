# AGENTS.md

## 项目概述

书法学习 Android 应用（面向手机和平板）。V1 已实现「选贴」与「看贴」两大功能。

## 技术栈

- Kotlin + Jetpack Compose + Material 3 (M3)
- MVVM 架构（ViewModel + StateFlow）
- Compose Navigation，单 Activity 架构
- Gradle Kotlin DSL，AGP 8.7.0，Kotlin 2.0.21，Gradle 8.9
- 包名：`com.example.shufa`，minSdk 24，targetSdk 35

## 构建命令（已验证）

```bash
./gradlew assembleDebug          # 构建 debug APK
./gradlew assembleRelease        # 构建 release APK
./gradlew lintDebug              # 运行 lint 检查
adb install -r app/build/outputs/apk/debug/app-debug.apk  # 安装到设备
adb shell am start -n com.example.shufa/.MainActivity      # 启动应用
```

## 项目结构

```
app/src/main/java/com/example/shufa/
├── MainActivity.kt              # 入口 Activity
├── model/CalligraphyPost.kt     # 字帖数据模型 + CalligraphyStyle 枚举
├── data/PostRepository.kt       # 本地 JSON 数据源
├── navigation/NavGraph.kt       # Compose Navigation 路由
└── ui/
    ├── theme/                   # M3 主题（Color.kt, Type.kt, Theme.kt）
    ├── select/                  # 选贴界面（SelectScreen + SelectViewModel）
    └── view/                    # 看贴界面（ViewScreen + ViewViewModel）
```

## 数据

字帖数据存储在 `app/src/main/assets/posts.json`，V1 阶段为本地 JSON，包含 5 个示例字帖（楷书/行书/草书/隶书/篆书各一）。

## 功能参考

- 「不厌书法」APP — 选贴、看贴功能参考
- 「书法字典大全」APP — 补充参考

## 约定

- 所有 UI 遵循 Material 3 设计规范
- 领域术语（选贴、看贴、字帖等）保持与「不厌书法」一致的表达习惯
- V2 及以后的功能（临摹、评测、社区等）暂不实现
