# 版本与发布

PatchViewer 的发版规则与产物链。原则:**版本号与构建都在本地完成,CI 只负责在 tag 推送后构建并创建 GitHub Release**,不得依赖 CI 改版本或出包。

## 版本号体系

| 项 | 约定 |
| --- | --- |
| 版本号形态 | 语义化 `X.Y.Z`,tag 同名加 `v` 前缀(如 `v2.4.0`) |
| 书写位置 | `mod.json`、`mod.hjson`、`build.gradle`(第 6 行 `version`)三处,**必须一致**,手工同步修改(无自动脚本) |
| dev 构建 | `version == "0.0.0"` 时判定为本地开发构建,产物名 `PatchViewer-dev.jar` / `PatchViewer-dev.zip` |
| release 构建 | 其他版本号,产物名 `PatchViewer-v<version>.jar` / `.zip` |

日常开发不需要动版本号;只有发版才把三处从 `0.0.0`/旧版本改到新版本。

## 发布前本地必做步骤

1. **更新三处版本号**:`mod.json`、`mod.hjson`、`build.gradle`。
2. **新增 Release 说明**:`.github/release-notes/vX.Y.Z.md`(中英双语要点)。CI 直接用它作为 Release 正文,缺文件会导致 Release 正文缺失。
3. **本地完整构建**:
   ```powershell
   .\gradlew.bat clean deploy
   ```
   需要可用的 D8(见 [development.md](development.md) 环境一节)。
4. **拷贝并核实产物**(release 构建的 finalizer 会自动拷贝):
   - `build/libs/PatchViewer-vX.Y.Z.jar` → `dist/` 与 `../构建/PatchViewer/`(zip 同理)。
   - 核实 jar 内含 `mod.json` / `mod.hjson` 与 `classes.dex`(安卓可用性以 dex 为准)。
5. **提交、打 tag 并推送**:`git tag vX.Y.Z` 后 push,CI 自动构建发版。

## 构建产物链

`deploy` 背后的任务链:

```text
classes ──► d8InputJar ──► dexAndroid ──► jarMerged ──► zipMerged ──► dist/ + 构建/
             (仅 class,      (d8 --min-api   (classes +      (同内容 zip)
              无外部依赖,     14 --release)   classes.dex +
              D8 专用输入)                    mod 描述文件)
```

要点:

- PatchViewer 依赖全部 compileOnly,没有运行期外部依赖,所以 `dexAndroid` 不需要 `--lib`,`jarMerged` 只合并 classes + `classes.dex` + `mod.json`/`mod.hjson`。
- `d8InputJar` 是 D8 专用输入,只留在 build 目录,不进入发布流程。
- dev 构建(version `0.0.0`)只拷贝到 `../构建/PatchViewer/`;release 构建额外拷贝到 `dist/`。
- 默认 `jar` 任务输出名是 `PatchViewer.zip`(历史遗留,内容仍是 jar),**不进入发布流程**。

## CI(`.github/workflows/release.yml`)

触发:推送 `v*` tag 或手动 dispatch。流程:

1. checkout + Temurin 17 + Gradle + Android SDK(build-tools 34.0.0)。
2. 清空 `dist/` 后执行 `clean deploy`。
3. 校验 `dist/` 内 jar 包含 `mod.json` / `mod.hjson` 描述文件,缺了直接失败。
4. 以 `.github/release-notes/<tag>.md` 为正文创建 GitHub Release,上传 `dist/*.jar` 与 `dist/*.zip`。

## Release 资产安全规则

Mindustry 游戏内安装器会取 Release API 返回的**第一个 `.jar`**,且不按操作系统挑资产。因此:

- 一个 Release 必须有且只有一个 `.jar` 资产,即最终可安装包(含 mod 描述 + 桌面主类 + `classes.dex` 的合并包),外加同名 `.zip`。
- 严禁把 `PatchViewer-d8-input.jar`、默认 `jar` 任务输出的 `PatchViewer.zip` 等中间产物传上 Release(当前 CI 只上传 `dist/`,天然满足;改动工作流时必须保持)。
- 发布后用 API 复查:
  ```bash
  gh release view <tag> --json assets
  ```
  发现多余 `.jar` 立即删除。
