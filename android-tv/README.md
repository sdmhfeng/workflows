# ownTV - Android TV 应用

IPTV 电视直播 Android TV 客户端，配合 [iptv-api](https://github.com/Guovin/iptv-api) 后端服务使用。

## 功能

- 📺 频道列表浏览 — 按分组展示 IPTV 频道，支持 TV 遥控器导航
- 🎬 视频播放 — 基于 ExoPlayer，支持 HLS、DASH、RTMP、RTSP 等协议
- ⚙️ 可配置服务器 — 连接自建的 iptv-api 后端服务获取直播源
- 🕹️ D-pad 导航 — 完全适配电视遥控器操作

## 前置要求

- **Android Studio** (Hedgehog 2023.1.1 或更高版本)
- **Android SDK** Platform 34
- **JDK** 17 或更高版本
- 一台运行 [iptv-api](https://github.com/Guovin/iptv-api) 的服务器（Raspberry Pi / NAS / VPS）

## 构建 APK

### 方法一：使用 Android Studio

1. 用 Android Studio 打开 `android-tv/` 目录
2. 等待 Gradle 同步完成
3. **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
4. APK 文件位于 `app/build/outputs/apk/debug/app-debug.apk`

### 方法二：命令行构建

```bash
# 进入 android-tv 目录
cd android-tv

# 生成 Gradle Wrapper（首次使用需要）
gradle wrapper

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK（需要签名配置）
./gradlew assembleRelease
```

> **注意**：如果系统没有安装 Gradle，需要先生成 Gradle Wrapper：
> - 在项目根目录运行 `gradle wrapper`（需要先安装 Gradle）
> - 或使用 Android Studio 打开项目，它会自动下载

## 安装到 Android TV

### 方法一：ADB 安装

```bash
# 连接 Android TV（需要在 TV 上开启开发者模式和 USB 调试）
adb connect <TV_IP_ADDRESS>:5555

# 安装 APK
adb install app/build/outputs/apk/debug/app-debug.apk

# 启动应用
adb shell am start -n com.owntv.app/.MainActivity
```

### 方法二：U 盘安装

1. 将 APK 复制到 U 盘
2. 插入 Android TV
3. 使用文件管理器打开并安装
4. 安装后应用会出现在 TV 主屏幕的应用列表中

## 使用说明

1. 启动 ownTV 应用
2. 首次使用需要配置服务器地址：
   - 按遥控器**菜单键**进入设置
   - 输入 iptv-api 后端服务地址（例如：`http://192.168.1.100:8000`）
   - 点击「测试连接」验证服务器是否正常
   - 点击「保存地址」
3. 返回频道列表，应用会自动加载直播源
4. 选择频道即可开始播放

## 架构

```
┌─────────────────────────────┐
│    ownTV Android TV App     │
│  ┌───────────────────────┐  │
│  │  MainActivity         │  │  ← 频道列表浏览 (Leanback)
│  │  ChannelListFragment  │  │
│  └─────────┬─────────────┘  │
│            │                 │
│  ┌─────────▼─────────────┐  │
│  │  PlaybackActivity     │  │  ← 视频播放 (ExoPlayer)
│  │  PlayerView           │  │
│  └───────────────────────┘  │
│  ┌───────────────────────┐  │
│  │  SettingsActivity     │  │  ← 配置服务器地址
│  └───────────────────────┘  │
│  ┌───────────────────────┐  │
│  │  ApiClient            │  │  ← HTTP 客户端 (OkHttp)
│  │  M3uParser            │  │  ← M3U 解析器
│  └───────────────────────┘  │
└─────────────┬───────────────┘
              │ HTTP
              ▼
┌─────────────────────────────┐
│  iptv-api Flask 后端        │
│  (运行在服务器/NAS/树莓派)    │
│  - /m3u  → M3U 直播源列表   │
│  - /txt  → TXT 直播源列表   │
│  - /     → Web 界面         │
└─────────────────────────────┘
```

## 依赖库

| 库 | 用途 |
|---|---|
| AndroidX Leanback | TV UI 框架 |
| ExoPlayer 2.19.1 | 视频播放器 |
| OkHttp 4.12.0 | HTTP 网络请求 |
| Gson 2.10.1 | JSON 解析 |
| Kotlin Coroutines | 异步任务处理 |

## 许可证

[MIT](../LICENSE) License
