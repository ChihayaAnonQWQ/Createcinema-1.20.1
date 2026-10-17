package com.yfy.createcinema.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import com.yfy.createcinema.film.MediaType;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class FilmItem extends Item {
    public static final String TAG_FILM_ID = "FilmId";
    public static final String TAG_TITLE = "Title";
    public static final String TAG_COPY_ID = "CopyId";
    public static final String TAG_COMPLETED = "Completed";
    public static final String TAG_DURATION_SECONDS = "DurationSeconds";
    public static final String TAG_MEDIA_TYPE = "MediaType";
    public static final String TAG_PAGE_COUNT = "PageCount";

    private final MediaType mediaType;

    public FilmItem(Properties properties) {
        this(properties, MediaType.VIDEO);
    }

    public FilmItem(Properties properties, MediaType mediaType) {
        super(properties);
        this.mediaType = mediaType;
    }

    public MediaType blankMediaType() {
        return mediaType;
    }

    public static ItemStack create(Item item, String filmId, String title) {
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_FILM_ID, filmId);
        tag.putString(TAG_TITLE, title);
        tag.putBoolean(TAG_COMPLETED, false);
        tag.putString(TAG_MEDIA_TYPE, item instanceof FilmItem film ? film.blankMediaType().id() : MediaType.VIDEO.id());
        if (!filmId.isBlank()) tag.putString(TAG_COPY_ID, newCopyId());
        if (!title.isBlank()) {
            stack.setHoverName(Component.literal(title));
        }
        return stack;
    }

    private static CompoundTag tagOrNull(ItemStack stack) {
        return stack.getTag();
    }

    public static String getFilmId(ItemStack stack) {
        CompoundTag data = tagOrNull(stack);
        if (data == null) return "";
        return data.getString(TAG_FILM_ID);
    }

    public static String getCopyId(ItemStack stack) {
        CompoundTag data = tagOrNull(stack);
        return data == null ? "" : data.getString(TAG_COPY_ID);
    }

    public static MediaType getMediaType(ItemStack stack) {
        CompoundTag data = tagOrNull(stack);
        if (data != null && data.contains(TAG_MEDIA_TYPE)) {
            return MediaType.fromId(data.getString(TAG_MEDIA_TYPE));
        }
        return stack.getItem() instanceof FilmItem film ? film.blankMediaType() : MediaType.VIDEO;
    }

    public static boolean isBlank(ItemStack stack) {
        return stack.getItem() instanceof FilmItem && getFilmId(stack).isBlank();
    }

    public static boolean isStatic(ItemStack stack) {
        return getMediaType(stack).isStatic();
    }

    public static void setRecorded(ItemStack stack, String filmId, String title, double durationSeconds,
                                   MediaType mediaType) {
        setRecorded(stack, filmId, title, durationSeconds, mediaType, 1);
    }

    public static void setRecorded(ItemStack stack, String filmId, String title, double durationSeconds,
                                   MediaType mediaType, int pageCount) {
        updateData(stack, tag -> {
            tag.putString(TAG_FILM_ID, filmId);
            tag.putString(TAG_TITLE, title);
            tag.putString(TAG_MEDIA_TYPE, mediaType.id());
            tag.putBoolean(TAG_COMPLETED, false);
            tag.putString(TAG_COPY_ID, newCopyId());
            tag.putDouble(TAG_DURATION_SECONDS, Math.max(0.0, durationSeconds));
            tag.putInt(TAG_PAGE_COUNT, Math.max(1, pageCount));
        });
        stack.setHoverName(Component.literal(title));
    }

    public static int getPageCount(ItemStack stack) {
        CompoundTag data = tagOrNull(stack);
        return data == null ? 1 : Math.max(1, data.getInt(TAG_PAGE_COUNT));
    }

    public static void setCopyId(ItemStack stack, String copyId) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_COPY_ID, copyId);
    }

    public static boolean isCompleted(ItemStack stack) {
        CompoundTag data = tagOrNull(stack);
        return data != null && data.getBoolean(TAG_COMPLETED);
    }

    public static void setCompleted(ItemStack stack, boolean completed) {
        updateData(stack, tag -> tag.putBoolean(TAG_COMPLETED, completed));
    }

    public static double getDurationSeconds(ItemStack stack) {
        CompoundTag data = tagOrNull(stack);
        return data == null ? 0.0 : Math.max(0.0, data.getDouble(TAG_DURATION_SECONDS));
    }

    public static void setDurationSeconds(ItemStack stack, double durationSeconds) {
        updateData(stack, tag -> tag.putDouble(TAG_DURATION_SECONDS, Math.max(0.0, durationSeconds)));
    }

    public static void prepareForPlayback(ItemStack stack) {
        setCompleted(stack, false);
    }

    private static void updateData(ItemStack stack, java.util.function.Consumer<CompoundTag> mutation) {
        CompoundTag tag = stack.getOrCreateTag();
        mutation.accept(tag);
    }

    public static String newCopyId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String filmId = getFilmId(stack);
        if (filmId.isBlank()) {
            tooltip.add(Component.translatable("item.createcinema.film.empty").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.literal(filmId).withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable(isCompleted(stack)
                    ? "tooltip.createcinema.film.completed" : "tooltip.createcinema.film.uncompleted")
                    .withStyle(isCompleted(stack) ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        }
        if (!Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.createcinema.hold_shift").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltip.add(Component.translatable("tooltip.createcinema.film.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.createcinema.film.2").withStyle(ChatFormatting.GRAY));
    }
}
