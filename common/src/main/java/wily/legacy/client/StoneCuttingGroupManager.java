package wily.legacy.client;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import wily.legacy.LegacyMinecraft;
import wily.legacy.util.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StoneCuttingGroupManager extends SimplePreparableReloadListener<Map<String,List<RecipeValue<Container,StonecutterRecipe>>>> {
    public static final Map<String,List<RecipeValue<Container,StonecutterRecipe>>> list = new LinkedHashMap<>();
    private static final String STONECUTTING_GROUPS = "stonecutting_groups.json";
    @Override
    protected Map<String,List<RecipeValue<Container,StonecutterRecipe>>> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        Map<String,List<RecipeValue<Container,StonecutterRecipe>>> groups = new LinkedHashMap<>();
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        manager.getNamespaces().stream().sorted(Comparator.comparingInt(s-> s.equals("legacy") ? 0 : 1)).forEach(name->manager.getResource(new ResourceLocation(name, STONECUTTING_GROUPS)).ifPresent(r->{
            try {
                BufferedReader bufferedReader = r.openAsReader();
                JsonObject obj = GsonHelper.parse(bufferedReader);
                if (obj.has("groups")) JsonUtil.addGroupedRecipeValuesFromJson(groups,obj.get("groups"));
                bufferedReader.close();
            } catch (IOException exception) {
                LegacyMinecraft.LOGGER.warn(exception.getMessage());
            }
        }));
        return groups;
    }


    @Override
    protected void apply(Map<String,List<RecipeValue<Container,StonecutterRecipe>>> object, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        list.clear();
        list.putAll(object);;
    }
}
