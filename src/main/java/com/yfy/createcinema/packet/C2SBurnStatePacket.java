package com.yfy.createcinema.packet;

import com.yfy.createcinema.blockentity.BurnerBlockEntity;
import com.yfy.createcinema.film.MediaType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SBurnStatePacket(BlockPos burnerPos, boolean active, MediaType mediaType) {
    public C2SBurnStatePacket(BlockPos burnerPos, boolean active) {
        this(burnerPos, active, MediaType.VIDEO);
    }

    public static void encode(C2SBurnStatePacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.burnerPos);
        buf.writeBoolean(packet.active);
        buf.writeUtf(packet.mediaType.id(), 16);
    }

    public static C2SBurnStatePacket decode(FriendlyByteBuf buf) {
        return new C2SBurnStatePacket(buf.readBlockPos(), buf.readBoolean(), MediaType.fromId(buf.readUtf(16)));
    }

    public void send() {
        ModNetwork.sendToServer(this);
    }

    public static void handle(C2SBurnStatePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (!(player.level().getBlockEntity(packet.burnerPos) instanceof BurnerBlockEntity burner)) return;
            if (player.distanceToSqr(packet.burnerPos.getX() + 0.5, packet.burnerPos.getY() + 0.5,
                    packet.burnerPos.getZ() + 0.5) > 64.0) return;

            if (packet.active && !burner.hasBlankFilm()) {
                new S2CBurnStatusPacket(packet.burnerPos, "Insert a blank film", 0.0f, false).sendTo(player);
                return;
            }
            if (packet.active && burner.getBlankMediaType() != packet.mediaType) {
                new S2CBurnStatusPacket(packet.burnerPos, "Media type changed", 0.0f, false).sendTo(player);
                return;
            }
            burner.setBurning(packet.active);
            if (!packet.active) C2SUploadFilmChunkPacket.cancelUploads(player, packet.burnerPos);
        });
        ctx.get().setPacketHandled(true);
    }
}
