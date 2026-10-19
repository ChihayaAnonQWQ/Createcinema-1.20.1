package com.yfy.createcinema.client.config;

import com.yfy.createcinema.ClientConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Optional in-game config screen built with Cloth Config. Only loaded when Cloth Config is installed; the same
 * options are always editable in {@code config/createcinema-client.toml}.
 */
public final class CinemaConfigScreen {
    private CinemaConfigScreen() {
    }

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.createcinema.title"));
        ConfigEntryBuilder entries = builder.entryBuilder();

        ConfigCategory playback = builder.getOrCreateCategory(
                Component.translatable("config.createcinema.category.playback"));
        playback.addEntry(entries
                .startIntSlider(Component.translatable("config.createcinema.decodeThreads"),
                        ClientConfig.projectorDecodeThreads(), 0, 16)
                .setDefaultValue(1)
                .setTextGetter(value -> value <= 0
                        ? Component.translatable("config.createcinema.decodeThreads.auto")
                        : Component.translatable("config.createcinema.decodeThreads.value", value))
                .setTooltip(Component.translatable("config.createcinema.decodeThreads.tooltip"))
                .setSaveConsumer(ClientConfig::setProjectorDecodeThreads)
                .build());
        playback.addEntry(entries
                .startBooleanToggle(Component.translatable("config.createcinema.projectorHardwareDecoding"),
                        ClientConfig.projectorHardwareDecoding())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.createcinema.projectorHardwareDecoding.tooltip"))
                .setSaveConsumer(ClientConfig::setProjectorHardwareDecoding)
                .build());

        ConfigCategory burning = builder.getOrCreateCategory(
                Component.translatable("config.createcinema.category.burning"));
        burning.addEntry(entries
                .startBooleanToggle(Component.translatable("config.createcinema.burnHardwareDecoding"),
                        ClientConfig.burnHardwareDecoding())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.createcinema.burnHardwareDecoding.tooltip"))
                .setSaveConsumer(ClientConfig::setBurnHardwareDecoding)
                .build());
        burning.addEntry(entries
                .startBooleanToggle(Component.translatable("config.createcinema.burnHardwareEncoding"),
                        ClientConfig.burnHardwareEncoding())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.createcinema.burnHardwareEncoding.tooltip"))
                .setSaveConsumer(ClientConfig::setBurnHardwareEncoding)
                .build());
        burning.addEntry(entries
                .startBooleanToggle(Component.translatable("config.createcinema.burnCacheEnabled"),
                        ClientConfig.burnCacheEnabled())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.createcinema.burnCacheEnabled.tooltip"))
                .setSaveConsumer(ClientConfig::setBurnCacheEnabled)
                .build());

        // Every entry persists through ConfigValue#save() in the ClientConfig setters above.
        return builder.build();
    }
}
