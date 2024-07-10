package wily.legacy.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import wily.legacy.Legacy4J;
import wily.legacy.client.screen.LegacyLoadingScreen;
import wily.legacy.client.screen.LegacyMenuAccess;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class LegacyTipManager extends SimplePreparableReloadListener<List<Supplier<LegacyTip>>> {
    private static final String TIPS = "texts/tips.json";

    public static float tipDiffPercentage;
    private static LegacyTip actualTip;
    private static LegacyTip lastTip;
    public static final List<Supplier<LegacyTip>> tips = new ArrayList<>();
    public static final List<Supplier<LegacyTip>> loadingTips = new ArrayList<>();

    public static LegacyTip getActualTip() {
        return actualTip;
    }
    public static LegacyTip getLastTip() {
        return lastTip;
    }
    public static float getTipXDiff(){
        return Minecraft.getInstance().screen instanceof LegacyMenuAccess<?> a ? Math.min(0,Math.max(a.getTipXDiff(),50 - a.getMenuRectangle().left()) * Math.max(0,Math.min(tipDiffPercentage,1))) : 0;
    }
    public static void setActualTip(LegacyTip tip) {
        lastTip = actualTip;
        actualTip = tip;
    }
    public static LegacyTip getUpdateTip() {
        if (tips.isEmpty()) setActualTip(null);
        else {
            setActualTip(tips.get(0).get());
            tips.remove(0);
        }
        return actualTip;
    }


    @Override
    protected List<Supplier<LegacyTip>> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        List<Supplier<LegacyTip>> loadingTips = new ArrayList<>();
        resourceManager.getNamespaces().forEach(name->resourceManager.getResource(new ResourceLocation(name,TIPS)).ifPresent(r->{
            try {
                BufferedReader bufferedReader = r.openAsReader();
                JsonObject obj = GsonHelper.parse(bufferedReader);
                if (obj.get("loadingTips") instanceof JsonArray array)
                    for (JsonElement jsonElement : array) {
                        if (jsonElement.isJsonPrimitive()){
                            loadingTips.add(()->new LegacyTip(Component.translatable(jsonElement.getAsString())).centered());
                        } else if (jsonElement instanceof JsonObject tipObj) {
                            if (tipObj.get("screenTime") instanceof JsonPrimitive p) loadingTips.add(()-> new LegacyTip(Component.translatable(tipObj.get("translationKey").getAsString())).disappearTime(p.getAsInt()).centered());
                            else loadingTips.add(()-> new LegacyTip(Component.translatable(tipObj.get("translationKey").getAsString())).centered());
                        }
                    }
                bufferedReader.close();
            } catch (IOException var8) {
                Legacy4J.LOGGER.warn(var8.getMessage());
            }
        }));
        return loadingTips;
    }

    @Override
    protected void apply(List<Supplier<LegacyTip>> object, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        loadingTips.clear();
        loadingTips.addAll(object);
        LegacyLoadingScreen.usingLoadingTips.clear();
        LegacyLoadingScreen.actualLoadingTip = null;
    }
}
