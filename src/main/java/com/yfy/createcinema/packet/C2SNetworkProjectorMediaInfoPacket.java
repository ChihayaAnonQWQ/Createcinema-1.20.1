package com.yfy.createcinema.packet;

import com.yfy.createcinema.blockentity.NetworkProjectorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SNetworkProjectorMediaInfoPacket(BlockPos projectorPos, String url, int mediaRevision,
                                                double durationSeconds, double timeSeconds, boolean live,
                                                NetworkProjectorBlockEntity.MediaStatus status) {

    public static void encode(C2SNetworkProjectorMediaInfoPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.projectorPos);
        buf.writeUtf(packet.url, NetworkProjectorBlockEntity.MAX_URL_LENGTH);
        buf.writeVarInt(packet.mediaRevision);
        buf.writeDouble(packet.durationSeconds);
        buf.writeDouble(packet.timeSeconds);
        buf.writeBoolean(packet.live);
        buf.writeEnum(packet.status);
    }

    public static C2SNetworkProjectorMediaInfoPacket decode(FriendlyByteBuf buf) {
        return new C2SNetworkProjectorMediaInfoPacket(buf.readBlockPos(),
                buf.readUtf(NetworkProjectorBlockEntity.MAX_URL_LENGTH), buf.readVarInt(), buf.readDouble(),
                buf.readDouble(), buf.readBoolean(), buf.readEnum(NetworkProjectorBlockEntity.MediaStatus.class));
    }

    public void send() {
        ModNetwork.sendToServer(this);
    }

    public static void handle(C2SNetworkProjectorMediaInfoPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null
                    || !(player.level().getBlockEntity(packet.projectorPos) instanceof NetworkProjectorBlockEntity projector)) {
                return;
            }
            projector.updateMediaInfo(player.getUUID(), packet.url, packet.mediaRevision, packet.durationSeconds,
                    packet.timeSeconds, packet.live, packet.status);
        });
        ctx.get().setPacketHandled(true);
    }
}
