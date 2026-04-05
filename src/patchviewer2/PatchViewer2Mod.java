package patchviewer2;

import arc.Events;
import arc.scene.actions.Actions;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.OrderedMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Scaling;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.ctype.Content;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.mod.Mod;
import mindustry.type.ItemStack;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.ContentInfoDialog;
import mindustry.world.Block;
import mindustry.world.blocks.units.RepairTurret;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatCat;
import mindustry.world.meta.StatValue;
import mindustry.world.meta.Stats;

public class PatchViewer2Mod extends Mod{
    private static final float panelWidth = 430f;

    private final ObjectMap<String, ContentSnapshot> baselineSnapshots = new ObjectMap<String, ContentSnapshot>();
    private boolean baselineCaptured;

    public PatchViewer2Mod(){
        Events.on(mindustry.game.EventType.ContentInitEvent.class, event -> arc.Core.app.post(this::captureBaseline));
        Events.on(mindustry.game.EventType.ClientLoadEvent.class, event -> installDialogHook());
    }

    private void installDialogHook(){
        try{
            Vars.ui.content = new ContentInfoDialog(){
                @Override
                public void show(UnlockableContent content){
                    showDualPane(content);
                }
            };
        }catch(Throwable error){
            Log.err("[PatchViewer2] Failed to hook ContentInfoDialog.", error);
        }
    }

    private void captureBaseline(){
        if(baselineCaptured || Vars.content == null) return;
        baselineSnapshots.clear();
        try{
            Seq<Content>[] contentMap = Vars.content.getContentMap();
            for(int i = 0; i < contentMap.length; i++){
                Seq<Content> seq = contentMap[i];
                for(int j = 0; j < seq.size; j++){
                    Content raw = seq.get(j);
                    if(raw instanceof UnlockableContent){
                        UnlockableContent content = (UnlockableContent)raw;
                        baselineSnapshots.put(contentKey(content), snapshot(content));
                    }
                }
            }
            baselineCaptured = true;
            Log.info("[PatchViewer2] Captured @ baseline snapshots after startup.", baselineSnapshots.size);
        }catch(Throwable error){
            Log.err("[PatchViewer2] Failed to capture baseline snapshots.", error);
        }
    }

    private void showDualPane(UnlockableContent content){
        if(!baselineCaptured) captureBaseline();

        ContentSnapshot left = baselineSnapshots.get(contentKey(content));
        ContentSnapshot right = snapshot(content);
        if(left == null) left = right;

        final ContentSnapshot leftSnapshot = left;
        final ContentSnapshot rightSnapshot = right;

        Vars.ui.content.cont.clear();

        Table root = new Table();
        root.margin(10f);

        addHeader(root, content);
        root.row();

        root.table(panes -> {
            panes.defaults().top();
            panes.table(Styles.grayPanel, leftPane -> buildSnapshotPane(leftPane, "原始", leftSnapshot, rightSnapshot, false)).width(panelWidth).growY();
            panes.add().width(10f);
            panes.table(Styles.grayPanel, rightPane -> buildSnapshotPane(rightPane, "当前", rightSnapshot, leftSnapshot, true)).width(panelWidth).growY();
        }).growX().top();

        root.row();

        root.table(t -> {
            t.defaults().size(40f);
            t.button(content.emoji(), Styles.cleart, () -> arc.Core.app.setClipboardText(content.emoji())).tooltip(content.emoji());
            t.button(Icon.info, Styles.clearNonei, () -> arc.Core.app.setClipboardText(content.name)).tooltip(content.name);
            if(content.description != null){
                t.button(Icon.book, Styles.clearNonei, () -> arc.Core.app.setClipboardText(content.description)).tooltip(content.description);
            }
        }).fillX().padLeft(10f).padTop(8f);

        ScrollPane pane = new ScrollPane(root);
        root.marginRight(30f);
        Vars.ui.content.cont.add(pane);

        if(Vars.ui.content.isShown()){
            Vars.ui.content.show(arc.Core.scene, Actions.fadeIn(0f));
        }else{
            Vars.ui.content.show();
        }
    }

    private void addHeader(Table table, UnlockableContent content){
        table.table(title -> {
            title.image(content.uiIcon).size(Vars.iconXLarge).scaling(Scaling.fit).get().clicked(() -> arc.Core.app.setClipboardText(content.emoji()));
            int logicId = content.getLogicId();
            title.add("[accent]" + content.localizedName + "\n[gray]" + content.name + (logicId != -1 ? " <#" + logicId + ">" : "")).padLeft(5f).left();
        }).left();

        if(Vars.state.isGame() && Vars.state.patcher != null && Vars.state.patcher.isPatched(content)){
            table.row();
            table.table(t -> {
                t.image(Icon.info).color(Pal.lightishGray);
                t.add("@database.patched").color(Pal.lightishGray).padLeft(4f);
            }).pad(4f).left();
        }
    }

