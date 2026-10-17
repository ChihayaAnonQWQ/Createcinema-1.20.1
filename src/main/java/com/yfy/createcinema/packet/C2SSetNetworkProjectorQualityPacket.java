package com.yfy.createcinema.packet;

import com.yfy.createcinema.NetworkVideoQuality;
import com.yfy.createcinema.blockentity.NetworkProjectorBlockEntity;
import com.yfy.createcinema.gui.NetworkProjectorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SSetNetworkProjectorQualityPacket(BlockPos projectorPos, int qualityId) {

    public static void encode(C2SSetNetworkProjectorQualityPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.projectorPos);
        buf.writeVarInt(packet.qualityId);
    }

    public static C2SSetNetworkProjectorQualityPacket decode(FriendlyByteBuf buf) {
        return new C2SSetNetworkProjectorQualityPacket(buf.readBlockPos(), buf.readVarInt());
    }

    public void send() {
        ModNetwork.sendToServer(this);
    }

    public static void handle(C2SSetNetworkProjectorQualityPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null
                    || !(player.containerMenu instanceof NetworkProjectorMenu menu)
                    || !menu.pos.equals(packet.projectorPos) || !menu.stillValid(player)
                    || !(player.level().getBlockEntity(packet.projectorPos) instanceof NetworkProjectorBlockEntity projector)) return;
            projector.setQuality(NetworkVideoQuality.byId(packet.qualityId));
        });
        ctx.get().setPacketHandled(true);
    }
}
