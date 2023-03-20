/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.entity.DropItemsEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.packets.InventoryEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.CloseHandledScreenC2SPacketAccessor;
import meteordevelopment.meteorclient.mixin.HandledScreenAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.FilterMode;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.AbstractSkullBlock;
import net.minecraft.block.CarvedPumpkinBlock;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Equipment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class InventoryTweaks extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSorting = settings.createGroup("Sorting");
    private final SettingGroup sgAutoDrop = settings.createGroup("Auto Drop");

    // General

    private final Setting<Boolean> mouseDragItemMove = sgGeneral.add(new BoolSetting.Builder()
        .name("mouse-drag-item-move")
        .description("Moving mouse over items while holding shift will transfer it to the other container.")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<Item>> antiDropItems = sgGeneral.add(new ItemListSetting.Builder()
        .name("anti-drop-items")
        .description("Items to prevent dropping. Doesn't work in creative inventory screen.")
        .build()
    );

    private final Setting<Boolean> xCarry = sgGeneral.add(new BoolSetting.Builder()
        .name("xcarry")
        .description("Allows you to store four extra item stacks in your crafting grid.")
        .defaultValue(true)
        .onChanged(v -> {
            if (v || !Utils.canUpdate()) return;
            mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.playerScreenHandler.syncId));
            invOpened = false;
        })
        .build()
    );

    private final Setting<Boolean> armorStorage = sgGeneral.add(new BoolSetting.Builder()
        .name("armor-storage")
        .description("Allows you to put normal items in your armor slots.")
        .defaultValue(true)
        .build()
    );

    // Sorting

    private final Setting<Boolean> sortingEnabled = sgSorting.add(new BoolSetting.Builder()
        .name("sorting-enabled")
        .description("Automatically sorts stacks in inventory.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Keybind> sortingKey = sgSorting.add(new KeybindSetting.Builder()
        .name("sorting-key")
        .description("Key to trigger the sort.")
        .visible(sortingEnabled::get)
        .defaultValue(Keybind.fromButton(GLFW.GLFW_MOUSE_BUTTON_MIDDLE))
        .build()
    );

    private final Setting<Integer> sortingDelay = sgSorting.add(new IntSetting.Builder()
        .name("sorting-delay")
        .description("Delay in ticks between moving items when sorting.")
        .visible(sortingEnabled::get)
        .defaultValue(1)
        .min(0)
        .build()
    );

    // Auto Drop

    private final Setting<List<Item>> autoDropItems = sgAutoDrop.add(new ItemListSetting.Builder()
        .name("auto-drop-items")
        .description("Items to drop.")
        .build()
    );

    private final Setting<Boolean> autoDropExcludeEquipped = sgAutoDrop.add(new BoolSetting.Builder()
        .name("exclude-equipped")
        .description("Whether or not to drop items equipped in armor slots.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoDropExcludeHotbar = sgAutoDrop.add(new BoolSetting.Builder()
        .name("exclude-hotbar")
        .description("Whether or not to drop items from your hotbar.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> autoDropOnlyFullStacks = sgAutoDrop.add(new BoolSetting.Builder()
        .name("only-full-stacks")
        .description("Only drops the items if the stack is full.")
        .defaultValue(false)
        .build()
    );

    public final Button[] buttons = {
        new Delete(),
        new Steal(),
        new Dump()
    };

    private InventorySorter sorter;
    private boolean invOpened;

    public InventoryTweaks() {
        super(Categories.Misc, "inventory-tweaks", "Various inventory related utilities.");
    }

    @Override
    public void onActivate() {
        invOpened = false;
    }

    @Override
    public void onDeactivate() {
        sorter = null;

        if (invOpened) {
            mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.playerScreenHandler.syncId));
        }
    }

    // Sorting and armour swapping

    @EventHandler
    private void onKey(KeyEvent event) {
        if (event.action != KeyAction.Press) return;

        if (sortingKey.get().matches(true, event.key)) {
            if (sort()) event.cancel();
        }
    }

    @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        if (event.action != KeyAction.Press) return;

        if (sortingKey.get().matches(false, event.button)) {
            if (sort()) event.cancel();
        }
    }

    private boolean sort() {
        if (!sortingEnabled.get() || !(mc.currentScreen instanceof HandledScreen<?> screen) || sorter != null)
            return false;

        if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
            FindItemResult empty = InvUtils.findEmpty();
            if (!empty.found()) InvUtils.click().slot(-999);
            else InvUtils.click().slot(empty.slot());
        }

        Slot focusedSlot = ((HandledScreenAccessor) screen).getFocusedSlot();
        if (focusedSlot == null) return false;

        sorter = new InventorySorter(screen, focusedSlot);
        return true;
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        sorter = null;
    }

    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        if (sorter != null && sorter.tick(sortingDelay.get())) sorter = null;
    }

    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        // Auto Drop
        if (mc.currentScreen instanceof HandledScreen<?> || autoDropItems.get().isEmpty()) return;

        for (int i = autoDropExcludeHotbar.get() ? 9 : 0; i < mc.player.getInventory().size(); i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);

            if (autoDropItems.get().contains(itemStack.getItem())) {
                if ((!autoDropOnlyFullStacks.get() || itemStack.getCount() == itemStack.getMaxCount()) &&
                    !(autoDropExcludeEquipped.get() && SlotUtils.isArmor(i))) InvUtils.drop().slot(i);
            }
        }
    }

    @EventHandler
    private void onDropItems(DropItemsEvent event) {
        if (antiDropItems.get().contains(event.itemStack.getItem())) event.cancel();
    }

    // XCarry

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!xCarry.get() || !(event.packet instanceof CloseHandledScreenC2SPacket)) return;

        if (((CloseHandledScreenC2SPacketAccessor) event.packet).getSyncId() == mc.player.playerScreenHandler.syncId) {
            invOpened = true;
            event.cancel();
        }
    }

    @EventHandler
    private void onInventory(InventoryEvent event) {
        ScreenHandler handler = mc.player.currentScreenHandler;
        if (event.packet.getSyncId() != handler.syncId) return;

        for (Button button : buttons) {
            if (button.canAutoExecute(handler)) {
                button.execute(handler);
            }
        }
    }

    public boolean mouseDragItemMove() {
        return isActive() && mouseDragItemMove.get();
    }

    public boolean armorStorage() {
        return isActive() && armorStorage.get();
    }

    public abstract class Button {
        public final String buttonName;

        final SettingGroup sg;

        final Setting<TriggerMode> triggerMode;
        final Setting<List<ScreenHandlerType<?>>> containersList;
        final Setting<FilterMode> itemsFilter;
        final Setting<List<Item>> items;
        final Setting<Integer> delay;
        final Setting<Integer> initDelay;
        final Setting<Integer> randomDelay;


        protected Button(String buttonName, String category) {
            this.buttonName = buttonName;
            this.sg = settings.createGroup(category);

            triggerMode = sg.add(new EnumSetting.Builder<TriggerMode>()
                .name("trigger")
                .description("How this action will be triggered.")
                .defaultValue(TriggerMode.Button)
                .onChanged(this::checkButtons)
                .build()
            );

            containersList = sg.add(new ScreenHandlerListSetting.Builder()
                .name("containers-list")
                .description("Containers list where this action can be triggered in.")
                .defaultValue(ScreenHandlerType.GENERIC_9X3, ScreenHandlerType.GENERIC_9X6)
                .visible(() -> triggerMode.get() != TriggerMode.Disabled)
                .build()
            );

            itemsFilter = sg.add(new EnumSetting.Builder<FilterMode>()
                .name("items-filter")
                .description("The method for filtering items.")
                .defaultValue(FilterMode.Whitelist)
                .visible(() -> triggerMode.get() != TriggerMode.Disabled)
                .build()
            );

            items = sg.add(new ItemListSetting.Builder()
                .name("items")
                .description("Items list.")
                .visible(() -> triggerMode.get() != TriggerMode.Disabled && !itemsFilter.get().isWildCard())
                .build()
            );

            delay = sg.add(new IntSetting.Builder()
                .name("delay")
                .description("The minimum delay between moving on to next stack.")
                .defaultValue(20)
                .min(0)
                .sliderMax(1000)
                .visible(() -> triggerMode.get() != TriggerMode.Disabled)
                .build()
            );

            initDelay = sg.add(new IntSetting.Builder()
                .name("initial-delay")
                .description("The initial delay before starting in milliseconds. Use 0 to use normal delay instead.")
                .defaultValue(50)
                .min(0)
                .sliderMax(1000)
                .visible(() -> triggerMode.get() != TriggerMode.Disabled)
                .build()
            );

            randomDelay = sg.add(new IntSetting.Builder()
                .name("random-delay")
                .description("Randomly adds delay of up to the specified time in milliseconds.")
                .defaultValue(50)
                .min(0)
                .sliderMax(1000)
                .visible(() -> triggerMode.get() != TriggerMode.Disabled)
                .build()
            );
        }

        public abstract void execute(ScreenHandler handler);

        public boolean showButton(ScreenHandler handler) {
            try {
                return triggerMode.get() == TriggerMode.Button && containersList.get().contains(handler.getType());
            } catch (UnsupportedOperationException e) {
                return false;
            }
        }

        int getSleepTime() {
            return delay.get() + (randomDelay.get() > 0 ? ThreadLocalRandom.current().nextInt(0, randomDelay.get()) : 0);
        }

        boolean canAutoExecute(ScreenHandler handler) {
            try {
                return triggerMode.get() == TriggerMode.Auto && containersList.get().contains(handler.getType());
            } catch (UnsupportedOperationException e) {
                return false;
            }
        }

        private void checkButtons(TriggerMode newValue) {
            for (Button button : buttons) {
                if (button != this && (button.triggerMode.get() == TriggerMode.Auto && newValue == TriggerMode.Auto)) {
                    error("You can't enable Auto-Trigger mode for multiple buttons at same time.");
                    button.triggerMode.set(TriggerMode.Disabled);
                }
            }
        }

        public enum TriggerMode {
            Auto,
            Button,
            Disabled
        }
    }

    private class Delete extends Button {
        private final Setting<Boolean> excludeSelf;

        protected Delete() {
            super("Delete","Item Deleter");

            excludeSelf = sg.add(new BoolSetting.Builder()
                .name("exclude-self")
                .description("Whether or not to exclude deleting your own Inventory's items.")
                .defaultValue(true)
                .visible(() -> triggerMode.get() != TriggerMode.Disabled)
                .build()
            );
        }

        @Override
        public void execute(ScreenHandler handler) {
            MeteorExecutor.execute(() -> {
                boolean initial = initDelay.get() != 0;
                int playerInvOffset = SlotUtils.indexToId(SlotUtils.MAIN_START);
                int playerInvEnd = excludeSelf.get() ? playerInvOffset : playerInvOffset + 4 * 9;

                for (int i = 0; i < playerInvEnd; i++) {
                    if (!handler.getSlot(i).hasStack()) continue;

                    int sleep;
                    if (initial) {
                        sleep = initDelay.get();
                        initial = false;
                    } else sleep = getSleepTime();
                    if (sleep > 0) {
                        try {
                            Thread.sleep(sleep);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    if (mc.currentScreen == null || !Utils.canUpdate()) break;

                    Item item = handler.getSlot(i).getStack().getItem();
                    if (!itemsFilter.get().test(items.get() , item)) continue;

                    mc.interactionManager.clickSlot(handler.syncId, i, 100, SlotActionType.SWAP, mc.player);
                }
            });
        }
    }

    private class Steal extends Button {
        private final Setting<Boolean> drop;
        private final Setting<Boolean> dropBackwards;

        protected Steal() {
            super("Steal","Item Stealer");

            drop = sg.add(new BoolSetting.Builder()
                .name("drop-items")
                .description("Drop items to the ground instead of stealing them.")
                .defaultValue(false)
                .visible(() -> triggerMode.get() != TriggerMode.Disabled)
                .build()
            );

            dropBackwards = sg.add(new BoolSetting.Builder()
                .name("drop-backwards")
                .description("Drop items behind you.")
                .defaultValue(false)
                .visible(() -> triggerMode.get() != TriggerMode.Disabled && drop.get())
                .build()
            );
        }

        @Override
        public void execute(ScreenHandler handler) {
            MeteorExecutor.execute(() -> {
                boolean initial = initDelay.get() != 0;
                for (int i = 0; i < SlotUtils.indexToId(SlotUtils.MAIN_START); i++) {
                    if (!handler.getSlot(i).hasStack()) continue;

                    int sleep;
                    if (initial) {
                        sleep = initDelay.get();
                        initial = false;
                    } else sleep = getSleepTime();
                    if (sleep > 0) {
                        try {
                            Thread.sleep(sleep);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    if (mc.currentScreen == null || !Utils.canUpdate()) break;

                    Item item = handler.getSlot(i).getStack().getItem();
                    if (!itemsFilter.get().test(items.get(), item)) continue;

                    if (drop.get()) {
                        if (dropBackwards.get()) {
                            int iCopy = i;
                            Rotations.rotate(mc.player.getYaw() - 180, mc.player.getPitch(), () -> InvUtils.drop().slotId(iCopy));
                        } else {
                            InvUtils.drop().slotId(i);
                        }
                    } else InvUtils.quickMove().slotId(i);
                }
            });
        }
    }

    private class Dump extends Button {
        private final Setting<Boolean> excludeHotbar;

        protected Dump() {
            super("Dump","Item Dumper");

            excludeHotbar = sg.add(new BoolSetting.Builder()
                .name("exclude-hotbar")
                .description("Exclude items in hotbar from dumping into container.")
                .defaultValue(false)
                .visible(() -> triggerMode.get() != TriggerMode.Disabled)
                .build()
            );
        }

        @Override
        public void execute(ScreenHandler handler) {
            MeteorExecutor.execute(() -> {
                boolean initial = initDelay.get() != 0;

                int playerInvOffset = SlotUtils.indexToId(SlotUtils.MAIN_START);
                int playerInvEnd = excludeHotbar.get() ? (playerInvOffset + 3 * 9) : (playerInvOffset + 4 * 9);

                for (int i = playerInvOffset; i < playerInvEnd; i++) {
                    if (!handler.getSlot(i).hasStack()) continue;

                    int sleep;
                    if (initial) {
                        sleep = initDelay.get();
                        initial = false;
                    } else sleep = getSleepTime();
                    if (sleep > 0) {
                        try {
                            Thread.sleep(sleep);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    if (mc.currentScreen == null || !Utils.canUpdate()) break;

                    Item item = handler.getSlot(i).getStack().getItem();
                    if (!itemsFilter.get().test(items.get(), item)) continue;

                    InvUtils.quickMove().slotId(i);
                }
            });
        }
    }
}
