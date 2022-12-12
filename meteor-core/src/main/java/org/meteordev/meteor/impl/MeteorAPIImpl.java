/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package org.meteordev.meteor.impl;

import meteordevelopment.orbit.EventBus;
import meteordevelopment.orbit.IEventBus;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import org.meteordev.meteor.api.MeteorAPI;
import org.meteordev.meteor.api.addons.AddonManager;
import org.meteordev.meteor.api.commands.CommandManager;
import org.meteordev.meteor.impl.addons.AddonManagerImpl;
import org.meteordev.meteor.impl.commands.CommandManagerImpl;

public class MeteorAPIImpl implements MeteorAPI {
    public static final MeteorAPIImpl INSTANCE = new MeteorAPIImpl();

    private ModMetadata metadata;

    private final IEventBus eventBus = new EventBus();

    private MeteorAPIImpl() {}

    @Override
    public String getVersion() {
        checkMetadata();
        return metadata.getVersion().toString();
    }

    @Override
    public AddonManager getAddons() {
        return AddonManagerImpl.INSTANCE;
    }

    @Override
    public IEventBus getEventBus() {
        return eventBus;
    }

    @Override
    public CommandManager getCommands() {
        return CommandManagerImpl.INSTANCE;
    }

    private void checkMetadata() {
        if (metadata == null) {
            ModContainer container = FabricLoader.getInstance().getModContainer("meteor-api")
                .orElseThrow(() -> new IllegalStateException("Mod with id 'meteor-api' is not loaded."));

            metadata = container.getMetadata();
        }
    }
}
