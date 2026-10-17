package com.yfy.createcinema.client.jei;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yfy.createcinema.CreateCinema;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JeiPlugin
public final class CreateCinemaJeiPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_ID =
            new ResourceLocation(CreateCinema.MODID, "jei_plugin");
    private static final List<String> RECIPE_FILES = List.of(
            "black_screen", "blank_album", "blank_image", "blank_slides", "burner", "cable",
            "config_manager", "continuous_play_upgrade", "darkroom_block", "display_upgrade", "film",
            "network_projector", "projector", "remote_control_upgrade", "screen", "speaker");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<CraftingRecipe> recipes = new ArrayList<>();
        for (String name : RECIPE_FILES) {
            try {
                recipes.add(loadRecipe(name));
            } catch (Exception error) {
                CreateCinema.LOGGER.warn("Could not register {} recipe with JEI", name, error);
            }
        }
        registration.addRecipes(RecipeTypes.CRAFTING, recipes);
    }

    private static CraftingRecipe loadRecipe(String name) throws IOException {
        JsonObject json = readRecipeJson(name);
        Map<Character, Ingredient> key = new HashMap<>();
        for (var entry : json.getAsJsonObject("key").entrySet()) {
            JsonObject ingredient = entry.getValue().getAsJsonObject();
            ResourceLocation itemId = new ResourceLocation(ingredient.get("item").getAsString());
            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item == Items.AIR) throw new IOException("Unknown recipe item " + itemId);
            key.put(entry.getKey().charAt(0), Ingredient.of(item));
        }

        JsonArray patternArray = json.getAsJsonArray("pattern");
        List<String> pattern = new ArrayList<>();
        for (var row : patternArray) pattern.add(row.getAsString());
        if (pattern.isEmpty()) throw new IOException("Recipe pattern is empty");

        int height = pattern.size();
        int width = 0;
        for (String row : pattern) width = Math.max(width, row.length());
        NonNullList<Ingredient> ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);
        for (int rowIndex = 0; rowIndex < height; rowIndex++) {
            String row = pattern.get(rowIndex);
            for (int colIndex = 0; colIndex < row.length(); colIndex++) {
                char symbol = row.charAt(colIndex);
                if (symbol == ' ') continue;
                Ingredient ingredient = key.get(symbol);
                if (ingredient == null) throw new IOException("Recipe pattern uses undefined key " + symbol);
                ingredients.set(colIndex + rowIndex * width, ingredient);
            }
        }

        JsonObject resultJson = json.getAsJsonObject("result");
        // 1.20.1 uses "item", 1.21+ uses "id"; accept both so this keeps working whichever layout the
        // shipped data files end up in.
        String resultKey = resultJson.has("item") ? "item" : "id";
        ResourceLocation resultId = new ResourceLocation(resultJson.get(resultKey).getAsString());
        Item resultItem = BuiltInRegistries.ITEM.get(resultId);
        if (resultItem == Items.AIR) throw new IOException("Unknown recipe result " + resultId);
        int count = resultJson.has("count") ? resultJson.get("count").getAsInt() : 1;
        CraftingBookCategory category = json.has("category")
                ? CraftingBookCategory.valueOf(json.get("category").getAsString().toUpperCase())
                : CraftingBookCategory.MISC;
        return new ShapedRecipe(new ResourceLocation(CreateCinema.MODID, name), "", category, width, height,
                ingredients, new ItemStack(resultItem, count));
    }

    private static JsonObject readRecipeJson(String name) throws IOException {
        for (String directory : new String[]{"recipes", "recipe"}) {
            String path = "/data/" + CreateCinema.MODID + "/" + directory + "/" + name + ".json";
            InputStream input = CreateCinemaJeiPlugin.class.getResourceAsStream(path);
            if (input == null) continue;
            try (InputStream stream = input) {
                return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            }
        }
        throw new IOException("Missing recipe resource for " + name);
    }
}
