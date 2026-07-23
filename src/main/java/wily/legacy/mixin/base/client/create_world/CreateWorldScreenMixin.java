package wily.legacy.mixin.base.client.create_world;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.*;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.base.Bearer;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyClientWorldSettings;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.PackAlbum;
import wily.legacy.client.RemoteResourceAlbums;
import wily.legacy.client.screen.*;
import wily.legacy.util.ScreenUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin extends Screen implements ControlTooltip.Event{
    @Shadow @Final private WorldCreationUiState uiState;
    @Shadow @Final private static Component GAME_MODEL_LABEL;
    @Shadow @Final private static Component NAME_LABEL;


    @Shadow public abstract void popScreen();

    @Shadow protected abstract void onCreate();

    protected Bearer<Boolean> trustPlayers = Bearer.of(true);
    protected Bearer<ResourceKey<WorldPreset>> legacyBiomeScale = Bearer.of(WorldPresets.NORMAL);
    protected Panel panel;
    protected PublishScreen publishScreen;
    protected TickBox onlineTickBox;
    protected PackAlbum.Selector resourceAssortSelector;
    @Unique
    private boolean legacy$preparingRemoteResourceAlbum;
    @Unique
    private boolean legacy$resourceAlbumPrepared;

    protected CreateWorldScreenMixin(Component component) {
        super(component);
    }
    private CreateWorldScreen self(){
        return (CreateWorldScreen) (Object) this;
    }

    @Inject(method = "<init>",at = @At("RETURN"))
    public void initReturn(Minecraft minecraft, Screen screen, WorldCreationContext worldCreationContext, Optional optional, OptionalLong optionalLong,/*? if >=1.21.2 {*/ /*CreateWorldCallback createWorldCallback, *//*?}*/ CallbackInfo ci){
        LegacyOptions.resetAdvancedWorldOptions();
        uiState.setDifficulty(LegacyOptions.createWorldDifficulty.get());
        panel = Panel.createPanel(this, p-> (width - (p.width + (ScreenUtil.hasTooltipBoxes(UIAccessor.of(this)) ? PackAlbum.Selector.getDefaultWidth() : 0))) / 2, p-> (height - p.height) / 2, 245, 228);
        resourceAssortSelector = PackAlbum.Selector.creationResources(panel.x + 13, panel.y + 106, 220,45, !ScreenUtil.hasTooltipBoxes());
        publishScreen = new PublishScreen(this, uiState.getGameMode().gameType);
        legacyBiomeScale.set(WorldMoreOptionsScreen.getLegacyBiomeScalePreset(uiState.getWorldType().preset()));
    }

    @Override
    public void added() {
        super.added();
        OptionsScreen.setupSelectorControlTooltips(ControlTooltip.Renderer.of(this), this);
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    public void init(CallbackInfo ci) {
        ci.cancel();
        UIAccessor accessor = UIAccessor.of(this);
        panel.init();
        int layoutX = accessor.getInteger("layout.x", panel.x + 13);
        int layoutWidth = accessor.getInteger("layout.width", 220);
        addRenderableOnly(panel);

        EditBox nameEdit = new EditBox(font, layoutX, accessor.getInteger("nameEditBox.y", panel.y + 25), layoutWidth, accessor.getInteger("nameEditBox.height", 20), Component.translatable("selectWorld.enterName"));
        nameEdit.setValue(uiState.getName());
        nameEdit.setResponder(uiState::setName);
        uiState.addListener(worldCreationUiState -> nameEdit.setTooltip(Tooltip.create(Component.translatable("selectWorld.targetFolder", Component.literal(worldCreationUiState.getTargetFolder()).withStyle(ChatFormatting.ITALIC)))));
        setInitialFocus(nameEdit);
        addRenderableWidget(accessor.putWidget("nameEditBox", nameEdit));

        LegacySliderButton<WorldCreationUiState.SelectedGameMode> gameModeButton = addRenderableWidget(accessor.putWidget("gameTypeSlider", new LegacySliderButton<>(layoutX, accessor.getInteger("gameTypeSlider.y", panel.y + 51), layoutWidth, accessor.getInteger("gameTypeSlider.height", 16), b -> b.getDefaultMessage(GAME_MODEL_LABEL,b.getObjectValue().displayName),b->Tooltip.create(uiState.getGameMode().getInfo()),uiState.getGameMode(),()-> List.of(WorldCreationUiState.SelectedGameMode.SURVIVAL, WorldCreationUiState.SelectedGameMode.HARDCORE, WorldCreationUiState.SelectedGameMode.CREATIVE), b->uiState.setGameMode(b.getObjectValue()))));
        uiState.addListener(worldCreationUiState -> gameModeButton.active = !worldCreationUiState.isDebug());

        LegacySliderButton<Difficulty> difficultyButton = addRenderableWidget(accessor.putWidget("difficultySlider", new LegacySliderButton<>(layoutX, accessor.getInteger("difficultySlider.y", panel.y + 77), layoutWidth, accessor.getInteger("difficultySlider.height", 16), b -> b.getDefaultMessage(Component.translatable("options.difficulty"),b.getObjectValue().getDisplayName()),b->Tooltip.create(uiState.getDifficulty().getInfo()),uiState.getDifficulty(),()-> Arrays.asList(Difficulty.values()), b->uiState.setDifficulty(b.getObjectValue()))));
        uiState.addListener(worldCreationUiState -> {
            difficultyButton.setObjectValue(uiState.getDifficulty());
            difficultyButton.active = !uiState.isHardcore();
        });

        addRenderableWidget(accessor.putWidget("moreOptionsButton", Button.builder(Component.translatable("createWorld.tab.more.title"), button -> minecraft.setScreen(new WorldMoreOptionsScreen(self(), trustPlayers, Bearer.of(() -> publishScreen.publish, b -> publishScreen.publish = b), legacyBiomeScale))).bounds(layoutX, accessor.getInteger("moreOptionsButton.y", panel.y + 172), layoutWidth, accessor.getInteger("moreOptionsButton.height", 20)).build()));
        addRenderableWidget(accessor.putWidget("createButton", Button.builder(Component.translatable("selectWorld.create"), button -> legacy$createWorld()).bounds(layoutX, accessor.getInteger("createButton.y", panel.y + 197), layoutWidth, accessor.getInteger("createButton.height", 20)).build()));

        onlineTickBox = addRenderableWidget(accessor.putWidget("onlineTickBox", new TickBox(layoutX + 1, accessor.getInteger("onlineTickBox.y", panel.y + 155), layoutWidth, publishScreen.publish, b-> PublishScreen.getPublishComponent(), b->PublishScreen.getPublishTooltip(), button -> {
            if (LegacyOptions.useLegacyWorldOptions()) {
                if (button.selected) publishScreen.setGameType(uiState.getGameMode().gameType);
                publishScreen.publish = button.selected;
                return;
            }
            if (!button.selected) {
                publishScreen.publish = false;
                return;
            }
            publishScreen.setGameType(uiState.getGameMode().gameType);
            minecraft.setScreen(publishScreen);
        }, () -> publishScreen.publish)));
        resourceAssortSelector.setX(layoutX);
        resourceAssortSelector.setY(accessor.getInteger("resourceAlbumSelector.y", panel.y + 106));
        resourceAssortSelector.setWidth(layoutWidth);
        addRenderableWidget(accessor.putWidget("resourceAlbumSelector", resourceAssortSelector));
        this.uiState.onChanged();
    }

    @Inject(method = "repositionElements", at = @At("HEAD"), cancellable = true)
    public void repositionElements(CallbackInfo ci) {
        rebuildWidgets();
        ci.cancel();
    }

    @Inject(method = /*? if >=1.21.2 {*/ /*"createWorldAndCleanup"*//*?} else {*/"createNewWorld"/*?}*/,at = @At("RETURN"))
    private void onCreate(CallbackInfo ci) {
        resourceAssortSelector.applyChanges(!legacy$resourceAlbumPrepared);
        legacy$resourceAlbumPrepared = false;
        Legacy4JClient.serverPlayerJoinConsumer = s->{
            LegacyClientWorldSettings.of(s.server.getWorldData()).setTrustPlayers(trustPlayers.get());
            s.server.getPlayerList().sendPlayerPermissionLevel(s);
            publishScreen.setGameType(uiState.getGameMode().gameType);
            publishScreen.publish((IntegratedServer) s.server);
            LegacyClientWorldSettings.of(minecraft.getSingleplayerServer().getWorldData()).setSelectedResourceAlbum(resourceAssortSelector.getSelectedAlbum());
        };
    }

    @Unique
    private void legacy$createWorld() {
        if (legacy$preparingRemoteResourceAlbum) return;
        legacy$resourceAlbumPrepared = false;
        Optional<CompletableFuture<PackAlbum>> install = RemoteResourceAlbums.install(resourceAssortSelector.getSelectedAlbum());
        if (install.isEmpty()) {
            onCreate();
            return;
        }
        legacy$preparingRemoteResourceAlbum = true;
        LegacyLoadingScreen loadingScreen = new LegacyLoadingScreen();
        loadingScreen.setGenericLoading(true);
        loadingScreen.setBlackBackground(true);
        minecraft.setScreen(loadingScreen);
        install.get().whenComplete((installedAlbum, throwable) -> minecraft.execute(() -> {
            if (throwable != null || installedAlbum == null) {
                legacy$finishRemoteResourceAlbum(throwable == null ? new IOException("Failed to install resource pack") : throwable);
                return;
            }
            try {
                minecraft.getResourcePackRepository().reload();
                resourceAssortSelector.selectAlbum(installedAlbum);
                resourceAssortSelector.applyChanges(false);
                PackAlbum.updateSavedResourcePacks();
                minecraft.reloadResourcePacks().whenComplete((unused, reloadThrowable) -> minecraft.execute(() -> legacy$finishRemoteResourceAlbum(reloadThrowable)));
            } catch (Throwable preparationThrowable) {
                legacy$finishRemoteResourceAlbum(preparationThrowable);
            }
        }));
    }

    @Unique
    private void legacy$finishRemoteResourceAlbum(@Nullable Throwable throwable) {
        legacy$preparingRemoteResourceAlbum = false;
        if (throwable != null) {
            Legacy4J.LOGGER.warn("Failed to prepare remote resource album before world creation", throwable);
            minecraft.setScreen(self());
            return;
        }
        legacy$resourceAlbumPrepared = true;
        onCreate();
    }

    @ModifyExpressionValue(method = "createNewWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;createWorldOpenFlows()Lnet/minecraft/client/gui/screens/worldselection/WorldOpenFlows;"))
    private WorldOpenFlows createNewWorld(WorldOpenFlows original) {
        return LegacyOptions.saveCache.get() ? new WorldOpenFlows(minecraft,Legacy4JClient.currentWorldSource) : original;
    }

    @Inject(method = "createNewWorldDirectory", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/LevelStorageSource;createAccess(Ljava/lang/String;)Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;"))
    private /*? >=1.21.2 {*//*static*//*?}*/ void createNewWorldDirectory(/*? >=1.21.2 {*//*Minecraft minecraft, String string, @Nullable Path path, *//*?}*/CallbackInfoReturnable<Optional<LevelStorageSource.LevelStorageAccess>> cir) {
        if (!LegacyOptions.saveCache.get()) return;
        try {
            LevelStorageSource.LevelStorageAccess access = Legacy4JClient.currentWorldSource.createAccess(/*? <1.21.2 {*/uiState.getTargetFolder()/*?} else {*//*string*//*?}*/);
            access.close();
            if (Files.exists(access.getDimensionPath(Level.OVERWORLD))) FileUtils.deleteDirectory(access.getDimensionPath(Level.OVERWORLD).toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @ModifyExpressionValue(method = "createNewWorldDirectory", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getLevelSource()Lnet/minecraft/world/level/storage/LevelStorageSource;"))
    private /*? if >=1.21.2 {*//*static*//*?}*/ LevelStorageSource createNewWorldDirectory(LevelStorageSource original) {
        return LegacyOptions.saveCache.get() ? Legacy4JClient.currentWorldSource : original;
    }

    //? if >1.20.1 {
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
    }
    //?}

    @Override
    public boolean mouseScrolled(double d, double e/*? if >1.20.1 {*/, double f/*?}*/, double g) {
        if (resourceAssortSelector.scrollableRenderer.mouseScrolled(g)) return true;
        return super.mouseScrolled(d, e/*? if >1.20.1 {*/, f/*?}*/, g);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        if (onlineTickBox != null) onlineTickBox.updateValue();
        ScreenUtil.renderDefaultBackground(UIAccessor.of(this), guiGraphics, false);
        resourceAssortSelector.renderTooltipBox(guiGraphics,panel);
        super.render(guiGraphics, i, j, f);
        UIAccessor accessor = UIAccessor.of(this);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(accessor.getInteger("nameLabel.x", panel.x + 14), accessor.getInteger("nameLabel.y", panel.y + 15), 0);
        ScreenUtil.applySDFont(ignored -> guiGraphics.drawString(font,NAME_LABEL, 0, 0, CommonColor.INVENTORY_GRAY_TEXT.get(),false));
        guiGraphics.pose().popPose();
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (i == InputConstants.KEY_O && LegacyOptions.revealAdvancedWorldOptions()) {
            onlineTickBox.setMessage(PublishScreen.getPublishComponent());
            onlineTickBox.updateMessage();
            return true;
        }
        return super.keyPressed(i,j,k);
    }

    @Override
    public void onClose() {
        ScreenUtil.playBackSound();
        popScreen();
    }
}
