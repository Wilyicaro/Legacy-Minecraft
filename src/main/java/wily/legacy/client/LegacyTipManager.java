package wily.legacy.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import wily.factoryapi.FactoryAPI;
import wily.legacy.Legacy4J;
import wily.legacy.client.screen.LegacyLoadingScreen;
import wily.legacy.client.screen.LegacyMenuAccess;
import wily.legacy.util.ScreenUtil;

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
        return LegacyOption.hints.get() && Minecraft.getInstance().screen instanceof LegacyMenuAccess<?> a ? Math.min(0,Math.max(a.getTipXDiff(),50 - a.getMenuRectangle().left()) * Math.max(0,Math.min(tipDiffPercentage,1))) : 0;
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
    public static boolean setTip(Supplier<LegacyTip> tipSupplier){
        if (tipSupplier != null) {
            setActualTip(tipSupplier.get());
            return true;
        }
        return false;
    }
    public static void addTip(Supplier<LegacyTip> tipSupplier){
        if (tipSupplier != null) tips.add(tipSupplier);
    }

    public static void addTip(Entity entity){
        addTip(getTip(entity));
    }

    public static void addTip(EntityType<?> entityType){
        addTip(getTip(entityType));
    }

    public static LegacyTip getCustomTip(Component title, Component tip, ItemStack stack, long time){
        return (title.getString().isEmpty() && tip.getString().isEmpty() && !stack.isEmpty() ? new LegacyTip(stack) : new LegacyTip(title,tip).itemStack(stack)).disappearTime(time);
    }

    public static void addTip(ItemStack stack){
        if (hasTip(stack)) tips.add(()->new LegacyTip(stack));
    }

    public static Supplier<LegacyTip> getTip(ItemStack item){
        return hasTip(item) ? ()-> new LegacyTip(item) : null;
    }

    public static Supplier<LegacyTip> getTip(Entity entity) {
        if (hasTip(entity.getType())) return ()-> new LegacyTip(entity.getType().getDescription(), getTipComponent(entity.getType()));
        else if (entity.getPickResult() != null && !entity.getPickResult().isEmpty() && hasTip(entity.getPickResult())) return getTip(entity.getPickResult());
        else return null;
    }

    public static Supplier<LegacyTip> getTip(EntityType<?> entityType){
        return hasTip(entityType) ? ()->new LegacyTip(entityType.getDescription(), getTipComponent(entityType)) : null;
    }

    public static Component getTipComponent(ItemStack item){
        return hasValidTipOverride(item) ? LegacyTipOverride.getOverride(item) : Component.translatable(getTipId(item));
    }

    public static Component getTipComponent(EntityType<?> type){
        return hasValidTipOverride(type) ? LegacyTipOverride.getOverride(type) : Component.translatable(getTipId(type));
    }

    public static boolean hasTip(ItemStack item){
        return hasTip(getTipId(item)) || hasValidTipOverride(item);
    }

    public static boolean hasValidTipOverride(ItemStack item){
        return !LegacyTipOverride.getOverride(item).getString().isEmpty() && hasTip(((TranslatableContents)LegacyTipOverride.getOverride(item).getContents()).getKey());
    }

    public static boolean hasValidTipOverride(EntityType<?> type){
        return !LegacyTipOverride.getOverride(type).getString().isEmpty() && hasTip(((TranslatableContents)LegacyTipOverride.getOverride(type).getContents()).getKey());
    }

    public static boolean hasTip(String s){
        return Language.getInstance().has(s);
    }

    public static boolean hasTip(EntityType<?> s){
        return hasTip(getTipId(s)) || hasValidTipOverride(s);
    }

    public static String getTipId(ItemStack item){
        return item.getItem().getDescriptionId() + ".tip";
    }

    public static String getTipId(EntityType<?> item){
        return item.getDescriptionId() + ".tip";
    }

    public static Component getTipComponent(ResourceLocation location){
        return Component.translatable(location.toLanguageKey() +".tip");
    }


    @Override
    protected List<Supplier<LegacyTip>> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        List<Supplier<LegacyTip>> loadingTips = new ArrayList<>();
        resourceManager.getNamespaces().forEach(name->resourceManager.getResource(FactoryAPI.createLocation(name,TIPS)).ifPresent(r->{
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

    @Override
    public String getName() {
        return "legacy:tip_manager";
    }
}
