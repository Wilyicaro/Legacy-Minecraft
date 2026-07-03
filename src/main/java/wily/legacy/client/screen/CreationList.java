package wily.legacy.client.screen;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import org.apache.commons.io.FileUtils;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.factoryapi.base.client.UIDefinition;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.LegacyWorldTemplate;
import wily.legacy.client.PackAlbum;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.ScreenUtil;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Consumer;

public class CreationList extends RenderableVList{
    protected final Minecraft minecraft;
    private static final LoadingCache<String, ResourceLocation> packIcons = CacheBuilder.newBuilder().build(new CacheLoader<>() {
        @Override
        public ResourceLocation load(String key) throws Exception {
            java.nio.file.Path path = Minecraft.getInstance().getResourcePackDirectory().resolve(key).resolve("pack.png");
            if (!Files.isRegularFile(path)) return PackAlbum.Selector.DEFAULT_ICON;
            try (InputStream inputStream = Files.newInputStream(path)) {
                NativeImage image = NativeImage.read(inputStream);
                ResourceLocation location = Legacy4J.createModLocation("template_pack_icon/" + Integer.toHexString(key.hashCode()));
                Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(/*? if >=1.21.5 {*//*location::toString, *//*?}*/image));
                return location;
            }
        }
    });

    public CreationList(UIAccessor accessor) {
        super(accessor);
        layoutSpacing(l->0);
        minecraft = Minecraft.getInstance();
        addIconButton(this, Legacy4J.createModLocation("creation_list/create_world"),Component.translatable("legacy.menu.create_world"), c-> CreateWorldScreen.openFresh(this.minecraft, getScreen()));
        LegacyWorldTemplate.list.forEach(t-> addTemplateButton(this,t, c-> {
            if (t.isGamePath() && !Files.exists(t.getPath())){
                minecraft.setScreen(ConfirmationScreen.createInfoScreen(getScreen(), LegacyComponents.MISSING_WORLD_TEMPLATE, Component.translatable("legacy.menu.missing_world_template_message",t.buttonMessage())));
                return;
            }
            try (LevelStorageSource.LevelStorageAccess access = Legacy4JClient.getLevelStorageSource().createAccess(Legacy4JClient.importSaveFile(t.open(), minecraft.getLevelSource()::levelExists,Legacy4JClient.getLevelStorageSource(),t.folderName()))) {
                LevelSummary summary = access.getSummary(/*? if >1.20.2 {*/access.getDataTag()/*?}*/);
                access.close();
                if (t.directJoin()) {
                    Legacy4JClient.hideNextExperimentalWorldWarning(() -> LoadSaveScreen.loadWorld(getScreen(), minecraft, Legacy4JClient.getLevelStorageSource(), summary));
                } else minecraft.setScreen(new LoadSaveScreen(getScreen(),summary,access,t.isLocked()) {
                    @Override
                    public void onClose() {
                        if (!LegacyOptions.saveCache.get() || LegacyOptions.alwaysClearSaveCache.get()) FileUtils.deleteQuietly(access.getDimensionPath(Level.OVERWORLD).toFile());
                        super.onClose();
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    public static void addIconButton(RenderableVList list, ResourceLocation iconSprite, Component message, Consumer<AbstractButton> onPress){
        addIconButton(list,iconSprite,message,onPress,null);
    }

    public static void addIconButton(RenderableVList list, ResourceLocation iconSprite, Component message, Consumer<AbstractButton> onPress, Tooltip tooltip){
        AbstractButton button;
        list.addRenderable(button = new AbstractButton(0,0,270,30,message) {
            @Override
            protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
                super.renderWidget(guiGraphics, i, j, f);
                if (!list.accessor.getBoolean("allowButtonsWithIcons",true)) return;
                FactoryScreenUtil.enableBlend();
                FactoryGuiGraphics.of(guiGraphics).blitSprite(iconSprite, getX() + 5, getY() + 5, 20, 20);
                FactoryScreenUtil.disableBlend();
                if (Minecraft.getInstance().options.touchscreen().get().booleanValue() || isHovered) {
                    guiGraphics.fill(getX() + 5, getY() + 5, getX() + 25, getY() + 25, -1601138544);
                }
            }

            @Override
            protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
                ScreenUtil.renderScrollingString(guiGraphics, font, this.getMessage(), this.getX() + 35, this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), j, true);
            }

            @Override
            public void onPress() {
                onPress.accept(this);
            }

            @Override
            protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                defaultButtonNarrationText(narrationElementOutput);
            }
        });
        button.setTooltip(tooltip);
    }

    public static void addTemplateButton(RenderableVList list, LegacyWorldTemplate template, Consumer<AbstractButton> onPress){
        AbstractButton button;
        list.addRenderable(button = new AbstractButton(0,0,270,30,template.buttonMessage()) {
            @Override
            protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
                super.renderWidget(guiGraphics, i, j, f);
                if (!list.accessor.getBoolean("allowButtonsWithIcons",true)) return;
                FactoryScreenUtil.enableBlend();
                ResourceLocation icon = getTemplatePackIcon(template);
                if (icon == null) FactoryGuiGraphics.of(guiGraphics).blitSprite(template.icon(), getX() + 5, getY() + 5, 20, 20);
                else FactoryGuiGraphics.of(guiGraphics).blit(icon, getX() + 5, getY() + 5, 0.0f, 0.0f, 20, 20, 20, 20);
                FactoryScreenUtil.disableBlend();
                if (Minecraft.getInstance().options.touchscreen().get().booleanValue() || isHovered) {
                    guiGraphics.fill(getX() + 5, getY() + 5, getX() + 25, getY() + 25, -1601138544);
                }
            }

            @Override
            protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
                ScreenUtil.renderScrollingString(guiGraphics, font, this.getMessage(), this.getX() + 35, this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), j, true);
            }

            @Override
            public void onPress() {
                onPress.accept(this);
            }

            @Override
            protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                defaultButtonNarrationText(narrationElementOutput);
            }
        });
    }

    private static ResourceLocation getTemplatePackIcon(LegacyWorldTemplate template) {
        String packId = template.albumId()
            .map(PackAlbum::resourceById)
            .map(PackAlbum::getDisplayPackId)
            .orElse(null);
        if (packId == null || packId.isBlank()) return null;
        if (packId.startsWith("file/")) packId = packId.substring(5);
        try {
            return packIcons.getUnchecked(packId);
        } catch (Exception e) {
            return null;
        }
    }

}
