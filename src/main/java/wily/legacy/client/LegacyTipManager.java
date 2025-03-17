package wily.legacy.client;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import wily.factoryapi.FactoryAPI;
import wily.legacy.Legacy4J;
import wily.legacy.client.screen.LegacyLoadingScreen;
import wily.legacy.client.screen.LegacyMenuAccess;
import wily.legacy.util.LegacyTipBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class LegacyTipManager implements ResourceManagerReloadListener {
    private static final String TIPS = "texts/tips.json";

    public static float tipDiffPercentage;
    private static LegacyTip actualTip;
    private static Supplier<LegacyTip> actualTipSupplier;
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
        return LegacyOptions.hints.get() && Minecraft.getInstance().screen instanceof LegacyMenuAccess<?> a && a.getTipXDiff() != 0 ? Math.min(0,Math.max(a.getTipXDiff(),50 - a.getMenuRectangle().left()) * Math.max(0,Math.min(tipDiffPercentage,1))) : 0;
    }

    public static void setActualTip(LegacyTip tip) {
        lastTip = actualTip;
        actualTip = tip;
        actualTipSupplier = null;
    }

    public static LegacyTip updateTip() {
        if (tips.isEmpty()) setActualTip(null);
        else {
            setActualTip(tips.get(0).get());
            tips.remove(0);
        }
        return actualTip;
    }

    public static boolean rebuildActual(){
        return setTip(actualTipSupplier);
    }

    public static boolean setTip(Supplier<LegacyTip> tipSupplier){
        if (tipSupplier != null) {
            setActualTip(tipSupplier.get());
            actualTipSupplier = tipSupplier;
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

    public static LegacyTip getTipFromBuilder(LegacyTipBuilder builder){
        return getTipFromBuilder(builder.getTitle(), builder.getTip(), builder.getItem(), builder.getTime() * 1000L);
    }

    public static LegacyTip getTipFromBuilder(Optional<Component> title, Optional<Component> tip, ItemStack stack, long time){
        return new LegacyTip(title.orElse(null),tip.orElse(CommonComponents.EMPTY)).itemStack(stack).disappearTime(time);
    }

    public static LegacyTip getLoadingTipFromBuilder(LegacyTipBuilder builder){
        return getLoadingTipFromBuilder(builder.getTitle(), builder.getTip(), builder.getItem(), builder.getTime() * 1000L);
    }

    public static LegacyTip getLoadingTipFromBuilder(Optional<Component> title, Optional<Component> tip, ItemStack stack, long time){
        return new LegacyTip(tip.orElse(CommonComponents.EMPTY),400,55).centered().title(title.orElse(null)).itemStack(stack).disappearTime(time);
    }

    public static Supplier<LegacyTip> getTip(ItemStack item, LegacyTipBuilder modifier){
        LegacyTipBuilder builder = new LegacyTipBuilder().item(item).copyFrom(LegacyTipOverride.getOverride(item)).copyFrom(modifier,true);
        if (builder.getTip().isPresent() && !hasTip(builder.getTip().get())) return null;
        return ()-> getTipFromBuilder(builder);
    }

    public static Supplier<LegacyTip> getTip(ItemStack item){
        if (hasTip(item)) {
            LegacyTipBuilder builder = new LegacyTipBuilder().item(item).copyFrom(LegacyTipOverride.getOverride(item));
            return ()-> getTipFromBuilder(builder);
        }
        else return null;
    }

    public static Supplier<LegacyTip> getTip(Entity entity) {
        Supplier<LegacyTip> entityTypeTip = getTip(entity.getType());
        if (entityTypeTip != null) return entityTypeTip;
        ItemStack pickResult = entity.getPickResult();
        return pickResult != null && !pickResult.isEmpty() && !(pickResult.getItem() instanceof SpawnEggItem) ? getTip(pickResult) : null;
    }

    public static Supplier<LegacyTip> getTip(EntityType<?> entity) {
        if (hasTip(entity)) {
            LegacyTipBuilder builder = new LegacyTipBuilder().title(entity.getDescription()).tip(Component.translatable(LegacyTipBuilder.getTipId(entity)));
            builder.copyFrom(LegacyTipOverride.getOverride(entity));
            return ()-> getTipFromBuilder(builder);
        } return null;
    }

    public static Component getTipComponent(ItemStack item){
        return hasValidTipOverride(item) ? LegacyTipOverride.getOverride(item).getTip().orElse(Component.empty()) : LegacyTipBuilder.getTip(item);
    }

    public static Component getTipComponent(EntityType<?> type){
        return hasValidTipOverride(type) ? LegacyTipOverride.getOverride(type).getTip().orElse(Component.empty())  : Component.translatable(LegacyTipBuilder.getTipId(type));
    }

    public static boolean hasTip(ItemStack item){
        return hasTip(LegacyTipBuilder.getTip(item)) || hasValidTipOverride(item);
    }

    public static boolean hasValidTipOverride(ItemStack item){
        return LegacyTipOverride.getOverride(item).getTip().isPresent() && (!LegacyTipOverride.getOverride(item).getItem().isEmpty() || hasTip(LegacyTipOverride.getOverride(item).getTip().get()));
    }

    public static boolean hasValidTipOverride(EntityType<?> type){
        return LegacyTipOverride.getOverride(type).getTip().isPresent() && (!LegacyTipOverride.getOverride(type).getItem().isEmpty() || hasTip(LegacyTipOverride.getOverride(type).getTip().get()));
    }

    public static boolean hasTip(Component c){
        return !(c.getContents() instanceof TranslatableContents t) || hasTip(t.getKey()) ;
    }

    public static boolean hasTip(String s){
        return Language.getInstance().has(s);
    }

    public static boolean hasTip(EntityType<?> s){
        return hasTip(LegacyTipBuilder.getTipId(s)) || hasValidTipOverride(s);
    }

    public static Component getTipComponent(ResourceLocation location){
        return Component.translatable(location.toLanguageKey() +".tip");
    }


    @Override
    public void onResourceManagerReload(ResourceManager resourceManager){
        loadingTips.clear();
        LegacyLoadingScreen.usingLoadingTips.clear();
        LegacyLoadingScreen.actualLoadingTip = null;
        resourceManager.getNamespaces().forEach(name->resourceManager.getResource(FactoryAPI.createLocation(name,TIPS)).ifPresent(r->{
            try (BufferedReader bufferedReader = r.openAsReader()) {
                JsonObject obj = GsonHelper.parse(bufferedReader);
                LegacyTipBuilder.LIST_CODEC.parse(JsonOps.INSTANCE, obj.get("loadingTips")).result().ifPresent(l-> l.forEach(b-> loadingTips.add(()->getLoadingTipFromBuilder(b))));
            } catch (IOException var8) {
                Legacy4J.LOGGER.warn(var8.getMessage());
            }
        }));
    }

    @Override
    public String getName() {
        return "legacy:tip_manager";
    }
}
