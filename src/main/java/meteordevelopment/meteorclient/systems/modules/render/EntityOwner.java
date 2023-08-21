/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.mixin.ProjectileEntityAccessor;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EntityOwner extends Module {
    private static final Color BACKGROUND = new Color(0, 0, 0, 75);
    private static final Color TEXT = new Color(255, 255, 255);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> scale;

    private final Setting<Boolean> projectiles;

    private final Vector3d pos = new Vector3d();
    private final Map<UUID, String> uuidToName = new HashMap<>();

    public EntityOwner() {
        super(Categories.Render, "entity-owner", "Displays the name of the player who owns the entity you're looking at.");

        scale = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale")
            .description("The scale of the text.")
            .defaultValue(1)
            .min(0)
            .build()
        );

        projectiles = sgGeneral.add(new BoolSetting.Builder()
            .name("projectiles")
            .description("Display owner names of projectiles.")
            .defaultValue(false)
            .build()
        );
    }

    @Override
    public void onDeactivate() {
        uuidToName.clear();
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        // iterate the list of entities in the world
        assert mc.world != null;
        for (Entity entity : mc.world.getEntities()) {
            UUID ownerUuid;

            // checks if the Entity is tameable
            if (entity instanceof TameableEntity tameable) {
                // Sets the owner UUID to the tameable entities specified UUID
                ownerUuid = tameable.getOwnerUuid();
            } else {
                // checks if the Entity is a horse
                if (entity instanceof AbstractHorseEntity horse) {
                    // Sets the owner UUID to the horses owner UUID
                    ownerUuid = horse.getOwnerUuid();
                } else {
                    // checks if the Entity is a projectile.
                    if (entity instanceof ProjectileEntity && projectiles.get()) {
                        // Sets the owner UUID to the projectiles owner UUID
                        ownerUuid = ((ProjectileEntityAccessor) entity).getOwnerUuid();
                    } else {
                        continue;
                    }
                }
            }

            // checks if the owner UUID does not equal null
            if (ownerUuid != null) {
                Utils.set(pos, entity, event.tickDelta);
                pos.add(0, entity.getEyeHeight(entity.getPose()) + 0.75, 0);

                if (NametagUtils.to2D(pos, scale.get())) {
                    renderNametag(getOwnerName(ownerUuid));
                }
            }
        }
    }

    private void renderNametag(String name) {
        TextRenderer text = TextRenderer.get();

        NametagUtils.begin(pos);
        text.beginBig();

        // initialize the position / text variables
        double w = text.getWidth(name);
        double x = -w / 2;
        double y = -text.getHeight();

        // render the items
        Renderer2D.COLOR.begin();
        Renderer2D.COLOR.quad(x - 1, y - 1, w + 2, text.getHeight() + 2, BACKGROUND);
        Renderer2D.COLOR.render(null);

        text.render(name, x, y, TEXT);

        text.end();
        NametagUtils.end();
    }

    private String getOwnerName(UUID uuid) {
        // Check if the player is online
        assert mc.world != null;
        PlayerEntity player = mc.world.getPlayerByUuid(uuid);

        // Check if the player is null
        if (player == null) {
            // Check cache
            String name = uuidToName.get(uuid);

            if (name == null) {

                // Makes a HTTP request to Mojang API
                MeteorExecutor.execute(() -> {
                    if (isActive()) {
                        ProfileResponse res = Http.get("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replace("-", "")).sendJson(ProfileResponse.class);

                        if (isActive()) {
                            if (res == null) uuidToName.put(uuid, "Failed to get name");
                            else uuidToName.put(uuid, res.name);
                        }
                    }
                });

                name = "Retrieving";
                uuidToName.put(uuid, name);
                return name;
            } else {
                return name;
            }
        } else {
            return player.getEntityName();
        }

    }

    private static class ProfileResponse {
        public String name;
    }
}
