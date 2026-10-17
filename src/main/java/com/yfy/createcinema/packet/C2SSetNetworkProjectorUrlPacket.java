package com.yfy.createcinema.packet;

import com.yfy.createcinema.blockentity.NetworkProjectorBlockEntity;
import com.yfy.createcinema.gui.NetworkProjectorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.net.URI;
import java.util.function.Supplier;

public record C2SSetNetworkProjectorUrlPacket(BlockPos projectorPos, String url) {

    public static void encode(C2SSetNetworkProjectorUrlPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.projectorPos);
        buf.writeUtf(packet.url, NetworkProjectorBlockEntity.MAX_URL_LENGTH);
    }

    public static C2SSetNetworkProjectorUrlPacket decode(FriendlyByteBuf buf) {
        return new C2SSetNetworkProjectorUrlPacket(buf.readBlockPos(),
                buf.readUtf(NetworkProjectorBlockEntity.MAX_URL_LENGTH));
    }

    public void send() {
        ModNetwork.sendToServer(this);
    }

    public static void handle(C2SSetNetworkProjectorUrlPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !(player.containerMenu instanceof NetworkProjectorMenu menu)
                    || !menu.pos.equals(packet.projectorPos) || !menu.stillValid(player)
                    || !(player.level().getBlockEntity(packet.projectorPos) instanceof NetworkProjectorBlockEntity projector)) return;
            String value = packet.url.trim();
            if (!value.isEmpty() && !isValidWebUrl(value)) return;
            projector.setUrl(value, player.getUUID());
        });
        ctx.get().setPacketHandled(true);
    }

    private static boolean isValidWebUrl(String value) {
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            return (scheme != null && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")))
                    && uri.getHost() != null && uri.getUserInfo() == null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
