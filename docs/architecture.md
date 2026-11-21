# 架构总览

改 `src/patchviewer/` 下的任何代码前先读本文。它解释 PatchViewer 如何抓取内容快照、生成差异、以及如何把差异叠加到原版界面上。玩家视角的简介见[README](../README.md)。

## 定位与双形态

PatchViewer 是纯客户端的**只读内容检查工具**:在游戏加载数据库后,把 datapatch 修改前后的内容属性放进同一视野(数据库详情视图 + 长按快捷差异)。它不修改数据库、不改游戏规则,描述文件声明 `hidden: true`,不在模组列表中单独露出。

它同时是 Neon 聚合模组的子模组(id=`pv`,真源即本仓库)。本仓库源码保持**独立形态**,不含聚合契约代码;并入 Neon 时由同步脚本注入(见[下文](#与-neon-聚合的关系))。

## 入口与生命周期

主类只有一个:`patchviewer.PatchViewerMod`(`mod.json` 的 `main`),全部逻辑在这一个文件里。核心流程:

1. **构造**:`setupDefaults()` 写入全部设置默认值;注册三个事件——`ClientLoadEvent`、`WorldLoadEvent`、`ResetEvent`,以及每帧的 `Trigger.uiDrawEnd`。
2. **ClientLoadEvent**:`installDialogHook()` 把 `Vars.ui.content`(`ContentInfoDialog`)替换为一个子类实例,在原版 `show()` 之后追加差异渲染;`registerKeybinds()` 注册快捷键 `patchviewer-quick-view`(默认左 Alt,分类 patchviewer);`tryInitPlacementReflection()` 与 `probeMi2u()` 初始化反射探针;`Core.app.post(this::captureBaselineAtStartup)` 延迟抓取基线快照。
3. **init()**:注册设置分类 `@settings.patchviewer`(仅一次,`settingsAdded` 防重入;聚合形态下此调用被注入的守卫跳过,改由 Neon 总设置页接管)。
4. **WorldLoadEvent**:`rebuildAfterSnapshots()` 重建"补丁后"快照并生成差异表(datapatch 在进图阶段生效,所以基线取启动时、after 取进图后)。
5. **ResetEvent**:清空 after 快照、差异表并隐藏快捷 HUD。
6. **uiDrawEnd(每帧)**:`updateQuickDisplay()` 决定是否显示快捷差异。

## 快照与差异管线(数据流)

```text
ClientLoadEvent ─► captureBaselineAtStartup ─► baselineSnapshots   (补丁前)
WorldLoadEvent  ─► rebuildAfterSnapshots    ─► afterSnapshots      (补丁后)
                            │                    │
                            ├─ diff() ─► diffsByContent        (详细差异,InlineRow)
                            └─ buildCompactDiff() ─► compactDiffsByContent (紧凑差异,CompactDiff)
```

- **快照(ContentSnapshot)**:遍历所有 `UnlockableContent`,抓取 title / description / details / buildCost(反射读 `requirements`)/ 全部属性行。每个属性行(`SnapshotRow`)按 `RowKind` 分类(TEXT、STACK_LIST、BUILD_COST、NATIVE_PANEL、AMMO_PANEL、WEAPON_PANEL、ABILITY_PANEL),分类决定后续的比较与渲染方式;同时把 `StatValue.display()` 离屏渲染成 Table,抽出文本签名、数字序列和逐 Label 状态(`LabelState`)。
- **详细差异(diff)**:按前后行 key 的并集比较。文本行走 `buildDiff()` 生成带颜色的 `旧值 -> 新值` markup;面板行(STACK_LIST 等)保留前后两份渲染好的 Table,用 LCS 对齐 Label 后逐个染色(`prepareRenderedDiff`)。
- **紧凑差异(buildCompactDiff)**:把差异压成带 Added / Modified / Removed 分组的行列表(`CompactDiff` / `CompactChange`);STACK_LIST 与 BUILD_COST 会解析成物品条目(`iconToken` + 数量)做逐条增/删/改匹配。内部用 `<patchviewer-icon:<type>:<id>>` 图标 token 把内容图标序列化进纯文本,渲染时再还原成图片。

## 差异展示的两个界面

1. **数据库差异视图**:`installDialogHook()` 替换的 `ContentInfoDialog` 在原版 `show()` 后调用 `showPatched()`——**不重建**原版控件,只在被修改的属性行外包一层"基线 inset + `->` + 原 inset"(`wrapNativeStatInset`),文本差异则直接改写对应 Label。宽度不够时只让新值换行,箭头始终跟住旧值。
2. **快捷差异**:`uiDrawEnd` 每帧检查:开关开启、在局内、按住快捷键、无对话框/输入框抢占。目标查找顺序(`findQuickTarget`):MI2U `replaceTopTable` 悬停建筑(反射)→ 原版 `PlacementFragment` 的 hover/hover2/menuHoverBlock(反射)→ 鼠标下建筑 → 鼠标下单位。两种显示模式:
   - `cursor`:跟随鼠标的悬浮 HUD(`quickHud` ScrollPane,宽度/透明度/背景色可调)。
   - `buildinfo`:把差异表注入建造栏信息的 `topTable`(含 MI2U 接管时的 `buildt`)。

## 隐藏词条(非词条字段差异)

数据库只展示 `content.stats` 里登记的行;datapatch 改到没有对应 Stat 行的字段(如 `solid`、`rotate`、`configurable`、`destroyBullet.damage`)时,常规差异管线看不到。隐藏词条就是补上这一段:

- **数据来源**:反射 `Vars.state.data.patcher.usedpatches`(v157 形态为 `state.patcher`)。集合里的 `PatchRecord` 保存 `target` + `java.lang.reflect.Field` + 赋值前的旧值;新值直接 `Field.get(target)` 读取。**不调用 `unapply()`**,不改游戏状态。
- **范围**:仅 `Block` 自身字段,以及建筑直接字段指向的一层子对象(`DrawBlock` / `BulletType`;同一子对象被多个建筑共用时跳过归属)。数组/Seq 等容器字段发生整体替换时只显示"已变更";`requirements`/`attributes`/`ammoTypes`/`consumes` 等 map/consumer 补丁、`afterPatch()` 派生字段、补丁新增内容都不在范围内,属已知缺口。`consPower`/`consumeBuilder` 等字段本身带 `@NoPatch`,引擎会拒绝其嵌套修改,因此不出现在结果里。
- **去重**:字段能映射到可见 Stat(`health`/`armor`/`size`/`buildTime`/`itemCapacity`/`liquidCapacity`/`unitCapModifier`、按声明类区分的 `range`/`reload`、炮塔的 `inaccuracy`/`targetAir`/`targetGround`/`ammoPerShot`/`heatRequirement`/`maxHeatEfficiency`,以及反射解析的 `StatExt.canOverdrive`/`mendReload`)且该 Stat 行已经出现在本次可见差异里时,不再重复展示;`localizedName`/`name`/`description`/`details`/`requirements` 五个专用通道直接跳过。无法渲染的值与格式化后相同的值折叠成单条"已变更"。
- **展示**:只有确实发生变化时才出现。数据库页在原生词条之后追加带 `name = patchviewer-hidden-section` 的小节;快捷 HUD / 建造栏在 Added / Modified / Removed 之后追加固定表头的小节。无变化时两个界面与之前完全一致。
- **刷新**:`WorldLoadEvent` 后构建一次;局内按住快捷查看键时每 60 帧按 `patcher` 身份 + 记录集合签名检查一次,数据库对话框每次打开强制刷新,`ResetEvent` 清空。编辑器里运行时重新应用补丁只会刷新隐藏小节,HUD 的可见差异(`afterSnapshots` / `diffsByContent`)仍来自上次进图,需要重新进图才会同步;全部反射失败只关闭该功能,不影响常规差异。

## 设置系统

设置直接使用 `Core.settings` + `SettingsMenuDialog.SettingsTable`,由 `buildSettings()` 构建:

| key | 含义 |
| --- | --- |
| `patchviewer-enabled` | 总开关(关闭后内容信息完全回到原版行为) |
| `patchviewer-hidden-entries` | 隐藏词条开关(默认开;关闭后只影响非词条字段小节,HUD 与数据库页都不再显示) |
| `patchviewer-quick-display-mode` | 快捷显示模式:`cursor` / `buildinfo` |
| `patchviewer-quick-hud-opacity` / `-width` / `-background` | HUD 透明度(20–100)、宽度(100–900px)、背景色 |
| `patchviewer-color-removed` / `-modified-old` / `-modified-new` / `-added` | 删除 / 修改前 / 修改后 / 新增的染色 |

颜色值支持 Arc 命名色(如 `gold`)或十六进制(`#ffd700` / `ffd700`),非法输入实时标"无效"。自定义 Setting 子类:`QuickDisplayModeSetting`、`ColorSetting`(带预览与重置)、`MessageSetting`。

## 与 Neon 聚合的关系

- Neon 的 `tools/submods.json` 中本仓库 id 为 `pv`:`src/patchviewer` → Neon `src/main/java/patchviewer`,`assets/bundles` → Neon 合并 bundle。
- 本仓库**不写** `bekBundled` 契约;pv 条目未关 `injectBekHooks`(默认开启),同步时由 Neon 的 `inject_bek_hooks()` 自动注入:`bekBundled` 字段、`init()` 的 `if(bekBundled) return;` 早退、`addCategory` 的 `if(!bekBundled)` 守卫、`buildSettings` 的 `bekBuildSettings` 包装,并做结构断言。
- 因此本仓库的 `init()` / `buildSettings()` 结构是注入脚本的匹配目标:改动这两个方法的结构后,必须回 Neon 跑 `python tools/update_submods.py --only pv` 确认注入与断言仍通过。
- 聚合形态下设置项(含 `patchviewer-enabled` 开关)由 Neon 总设置页挂载,key 不变,两种形态共用同一份配置。

## 目录速查

```text
src/patchviewer/       唯一源码包:PatchViewerMod.java(快照/差异/渲染/设置全部逻辑)
assets/bundles/        i18n:bundle.properties + es / fr / ru / zh_CN / zh_TW
.github/workflows/     release.yml:tag 触发的构建与 GitHub Release
.github/release-notes/ 每个版本一份中英双语 Release 正文
mod.json / mod.hjson   模组描述(内容保持一致)
```

## 设计约束(务必保持)

- 纯客户端、只读展示:不改数据库、不改游戏规则,只做可视化。
- 尊重原版界面:差异优先在原版 `ContentInfoDialog` 上原位叠加,不重建 StatValue 控件,保留原版的图标尺寸、点击行为与布局。
- 对游戏内部字段与第三方模组(MI2U、MindustryX ShareFeature)一律反射访问并逐级降级,缺类静默跳过,不得引入编译期硬依赖(MindustryX classes 仅可选 compileOnly)。
- 用户可见文案一律走 bundle(六语言),不硬编码。
- 目标字节码 Java 8(`minGameVersion: 154`),不使用更高版本语法。
- 保持单文件结构:注入脚本依赖 `PatchViewerMod.java` 的既有形状,不做拆包/重构。
