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
import wily.legacy.LegacyMinecraft;
import wily.legacy.client.screen.LegacyLoadingScreen;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class LegacyTipManager extends SimplePreparableReloadListener<List<Supplier<LegacyTip>>> {
    private static final ResourceLocation TIPS_LOCATION = new ResourceLocation(LegacyMinecraft.MOD_ID,"texts/tips.json");

    public static final List<Supplier<LegacyTip>> loadingTips = new ArrayList<>();
    @Override
    protected List<Supplier<LegacyTip>> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        try {
            List<Supplier<LegacyTip>> loadingTips = new ArrayList<>();
            BufferedReader bufferedReader = Minecraft.getInstance().getResourceManager().openAsReader(TIPS_LOCATION);
            JsonObject obj = GsonHelper.parse(bufferedReader);
            if (obj.get("loadingTips") instanceof JsonArray array)
                for (JsonElement jsonElement : array) {
                    if (jsonElement.isJsonPrimitive()){
                        loadingTips.add(()->new LegacyTip(Component.translatable(jsonElement.getAsString())));
                    } else if (jsonElement instanceof JsonObject tipObj) {
                        if (tipObj.get("screenTime") instanceof JsonPrimitive p) loadingTips.add(()-> new LegacyTip(Component.translatable(tipObj.get("translationKey").getAsString())).disappearTime(p.getAsInt()));
                        else loadingTips.add(()-> new LegacyTip(Component.translatable(tipObj.get("translationKey").getAsString())));
                    }
                }
            bufferedReader.close();
            return loadingTips;
        } catch (IOException var8) {
            return Collections.emptyList();
        }
    }

    @Override
    protected void apply(List<Supplier<LegacyTip>> object, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        loadingTips.clear();
        loadingTips.addAll(object);
        LegacyLoadingScreen.actualLoadingTip = null;
    }
}
