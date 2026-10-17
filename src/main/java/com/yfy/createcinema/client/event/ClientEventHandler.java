package com.yfy.createcinema.client.event;

import com.yfy.createcinema.client.render.PartialProjectorRenderer;
import com.yfy.createcinema.client.render.NetworkProjectorRenderer;
import com.yfy.createcinema.client.douyin.DouyinBrowserBridge;
import com.yfy.createcinema.client.render.ClientVideoBurner;
import com.yfy.createcinema.client.browser.BrowserProvider;
import com.yfy.createcinema.CreateCinema;
import com.yfy.createcinema.ModRegistry;
import com.yfy.createcinema.gui.BurnerScreen;
import com.yfy.createcinema.gui.NetworkProjectorScreen;
import com.yfy.createcinema.gui.ProjectorScreen;
import com.yfy.createcinema.gui.RemoteControlUpgradeScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;

@Mod.EventBusSubscriber(modid = CreateCinema.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientEventHandler {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            com.yfy.createcinema.ClientConfig.clearLegacyDouyinCookie();
            com.yfy.createcinema.ClientConfig.setBrowserShutdown(provider ->
                    java.util.concurrent.CompletableFuture.runAsync(() ->
                            DouyinBrowserBridge.disable(BrowserProvider.byId(provider))));
            ClientVideoBurner.preloadFfmpeg();
            registerScreens();
        });
    }

    /**
     * Forge 1.20.1 has no menu-screen registration event: screens are bound to their menu types through
     * {@link MenuScreens#register} during client setup.
     */
    private static void registerScreens() {
        MenuScreens.register(ModRegistry.BURNER_MENU.get(), BurnerScreen::new);
        MenuScreens.register(ModRegistry.PROJECTOR_MENU.get(), ProjectorScreen::new);
        MenuScreens.register(ModRegistry.NETWORK_PROJECTOR_MENU.get(), NetworkProjectorScreen::new);
        MenuScreens.register(ModRegistry.REMOTE_CONTROL_UPGRADE_MENU.get(), RemoteControlUpgradeScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModRegistry.PROJECTOR_BE.get(), PartialProjectorRenderer::new);
        event.registerBlockEntityRenderer(ModRegistry.NETWORK_PROJECTOR_BE.get(), NetworkProjectorRenderer::new);
    }
}
