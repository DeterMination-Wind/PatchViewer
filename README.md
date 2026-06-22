# PatchViewer / 补丁查看器

PatchViewer is a Mindustry Java mod for viewing before/after diffs introduced by datapatches. It highlights patched content in the core database and can show a compact diff while hovering blocks or units in-game.

PatchViewer 是一个 Mindustry Java 模组，用于查看 datapatch 对内容造成的修改。它会在核心数据库中高亮补丁前后的属性差异，也可以在游戏内悬停建筑或单位时显示紧凑差异。

## Features / 功能

- Inline before/after diff panels in the core database
- Native stat panel highlighting for modified, removed, and added values
- Hold the PatchViewer quick-view key, default `Alt`, to inspect patched blocks or units under the cursor
- Quick diff display modes: near-cursor HUD or inserted build-info panel
- Configurable diff colors, HUD opacity, HUD width, and HUD background color
- Compact icon/stack rendering for costs, inputs, outputs, mineable ores, weapons, and other icon-heavy stats
- Unit icons in patched database stats use a consistent size and can be clicked to open the unit database page
- Merged desktop + Android release artifacts

- 在核心数据库属性行内显示修改前/修改后的 diff 面板
- 对原生属性面板中的修改、删除、新增值进行高亮
- 长按 PatchViewer 快捷查看键，默认 `Alt`，可查看鼠标下建筑或单位的补丁差异
- 快捷差异支持鼠标旁 HUD 与建造栏插入两种显示模式
- 可配置 diff 颜色、HUD 透明度、HUD 宽度和 HUD 背景色
- 对造价、输入、输出、可采集矿物、武器等图标密集属性使用紧凑 icon/stack 显示
- 被补丁影响的数据库属性中，单位图标会统一尺寸，并可点击跳转到对应单位详情页
- 同时输出桌面版和安卓版可用的合并产物

## Build / 构建

Use the merged build task:

```powershell
./gradlew.bat clean deploy
```

Artifacts will be generated here:

- `dist/PatchViewer.jar`
- `dist/PatchViewer.zip`
- `../构建/PatchViewer/PatchViewer-v2.1.0.jar`
- `../构建/PatchViewer/PatchViewer-v2.1.0.zip`

## Release / 发布

GitHub Actions workflow: `.github/workflows/release.yml`

- Trigger: push tag `v*` or manual dispatch
- Build: `clean deploy`
- Release assets: `dist/*.jar` and `dist/*.zip`
