# RunAnywhere AI for Android

[English](README.EN.md) | [中文](README.md)

<p align="center">
  <img src="docs/logo.svg" alt="RunAnywhere" width="120"/>
</p>

<p align="center">
  <a href="https://github.com/normalwindow/runanywhere-android-ungoogle/releases">
    <img src="https://img.shields.io/badge/GitHub%20Release-下载-24292F?style=for-the-badge&logo=github&logoColor=white" alt="从 GitHub Releases 下载" />
  </a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-7.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android 7.0+" />
  <img src="https://img.shields.io/badge/Kotlin-2.4-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin 2.4" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/NPU-Snapdragon-C41230?style=flat-square&logo=qualcomm&logoColor=white" alt="Snapdragon NPU" />
  <img src="https://img.shields.io/badge/License-RunAnywhere-blue?style=flat-square" alt="RunAnywhere 许可证" />
</p>

RunAnywhere 的 Android 消费者应用，使用 Kotlin 编写。

可以向它提问、与它对话，或者让它看看摄像头所见的画面。模型在您的手机上运行，因此您输入或拍摄的任何内容都不会离开设备，并且可以在无网络环境下工作。在 Snapdragon 硬件上，推理在 Hexagon NPU 上运行。

## 获取应用

从项目的发布页面下载发布版 APK，并在 Android 7.0 或更高版本上侧载安装。该应用不需要 Google Play 服务、Google 账户或 Play 商店；发布版本需要 ARM64 设备。

<!-- 媒体占位：一个展示带工具调用的聊天、语音助手和摄像头视觉的 GIF。等待当前应用 bug 修复后的截屏素材。 -->

## 界面预览

在物理 arm64 设备上截取，运行的是通过 llama.cpp 后端加载的小型 GGUF 聊天模型。

| | |
|---|---|
| ![已加载模型的聊天界面](docs/screenshots/01-home.png) | ![回答界面](docs/screenshots/02-chat.png) |
| 标题栏显示已加载的模型名称，并标明是本地就绪状态。下方的标签是一键提示词。 | 在设备上生成的回答。 |
| ![语音对话](docs/screenshots/04-voice.png) | ![文档问答](docs/screenshots/06-documents.png) |
| “语音对话”会根据硬件选择语音识别、聊天、语音合成和语音检测模型，并同时加载这四个模型。 | 文档问答，支持重排序和多查询扩展（可分别开关）。 |
| ![高级中心](docs/screenshots/08-advanced.png) | ![设置界面](docs/screenshots/07-settings.png) |
| 聊天之外的所有功能都在这里：OCR、分割、图像生成、说话人分离、转录、基准测试。 | 采样参数、回复长度、系统提示词以及流式输出。 |

其余界面（包括横屏布局）请参见 [`docs/screenshots/`](docs/screenshots)。

## 功能概览

| | |
| --- | --- |
| **问答** | 支持思维链模式的流式聊天、工具调用，以及每次回复的分析数据 |
| **语音对话** | 免提语音助手：聆听、转录、思考、并语音回复 |
| **图像与实时画面** | 询问一张照片，或询问摄像头当前所见 |
| **文档** | 添加文档并提问，回答会引用来源 |
| **高级功能** | OCR、分割、说话人分离、图像生成、朗读、转录、语音活动检测、工具、基准测试 |

云端服务存在但需要用户主动开启，默认关闭。

该应用的本地模型运行、下载、通知、摄像头和麦克风均使用 Android 平台 API，不依赖 Google Mobile Services。Google Play 是可选的发行渠道，而不是运行时依赖。

## 模型

模型选择器按发布者分组，方便您选择熟悉的名称，然后选择尺寸。它涵盖当前一代的开放模型，包括聊天、视觉、语音和嵌入模型，从 230M 的即时响应模型到较新手机可容纳的更大模型。显示的尺寸是实测值而非估算值，应用会根据您的设备检查每个模型并给出推荐。

该选择器可从任何需要模型的界面进入，您也可以粘贴 Hugging Face 上的任意 GGUF 仓库，将其添加到精选集之外。

## Snapdragon NPU

在支持的 Qualcomm Hexagon 硬件上，应用会注册 QHexRT 后端，推理将在 NPU 上运行。在已验证的 V75、V79 和 V81 之外的部件上，注册会被内部拒绝；该后端仅限 ARM64，因此在 x86_64 模拟器上不可用。

私有的 `runanywhere/*_HNPU` 模型包需要 Hugging Face 令牌：进入设置 → 私有下载，粘贴令牌并保存。令牌保存在受保护的应用存储中，每次启动时重新应用，绝不会写入源码、资源或日志。

## 自行构建

```bash
git clone https://github.com/normalwindow/runanywhere-android-ungoogle.git
cd runanywhere-android-ungoogle
./gradlew :app:installDebug
```

您需要 Android Studio（最新稳定版）、JDK 17，以及若干 GB 的磁盘空间用于模型。不需要 NDK、CMake 或原生工具链：SDK 在其发布的 AAR 中已包含预构建的原生库。强烈建议使用 ARM64 物理设备，因为 NPU 后端在模拟器上不可用。

[`docs/DEVELOPMENT.md`](docs/DEVELOPMENT.md) 涵盖了 SDK 版本锁定规则、依赖验证、测试未发布的 SDK 构建、CI 以及故障排查。

## 架构

来自 Maven Central 的四个 AAR，没有本地项目路径。其中三个共享同一版本；QHexRT 使用自己的版本，因为它在 `0.20.19` 之后不再发布到 Maven Central。

```
        RunAnywhere AI (Jetpack Compose, MVVM)
                        │
        ┌───────────────┴────────────────┐
        │   io.github.sanchitmonga22:*   │
        └───────────────┬────────────────┘
                        │
   ┌───────────────┬────┴─────────┬──────────────────┐
   │               │              │                  │
runanywhere-sdk  llamacpp       onnx          qhexrt-android
core + commons   LLM · VLM   embeddings ·      Hexagon NPU
   0.20.24        0.20.24    STT·TTS·VAD        arm64 only
                              0.20.24            0.20.19
                        │
                        ▼
              C++ 通用库，一个核心
        与 Swift、Web 和 Electron 共享
```

业务逻辑位于 SDK 中。应用由 Compose UI、视图模型和轻量的 `RunAnywhere.*` 调用组成。模型目录会在首帧绘制后在后台注册，因此冷启动不会因大约一百次 `models.register()` JNI 调用而阻塞。

| 参考资料 | |
| --- | --- |
| 构建、版本锁定、测试、CI、故障排查 | [`docs/DEVELOPMENT.md`](docs/DEVELOPMENT.md) |
| 贡献者约定 | [`AGENTS.md`](AGENTS.md) |

## 其他平台应用

| 平台 | 仓库 |
| --- | --- |
| iOS 和 macOS，Swift | [runanywhere-ios](https://github.com/RunanywhereAI/runanywhere-ios) |
| Windows，Electron | [runanywhere-electron](https://github.com/RunanywhereAI/runanywhere-electron) |
| Web，TypeScript | [runanywhere-web](https://github.com/RunanywhereAI/runanywhere-web) |
| SDK 单体仓库 | [runanywhere-sdks](https://github.com/RunanywhereAI/runanywhere-sdks) |
| 文档 | [docs.runanywhere.ai](https://docs.runanywhere.ai) |
| Discord | [discord.gg/N359FBbDVd](https://discord.gg/N359FBbDVd) |

## 许可证

RunAnywhere 许可证，基于 Apache 2.0 并附加商业使用条款。请参见 [LICENSE](LICENSE)。