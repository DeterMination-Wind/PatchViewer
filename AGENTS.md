# PatchViewer 开发指南(AGENTS.md)

本文件面向在本仓库工作的 AI 代理与人类协作者。
面向人类的分类文档(架构 / 开发 / 发布 / 测试 / 术语表)在 [`docs/`](docs/README.md),
两者冲突时以本文件为准。

## 项目定位

- PatchViewer(补丁查看器)是 Mindustry **纯客户端、只读**的内容检查模组:把 datapatch
  修改前后的内容属性放进同一视野(数据库详情差异 + 长按快捷差异),不修改数据库或游戏规则。
- 本仓库是 Neon 聚合模组中子模组 `pv` 的**唯一真源**;Neon 侧的 `patchviewer` 包是同步
  产物,不要在 Neon 里手改,同步方向永远是:本仓库 → Neon(`tools/update_submods.py`)。
- 本仓库与 Neon 仓库是**不同 Git 上下文**,提交前先确认当前目录。

## 文件结构

```text
src/patchviewer/PatchViewerMod.java   唯一源码:主类 + 快照/差异/渲染/设置全部逻辑(单文件架构)
assets/bundles/                       i18n(bundle.properties + es/fr/ru/zh_CN/zh_TW)
.github/workflows/release.yml         tag 触发的构建与 GitHub Release
.github/release-notes/                每个版本一份中英双语 Release 正文
dist/                                 本地构建产物副本
mod.json / mod.hjson                  模组描述,内容保持一致
```

## 构建(真实任务)

```powershell
.\gradlew.bat clean deploy    # 完整合并产物(jarMerged + zipMerged)
.\gradlew.bat compileJava     # 只编译检查
.\gradlew.bat jarMerged       # 只出合并 jar
.\gradlew.bat dexAndroid      # 只出 classes.dex
```

- 目标字节码 Java 8(`minGameVersion: 154`);依赖全部 compileOnly,无运行期外部依赖。
- D8 探测:`D8_PATH` → `ANDROID_SDK_ROOT`/`ANDROID_HOME` → 工作区根 `commandlinetools-win-*`。
- **没有自动化测试**;验证走 `docs/testing.md` 的手测清单。
- 默认 `jar` 任务的输出名是 `PatchViewer.zip`(内容仍是 jar),是历史遗留命名,**不是可分发文件**。
- 版本号写在 mod.json、mod.hjson、build.gradle 三处,必须一致;`version = "0.0.0"` 时
  构建判定为 dev 构建(产物 `PatchViewer-dev.*`,只拷到 `../构建/PatchViewer/`)。

## 代码约束

- 保持**单文件结构**:新增能力以私有方法/内部类加入 `PatchViewerMod.java`,不拆包;
  Neon 的注入脚本按该文件的结构做匹配与断言。
- 游戏内部字段与第三方模组(MI2U、MindustryX ShareFeature)一律反射访问并逐级降级,
  缺类静默跳过;不得引入编译期硬依赖(MindustryX classes 仅可选 compileOnly)。
- 用户可见文案一律走 bundle,新增 key 同步写全六份语言文件。
- 差异渲染约定:`->` 箭头留在旧值所在行,空间不足时只在箭头与新值之间换行;
  快捷 HUD、详细对话框、原生属性行、文本差异等展示处换行行为保持一致。
- 只读原则:任何改动不得引入修改数据库/游戏规则的路径。

## 与 Neon 聚合的关系(bekBundled)

- 本仓库源码是**独立形态**,不写 `bekBundled` / `bekBuildSettings`;Neon 同步(id=`pv`,
  `injectBekHooks` 默认开启)时自动注入:字段声明、`init()` 早退、`addCategory` 守卫、
  `buildSettings` 的 `bekBuildSettings` 包装,并做结构断言。
- 因此**不要在本仓库手写聚合契约**,也不要预先"帮 Neon"改好;改了反而会与注入结果冲突。
- 改动 `init()` / `buildSettings()` 结构或 bundle key 后,必须回 Neon 执行
  `python tools/update_submods.py --only pv --verify-build`,确认注入与断言通过。
- 聚合形态下设置由 Neon 总设置页挂载,key(`patchviewer-*`)不变,两种形态共用配置。

## 验证清单(提交前)

1. `.\gradlew.bat compileJava` 通过(改动打包链时跑 `clean deploy`)。
2. 按 `docs/testing.md` 手测清单覆盖受影响功能(差异视图 / 快捷差异 / 设置 / 颜色)。
3. bundle 改动:六份语言文件同步,无裸 key。
4. 涉及 `init()` / `buildSettings()` / bundle:完成上节 Neon 同步验证。

## 提交规范

- 提交信息简短、面向变更本身,常用前缀:`release: vX.Y.Z`、`build:`、`docs:`、
  `PatchViewer: <变更>`;一次提交只做一件事。
- 发版流程(版本号 → release-notes → 构建 → tag)见 `docs/release.md`;tag 只用 `v*`。
- `dist/` 与 `../构建/PatchViewer/` 是产物区,不纳入功能提交的评审重点,但也不要
  意外清理他人产物。
