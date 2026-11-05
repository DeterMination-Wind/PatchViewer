# PatchViewer / 补丁查看器

> [NOTE] 若有反馈，请发送反馈到bugsoc@qq.com / If you have any feedback, please send it to bugsoc@qq.com

> 让 datapatch 改了什么变得可见，不再靠猜测排查内容冲突。

PatchViewer 是一个 Mindustry 内容检查工具。它把补丁前后的建筑、单位和数据库属性放到同一个视野中，帮助玩家和模组作者理解当前数据到底发生了哪些变化。

它适合调试模组组合、检查数据补丁影响和复核建筑属性。PatchViewer 只展示差异，不会主动修改数据库或游戏规则。

## 使用与安装

安装 Release 中的 PatchViewer.jar 或 PatchViewer.zip，在游戏内启用后即可从数据库界面或游戏内内容信息查看差异。

## 构建

~~~powershell
.\gradlew.bat clean deploy
~~~

## English

> See what a datapatch changed instead of guessing through a content conflict.

PatchViewer is a Mindustry content-inspection tool. It places before-and-after values for blocks, units, and database entries in the same view, helping players and mod authors understand the actual effect of a data patch.

It is useful for debugging mod combinations, reviewing patch impact, and checking content properties. PatchViewer displays differences; it does not rewrite the database or game rules.

## Usage and install

Install PatchViewer.jar or PatchViewer.zip from Releases and enable it in-game. Differences are available from the database view and in-game content information.

## Build

~~~powershell
.\gradlew.bat clean deploy
~~~

## 文档

架构、开发、发布、测试与术语表见 [docs/README.md](docs/README.md)(Chinese)。仓库维护约束见 [AGENTS.md](AGENTS.md)。

