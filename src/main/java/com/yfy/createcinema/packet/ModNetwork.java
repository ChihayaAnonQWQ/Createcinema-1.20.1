package com.yfy.createcinema.packet;

import com.yfy.createcinema.CreateCinema;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Forge 1.20.1 networking entry point for Create Cinema.
 * <p>
 * The channel is registered from {@link ModPackets} during {@code FMLCommonSetupEvent}; every payload id is unique
 * across the whole channel (Forge keeps one discriminator table per channel, regardless of direction).
 */
public final class ModNetwork {
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CreateCinema.MODID, "main"), () -> "1", "1"::equals, "1"::equals);

    private ModNetwork() {
    }

    /**
     * Registers all 12 payloads. Called from common setup, so ids must be identical on both sides.
     */
    public static void register() {
        int id = 0;

        CHANNEL.messageBuilder(C2SBurnStatePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SBurnStatePacket::encode)
                .decoder(C2SBurnStatePacket::decode)
                .consumerMainThread(C2SBurnStatePacket::handle)
                .add();

        CHANNEL.messageBuilder(C2SUploadFilmChunkPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SUploadFilmChunkPacket::encode)
                .decoder(C2SUploadFilmChunkPacket::decode)
                .consumerMainThread(C2SUploadFilmChunkPacket::handle)
                .add();

        CHANNEL.messageBuilder(C2SRequestFilmPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SRequestFilmPacket::encode)
                .decoder(C2SRequestFilmPacket::decode)
                .consumerMainThread(C2SRequestFilmPacket::handle)
                .add();

        CHANNEL.messageBuilder(C2SSetNetworkProjectorUrlPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SSetNetworkProjectorUrlPacket::encode)
                .decoder(C2SSetNetworkProjectorUrlPacket::decode)
                .consumerMainThread(C2SSetNetworkProjectorUrlPacket::handle)
                .add();

        CHANNEL.messageBuilder(C2SSetNetworkProjectorQualityPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SSetNetworkProjectorQualityPacket::encode)
                .decoder(C2SSetNetworkProjectorQualityPacket::decode)
                .consumerMainThread(C2SSetNetworkProjectorQualityPacket::handle)
                .add();

        CHANNEL.messageBuilder(C2SNetworkProjectorMediaInfoPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SNetworkProjectorMediaInfoPacket::encode)
                .decoder(C2SNetworkProjectorMediaInfoPacket::decode)
                .consumerMainThread(C2SNetworkProjectorMediaInfoPacket::handle)
                .add();

        CHANNEL.messageBuilder(C2SNetworkProjectorPlaybackHealthPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SNetworkProjectorPlaybackHealthPacket::encode)
                .decoder(C2SNetworkProjectorPlaybackHealthPacket::decode)
                .consumerMainThread(C2SNetworkProjectorPlaybackHealthPacket::handle)
                .add();

        CHANNEL.messageBuilder(C2SNetworkProjectorDouyinContentPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SNetworkProjectorDouyinContentPacket::encode)
                .decoder(C2SNetworkProjectorDouyinContentPacket::decode)
                .consumerMainThread(C2SNetworkProjectorDouyinContentPacket::handle)
                .add();

        CHANNEL.messageBuilder(S2CBurnStatusPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CBurnStatusPacket::encode)
                .decoder(S2CBurnStatusPacket::decode)
                .consumerMainThread(S2CBurnStatusPacket::handle)
                .add();

        CHANNEL.messageBuilder(S2CDownloadFilmChunkPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CDownloadFilmChunkPacket::encode)
                .decoder(S2CDownloadFilmChunkPacket::decode)
                .consumerMainThread(S2CDownloadFilmChunkPacket::handle)
                .add();

        CHANNEL.messageBuilder(S2CFilmDeletedPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CFilmDeletedPacket::encode)
                .decoder(S2CFilmDeletedPacket::decode)
                .consumerMainThread(S2CFilmDeletedPacket::handle)
                .add();

        CHANNEL.messageBuilder(S2CFilmAvailablePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CFilmAvailablePacket::encode)
                .decoder(S2CFilmAvailablePacket::decode)
                .consumerMainThread(S2CFilmAvailablePacket::handle)
                .add();
    }

    public static void sendToServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }

    public static void sendToPlayer(ServerPlayer player, Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }
}
