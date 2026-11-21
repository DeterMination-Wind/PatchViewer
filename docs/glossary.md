# 术语表

按主题归类的 PatchViewer 术语。每条说明它在 PatchViewer 语境下的准确含义。

## 项目形态

### datapatch
修改游戏数据库对象/属性的补丁内容,由地图/存档的 patch 资产携带或由服务端下发(模组目录不存放 patch)。补丁在内容加载后、进图阶段改写建筑/单位等的属性与描述;PatchViewer 抓取修改前后两份快照并展示差异。本工具只读展示,不生成也不改写 datapatch。

### 基线快照(baseline snapshot)
`ClientLoadEvent` 后对全部 `UnlockableContent` 抓取的"补丁前"属性集合(`ContentSnapshot`):标题、描述、详情、建造成本(反射读 `requirements`)与全部属性行。进图后不重建,是差异的左值来源。

### after 快照
`WorldLoadEvent` 时重建的"补丁后"快照。与基线快照逐行比较得到差异;`ResetEvent` 时清空,因此回到主菜单再进图会重新生成。

### hidden
`mod.json` / `mod.hjson` 中的标记。PatchViewer 声明 `hidden: true`,不在游戏的模组列表中单独露出,但仍正常加载。

## 差异机制

### 差异视图(diff view)
挂接原版 `ContentInfoDialog` 后的数据库详情展示:在被修改的属性行外包一层"基线 inset + `->` + 原 inset",文本则直接改写对应 Label。原则是不重建原版控件,保留原生的图标尺寸与点击行为。

### 快捷差异(quick diff / CompactDiff)
局内长按快捷键(默认左 Alt,`patchviewer-quick-view`)时显示的紧凑差异列表,按 Added / Modified / Removed 三组归纳。两种显示模式:`cursor`(跟随鼠标的 HUD)与 `buildinfo`(注入建造栏信息表)。

### 隐藏词条(hidden entry)
datapatch 改了、但数据库没有对应 Stat 行的建筑字段差异。数据来自游戏 `DataPatcher.usedpatches` 里的 `PatchRecord`(`target` + `Field` + 补丁前值),只在确实变化时单独成节展示;范围仅限建筑自身字段与一层 `DrawBlock`/`BulletType` 子对象,详见[架构](architecture.md#隐藏词条非词条字段差异)。

### RowKind
属性行的分类枚举(TEXT、STACK_LIST、BUILD_COST、NATIVE_PANEL、AMMO_PANEL、WEAPON_PANEL、ABILITY_PANEL),决定该行的比较方式与渲染方式,例如 STACK_LIST 按物品条目逐个匹配增/删/改。

### 图标 token(icon token)
内部序列化格式 `<patchviewer-icon:<type>:<id>>`,把内容图标嵌进纯文本差异串,渲染时再还原成图片。用于紧凑差异中携带物品/液体/单位图标。

### 颜色规格(color spec)
差异染色的配置值,支持 Arc 命名色(如 `gold`)或十六进制(`#ffd700` / `ffd700`)。非法值在设置页实时标记"无效"并回退默认。

## 兼容

### 反射探针
对游戏内部字段(`PlacementFragment` 的 hover 状态)与第三方模组的运行时探测:`probeMi2u()` 探测 MI2U、`tryInitPlacementReflection()` 缓存原版字段。全部失败降级、缺类不再重试,保证裸原版可用。

### MI2U 兼容
MI2U 开启 `replaceTopTable` 接管建造栏后,快捷差异改从 MI2U 的悬停信息取目标、注入其 `buildt` 表,经反射完成。

### MindustryX 分享
联机时详情视图提供"♐简 / ♻详"分享按钮,经反射调用 `mindustryX.features.ShareFeature`,未装 MindustryX 时按钮静默隐藏。

## 构建与发布

### D8 / classes.dex
把 Java 字节码转成安卓 `classes.dex` 的工具链。安卓端加载 Java 模组必须包含 dex;本仓库无运行期依赖,故不需要 `--lib`。可分发 jar 由 `jarMerged` 合并 classes + dex + 描述文件。

### bundle
Mindustry 的 i18n 文件。PatchViewer 的全部用户可见文案在 `assets/bundles/`(en / es / fr / ru / zh_CN / zh_TW 六份),不允许硬编码。

### 版本号三处同步
`mod.json`、`mod.hjson`、`build.gradle` 三处的版本号必须一致,发版时手工同步修改;tag 形如 `v2.4.0`。详见[版本与发布](release.md)。

### release-notes
`.github/release-notes/vX.Y.Z.md`,每个 tag 一份中英双语说明,CI 用它作为 GitHub Release 正文。
