# PatchViewer / 补丁查看器

<img width="733" height="1465" alt="image" src="https://github.com/user-attachments/assets/d8ee5421-84d8-4cb7-a069-a8fe4303a147" />


PatchViewer is a Mindustry Java mod that shows inline before/after stat diffs for datapatch changes inside the in-game database UI.

PatchViewer 是一个 Mindustry Java 模组，用于在游戏内数据库界面直接显示 datapatch 修改前后的属性差异。

## Features / 功能

- Inline before/after diff panels for patched content stats
- Native stat panel highlighting for modified, removed, and added values
- Configurable diff colors in the settings menu with live preview
- Merged desktop + Android release artifacts (`.jar` and `.zip`)

- 在数据库属性行内直接显示修改前/修改后的 diff 面板
- 对原生属性面板中的修改、删除、新增值进行高亮
- 在设置菜单中配置颜色，并支持实时预览
- 同时输出桌面版和安卓版可用的合并产物（`.jar` 与 `.zip`）

## Build / 构建

Use the merged build tasks:

```powershell
./gradlew.bat clean deploy
```

Artifacts will be generated here:

- `dist/PatchViewer.jar`
- `dist/PatchViewer.zip`
- `../构建/PatchViewer/PatchViewer-1.0.1.jar`
- `../构建/PatchViewer/PatchViewer-1.0.1.zip`

## Release / 发布

GitHub Actions workflow: `.github/workflows/release.yml`

- Trigger: push tag `v*` or manual dispatch
- Build: `clean deploy`
- Release assets: `dist/*.jar` and `dist/*.zip`
