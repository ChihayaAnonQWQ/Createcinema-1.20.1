package com.yfy.createcinema.packet;

import com.yfy.createcinema.blockentity.NetworkProjectorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SNetworkProjectorDouyinContentPacket(BlockPos projectorPos, String sourceUrl,
                                                     int navigationRevision, String contentId,
                                                     int playlistIndex, int playlistCount,
                                                     double contentStartTime) {

    public static void encode(C2SNetworkProjectorDouyinContentPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.projectorPos);
        buf.writeUtf(packet.sourceUrl, NetworkProjectorBlockEntity.MAX_URL_LENGTH);
        buf.writeVarInt(packet.navigationRevision);
        buf.writeUtf(packet.contentId, NetworkProjectorBlockEntity.MAX_DOUYIN_CONTENT_ID_LENGTH);
        buf.writeVarInt(packet.playlistIndex);
        buf.writeVarInt(packet.playlistCount);
        buf.writeDouble(packet.contentStartTime);
    }

    public static C2SNetworkProjectorDouyinContentPacket decode(FriendlyByteBuf buf) {
        return new C2SNetworkProjectorDouyinContentPacket(buf.readBlockPos(),
                buf.readUtf(NetworkProjectorBlockEntity.MAX_URL_LENGTH), buf.readVarInt(),
                buf.readUtf(NetworkProjectorBlockEntity.MAX_DOUYIN_CONTENT_ID_LENGTH),
                buf.readVarInt(), buf.readVarInt(), buf.readDouble());
    }

    public void send() {
        ModNetwork.sendToServer(this);
    }

    public static void handle(C2SNetworkProjectorDouyinContentPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null
                    || !(player.level().getBlockEntity(packet.projectorPos) instanceof NetworkProjectorBlockEntity projector)) {
                return;
            }
            projector.updateDouyinContent(player.getUUID(), packet.sourceUrl, packet.navigationRevision,
                    packet.contentId, packet.playlistIndex, packet.playlistCount, packet.contentStartTime);
        });
        ctx.get().setPacketHandled(true);
    }
}
