package com.yfy.createcinema.packet;

import com.yfy.createcinema.client.network.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CDownloadFilmChunkPacket(String filmId, int index, int total, byte[] data) {
    private static final int MAX_CHUNK_BYTES = 900_000;

    public static void encode(S2CDownloadFilmChunkPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.filmId);
        buf.writeVarInt(packet.index);
        buf.writeVarInt(packet.total);
        buf.writeByteArray(packet.data);
    }

    public static S2CDownloadFilmChunkPacket decode(FriendlyByteBuf buf) {
        return new S2CDownloadFilmChunkPacket(buf.readUtf(), buf.readVarInt(), buf.readVarInt(),
                buf.readByteArray(MAX_CHUNK_BYTES));
    }

    public void sendTo(ServerPlayer player) {
        ModNetwork.sendToPlayer(player, this);
    }

    public static void handle(S2CDownloadFilmChunkPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientPacketHandlers.handleFilmChunk(packet));
        ctx.get().setPacketHandled(true);
    }
}
