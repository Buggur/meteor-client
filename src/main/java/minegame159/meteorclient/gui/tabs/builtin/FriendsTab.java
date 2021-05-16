/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.tabs.builtin;

import minegame159.meteorclient.gui.GuiTheme;
import minegame159.meteorclient.gui.tabs.Tab;
import minegame159.meteorclient.gui.tabs.TabScreen;
import minegame159.meteorclient.gui.tabs.WindowTabScreen;
import minegame159.meteorclient.gui.widgets.containers.WHorizontalList;
import minegame159.meteorclient.gui.widgets.containers.WSection;
import minegame159.meteorclient.gui.widgets.containers.WTable;
import minegame159.meteorclient.gui.widgets.input.WDropdown;
import minegame159.meteorclient.gui.widgets.input.WTextBox;
import minegame159.meteorclient.gui.widgets.pressable.WMinus;
import minegame159.meteorclient.gui.widgets.pressable.WPlus;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.friends.Friend;
import minegame159.meteorclient.systems.friends.Friends;
import minegame159.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.screen.Screen;

public class FriendsTab extends Tab {
    public FriendsTab() {
        super("Friends");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new FriendsScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof FriendsScreen;
    }

    private static class FriendsScreen extends WindowTabScreen {
        public FriendsScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);

            Settings s = new Settings();

            SettingGroup sgEnemy = s.createGroup("Enemies");
            SettingGroup sgNeutral = s.createGroup("Neutral");
            SettingGroup sgTrusted = s.createGroup("Trusted");

            // Enemies

            Setting<Boolean> showEnemies = sgEnemy.add(new BoolSetting.Builder()
                    .name("show-in-tracers")
                    .description("Whether to show enemies in tracers.")
                    .defaultValue(true)
                    .onChanged(aBoolean -> Friends.get().showEnemies = aBoolean)
                    .onModuleActivated(booleanSetting -> booleanSetting.set(Friends.get().showEnemies))
                    .build()
            );

            sgEnemy.add(new ColorSetting.Builder()
                    .name("color")
                    .description("The color used to show enemies in ESP and Tracers.")
                    .defaultValue(new SettingColor(204, 0, 0))
                    .onChanged(Friends.get().enemyColor::set)
                    .onModuleActivated(colorSetting -> colorSetting.set(Friends.get().enemyColor))
                    .visible(showEnemies::get)
                    .build()
            );

            // Neutral

            Setting<Boolean> showNeutrals = sgNeutral.add(new BoolSetting.Builder()
                    .name("show-in-tracers")
                    .description("Whether to show neutrals in tracers.")
                    .defaultValue(true)
                    .onChanged(aBoolean -> Friends.get().showNeutral = aBoolean)
                    .onModuleActivated(booleanSetting -> booleanSetting.set(Friends.get().showNeutral))
                    .build()
            );

            sgNeutral.add(new ColorSetting.Builder()
                    .name("color")
                    .description("The color used to show neutrals in ESP and Tracers.")
                    .defaultValue(new SettingColor(60, 240,240))
                    .onChanged(Friends.get().neutralColor::set)
                    .onModuleActivated(colorSetting -> colorSetting.set(Friends.get().neutralColor))
                    .visible(showNeutrals::get)
                    .build()
            );

            sgNeutral.add(new BoolSetting.Builder()
                    .name("attack")
                    .description("Whether to attack neutrals.")
                    .defaultValue(false)
                    .onChanged(aBoolean -> Friends.get().attackNeutral = aBoolean)
                    .onModuleActivated(booleanSetting -> booleanSetting.set(Friends.get().attackNeutral))
                    .build()
            );

            // Trusted

            Setting<Boolean> showTrusted = sgTrusted.add(new BoolSetting.Builder()
                    .name("show-in-tracers")
                    .description("Whether to show trusted in tracers.")
                    .defaultValue(true)
                    .onChanged(aBoolean -> Friends.get().showTrusted = aBoolean)
                    .onModuleActivated(booleanSetting -> booleanSetting.set(Friends.get().showTrusted))
                    .build()
            );

            sgTrusted.add(new ColorSetting.Builder()
                    .name("color")
                    .description("The color used to show trusted in ESP and Tracers.")
                    .defaultValue(new SettingColor(57, 247, 47))
                    .onChanged(Friends.get().trustedColor::set)
                    .onModuleActivated(colorSetting -> colorSetting.set(Friends.get().trustedColor))
                    .visible(showTrusted::get)
                    .build()
            );

            s.onActivated();
            add(theme.settings(s)).expandX();

            // Friends
            WSection friends = add(theme.section("Friends")).expandX().widget();
            WTable table = friends.add(theme.table()).expandX().widget();

            fillTable(table);

            // New
            WHorizontalList list = friends.add(theme.horizontalList()).expandX().widget();

            WTextBox nameW = list.add(theme.textBox("")).minWidth(400).expandX().widget();
            nameW.setFocused(true);

            WPlus add = list.add(theme.plus()).widget();
            add.action = () -> {
                String name = nameW.get().trim();

                if (Friends.get().add(new Friend(name))) {
                    nameW.set("");

                    table.clear();
                    fillTable(table);
                }
            };

            enterAction = add.action;
        }

        private void fillTable(WTable table) {
            for (Friend friend : Friends.get()) {
                table.add(theme.label(friend.name));

                WDropdown<Friends.FriendType> type = table.add(theme.dropdown(friend.type)).widget();
                type.action = () -> friend.type = type.get();

                WMinus remove = table.add(theme.minus()).expandCellX().right().widget();
                remove.action = () -> {
                    Friends.get().remove(friend);

                    table.clear();
                    fillTable(table);
                };

                table.row();
            }
        }
    }
}
