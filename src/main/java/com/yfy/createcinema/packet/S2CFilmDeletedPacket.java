package com.yfy.createcinema.packet;

import com.yfy.createcinema.client.network.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CFilmDeletedPacket(String filmId) {

    public static void encode(S2CFilmDeletedPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.filmId);
    }

    public static S2CFilmDeletedPacket decode(FriendlyByteBuf buf) {
        return new S2CFilmDeletedPacket(buf.readUtf());
    }

    public void sendTo(ServerPlayer player) {
        ModNetwork.sendToPlayer(player, this);
    }

    public static void handle(S2CFilmDeletedPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientPacketHandlers.handleFilmDeleted(packet));
        ctx.get().setPacketHandled(true);
    }
}