    private void buildSnapshotPane(Table pane, String title, ContentSnapshot primary, ContentSnapshot secondary, boolean highlightChanges){
        pane.margin(10f);
        pane.top().left();
        pane.defaults().left().top().growX();
        pane.add("[accent]" + title).padBottom(6f);
        pane.row();

        if(primary.description != null){
            pane.add("[gold]用途[]").padTop(4f);
            pane.row();
            addWrapped(pane, primary.description, highlightChanges && !safeEquals(primary.description, secondary.description));
            pane.row();
        }

        for(int i = 0; i < primary.sections.size; i++){
            SectionSnapshot section = primary.sections.get(i);
            pane.add("[gold]" + section.title + "[]").padTop(6f);
            pane.row();
            for(int j = 0; j < section.rows.size; j++){
                RowSnapshot row = section.rows.get(j);
                String primaryValue = primary.valueOf(section.title, row.label);
                String otherValue = secondary.valueOf(section.title, row.label);
                String shownValue = primaryValue != null ? primaryValue : otherValue;
                boolean changed = highlightChanges && primaryValue != null && (otherValue == null || !safeEquals(primaryValue, otherValue));
                addRow(pane, row.label, shownValue, changed);
                pane.row();
            }
        }

        Seq<String> extraSections = extraSectionTitles(primary, secondary);
        for(int i = 0; i < extraSections.size; i++){
            String sectionTitle = extraSections.get(i);
            pane.add("[gold]" + sectionTitle + "[]").padTop(6f);
            pane.row();
            SectionSnapshot section = secondary.sectionByTitle(sectionTitle);
            for(int j = 0; j < section.rows.size; j++){
                RowSnapshot row = section.rows.get(j);
                String otherValue = secondary.valueOf(section.title, row.label);
                addRow(pane, row.label, otherValue, highlightChanges && otherValue != null);
                pane.row();
            }
        }

        if(primary.details != null || secondary.details != null){
            pane.add("[gold]详情[]").padTop(6f);
            pane.row();
            String shown = primary.details != null ? primary.details : secondary.details;
            boolean changed = highlightChanges && secondary.details != null && (primary.details == null || !safeEquals(primary.details, secondary.details));
            addWrapped(pane, shown, changed);
            pane.row();
        }

        Seq<Integer> weaponKeys = mergeWeaponKeys(primary, secondary);
        if(weaponKeys.any()){
            pane.add("[gold]武器[]").padTop(6f);
            pane.row();
            for(int i = 0; i < weaponKeys.size; i++){
                int key = weaponKeys.get(i);
                WeaponSnapshot primaryWeapon = primary.weaponByKey(key);
                WeaponSnapshot secondaryWeapon = secondary.weaponByKey(key);
                WeaponSnapshot shownWeapon = primaryWeapon != null ? primaryWeapon : secondaryWeapon;
                if(shownWeapon == null) continue;

                pane.add("[lightgray]" + shownWeapon.name).padTop(4f);
                pane.row();

                Seq<RowSnapshot> rows = mergeWeaponRows(primaryWeapon, secondaryWeapon);
                for(int j = 0; j < rows.size; j++){
                    RowSnapshot row = rows.get(j);
                    String primaryValue = primaryWeapon == null ? null : primaryWeapon.valueOf(row.label);
                    String otherValue = secondaryWeapon == null ? null : secondaryWeapon.valueOf(row.label);
                    String shownValue = primaryValue != null ? primaryValue : otherValue;
                    boolean changed = highlightChanges && primaryValue != null && (otherValue == null || !safeEquals(primaryValue, otherValue));
                    addRow(pane, row.label, shownValue, changed);
                    pane.row();
                }
            }
        }
    }

    private Seq<String> extraSectionTitles(ContentSnapshot primary, ContentSnapshot secondary){
        Seq<String> out = new Seq<String>();
        for(int i = 0; i < secondary.sections.size; i++){
            String title = secondary.sections.get(i).title;
            if(primary.sectionByTitle(title) == null) out.add(title);
        }
        return out;
    }

    private Seq<Integer> mergeWeaponKeys(ContentSnapshot primary, ContentSnapshot secondary){
        Seq<Integer> out = new Seq<Integer>();
        addWeaponKeys(out, primary);
        addWeaponKeys(out, secondary);
        return out;
    }

