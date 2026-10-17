package com.yfy.createcinema.packet;

import com.yfy.createcinema.CreateCinema;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = CreateCinema.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModPackets {
    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        ModNetwork.register();
    }
}
