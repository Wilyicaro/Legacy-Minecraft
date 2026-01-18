package wily.legacy.client;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.FactoryAPIClient;
import wily.legacy.Legacy4J;
import wily.legacy.client.screen.LegacyMenuAccess;
import wily.legacy.util.LegacyTipBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class LegacyTipManager implements ResourceManagerReloadListener {
    public static final int MOVEMENT_TIP_TICKS = 15;
    public static final int PAUSE_TIP_TICKS = 10;
    public static final int MAX_TIP_TICKS = MOVEMENT_TIP_TICKS * 2 + PAUSE_TIP_TICKS;
    public static final List<Supplier<LegacyTip>> tips = new ArrayList<>();
    public static final List<Supplier<LegacyTip>> loadingTips = new ArrayList<>();
    private static final String TIPS = "texts/tips.json";
    private static final List<Supplier<LegacyTip>> usingLoadingTips = new ArrayList<>();
    public static int tipTicks;
    public static boolean startTipOffset = false;
    public static boolean returningTip = false;
    private static LegacyTip actualTip;
    private static Supplier<LegacyTip> actualTipSupplier;
    private static LegacyTip lastTip;
    private static LegacyTip actualLoadingTip;
    private static Supplier<LegacyTip> actualLoadingTipSupplier;
    private final RandomSource random = RandomSource.create();

    public static LegacyTip getLoadingTip(RandomSource random) {
        if (usingLoadingTips.isEmpty()) {
            if (loadingTips.isEmpty()) return null;
            else usingLoadingTips.addAll(loadingTips);
        }
        if (actualLoadingTip == null) {
            int i = random.nextInt(usingLoadingTips.size());
            actualLoadingTip = usingLoadingTips.get(i).get();
            actualLoadingTipSupplier = usingLoadingTips.remove(i);
        } else if (actualLoadingTip.visibility == Toast.Visibility.HIDE) {
            actualLoadingTip = null;
            actualLoadingTipSupplier = null;
            return getLoadingTip(random);
        }
        return actualLoadingTip;
    }

    public static LegacyTip getRebuiltLoadingTip() {
        return actualLoadingTipSupplier == null ? null : actualLoadingTipSupplier.get();
    }

    public static LegacyTip getActualTip() {
        return actualTip;
    }

    public static void setActualTip(LegacyTip tip) {
        lastTip = actualTip;
        actualTip = tip;
        actualTipSupplier = null;
        if (actualTip != null) {
            startTipOffset = true;
        } else
            returningTip = true;
    }

    public static LegacyTip getLastTip() {
        return lastTip;
    }

    public static void updateTipTicks() {
        if (startTipOffset) {
            if (tipTicks < MAX_TIP_TICKS) {
                if (tipTicks < MOVEMENT_TIP_TICKS || returningTip)
                    tipTicks++;
            } else {
                resetTipOffset(actualTip == null);
            }
        }
    }

    public static void resetTipOffset(boolean resetStart) {
        if (resetStart)
            startTipOffset = false;
        returningTip = false;
        tipTicks = 0;
    }

    public static float getTipOffsetPercentage() {
        if (startTipOffset) {
            float ticks = tipTicks + FactoryAPIClient.getGamePartialTick(false);
            return Math.clamp(returningTip ? MAX_TIP_TICKS - ticks : ticks, 0, MOVEMENT_TIP_TICKS) / MOVEMENT_TIP_TICKS;
        }

        return 0;
    }

    public static float getTipXOffset() {
        return LegacyOptions.hints.get() && Minecraft.getInstance().screen instanceof LegacyMenuAccess<?> a && a.getTipXOffset() != 0 ? Math.min(0, Math.max(a.getTipXOffset(), 50 - a.getMenuRectangle().left()) * getTipOffsetPercentage()) : 0;
    }

    public static LegacyTip updateTip() {
        if (tips.isEmpty()) setActualTip(null);
        else {
            setActualTip(tips.get(0).get());
            tips.remove(0);
        }
        return actualTip;
    }

    public static boolean rebuildActual() {
        return setTip(actualTipSupplier);
    }

    public static void rebuildActualLoading() {
        actualLoadingTip = getRebuiltLoadingTip();
    }

    public static boolean setTip(Supplier<LegacyTip> tipSupplier) {
        if (tipSupplier != null) {
            setActualTip(tipSupplier.get());
            actualTipSupplier = tipSupplier;
            return true;
        }
        return false;
    }

    public static void addTip(Supplier<LegacyTip> tipSupplier) {
        if (tipSupplier != null) tips.add(tipSupplier);
    }

    public static void addTip(Entity entity) {
        addTip(getTip(entity));
    }

    public static void addTip(EntityType<?> entityType) {
        addTip(getTip(entityType));
    }

    public static LegacyTip getTipFromBuilder(LegacyTipBuilder builder) {
        return getTipFromBuilder(builder.getTitle(), builder.getTip(), builder.getItem(), builder.getTime() * 1000L);
    }

    public static LegacyTip getTipFromBuilder(Optional<Component> title, Optional<Component> tip, ItemStack stack, long time) {
        return new LegacyTip(title.orElse(null), tip.orElse(CommonComponents.EMPTY)).itemStack(stack).disappearTime(time);
    }

    public static LegacyTip getLoadingTipFromBuilder(LegacyTipBuilder builder) {
        return getLoadingTipFromBuilder(builder.getTitle(), builder.getTip(), builder.getItem(), builder.getTime() * 1000L);
    }

    public static LegacyTip getLoadingTipFromBuilder(Optional<Component> title, Optional<Component> tip, ItemStack stack, long time) {
        return new LegacyTip(tip.orElse(CommonComponents.EMPTY), LegacyOptions.getUIMode().isSD() ? 325 : 400, 55, false).centered().title(title.orElse(null)).itemStack(stack).disappearTime(time);
    }

    public static Supplier<LegacyTip> getTip(ItemStack item, LegacyTipBuilder modifier) {
        LegacyTipBuilder builder = new LegacyTipBuilder().item(item).copyFrom(LegacyTipOverride.getOverride(item)).copyFrom(modifier, true);
        if (builder.getTip().isPresent() && !hasTip(builder.getTip().get())) return null;
        return () -> getTipFromBuilder(builder);
    }

    public static Supplier<LegacyTip> getTip(ItemStack item) {
        if (hasTip(item)) {
            LegacyTipBuilder builder = new LegacyTipBuilder().item(item).copyFrom(LegacyTipOverride.getOverride(item));
            return () -> getTipFromBuilder(builder);
        } else return null;
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
            return () -> getTipFromBuilder(builder);
        }
        return null;
    }

    public static Component getTipComponent(ItemStack item) {
        return hasValidTipOverride(item) ? LegacyTipOverride.getOverride(item).getTip().orElse(Component.empty()) : LegacyTipBuilder.getTip(item);
    }

    public static Component getTipComponent(EntityType<?> type) {
        return hasValidTipOverride(type) ? LegacyTipOverride.getOverride(type).getTip().orElse(Component.empty()) : Component.translatable(LegacyTipBuilder.getTipId(type));
    }

    public static boolean hasTip(ItemStack item) {
        return hasTip(LegacyTipBuilder.getTip(item)) || hasValidTipOverride(item);
    }

    public static boolean hasValidTipOverride(ItemStack item) {
        return LegacyTipOverride.getOverride(item).getTip().isPresent() && (!LegacyTipOverride.getOverride(item).getItem().isEmpty() || hasTip(LegacyTipOverride.getOverride(item).getTip().get()));
    }

    public static boolean hasValidTipOverride(EntityType<?> type) {
        return LegacyTipOverride.getOverride(type).getTip().isPresent() && (!LegacyTipOverride.getOverride(type).getItem().isEmpty() || hasTip(LegacyTipOverride.getOverride(type).getTip().get()));
    }

    public static boolean hasTip(Component c) {
        return !(c.getContents() instanceof TranslatableContents t) || hasTip(t.getKey());
    }

    public static boolean hasTip(String s) {
        return Language.getInstance().has(s);
    }

    public static boolean hasTip(EntityType<?> s) {
        return hasTip(LegacyTipBuilder.getTipId(s)) || hasValidTipOverride(s);
    }

    public static Component getTipComponent(ResourceLocation location) {
        return Component.translatable(location.toLanguageKey() + ".tip");
    }

    public LegacyTip getLoadingTip() {
        return getLoadingTip(random);
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        loadingTips.clear();
        usingLoadingTips.clear();
        actualLoadingTip = null;
        resourceManager.getNamespaces().forEach(name -> resourceManager.getResource(FactoryAPI.createLocation(name, TIPS)).ifPresent(r -> {
            try (BufferedReader bufferedReader = r.openAsReader()) {
                JsonObject obj = GsonHelper.parse(bufferedReader);
                LegacyTipBuilder.LIST_CODEC.parse(JsonOps.INSTANCE, obj.get("loadingTips")).result().ifPresent(l -> l.forEach(b -> loadingTips.add(() -> getLoadingTipFromBuilder(b))));
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
