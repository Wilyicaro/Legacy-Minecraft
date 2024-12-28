package wily.legacy.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import wily.factoryapi.FactoryAPI;
import wily.legacy.Legacy4J;
import wily.legacy.util.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

public class StoneCuttingGroupManager implements ResourceManagerReloadListener {
    public static final Map<String,List<RecipeInfo.Filter>> listing = new LinkedHashMap<>();
    private static final String STONECUTTING_GROUPS = "stonecutting_groups.json";
    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        listing.clear();
        JsonUtil.getOrderedNamespaces(manager).forEach(name->manager.getResource(FactoryAPI.createLocation(name, STONECUTTING_GROUPS)).ifPresent(r->{
            try (BufferedReader bufferedReader = r.openAsReader()) {
                JsonElement element = JsonParser.parseReader(bufferedReader);
                RecipeInfo.Filter.LISTING_CODEC.parse(JsonOps.INSTANCE,element).result().ifPresent(listing::putAll);
            } catch (IOException exception) {
                Legacy4J.LOGGER.warn(exception.getMessage());
            }
        }));
    }

    @Override
    public String getName() {
        return "legacy:stone_cutting_group";
    }
}
