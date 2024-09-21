package wily.legacy;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.server.LanServerPinger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.util.HttpUtil;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;
import wily.legacy.block.entity.WaterCauldronBlockEntity;
import wily.legacy.client.*;
import wily.legacy.client.controller.Controller;
import wily.legacy.client.controller.ControllerManager;
import wily.legacy.client.screen.*;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.network.CommonNetwork;
import wily.legacy.network.PlayerInfoSync;
import wily.legacy.network.ServerOpenClientMenuPacket;
import wily.legacy.player.LegacyPlayerInfo;
import wily.legacy.util.JsonUtil;
import wily.legacy.util.MCAccount;
import wily.legacy.util.ModInfo;
import wily.legacy.util.ScreenUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static wily.legacy.Legacy4J.MOD_ID;
import static wily.legacy.init.LegacyRegistries.SHRUB;


public class Legacy4JClient {

    public static final CommonNetwork.SecureExecutor SECURE_EXECUTOR = new CommonNetwork.SecureExecutor() {
        @Override
        public boolean isSecure() {
            return Minecraft.getInstance().player != null;
        }
    };
    public static String lastLoadedVersion = "";
    public static LevelStorageSource currentWorldSource;
    public static GuiSpriteManager sprites;
    public static boolean isGameLoadFinished = false;
    public static boolean legacyFont = true;
    public static boolean forceVanillaFontShadowColor = false;
    public static boolean canLoadVanillaOptions = true;
    public static ResourceLocation defaultFontOverride = null;
    public static boolean manualSave = false;
    public static boolean saveExit = false;
    public static boolean retakeWorldIcon = false;
    public static final Map<Component, Component> OPTION_BOOLEAN_CAPTION = Map.of(Component.translatable("key.sprint"),Component.translatable("options.key.toggleSprint"),Component.translatable("key.sneak"),Component.translatable("options.key.toggleSneak"));
    public static LegacyLoadingScreen legacyLoadingScreen = new LegacyLoadingScreen();
    public static MultiBufferSource.BufferSource guiBufferSourceOverride = null;
    public static Renderable itemActivationRenderReplacement = null;
    public static final LegacyTipManager legacyTipManager = new LegacyTipManager();
    public static final LegacyCreativeTabListing.Manager legacyCreativeListingManager = new LegacyCreativeTabListing.Manager();
    public static final LegacyCraftingTabListing.Manager legacyCraftingListingManager = new LegacyCraftingTabListing.Manager();
    public static final LegacyBiomeOverride.Manager legacyBiomeOverrides = new LegacyBiomeOverride.Manager();
    public static final LegacyWorldTemplate.Manager legacyWorldTemplateManager = new LegacyWorldTemplate.Manager();
    public static final LegacyTipOverride.Manager legacyTipOverridesManager = new LegacyTipOverride.Manager();
    public static final LegacyResourceManager legacyResourceManager = new LegacyResourceManager();
    public static final StoneCuttingGroupManager stoneCuttingGroupManager = new StoneCuttingGroupManager();
    public static final LoomTabListing.Manager loomListingManager = new LoomTabListing.Manager();
    public static final ControlTooltip.GuiManager controlTooltipGuiManager = new ControlTooltip.GuiManager();
    public static final LeaderboardsScreen.Manager leaderBoardListingManager = new LeaderboardsScreen.Manager();

    public static final ControllerManager controllerManager = new ControllerManager(Minecraft.getInstance());

    public static KnownListing<Block> knownBlocks;
    public static KnownListing<EntityType<?>> knownEntities;
    public static GameType defaultServerGameType;
    public static Consumer<ServerPlayer> serverPlayerJoinConsumer;