    private void addWeaponKeys(Seq<Integer> out, ContentSnapshot snapshot){
        for(int i = 0; i < snapshot.weapons.size; i++){
            int key = snapshot.weapons.get(i).key;
            if(!out.contains(key)) out.add(key);
        }
    }

    private Seq<RowSnapshot> mergeWeaponRows(WeaponSnapshot primary, WeaponSnapshot secondary){
        Seq<RowSnapshot> out = new Seq<RowSnapshot>();
        addWeaponRows(out, primary);
        addWeaponRows(out, secondary);
        return out;
    }

    private void addWeaponRows(Seq<RowSnapshot> out, WeaponSnapshot weapon){
        if(weapon == null) return;
        for(int i = 0; i < weapon.rows.size; i++){
            RowSnapshot row = weapon.rows.get(i);
            if(!containsLabel(out, row.label)) out.add(row);
        }
    }

    private boolean containsLabel(Seq<RowSnapshot> rows, String label){
        for(int i = 0; i < rows.size; i++){
            if(rows.get(i).label.equals(label)) return true;
        }
        return false;
    }

    private void addRow(Table table, String label, String value, boolean changed){
        String color = changed ? "[green]" : "[white]";
        table.table(line -> {
            line.left().defaults().left().top();
            line.add("[lightgray]" + label + ":[] ");
            Label valueWidget = new Label(color + escape(value) + "[]");
            valueWidget.setWrap(true);
            line.add(valueWidget).growX().width(panelWidth - 90f);
        }).growX();
    }

    private void addWrapped(Table table, String text, boolean changed){
        Label label = new Label((changed ? "[green]" : "[lightgray]") + escape(text) + "[]");
        label.setWrap(true);
        table.add(label).width(panelWidth - 24f).growX();
    }

    private ContentSnapshot snapshot(UnlockableContent content){
        ContentSnapshot out = new ContentSnapshot();
        out.description = content.displayDescription();
        out.details = content.details;

        if(content instanceof Block){
            Block block = (Block)content;
            if(block.requirements != null && block.requirements.length > 0){
                addSectionRow(out, bundle("category.general", "基础"), Stat.buildCost.localized(), formatItemStacks(block.requirements));
            }
            if(block.hasPower && block.consPower != null && block.consPower.usage > 0f){
                addSectionRow(out, bundle("category.power", "电力"), block.consPower.buffered ? Stat.powerCapacity.localized() : Stat.powerUse.localized(), "⚡ " + Strings.autoFixed(block.consPower.usage * 60f, 0) + " 电力/秒");
            }
            if(block.hasLiquids){
                addSectionRow(out, bundle("category.liquids", "液体"), Stat.liquidCapacity.localized(), Strings.autoFixed(block.liquidCapacity, 0) + " 液体");
            }
        }

        if(content instanceof RepairTurret){
            RepairTurret turret = (RepairTurret)content;
            addSectionRow(out, bundle("category.function", "功能"), Stat.repairSpeed.localized(), Strings.autoFixed(turret.repairSpeed * 60f, 0) + " /秒");
            addSectionRow(out, bundle("category.function", "功能"), Stat.range.localized(), Strings.autoFixed(turret.repairRadius / Vars.tilesize, 3) + " 格");
        }

        content.checkStats();
        Stats stats = content.stats;
        for(StatCat cat : stats.toMap().keys()){
            OrderedMap<Stat, Seq<StatValue>> map = stats.toMap().get(cat);
            if(map == null || map.size == 0) continue;

            String sectionTitle = stats.useCategories ? bundle("category." + cat.name, cat.name) : null;
            if(sectionTitle == null) continue;

            for(Stat stat : map.keys()){
                if(content instanceof Block && stat == Stat.buildCost) continue;
                String value = flattenStatValues(map.get(stat));
                if(value == null || value.trim().isEmpty()) continue;
                addSectionRow(out, sectionTitle, stat.localized(), value);
            }
        }

        if(content instanceof UnitType){
            Seq<Weapon> weapons = ((UnitType)content).weapons;
            for(int i = 0; i < weapons.size; i++){
                Weapon weapon = weapons.get(i);
                if(weapon.flipSprite || !weapon.hasStats((UnitType)content)) continue;
                WeaponSnapshot snapshot = new WeaponSnapshot();
                snapshot.key = i;
                snapshot.name = weapon.name == null || weapon.name.isEmpty() ? "weapon-" + i : weapon.name;
                snapshot.rows.add(new RowSnapshot("武器范围", Strings.autoFixed(weapon.bullet.range / 8f, 1) + " 格"));
                if(weapon.rotate){
                    snapshot.rows.add(new RowSnapshot("旋转速度", String.format("%.0f °/s", weapon.rotateSpeed * 60f)));
                }
                if(!weapon.alwaysContinuous && weapon.reload > 0 && !weapon.bullet.killShooter){
                    snapshot.rows.add(new RowSnapshot(Stat.reload.localized(), (weapon.mirror ? "2x " : "") + Strings.autoFixed(60f / weapon.reload * weapon.shoot.shots, 2) + " /秒"));
                }
                snapshot.rows.add(new RowSnapshot(Stat.damage.localized(), Strings.autoFixed(weapon.bullet.damage, 2)));
                out.weapons.add(snapshot);
            }
        }

        return out;
    }

