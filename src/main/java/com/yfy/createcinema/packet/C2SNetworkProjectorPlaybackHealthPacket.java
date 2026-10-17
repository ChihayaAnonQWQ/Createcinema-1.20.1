package com.yfy.createcinema.packet;

import com.yfy.createcinema.blockentity.NetworkProjectorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SNetworkProjectorPlaybackHealthPacket(BlockPos projectorPos, String url, int navigationRevision,
                                                      NetworkProjectorBlockEntity.PlaybackHealth health) {

    public static void encode(C2SNetworkProjectorPlaybackHealthPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.projectorPos);
        buf.writeUtf(packet.url, NetworkProjectorBlockEntity.MAX_URL_LENGTH);
        buf.writeVarInt(packet.navigationRevision);
        buf.writeEnum(packet.health);
    }

    public static C2SNetworkProjectorPlaybackHealthPacket decode(FriendlyByteBuf buf) {
        return new C2SNetworkProjectorPlaybackHealthPacket(buf.readBlockPos(),
                buf.readUtf(NetworkProjectorBlockEntity.MAX_URL_LENGTH), buf.readVarInt(),
                buf.readEnum(NetworkProjectorBlockEntity.PlaybackHealth.class));
    }

    public void send() {
        ModNetwork.sendToServer(this);
    }

    public static void handle(C2SNetworkProjectorPlaybackHealthPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null
                    || player.distanceToSqr(Vec3.atCenterOf(packet.projectorPos)) > 128.0 * 128.0
                    || !player.level().hasChunkAt(packet.projectorPos)
                    || !(player.level().getBlockEntity(packet.projectorPos) instanceof NetworkProjectorBlockEntity projector)) {
                return;
            }
            projector.reportPlaybackHealth(player, packet.url, packet.navigationRevision, packet.health);
        });
        ctx.get().setPacketHandled(true);
    }
}
