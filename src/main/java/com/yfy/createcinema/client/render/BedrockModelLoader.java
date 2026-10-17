package com.yfy.createcinema.client.render;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.yfy.createcinema.CreateCinema;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.IModelBuilder;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.SimpleUnbakedGeometry;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

@Mod.EventBusSubscriber(modid = CreateCinema.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class BedrockModelLoader {
    private BedrockModelLoader() {
    }

    @SubscribeEvent
    public static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        // 1.20.1 registers geometry loaders by path only; the namespace is the active mod id.
        event.register("bedrock", Loader.INSTANCE);
    }

    private static final class Loader implements IGeometryLoader<Geometry> {
        private static final Loader INSTANCE = new Loader();

        @Override
        public Geometry read(JsonObject model, JsonDeserializationContext context) throws JsonParseException {
            if (!model.has("source")) throw new JsonParseException("Bedrock model requires a source resource");
            try {
                Set<String> bones = new HashSet<>();
                if (model.has("bones")) {
                    for (JsonElement bone : model.getAsJsonArray("bones")) bones.add(bone.getAsString());
                }
                return new Geometry(new ResourceLocation(model.get("source").getAsString()), Set.copyOf(bones));
            } catch (RuntimeException error) {
                throw new JsonParseException("Bedrock model source is invalid", error);
            }
        }
    }

    private static final class Geometry extends SimpleUnbakedGeometry<Geometry> {
        private final ResourceLocation source;
        private final Set<String> bones;

        private Geometry(ResourceLocation source, Set<String> bones) {
            this.source = source;
            this.bones = bones;
        }

        @Override
        protected void addQuads(IGeometryBakingContext context, IModelBuilder<?> builder, ModelBaker baker,
                                Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState,
                                ResourceLocation modelLocation) {
            BedrockGeometryRenderer.bakeStatic(source, bones, context, builder, spriteGetter, modelState);
        }
    }
}
