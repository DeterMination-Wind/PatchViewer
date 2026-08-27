# PatchViewer 文档

PatchViewer(补丁查看器)的分类文档。文档以中文为主,功能名保留英文本名,便于与代码、bundle key 和设置界面对照。

## 从哪里开始

| 你是 | 从这里开始 |
| --- | --- |
| 玩家 | [README](../README.md):功能简介与安装 |
| 想了解 PatchViewer 怎么实现的人 | [架构总览](architecture.md) |
| 想改 PatchViewer 代码 | [开发指南](development.md) 与[架构总览](architecture.md) |
| 准备发版 | [版本与发布](release.md) |
| 改动后想验证 | [测试指南](testing.md) |
| 遇到不认识的词 | [术语表](glossary.md) |

## 文档地图

```text
docs/
|-- README.md        本页:文档导航
|-- architecture.md  架构总览:入口、生命周期、快照与差异管线、设置系统、Neon 聚合形态
|-- development.md   开发指南:环境、构建命令、代码风格、调试建议
|-- release.md       版本与发布:版本号体系、构建产物链、CI、Release 资产安全
|-- testing.md       测试指南:手测清单(本仓库无自动化测试)
`-- glossary.md      术语表
```

## 相关文件

- [README.md](../README.md):项目主页(中英双语)。
- [AGENTS.md](../AGENTS.md):仓库维护约束(结构契约、与 Neon 聚合的注意事项)。
- [mod.json](../mod.json) / [mod.hjson](../mod.hjson):模组描述文件,版本号需与 build.gradle 三处一致。
- `.github/workflows/release.yml` 与 `.github/release-notes/`:发布流水线与各版本说明。

## 维护约定

- 文档描述"现状",不描述"计划";写进文档的行为必须能在 `src/patchviewer/PatchViewerMod.java` 中找到对应实现。
- 用户可见文案(设置标题、差异分组标题等)都来自 `assets/bundles/`;改文案时六种语言文件一起改。
- 版本号出现在 mod.json、mod.hjson、build.gradle 三处,发版步骤见 [release.md](release.md)。
