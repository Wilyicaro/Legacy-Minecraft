package wily.legacy.client;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import wily.legacy.Legacy4J;
import wily.legacy.util.JsonUtil;
import wily.legacy.util.RecipeValue;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

public class StoneCuttingGroupManager extends SimplePreparableReloadListener<Map<String,List<RecipeValue<SingleRecipeInput,StonecutterRecipe>>>> {
    public static final Map<String,List<RecipeValue<SingleRecipeInput,StonecutterRecipe>>> list = new LinkedHashMap<>();
    private static final String STONECUTTING_GROUPS = "stonecutting_groups.json";
    @Override
    protected Map<String,List<RecipeValue<SingleRecipeInput,StonecutterRecipe>>> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        Map<String,List<RecipeValue<SingleRecipeInput,StonecutterRecipe>>> groups = new LinkedHashMap<>();
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        JsonUtil.getOrderedNamespaces(manager).forEach(name->manager.getResource(ResourceLocation.fromNamespaceAndPath(name, STONECUTTING_GROUPS)).ifPresent(r->{
            try {
                BufferedReader bufferedReader = r.openAsReader();
                JsonObject obj = GsonHelper.parse(bufferedReader);
                if (obj.has("groups")) JsonUtil.addGroupedRecipeValuesFromJson(groups,obj.get("groups"));
                bufferedReader.close();
            } catch (IOException exception) {
                Legacy4J.LOGGER.warn(exception.getMessage());
            }
        }));
        return groups;
    }


    @Override
    protected void apply(Map<String,List<RecipeValue<SingleRecipeInput,StonecutterRecipe>>> object, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        list.clear();
        list.putAll(object);;
    }
}
