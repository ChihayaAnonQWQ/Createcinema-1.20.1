package com.yfy.createcinema.packet;

import com.yfy.createcinema.client.network.ClientPacketHandlers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CBurnStatusPacket(BlockPos burnerPos, String message, float progress, boolean active) {

    public static void encode(S2CBurnStatusPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.burnerPos);
        buf.writeUtf(packet.message);
        buf.writeFloat(packet.progress);
        buf.writeBoolean(packet.active);
    }

    public static S2CBurnStatusPacket decode(FriendlyByteBuf buf) {
        return new S2CBurnStatusPacket(buf.readBlockPos(), buf.readUtf(), buf.readFloat(), buf.readBoolean());
    }

    public void sendTo(ServerPlayer player) {
        ModNetwork.sendToPlayer(player, this);
    }

    public static void handle(S2CBurnStatusPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientPacketHandlers.handleBurnStatus(packet));
        ctx.get().setPacketHandled(true);
    }
}
