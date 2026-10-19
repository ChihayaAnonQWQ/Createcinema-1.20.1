package com.yfy.createcinema;

import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.yfy.createcinema.client.connected.CableCasingCTRegistration;
import com.yfy.createcinema.client.ponder.CreateCinemaPonderPlugin;
import com.yfy.createcinema.datagen.ModItemModelProvider;
import com.yfy.createcinema.client.browser.PlatformInfo;
import com.yfy.createcinema.display.CinemaDisplaySources;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(CreateCinema.MODID)
public class CreateCinema {
    public static final String MODID = "createcinema";
    public static final Logger LOGGER = LoggerFactory.getLogger(CreateCinema.class);
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MODID);

    public CreateCinema() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        LOGGER.info("Create Cinema initializing (Forge 1.20.1 port)");
        PlatformInfo.ensureJavacppPlatform();
        if (PlatformInfo.isAndroid()) {
            LOGGER.info("CreateCinema Android compatibility patch active; JavaCPP platform {}, pathsFirst={}",
                    System.getProperty("org.bytedeco.javacpp.platform"),
                    System.getProperty("org.bytedeco.javacpp.pathsFirst"));
        }
        ModRegistry.BLOCKS.register(modEventBus);
        ModRegistry.ITEMS.register(modEventBus);
        ModRegistry.BLOCK_ENTITIES.register(modEventBus);
        ModRegistry.MENU_TYPES.register(modEventBus);
        ModRegistry.CREATIVE_TABS.register(modEventBus);
        REGISTRATE.registerEventListeners(modEventBus);
        CinemaDisplaySources.register();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ModItemModelProvider::gatherData);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            CreateCinemaPonderPlugin.register();
            CableCasingCTRegistration.register();
            registerConfigScreen();
        }
    }

    /** Offers an in-game config screen through Forge when Cloth Config is installed; otherwise the TOML is used. */
    private void registerConfigScreen() {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("cloth_config")) return;
        net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(
                net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> com.yfy.createcinema.client.config.CinemaConfigScreen.create(parent)));
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BlockStressValues.IMPACTS.register(ModRegistry.PROJECTOR.get(), () -> 4.0);
            BlockStressValues.IMPACTS.register(ModRegistry.NETWORK_PROJECTOR.get(), () -> 4.0);
        });
    }
}
