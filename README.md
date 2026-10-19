# Create Cinema — 1.20.1 Forge 移植版

[Create Cinema](https://github.com/Abuous/Createcinema)（作者 **Caelius**）的非官方 **Minecraft 1.20.1 / Forge** 移植。

把本地视频、网络视频（B 站 / 腾讯 / 优酷 / 央视 / 抖音等）、PPT 幻灯片「烧录」成胶片，再用放映机投到银幕上——配合 Create 的动能系统供电。

> 上游 0.1.4 面向 NeoForge 1.21.1。本仓库是基于其源码移植到 **Forge 47.x / MC 1.20.1** 的版本，功能对齐上游 0.1.4。

---

## 下载

在 [Releases](../../releases) 里按平台取一个 jar，**只能装一个**（它们包含相同的代码，只是内联的原生库不同）：

| 文件 | 体积 | 适用平台 |
|---|---|---|
| `createcinema-0.1.4-1.20.1-fix5.jar` | 57 MB | Windows x86_64 ✅ 已实机验证 |
| `createcinema-0.1.4-1.20.1-fix5-macos.jar` | 71 MB | macOS（Apple Silicon + Intel 双架构） |
| `createcinema-0.1.4-1.20.1-fix5-linux.jar` | 78 MB | Linux（x86_64 + arm64 双架构） |
| `createcinema-0.1.4-1.20.1-fix5-android-arm64.jar` | 51 MB | Android（PojavLauncher / FCL，arm64） |

jar 内已内联 FFmpeg/JavaCV 原生库、jsoup、POI（PPT 渲染器）与 Android WebView 桥，**无需额外安装**。

## 前置依赖

| 模组 | 版本 |
|---|---|
| Minecraft | **1.20.1** |
| Forge | **47.1.3+**（实测 47.4.28） |
| [Create](https://www.curseforge.com/minecraft/mc-mods/create) | **6.0.x**（实测 6.0.8-291，1.20.1 Forge 版） |
| Flywheel | 1.0.x |
| Ponder | 1.0.x |
| JEI | 15.x（可选） |
| Java | 17 或 21（需与系统架构一致） |

> 注意：Create 必须用 **1.20.1 Forge 的 6.0.x**，不是 0.5.1，也不是 NeoForge 版。

## 安装

1. 安装 Forge 1.20.1-47.1.3 以上版本
2. 把 `create`、`flywheel`、`ponder`、以及本模组对应平台的 jar 放进 `mods/`
3. 启动游戏

## 使用要点

- **影片烧录**：暗室方块 + 烧录机 + 空白胶片，放入视频文件即可烧录（支持 mp4/mkv 等常见格式，以及 `.ppt`/`.pptx`）
- **放映机**：需要 Create 的**旋转动能**供电；用扳手可调整朝向
- **网络放映机**：粘贴视频网址即可播放，可选清晰度；连播升级卡可自动连播分 P
- **扬声器声音**：扬声器音量 = **接收到的红石信号强度 / 15**。**没有红石信号就是静音**（上游原设计）
- **声音必须接线**：扬声器要用**线缆**连到放映机，否则日志会提示 `found no cable-connected speakers`
- **浏览器授权**（抖音/爱奇艺等需要登录的内容）：用「配置管理器」打开，见下方平台差异

## 更新与修复

| 构建 | 内容 |
|---|---|
| `fix5` | **修复原生崩溃（0xC0000005）**：崩溃落在 `avcodec-60.dll` 固定的 `h264` 参考帧列表代码上，属 FFmpeg 6.1.1 **帧级多线程解码**路径。现在网络播放统一 `threads=1`（单线程解码），同一 1080p 视频复测不再崩溃；同时新增「Opened network video stream」与「Hard resync seek」诊断日志。 |
| `fix4` | **修复原生崩溃**：`avcodec-60.dll` 访问违例（0xC0000005）。音频流关闭时，解码线程可能仍停留在 `avcodec_receive_frame()` 内部，异步 `close()` 会造成 use-after-free。现在所有原生 FFmpeg 调用串行化，关闭会等解码线程释放锁，必要时延后关闭。上游 1.21.1 同样存在此问题。 |
| `fix3` | 修复 Windows 上 FFmpeg 原生库加载失败：JavaCPP 每次加载都会「删掉缓存 DLL 再重写」，与安全软件（火绒 HIPS 等）实时扫描冲突，失败发生在类初始化里会废掉整局解码。改为每进程独立缓存目录 + 自行解压 + `pathsFirst` 直接加载文件。 |
| `fix1` / `fix2` | 早期诊断版本，已废弃，请勿使用。 |

## 平台差异与已知限制

| 功能 | Windows | macOS | Linux | Android |
|---|---|---|---|---|
| 视频解析 / 放映 / 烧录 / 音频 | ✅ | ✅ | ✅ | ✅ |
| 浏览器授权（抖音 / 爱奇艺） | WebView2 原生桥 | ⚠️ 走 Chromium CDP，但只搜索 `PATH`，macOS 的 Chrome 不在 `PATH` 中，通常不可用 | 走 CDP，需系统里装有 `chromium`/`google-chrome` | Android WebView 桥 |
| 硬件解码 | cuda / qsv | videotoolbox | — | mediacodec |

其他说明：

- **仅 Windows 与 macOS(x86_64/arm64) 的打包结构做过实机验证**；Windows 已实测视频+声音；macOS / Linux / Android 按代码与打包结构支持，但未实机验证
- Android 只提供 arm64：上游注明 android-arm（armv7）需自行编译 native
- 存档中的物品数据格式与 1.21.1 版不通用（1.21 用数据组件，1.20.1 用 NBT）
- 若 FFmpeg 原生库加载失败，会在 `createcinema/ffmpeg-error.log` 留下完整堆栈

## 从源码构建

需要 **JDK 17**（或 21）与 **Gradle 8.1.1**。

```bash
# Windows
gradlew.bat build -Pbundled_platforms=windows-x86_64

# macOS（双架构）
./gradlew build -Pbundled_platforms=macosx-arm64,macosx-x86_64

# Linux（双架构）
./gradlew build -Pbundled_platforms=linux-x86_64,linux-arm64

# Android
./gradlew build -Pbundled_platforms=android-arm64
```

产物在 `build/libs/`。`-Pbundled_platforms` 决定内联哪些平台的 FFmpeg/JavaCPP 原生库（可用逗号分隔多个）。

> 首次构建会下载 Forge userdev 并反编译 MC，视网络情况需要几分钟。国内网络建议给 Gradle 配置镜像。

## 移植说明（技术摘要）

从 NeoForge 1.21.1 → Forge 1.20.1 的主要改动：

- **构建**：NeoForge moddev → ForgeGradle 6.0.24 + MixinGradle；`neoforge.mods.toml` → `mods.toml`；`pack_format` 34 → 15
- **网络**：`CustomPacketPayload`/`StreamCodec`/`PayloadRegistrar` → Forge `SimpleChannel`（12 个包，id 0–11）
- **数据组件**：1.20.5+ 才有的 `DataComponents` → NBT（`FilmItem`、遥控频率存储）
- **注册/能力**：`DeferredItem/Block/Holder` → `RegistryObject`；能力注册 → 方块实体覆写 `getCapability`
- **方块**：`ItemInteractionResult` → `InteractionResult`；`useItemOn`/`useWithoutItem` → 统一 `use(...)`；`getShape` 等改回 `public`
- **方块实体**：`saveAdditional/load/getUpdateTag` 去掉 `HolderLookup.Provider`
- **渲染**：`QuadBakingVertexConsumer` 链式顶点 API、`SimpleUnbakedGeometry#addQuads` 6 参、删除 `getRenderBoundingBox`；`TextureAtlasSprite#getU/V` 是 0–16 像素域（需 ×16 换算）
- **资源**：`data/createcinema/recipe/` → `recipes/`，配方 `result.id` → `result.item`
- **原生库加载**：JavaCPP 每次加载都会「删掉缓存 DLL 再重写」，在 Windows 上会与安全软件的实时扫描冲突并导致整个 FFmpeg 栈失效。移植版改为**每进程独立缓存目录 + 自行解压 + `pathsFirst` 直接加载文件**，避免重写

## 许可

本项目沿用上游 **MIT License**，原始版权归 **Caelius** 所有，见 [LICENSE](LICENSE)。

上游项目：[Abuous/Createcinema](https://github.com/Abuous/Createcinema)

### Credits

- **Caelius** — Create Cinema 原作者（设计、美术、全部功能逻辑）
- **Create 团队** — Create / Flywheel / Ponder
- **Bytedeco** — JavaCV / FFmpeg / JavaCPP
- 移植与打包：**ChihayaAnonQWQ**

---

# English

Unofficial **Minecraft 1.20.1 / Forge** port of [Create Cinema](https://github.com/Abuous/Createcinema) by **Caelius**.

Burn local or online videos (Bilibili, Tencent, Youku, CCTV, Douyin, …) and PowerPoint slides onto film, then project them onto screens with Create-powered kinetics.

**Requirements**: MC 1.20.1, Forge 47.1.3+, Create **6.0.x for 1.20.1 Forge**, Flywheel 1.0.x, Ponder 1.0.x, Java 17/21.

Download the jar matching your platform from [Releases](../../releases). Native libraries are bundled — no extra setup.

**Speaker audio** is controlled by redstone: volume = signal strength / 15, so an unpowered speaker is silent. Speakers must also be connected to the projector with cables.

Licensed under **MIT**, original copyright **Caelius**.
