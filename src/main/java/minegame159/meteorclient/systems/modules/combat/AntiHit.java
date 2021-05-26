/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.combat;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import minegame159.meteorclient.events.entity.player.AttackEntityEvent;
import minegame159.meteorclient.events.entity.player.StartBreakingBlockEvent;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.friends.Friends;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

public class AntiHit extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Entities to avoid attacking.")
            .defaultValue(new Object2BooleanOpenHashMap<>(0))
            .onlyAttackable()
            .build()
    );

    private final Setting<Boolean> friends = sgGeneral.add(new BoolSetting.Builder()
            .name("friends")
            .description("Doesn't allow friends to be attacked.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> babies = sgGeneral.add(new BoolSetting.Builder()
            .name("babies")
            .description("Doesn't allow babies to be attacked.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> nametagged = sgGeneral.add(new BoolSetting.Builder()
            .name("nametagged")
            .description("Doesn't allow nametagged enities to be attacked.")
            .defaultValue(false)
            .build()
    );
    
    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
            .name("blocks")
            .description("Blocks to avoid hitting.")
            .defaultValue(new ArrayList<>())
            .build()
    );

    public AntiHit() {
        super(Categories.Combat, "AntiHit", "Prevents you from attacking certain entities.");
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onAttackEntity(AttackEntityEvent event) {
        // Friends
        if (friends.get() && event.entity instanceof PlayerEntity && !Friends.get().shouldAttack((PlayerEntity) event.entity)) event.cancel();

        // Babies
        if (babies.get() && event.entity instanceof AnimalEntity && ((AnimalEntity) event.entity).isBaby()) event.cancel();

        // NameTagged
        if (nametagged.get() && event.entity.hasCustomName()) event.cancel();

        // Entities
        if (entities.get().getBoolean(event.entity.getType())) event.cancel();
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    private void onStartBreakingBlockEvent(StartBreakingBlockEvent event) {
        if (blocks.get().contains(mc.world.getBlockState(event.blockPos).getBlock())) event.cancel();
    }
}