    private void addSectionRow(ContentSnapshot out, String sectionTitle, String label, String value){
        SectionSnapshot section = out.sectionByTitle(sectionTitle);
        if(section == null){
            section = new SectionSnapshot();
            section.title = sectionTitle;
            out.sections.add(section);
        }
        if(section.valueOf(label) == null){
            section.rows.add(new RowSnapshot(label, value));
        }
    }

    private String formatItemStacks(ItemStack[] stacks){
        if(stacks == null || stacks.length == 0) return null;
        StringBuilder out = new StringBuilder();
        for(int i = 0; i < stacks.length; i++){
            ItemStack stack = stacks[i];
            if(stack == null || stack.item == null) continue;
            if(out.length() > 0) out.append("  ");
            out.append(stack.item.emoji()).append("x").append(stack.amount);
        }
        return out.toString();
    }

    private String flattenStatValues(Seq<StatValue> values){
        if(values == null || values.isEmpty()) return null;
        Table capture = new Table();
        capture.left();
        for(int i = 0; i < values.size; i++){
            try{
                values.get(i).display(capture);
            }catch(Throwable ignored){
            }
        }
        StringBuilder out = new StringBuilder();
        Seq cells = capture.getCells();
        for(int i = 0; i < cells.size; i++){
            Object cellObj = cells.get(i);
            if(!(cellObj instanceof arc.scene.ui.layout.Cell)) continue;
            Object element = ((arc.scene.ui.layout.Cell)cellObj).get();
            if(element instanceof Label){
                CharSequence text = ((Label)element).getText();
                if(text != null){
                    String cleaned = Strings.stripColors(text.toString()).trim();
                    if(!cleaned.isEmpty()){
                        if(out.length() > 0) out.append(' ');
                        out.append(cleaned);
                    }
                }
            }
        }
        return out.toString().trim();
    }

    private String contentKey(UnlockableContent content){
        ContentType type = content.getContentType();
        return type.name() + ":" + content.name;
    }

    private String bundle(String key, String fallback){
        String value = arc.Core.bundle.getOrNull(key);
        return value == null || value.isEmpty() ? fallback : value;
    }

    private boolean safeEquals(String a, String b){
        return a == null ? b == null : a.equals(b);
    }

    private String escape(String text){
        return text == null ? "" : text.replace("[", "[[");
    }

    private static class ContentSnapshot{
        String description;
        String details;
        Seq<SectionSnapshot> sections = new Seq<SectionSnapshot>();
        Seq<WeaponSnapshot> weapons = new Seq<WeaponSnapshot>();

        String valueOf(String sectionTitle, String label){
            SectionSnapshot section = sectionByTitle(sectionTitle);
            return section == null ? null : section.valueOf(label);
        }

        SectionSnapshot sectionByTitle(String sectionTitle){
            for(int i = 0; i < sections.size; i++){
                SectionSnapshot section = sections.get(i);
                if(section.title.equals(sectionTitle)) return section;
            }
            return null;
        }

        WeaponSnapshot weaponByKey(int key){
            for(int i = 0; i < weapons.size; i++){
                WeaponSnapshot weapon = weapons.get(i);
                if(weapon.key == key) return weapon;
            }
            return null;
        }
    }

    private static class SectionSnapshot{
        String title;
        Seq<RowSnapshot> rows = new Seq<RowSnapshot>();

        String valueOf(String label){
            for(int i = 0; i < rows.size; i++){
                RowSnapshot row = rows.get(i);
                if(row.label.equals(label)) return row.value;
            }
            return null;
        }
    }

    private static class WeaponSnapshot{
        int key;
        String name;
        Seq<RowSnapshot> rows = new Seq<RowSnapshot>();

        String valueOf(String label){
            for(int i = 0; i < rows.size; i++){
                RowSnapshot row = rows.get(i);
                if(row.label.equals(label)) return row.value;
            }
            return null;
        }
    }

    private static class RowSnapshot{
        final String label;
        final String value;

        RowSnapshot(String label, String value){
            this.label = label;
            this.value = value;
        }
    }
}
