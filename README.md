# PatchViewer / 补丁查看器
<h1 align="center">
  <a href="https://github.com/DeterMination-Wind/PatchViewer/releases/latest"><img src="https://img.shields.io/github/v/release/DeterMination-Wind/PatchViewer?display_name=release&label=Latest%20Release&color=green"></a>
  <a href="https://github.com/DeterMination-Wind/PatchViewer/releases"><img src="https://img.shields.io/github/downloads/DeterMination-Wind/PatchViewer/total?label=Downloads&color=blue"></a>
  <a href="LICENSE"><img src="https://img.shields.io/github/license/DeterMination-Wind/PatchViewer?label=License"></a>
  <a href="https://github.com/DeterMination-Wind/PatchViewer"><img src="https://img.shields.io/github/stars/DeterMination-Wind/PatchViewer?style=flat&label=Star%20this%20mod!&color=yellow"></a>
</h1>

PatchViewer is a Mindustry Java mod for viewing before/after diffs introduced by datapatches. It highlights patched content in the core database and can show a compact diff while hovering blocks or units in-game.

PatchViewer 是一个 Mindustry Java 模组，用于查看 datapatch 对内容造成的修改。它会在核心数据库中高亮补丁前后的属性差异，也可以在游戏内悬停建筑或单位时显示紧凑差异。

## Features / 功能

- Native core-database stats with in-place before/after diff panels
- Native stat snapshots for modified, removed, and added values
- Hold the PatchViewer quick-view key, default `Alt`, to inspect patched blocks or units under the cursor
- Quick diff display modes: near-cursor HUD or inserted build-info panel
- Configurable diff colors, HUD opacity, HUD width, and HUD background color
- Compact icon/stack rendering for costs, inputs, outputs, mineable ores, weapons, and other icon-heavy stats
- Patched database stats preserve the game's native icon sizes, wrapping, layout, and icon interactions
- Merged desktop + Android release artifacts

- 保持核心数据库原版属性显示，并在变化属性位置插入修改前/修改后的差异面板
- 对修改、删除、新增的原生属性快照显示前后对照
- 长按 PatchViewer 快捷查看键，默认 `Alt`，可查看鼠标下建筑或单位的补丁差异
- 快捷差异支持鼠标旁 HUD 与建造栏插入两种显示模式
- 可配置 diff 颜色、HUD 透明度、HUD 宽度和 HUD 背景色
- 对造价、输入、输出、可采集矿物、武器等图标密集属性使用紧凑 icon/stack 显示
- 被补丁影响的数据库属性保留游戏原有的图标尺寸、换行、排版和图标交互
- 同时输出桌面版和安卓版可用的合并产物

## Build / 构建

Use the merged build task:

```powershell
./gradlew.bat clean deploy
```

Artifacts will be generated here:

- `dist/PatchViewer.jar`
- `dist/PatchViewer.zip`
- `../构建/PatchViewer/PatchViewer-v2.2.1.jar`
- `../构建/PatchViewer/PatchViewer-v2.2.1.zip`

## Release / 发布

GitHub Actions workflow: `.github/workflows/release.yml`

- Trigger: push tag `v*` or manual dispatch
- Build: `clean deploy`
- Release assets: `dist/*.jar` and `dist/*.zip`
