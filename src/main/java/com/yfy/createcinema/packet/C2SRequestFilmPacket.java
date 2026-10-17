package com.yfy.createcinema.packet;

import com.yfy.createcinema.CreateCinema;
import com.yfy.createcinema.film.FilmReferenceData;
import com.yfy.createcinema.film.FilmStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

public record C2SRequestFilmPacket(String filmId) {
    private static final int CHUNK_SIZE = 900_000;

    public static void encode(C2SRequestFilmPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.filmId);
    }

    public static C2SRequestFilmPacket decode(FriendlyByteBuf buf) {
        return new C2SRequestFilmPacket(buf.readUtf());
    }

    public void send() {
        ModNetwork.sendToServer(this);
    }

    public static void handle(C2SRequestFilmPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !FilmStorage.isValidFilmId(packet.filmId)) return;
            if (FilmReferenceData.get(player.server).isDeleted(packet.filmId)) {
                new S2CFilmDeletedPacket(packet.filmId).sendTo(player);
                return;
            }
            try {
                Path zip = FilmStorage.serverZipPath(player.server, packet.filmId);
                long size = Files.size(zip);
                long chunks = Math.max(1L, (size + CHUNK_SIZE - 1L) / CHUNK_SIZE);
                if (chunks > Integer.MAX_VALUE) throw new IOException("Film package is too large for the transfer protocol");
                try (InputStream input = Files.newInputStream(zip)) {
                    for (int i = 0; i < (int) chunks; i++) {
                        byte[] chunk = input.readNBytes(CHUNK_SIZE);
                        if (chunk.length == 0) throw new IOException("Film package ended before all chunks were sent");
                        new S2CDownloadFilmChunkPacket(packet.filmId, i, (int) chunks, chunk).sendTo(player);
                    }
                }
            } catch (IOException e) {
                CreateCinema.LOGGER.warn("Failed to send film {}", packet.filmId, e);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
