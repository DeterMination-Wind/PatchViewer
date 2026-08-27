# 开发指南

环境、日常构建命令与代码约束。仓库级硬性约束(结构契约、聚合注意事项)在根目录 [AGENTS.md](../AGENTS.md),两者冲突时以 AGENTS.md 为准。

## 环境

- JDK:以 build.gradle 为准,目标字节码 **Java 8**(`sourceCompatibility = 1.8`,JDK 9+ 时 `options.release.set(8)`)。本地建议 JDK 17(与 CI 一致),JDK 8 及以上均可编译。
- Gradle:wrapper 8.14.3(`gradlew.bat` 自动获取,无需本机安装)。
- Mindustry/Arc 依赖(全部 compileOnly,无运行期依赖):
  - 默认走 JitPack `com.github.Anuken.MindustryJitpack:core:v154.3`(可用 `-PmindustryVersion=` 覆盖);POM 中 Arc 的 commit-hash 坐标会被自动重定向到已发布的 Arc tag `v155.4`。
  - 若根项目包含 `:core` 子项目则优先使用本地 core(当前 settings.gradle 未 include,实际走 JitPack)。
  - 可选:MindustryX 编译类路径——`../MindustryX-main/work/core/build/classes/{java,kotlin}/main` 存在时以 compileOnly 挂入;源码对 mindustryX 能力只走反射,不直接 import。
- 安卓 dex 需要 D8,探测顺序:`D8_PATH` 环境变量 → `ANDROID_SDK_ROOT` / `ANDROID_HOME` 下的 `build-tools` → 工作区根的 `commandlinetools-win-*` 目录。都没有时 `dexAndroid` 直接失败。
- Windows 下命令请用 PowerShell 7(`pwsh`)。

## 常用命令

```powershell
# 完整合并产物(桌面+安卓 jar 与 zip),日常验证首选
.\gradlew.bat clean deploy

# 只编译检查
.\gradlew.bat compileJava

# 只出合并 jar / 只出 classes.dex
.\gradlew.bat jarMerged
.\gradlew.bat dexAndroid
```

`deploy` = `jarMerged` + `zipMerged`(各自带 finalizer 拷贝,产物去向见 [release.md](release.md))。

注意区分:默认 `jar` 任务的输出文件名是 `PatchViewer.zip`(内容仍是 jar,历史遗留命名),**不是可分发文件**;可分发形态只有 `jarMerged` / `zipMerged` 的产物。

本仓库没有自动化测试任务(`test` 只是 Java 插件默认空壳,无测试源码);验证以[手测清单](testing.md)为准。

## 代码风格要点

- **单文件架构**:全部逻辑在 `patchviewer.PatchViewerMod` 一个类里,数据类型用 private static class / enum 表达(`ContentSnapshot`、`ContentDiff`、`CompactDiff`、`RowKind` 等)。新增能力优先作为私有方法或内部类加入,遵循既有分区:快照 → 差异 → 渲染 → 设置。
- **反射 + 降级**:游戏内部字段(`PlacementFragment`、`ContentInfoDialog`)与第三方模组(MI2U)只经反射访问;所有反射调用必须 try/catch,缺类时置标志位并不再重试(如 `mi2uClassMissing`)。
- **文案走 bundle**:用户可见文案用 `bundle(key, fallback)` 取值;新增 key 必须同步写进 `assets/bundles/` 下全部六个语言文件。
- **布局细节有约定**:`->` 箭头必须留在旧值所在行,空间不足时只在箭头与新值之间换行;相关逻辑集中在 `wrapNativeStatInset`、`renderCompactStackDiff`、`AtomicStatFlow`,宽度测量用离屏 `pack()`。改动换行行为时六处展示(快捷 HUD、详细对话框、原生属性行、文本差异、建造栏注入、建造成本)要保持一致。
- 保持既有大括号/缩进风格;变更聚焦,不顺手重构无关代码。

## 调试建议

- 找一个会修改内容属性的 datapatch 模组做对照;必须先进入一局(WorldLoadEvent 之后才有 after 快照与差异),再打开数据库或内容信息查看。
- 快捷差异不显示时按序检查:总开关 → 是否在局内 → 是否按住 `patchviewer-quick-view`(默认左 Alt)→ 是否有对话框/输入框抢占焦点 → 该内容是否真的存在差异。
- 反射目标会随游戏版本变化:`findQuickTarget()` 与 `showPatched()` 失败时静默降级,可临时插 `Log.debug` 定位。
- 验证聚合形态:改动 `init()` / `buildSettings()` 结构后,回 Neon 执行 `python tools/update_submods.py --only pv --verify-build`,确认注入与结构断言通过(见 [architecture.md](architecture.md))。
