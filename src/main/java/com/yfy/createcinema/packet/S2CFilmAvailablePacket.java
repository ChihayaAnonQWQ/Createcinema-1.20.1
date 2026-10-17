package com.yfy.createcinema.packet;

import com.yfy.createcinema.client.network.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CFilmAvailablePacket(String filmId) {

    public static void encode(S2CFilmAvailablePacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.filmId);
    }

    public static S2CFilmAvailablePacket decode(FriendlyByteBuf buf) {
        return new S2CFilmAvailablePacket(buf.readUtf());
    }

    public void sendTo(ServerPlayer player) {
        ModNetwork.sendToPlayer(player, this);
    }

    public static void handle(S2CFilmAvailablePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientPacketHandlers.handleFilmAvailable(packet));
        ctx.get().setPacketHandled(true);
    }
}
