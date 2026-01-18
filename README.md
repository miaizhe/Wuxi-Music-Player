# Wuxi-Player

Wuxi-Player 是一款基于 Jetpack Compose 和 Media3 开发的现代 Android 音乐播放器，专为极致体验与系统深度集成而生。

## 功能特性

- **ColorOS 15 深度适配**：
  - **流体云 (Fluid Cloud)**：支持 ColorOS 15 系统级流体云显示，状态栏胶囊实时展示播放状态。
  (！！需要更改为已适配应用的包名！！)
  - **控制中心集成**：深度定制系统媒体控制控件，支持上一首、下一首、播放/暂停及**一键收藏**功能。
- **高性能播放引擎**：基于 Media3 ExoPlayer，支持无损播放、自动焦点管理及拔出耳机自动暂停。
- **稳定性保障**：
  - **持久化服务**：优化前台服务生命周期，确保在应用后台清理后，媒体控制依然常驻。
  - **防休眠机制**：内置 WakeLock 保护，防止系统深度休眠导致播放中断。
- **现代 UI 交互**：
  - 采用 Material 3 设计语言，支持动态色彩。
  - 自定义背景图片及透明度调节。
  - 歌词同步滚动与实时颜色高亮调整。
- **在线与本地**：支持在线音乐搜索、歌词同步，以及本地音频扫描。
- **权限管理**：适配 Android 13+ 通知权限及媒体访问权限。

## 技术栈

- **UI 框架**：Jetpack Compose
- **核心架构**：MVVM + Kotlin Coroutines & Flow
- **媒体库**：AndroidX Media3 (ExoPlayer + MediaSession)
- **网络层**：Retrofit + OkHttp
- **图片加载**：Coil

## 开源协议

本项目采用 [GPL-3.0](LICENSE) 开源协议。

---
Developed with ❤️ by Wuxi Team.
