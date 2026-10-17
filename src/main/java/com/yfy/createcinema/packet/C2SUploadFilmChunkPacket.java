package com.yfy.createcinema.packet;

import com.yfy.createcinema.CreateCinema;
import com.yfy.createcinema.blockentity.BurnerBlockEntity;
import com.yfy.createcinema.film.FilmMetadata;
import com.yfy.createcinema.film.FilmStorage;
import com.yfy.createcinema.item.FilmItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = CreateCinema.MODID)
public record C2SUploadFilmChunkPacket(UUID uploadId, BlockPos burnerPos, int index, int total, byte[] data) {
    private static final int MAX_CHUNK_BYTES = 900_000;
    private static final long SESSION_TIMEOUT_MILLIS = 10 * 60 * 1000L;
    private static final Map<UUID, UploadSession> UPLOADS = new HashMap<>();

    public static void encode(C2SUploadFilmChunkPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.uploadId);
        buf.writeBlockPos(packet.burnerPos);
        buf.writeVarInt(packet.index);
        buf.writeVarInt(packet.total);
        buf.writeByteArray(packet.data);
    }

    public static C2SUploadFilmChunkPacket decode(FriendlyByteBuf buf) {
        return new C2SUploadFilmChunkPacket(buf.readUUID(), buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(),
                buf.readByteArray(MAX_CHUNK_BYTES));
    }

    public void send() {
        ModNetwork.sendToServer(this);
    }

    public static void cancelUploads(ServerPlayer player, BlockPos burnerPos) {
        UPLOADS.entrySet().removeIf(entry -> {
            UploadSession session = entry.getValue();
            if (!session.owner.equals(player.getUUID()) || !session.burnerPos.equals(burnerPos)) return false;
            session.delete();
            return true;
        });
    }

    @SubscribeEvent
    public static void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UPLOADS.entrySet().removeIf(entry -> {
                if (!entry.getValue().owner.equals(player.getUUID())) return false;
                entry.getValue().delete();
                return true;
            });
        }
    }

    public static void handle(C2SUploadFilmChunkPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            discardExpiredUploads();
            if (!packet.validChunk()) return;
            if (!(player.level().getBlockEntity(packet.burnerPos) instanceof BurnerBlockEntity burner)
                    || !burner.hasBlankFilm() || !burner.isBurning()) {
                discard(packet.uploadId);
                new S2CBurnStatusPacket(packet.burnerPos, "Burn cancelled: blank film removed", 0.0f, false)
                        .sendTo(player);
                return;
            }

            try {
                UploadSession session = UPLOADS.get(packet.uploadId);
                if (session == null) {
                    session = new UploadSession(player, packet.burnerPos, packet.total);
                    UPLOADS.put(packet.uploadId, session);
                }
                if (!session.matches(player, packet.burnerPos, packet.total)) {
                    discard(packet.uploadId);
                    new S2CBurnStatusPacket(packet.burnerPos, "Upload failed: transfer changed", 0.0f, false)
                            .sendTo(player);
                    return;
                }
                session.write(packet.index, packet.data);
                float uploadProgress = 0.90f + 0.08f * (session.receivedCount() / (float) packet.total);
                new S2CBurnStatusPacket(packet.burnerPos,
                        "Uploading " + (session.receivedCount() * 100 / packet.total) + "%", uploadProgress, true)
                        .sendTo(player);
                if (!session.complete()) return;

                try {
                    UPLOADS.remove(packet.uploadId);
                    Path packageFile = session.finish();
                    FilmMetadata metadata = FilmStorage.saveUploadedFilm(player.server, packageFile);
                    if (burner.hasBlankFilm() && FilmItem.getMediaType(burner.getFilm()) == metadata.mediaTypeValue()) {
                        burner.writeFilmId(metadata.id(), metadata.title(), metadata.durationSeconds(),
                                metadata.mediaTypeValue(), metadata.frameCount());
                        new S2CBurnStatusPacket(packet.burnerPos, "Burn complete: " + metadata.title(), 1.0f, false)
                                .sendTo(player);
                    } else {
                        new S2CBurnStatusPacket(packet.burnerPos, "Burner has no blank film", 0.0f, false)
                                .sendTo(player);
                    }
                } finally {
                    session.delete();
                }
            } catch (IOException e) {
                discard(packet.uploadId);
                burner.setBurning(false);
                CreateCinema.LOGGER.warn("Failed to save uploaded film", e);
                new S2CBurnStatusPacket(packet.burnerPos, "Burn failed: " + e.getMessage(), 0.0f, false)
                        .sendTo(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private boolean validChunk() {
        return index >= 0 && total > 0 && index < total && data.length > 0 && data.length <= MAX_CHUNK_BYTES
                && (index == total - 1 || data.length == MAX_CHUNK_BYTES);
    }

    private static void discard(UUID uploadId) {
        UploadSession session = UPLOADS.remove(uploadId);
        if (session != null) session.delete();
    }

    private static void discardExpiredUploads() {
        long cutoff = System.currentTimeMillis() - SESSION_TIMEOUT_MILLIS;
        UPLOADS.entrySet().removeIf(entry -> {
            if (entry.getValue().lastUpdated >= cutoff) return false;
            entry.getValue().delete();
            return true;
        });
    }

    private static class UploadSession {
        private final UUID owner;
        private final BlockPos burnerPos;
        private final int total;
        private final Path packageFile;
        private final FileChannel channel;
        private final Set<Integer> received = new HashSet<>();
        private long lastUpdated = System.currentTimeMillis();

        private UploadSession(ServerPlayer player, BlockPos burnerPos, int total) throws IOException {
            this.owner = player.getUUID();
            this.burnerPos = burnerPos.immutable();
            this.total = total;
            Path root = FilmStorage.serverFilmRoot(player.server).resolve(".uploads");
            Files.createDirectories(root);
            deleteExpiredFiles(root);
            this.packageFile = Files.createTempFile(root, "upload-", ".zip");
            this.channel = FileChannel.open(packageFile, StandardOpenOption.WRITE);
        }

        private boolean matches(ServerPlayer player, BlockPos burnerPos, int total) {
            return owner.equals(player.getUUID()) && this.burnerPos.equals(burnerPos) && this.total == total;
        }

        private void write(int index, byte[] data) throws IOException {
            lastUpdated = System.currentTimeMillis();
            if (!received.add(index)) return;
            channel.position(Math.multiplyExact((long) index, MAX_CHUNK_BYTES));
            ByteBuffer buffer = ByteBuffer.wrap(data);
            while (buffer.hasRemaining()) channel.write(buffer);
        }

        private int receivedCount() {
            return received.size();
        }

        private boolean complete() {
            return received.size() == total;
        }

        private Path finish() throws IOException {
            channel.close();
            return packageFile;
        }

        private void delete() {
            try {
                channel.close();
            } catch (IOException ignored) {
            }
            try {
                Files.deleteIfExists(packageFile);
            } catch (IOException e) {
                CreateCinema.LOGGER.debug("Failed to delete temporary film upload {}", packageFile, e);
            }
        }

        private static void deleteExpiredFiles(Path root) throws IOException {
            long cutoff = System.currentTimeMillis() - SESSION_TIMEOUT_MILLIS;
            try (var files = Files.list(root)) {
                for (Path file : files.toList()) {
                    boolean active = UPLOADS.values().stream().anyMatch(session -> session.packageFile.equals(file));
                    if (!active && Files.getLastModifiedTime(file).toMillis() < cutoff) Files.deleteIfExists(file);
                }
            }
        }
    }
}