    public static PostChain gammaEffect;
    public static int[] MAP_PLAYER_COLORS = new int[]{0xFFFFFF,0x00FF4C,0xFF2119,0x6385FF,0xFF63D9,0xFF9C00,0xFFFB19,0x63FFE4};
    public static float[] getVisualPlayerColor(LegacyPlayerInfo info){
        return getVisualPlayerColor(info.getPosition() >= 0 ? info.getPosition() : info.legacyMinecraft$getProfile().getId().hashCode());
    }
    public static float[] getVisualPlayerColor(int i){
        i = Math.abs(i);
        int baseColor = MAP_PLAYER_COLORS[i % MAP_PLAYER_COLORS.length];
        if (i < MAP_PLAYER_COLORS.length) return new float[]{(baseColor >> 16 & 255) / 255f,(baseColor >> 8 & 255) / 255f,(baseColor & 255) / 255f};
        float f = ((i - MAP_PLAYER_COLORS.length) % 101) / 250f;
        float r = ((baseColor >> 16 & 255) * (0.8f + f)) / 255f;
        float g = ((baseColor >> 8 & 255) * (0.8f +(f * 1.2f))) / 255f;
        float b = ((baseColor & 255) * (0.8f +(f / 2f))) / 255f;
        return new float[]{r,g,b};
    }
    public static void updateLegacyPlayerInfos(Map<UUID, LegacyPlayerInfo> map){
        Minecraft minecraft = Minecraft.getInstance();
        map.forEach((s,i)->{
            if (minecraft.getConnection() != null && minecraft.getConnection().getPlayerInfo(s) instanceof LegacyPlayerInfo info)
                info.copyFrom(i);
        });
        LeaderboardsScreen.refreshStatsBoards(minecraft);
        if (minecraft.screen instanceof LeaderboardsScreen s && LeaderboardsScreen.statsBoards.get(s.selectedStatBoard).statsList.isEmpty()) minecraft.executeIfPossible(()-> s.changeStatBoard(false));
        if (minecraft.player != null) CommonNetwork.sendToServer(new PlayerInfoSync(ScreenUtil.hasClassicCrafting() ? 1 : 2,minecraft.player));
    }
    public static void displayActivationAnimation(Renderable renderable){
        itemActivationRenderReplacement = renderable;
        Minecraft.getInstance().gameRenderer.displayItemActivation(ItemStack.EMPTY);
    }
    public static void applyFontOverrideIf(boolean b, ResourceLocation override, Consumer<Boolean> fontRender){
        if (b) defaultFontOverride = override;
        fontRender.accept(b);
        if (b) defaultFontOverride = null;
    }
    public static void saveLevel(LevelStorageSource.LevelStorageAccess storageSource){
        if (storageSource.getDimensionPath(Level.OVERWORLD).getParent().equals(Legacy4JClient.currentWorldSource.getBaseDir())) Legacy4JClient.copySaveBtwSources(storageSource,Minecraft.getInstance().getLevelSource());
    }
    public static void displayEffectActivationAnimation(MobEffect mobEffect){
        displayActivationAnimation(((guiGraphics, i, j, f) -> {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(0.5f,0.5f,0.5f);
            guiGraphics.blit(0, 1, 0, 1, -1, Minecraft.getInstance().getMobEffectTextures().get(mobEffect));
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.5f,0.5f,0);
            guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(180));
            guiGraphics.pose().translate(-0.5f,-0.5f,0);
            guiGraphics.blit(0, 0, 0, 1, 1, Minecraft.getInstance().getMobEffectTextures().get(mobEffect));
            guiGraphics.pose().popPose();
            guiGraphics.pose().popPose();
        }));
    }

    public static final Map<Optional<ResourceKey<WorldPreset>>,PresetEditor> VANILLA_PRESET_EDITORS = new HashMap<>(Map.of(Optional.of(WorldPresets.FLAT), (createWorldScreen, settings) -> {
        ChunkGenerator chunkGenerator = settings.selectedDimensions().overworld();
        RegistryAccess.Frozen registryAccess =  settings.worldgenLoadContext();
        HolderLookup.RegistryLookup<Biome> biomeGetter = registryAccess.lookupOrThrow(Registries.BIOME);
        HolderLookup.RegistryLookup<StructureSet> structureGetter = registryAccess.lookupOrThrow(Registries.STRUCTURE_SET);
        HolderLookup.RegistryLookup<PlacedFeature> placeFeatureGetter = registryAccess.lookupOrThrow(Registries.PLACED_FEATURE);
        return new LegacyFlatWorldScreen(createWorldScreen, createWorldScreen.getUiState(),biomeGetter, structureGetter, flatLevelGeneratorSettings -> createWorldScreen.getUiState().updateDimensions(PresetEditor.flatWorldConfigurator(flatLevelGeneratorSettings)), chunkGenerator instanceof FlatLevelSource ? ((FlatLevelSource)chunkGenerator).settings() : FlatLevelGeneratorSettings.getDefault(biomeGetter, structureGetter, placeFeatureGetter));
    }, Optional.of(WorldPresets.SINGLE_BIOME_SURFACE), (createWorldScreen, settings) -> new LegacyBuffetWorldScreen(createWorldScreen, settings.worldgenLoadContext().lookupOrThrow(Registries.BIOME), holder -> createWorldScreen.getUiState().updateDimensions(PresetEditor.fixedBiomeConfigurator(holder)))));

    public static Screen getReplacementScreen(Screen screen){
        if (screen instanceof JoinMultiplayerScreen)
            return new PlayGameScreen(new TitleScreen(),2);
        else if (screen instanceof DisconnectedScreen s)
            return ConfirmationScreen.createInfoScreen(getReplacementScreen(s.parent), s.getTitle(),s.reason);
        else if (screen instanceof AlertScreen s) {
            MultiLineLabel messageLines = MultiLineLabel.create(Minecraft.getInstance().font,s.messageText,200);
            return new ConfirmationScreen(Minecraft.getInstance().screen, 230, 97 + messageLines.getLineCount() * 12, s.getTitle(), messageLines, b -> true) {
                protected void addButtons() {
                    renderableVList.addRenderable(okButton = Button.builder(Component.translatable("gui.ok"), b -> s.callback.run()).bounds(panel.x + 15, panel.y + panel.height - 30, 200, 20).build());
                }
                public boolean shouldCloseOnEsc() {
                    return s.shouldCloseOnEsc();
                }
            };
        }else if (screen instanceof BackupConfirmScreen s) {
            MultiLineLabel messageLines = MultiLineLabel.create(Minecraft.getInstance().font,s.description,200);
            return new ConfirmationScreen(Minecraft.getInstance().screen, 230, 141 + messageLines.getLineCount() * 12 + (s.promptForCacheErase ? 14 : 0), s.getTitle(), messageLines, b -> true) {
                boolean eraseCache = false;
                protected void addButtons() {
                    if (s.promptForCacheErase) addRenderableWidget(new TickBox(panel.x + 15, panel.y + panel.height - 88,eraseCache,b->Component.translatable("selectWorld.backupEraseCache"),b->null,b-> eraseCache = b.selected));
                    okButton = addRenderableWidget(Button.builder(Component.translatable("selectWorld.backupJoinConfirmButton"), b -> s.listener.proceed(true, eraseCache)).bounds(panel.x + 15, panel.y + panel.height - 74, 200, 20).build());
                    addRenderableWidget(Button.builder(Component.translatable("selectWorld.backupJoinSkipButton"), b -> s.listener.proceed(false, eraseCache)).bounds(panel.x + 15, panel.y + panel.height - 52, 200, 20).build());
                    addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose()).bounds(panel.x + 15, panel.y + panel.height - 30, 200, 20).build());
                }
            };
        }
        return screen;
    }
    @ExpectPlatform
    public static void registerRenderType(RenderType renderType, Block... blocks) {
        throw new AssertionError();
    }
    @ExpectPlatform
    public static Screen getConfigScreen(ModInfo mod, Screen screen) {
        throw new AssertionError();
    }

    public static void registerKeyMappings(Consumer<KeyMapping> registry){
        registry.accept(keyCrafting);
        registry.accept(keyHostOptions);
        registry.accept(keyCycleHeldLeft);
        registry.accept(keyCycleHeldRight);
        registry.accept(keyToggleCursor);
        registry.accept(keyFlyUp);
        registry.accept(keyFlyDown);
        registry.accept(keyFlyLeft);
        registry.accept(keyFlyRight);
    }
    public static void registerReloadListeners(Consumer<PreparableReloadListener> registry){
        registry.accept(legacyTipManager);
        registry.accept(legacyCreativeListingManager);
        registry.accept(legacyCraftingListingManager);
        registry.accept(legacyWorldTemplateManager);
        registry.accept(legacyTipOverridesManager);
        registry.accept(legacyBiomeOverrides);
        registry.accept(legacyResourceManager);
        registry.accept(stoneCuttingGroupManager);
        registry.accept(loomListingManager);
        registry.accept(controlTooltipGuiManager);
        registry.accept(leaderBoardListingManager);
    }
    public static void serverSave(MinecraftServer server){
        Legacy4JClient.retakeWorldIcon = true;
        knownBlocks.save();
        knownEntities.save();
    }
    public static void clientStopping(Minecraft minecraft){
        knownBlocks.save();
        knownEntities.save();
        sprites.close();
        Assort.applyDefaultResourceAssort();
    }
    public static void preServerTick(MinecraftServer server){

    }
    public static void postTick(Minecraft minecraft){
        if (minecraft.level != null && minecraft.screen == null && ((LegacyOptions)minecraft.options).hints().get() && LegacyTipManager.getActualTip() == null) {
            HitResult hit = minecraft.hitResult;
            if (hit instanceof BlockHitResult blockHitResult) {
                BlockState state = minecraft.level.getBlockState(blockHitResult.getBlockPos());
                if (!state.isAir() && !(state.getBlock() instanceof LiquidBlock) && state.getBlock().asItem() instanceof BlockItem) {
                    if (!knownBlocks.contains(state.getBlock()) && LegacyTipManager.setTip(LegacyTipManager.getTip(state.getBlock().asItem().getDefaultInstance()))) knownBlocks.add(state.getBlock());
                }
            } else if (hit instanceof EntityHitResult r){
                Entity e = r.getEntity();
                if (!knownEntities.contains(e.getType()) && LegacyTipManager.setTip(LegacyTipManager.getTip(e))) knownEntities.add(e.getType());
            }
        }
    }
    public static void preTick(Minecraft minecraft){
        SECURE_EXECUTOR.executeAll();
        if (((LegacyOptions)minecraft.options).unfocusedInputs().get()) minecraft.setWindowActive(true);
        while (keyCrafting.consumeClick()){
            if (minecraft.gameMode != null && minecraft.gameMode.hasInfiniteItems()) {
                minecraft.setScreen(CreativeModeScreen.getActualCreativeScreenInstance(minecraft));
                continue;
            }
            if (ScreenUtil.hasClassicCrafting()) {
                minecraft.getTutorial().onOpenInventory();
                minecraft.setScreen(new InventoryScreen(minecraft.player));
            }else CommonNetwork.sendToServer(minecraft.hitResult != null && minecraft.hitResult instanceof BlockHitResult r && minecraft.level.getBlockState(r.getBlockPos()).getBlock() instanceof CraftingTableBlock ?
                    new ServerOpenClientMenuPacket(r.getBlockPos(),0) : new ServerOpenClientMenuPacket(1));
        }
        while (keyHostOptions.consumeClick()) {
            minecraft.setScreen(new HostOptionsScreen());
        }
        boolean left;
        while ((left=keyCycleHeldLeft.consumeClick()) || keyCycleHeldRight.consumeClick()){
            if (minecraft.player != null)  minecraft.player.getInventory().swapPaint(left ? 1 : -1);
        }
    }
    public static void postScreenInit(Screen screen){
        if (screen.getFocused() != null && !screen.children().contains(screen.getFocused())) screen.setFocused(null);

        if ((Minecraft.getInstance().getLastInputType().isKeyboard() || controllerManager.connectedController != null || controllerManager.getCursorMode() == 2) && controllerManager.getCursorMode() != 1) {
            Controller.Event e = Controller.Event.of(screen);
            if (e.disableCursorOnInit() && controllerManager.getCursorMode() != 1) controllerManager.disableCursor();
            if (controllerManager.isCursorDisabled && (!e.disableCursorOnInit() || controllerManager.getCursorMode() == 1)) controllerManager.enableAndResetCursor();
            if (screen.getFocused() == null || !screen.getFocused().isFocused()) {
                ComponentPath path = screen.nextFocusPath(new FocusNavigationEvent.ArrowNavigation(ScreenDirection.DOWN));
                if (path != null) path.applyFocus(true);
            }
        }
        controllerManager.resetCursor();
    }
    public static void clientPlayerJoin(LocalPlayer p){
        Minecraft minecraft = Minecraft.getInstance();
        JsonUtil.JSON_ITEMS.clear();
        LegacyCreativeTabListing.list.forEach(l-> l.displayItems().forEach(Supplier::get));
        LegacyCreativeTabListing.rebuildVanillaCreativeTabsItems(minecraft);
    }
    public static void serverPlayerJoin(ServerPlayer player){
        if (serverPlayerJoinConsumer != null) {
            serverPlayerJoinConsumer.accept(player);
            serverPlayerJoinConsumer = null;
        }
    }
    public static void init() {
        knownBlocks = new KnownListing<>(BuiltInRegistries.BLOCK,Minecraft.getInstance().gameDirectory.toPath());
        knownEntities = new KnownListing<>(BuiltInRegistries.ENTITY_TYPE,Minecraft.getInstance().gameDirectory.toPath());
        currentWorldSource = LevelStorageSource.createDefault(Minecraft.getInstance().gameDirectory.toPath().resolve("current-world"));
    }
    public static boolean isModEnabledOnServer(){
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.getConnection() != null && minecraft.getConnection().getPlayerInfo(minecraft.player.getUUID()) instanceof LegacyPlayerInfo i && i.getPosition() >= 0;
    }
    public static int getEffectiveRenderDistance(){
        return Minecraft.getInstance().options.getEffectiveRenderDistance();
    }
    public static void onClientPlayerInfoChange(){
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.player != null)
            CommonNetwork.sendToServer(new PlayerInfoSync(0, minecraft.player));
        if (minecraft.screen instanceof HostOptionsScreen s) s.reloadPlayerButtons();
        else if (minecraft.screen instanceof LeaderboardsScreen s){
            s.rebuildRenderableVList(minecraft);
            s.repositionElements();
        }
    }
    public static void setup() {
        registerRenderType(RenderType.cutoutMipped(), SHRUB.get());
        registerRenderType(RenderType.translucent(), Blocks.WATER);
        controllerManager.setup();
        MCAccount.loadAll();
    }
    public static void registerBlockColors(BiConsumer<BlockColor, Block> registry){
        registry.accept((blockState, blockAndTintGetter, blockPos, i) -> blockAndTintGetter == null || blockPos == null ? GrassColor.getDefaultColor() : BiomeColors.getAverageGrassColor(blockAndTintGetter, blockPos),SHRUB.get());
        registry.accept((blockState, blockAndTintGetter, blockPos, i) -> {
            if (blockAndTintGetter != null && blockPos != null){
                if (blockAndTintGetter.getBlockEntity(blockPos) instanceof WaterCauldronBlockEntity be){
                    if (be.potion != Potions.WATER) return PotionUtils.getColor(be.potion);
                    else if (be.waterColor != null) return be.waterColor;
                }
                return BiomeColors.getAverageWaterColor(blockAndTintGetter, blockPos);
            }
            return -1;
        }, Blocks.WATER_CAULDRON);
    }
    public static void registerItemColors(BiConsumer<ItemColor, Item> registry){
        registry.accept((item,i) ->0xFF3153AF, LegacyRegistries.WATER.get());
        registry.accept((itemStack, i) -> GrassColor.getDefaultColor(),SHRUB.get().asItem());
    }
    public interface MenuScreenRegister{
        <H extends AbstractContainerMenu, S extends Screen & MenuAccess<H>> void register(MenuType<? extends H> type, MenuScreens.ScreenConstructor<H, S> factory);
    }
    public static void registerScreen(MenuScreenRegister registry) {
        registry.register(LegacyRegistries.CRAFTING_PANEL_MENU.get(),LegacyCraftingScreen::craftingScreen);
        registry.register(LegacyRegistries.PLAYER_CRAFTING_PANEL_MENU.get(),LegacyCraftingScreen::playerCraftingScreen);
        registry.register(LegacyRegistries.LOOM_PANEL_MENU.get(),LegacyLoomScreen::new);
        registry.register(LegacyRegistries.STONECUTTER_PANEL_MENU.get(),LegacyStonecutterScreen::new);
        registry.register(LegacyRegistries.MERCHANT_MENU.get(),LegacyMerchantScreen::new);
    }
    public static final KeyMapping keyCrafting = new KeyMapping("legacy.key.crafting", InputConstants.KEY_E, "key.categories.inventory");
    public static final KeyMapping keyCycleHeldLeft = new KeyMapping("legacy.key.cycleHeldLeft", InputConstants.KEY_PAGEDOWN, "key.categories.inventory");
    public static final KeyMapping keyCycleHeldRight = new KeyMapping("legacy.key.cycleHeldRight", InputConstants.KEY_PAGEUP, "key.categories.inventory");
    public static final KeyMapping keyToggleCursor = new KeyMapping("legacy.key.toggleCursor", -1, "key.categories.misc");
    public static KeyMapping keyHostOptions = new KeyMapping( MOD_ID +".key.host_options", InputConstants.KEY_H, "key.categories.misc");
    public static KeyMapping keyFlyUp = new KeyMapping( MOD_ID +".key.flyUp", InputConstants.KEY_UP, "key.categories.movement");
    public static KeyMapping keyFlyDown = new KeyMapping( MOD_ID +".key.flyDown", InputConstants.KEY_DOWN, "key.categories.movement");
    public static KeyMapping keyFlyLeft = new KeyMapping( MOD_ID +".key.flyLeft", InputConstants.KEY_LEFT, "key.categories.movement");
    public static KeyMapping keyFlyRight = new KeyMapping( MOD_ID +".key.flyRight", InputConstants.KEY_RIGHT, "key.categories.movement");
    public static void resetVanillaOptions(Minecraft minecraft){
        canLoadVanillaOptions = false;
        minecraft.options = new Options(minecraft,minecraft.gameDirectory);
        minecraft.options.save();
        canLoadVanillaOptions = true;
    }
    public static String manageAvailableSaveDirName(Consumer<File> copy, Predicate<String> exists, LevelStorageSource source, String levelId){
        String destId = manageAvailableName(exists,levelId);
        copy.accept(source.getLevelPath(destId).toFile());
        return destId;
    }
    public static String manageAvailableName(Predicate<String> exists, String saveDirName){
        StringBuilder builder = new StringBuilder(saveDirName);
        int repeat = 0;
        while (exists.test(builder +(repeat > 0 ? String.format(" (%s)",repeat) : "")))
            repeat++;
        if (repeat > 0)
            builder.append(String.format(" (%s)",repeat));
        return builder.toString();
    }
    public static String importSaveFile(InputStream saveInputStream,Predicate<String> exists, LevelStorageSource source, String saveDirName){
        return manageAvailableSaveDirName(f-> Legacy4J.copySaveToDirectory(saveInputStream,f),exists,source,saveDirName);
    }
    public static String importSaveFile(InputStream saveInputStream, LevelStorageSource source, String saveDirName){
        return importSaveFile(saveInputStream,source::levelExists,source,saveDirName);
    }
    public static String copySaveFile(Path savePath, LevelStorageSource source, String saveDirName){
        return manageAvailableSaveDirName(f-> {
            try {
                FileUtils.copyDirectory(savePath.toFile(),f);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        },source::levelExists,source,saveDirName);
    }
    public static void copySaveBtwSources(LevelStorageSource.LevelStorageAccess sendSource, LevelStorageSource destSource){
        try (LevelStorageSource.LevelStorageAccess access = destSource.createAccess(sendSource.getLevelId())) {
            File destLevelDirectory = access.getDimensionPath(Level.OVERWORLD).toFile();
            if (destLevelDirectory.exists()) FileUtils.deleteQuietly(destLevelDirectory);
            FileUtils.copyDirectory(sendSource.getDimensionPath(Level.OVERWORLD).toFile(), destLevelDirectory, p -> !p.getName().equals("session.lock"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void registerExtraModels(Consumer<ResourceLocation> register){

    }
    public static Pair<Integer,Component> tryParsePort(String string) {
        if (string.isBlank())
            return Pair.of(HttpUtil.getAvailablePort(),null);
        try {
            int port = Integer.parseInt(string);
            if (port < 1024 || port > 65535) {
                return Pair.of(port,Component.translatable("lanServer.port.invalid.new", 1024, 65535));
            }
            if (!HttpUtil.isPortAvailable(port)) {
                return Pair.of(port,Component.translatable("lanServer.port.unavailable.new", 1024, 65535));
            }
            return Pair.of(port,null);
        } catch (NumberFormatException numberFormatException) {
            return  Pair.of(HttpUtil.getAvailablePort(),Component.translatable("lanServer.port.invalid.new", 1024, 65535));
        }
    }
    public static boolean publishUnloadedServer(Minecraft minecraft, @Nullable GameType gameType, boolean bl, int i) {
        IntegratedServer server = minecraft.getSingleplayerServer();
        try {
            minecraft.prepareForMultiplayer();
            minecraft.getProfileKeyPairManager().prepareKeyPair().thenAcceptAsync((optional) -> {
                optional.ifPresent((profileKeyPair) -> {
                    ClientPacketListener clientPacketListener = minecraft.getConnection();
                    if (clientPacketListener != null) {
                        clientPacketListener.setKeyPair(profileKeyPair);
                    }

                });
            }, minecraft);
            server.getConnection().startTcpServerListener(null, i);
            Legacy4J.LOGGER.info("Started serving on {}", i);
            server.publishedPort = i;
            server.lanPinger = new LanServerPinger(server.getMotd(), "" + i);
            server.lanPinger.start();
            server.publishedGameType = gameType;
            server.getPlayerList().setAllowCheatsForAllPlayers(bl);
            return true;
        } catch (IOException var7) {
            return false;
        }
    }

}