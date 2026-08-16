package wily.legacy.client.screen;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.FactoryAPIPlatform;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.AdvancedTextWidget;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.WidgetAccessor;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.factoryapi.util.ModInfo;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.PackAlbum;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;

public class ModsScreen extends PanelVListScreen {
    public static final Function<ModInfo, SizedLocation> modLogosCache = Util.memoize(mod->{
        Optional<String> opt = mod.getLogoFile(100);
        if (opt.isPresent() && mod.findResource(opt.get()).isPresent())
            try {
                NativeImage image = NativeImage.read(Files.newInputStream(mod.findResource(opt.get()).get()));
                ResourceLocation location = FactoryAPI.createLocation(mod.getId(),opt.get().toLowerCase(Locale.ENGLISH));
                Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(/*? if >=1.21.5 {*//*location::toString, *//*?}*/image));
                if (location != null) return new SizedLocation(location,image.getWidth(),image.getHeight());
            } catch (IOException e) {
            }
        ResourceLocation defaultLogo = PackAlbum.Selector.DEFAULT_ICON;
        if (mod.getId().equals("minecraft")) defaultLogo = PackAlbum.Selector.getPackIcon(Minecraft.getInstance().getResourcePackRepository().getPack("vanilla"));
        return new SizedLocation(defaultLogo,1,1);
    });
    protected final Panel panelRecess;
    protected final Panel tooltipBox = Panel.tooltipBoxOf(panel, 192);
    protected final Stocker.Sizeable sorting = new Stocker.Sizeable(1, 1);
    protected ScrollableRenderer scrollableRenderer = new ScrollableRenderer(new LegacyScrollRenderer());
    protected ModInfo focusedMod;
    protected final LoadingCache<ModInfo, AdvancedTextWidget> modLabelsCache = CacheBuilder.newBuilder().build(new CacheLoader<>() {
        @Override
        public AdvancedTextWidget load(ModInfo key) {
            List<Component> components = new ArrayList<>();
            SizedLocation logo = modLogosCache.apply(key);
            if (logo != null && logo.getScaledWidth(28) >= 120) {
                components.add(Component.literal(focusedMod.getName()));
                components.add(Component.translatable("legacy.menu.mods.id", focusedMod.getId()));
            }
            if (!key.getAuthors().isEmpty())
                components.add(Component.translatable("legacy.menu.mods.authors", String.join(", ", key.getAuthors())));
            if (!key.getCredits().isEmpty())
                components.add(Component.translatable("legacy.menu.mods.credits", String.join(", ", key.getCredits())));
            key.getHomepage().ifPresent(s -> components.add(Component.translatable("legacy.menu.mods.homepage", s).withStyle(urlClickStyle(s))));
            key.getIssues().ifPresent(s -> components.add(Component.translatable("legacy.menu.mods.issues", s).withStyle(urlClickStyle(s))));
            key.getSources().ifPresent(s -> components.add(Component.translatable("legacy.menu.mods.sources", s).withStyle(urlClickStyle(s))));
            if (key.getLicense() != null && !key.getLicense().isEmpty())
                components.add(Component.translatable("legacy.menu.mods.license", String.join(", ", key.getLicense())));
            components.add(Component.literal(key.getDescription()));
            return new AdvancedTextWidget(accessor).withLines(LegacyRenderUtil.getAppendedComponent(components), tooltipBox.getWidth() - 16);
        }
    });
    public ModsScreen(Screen parent) {
        super(parent, 282, 243, Component.empty());
        renderableVList.layoutSpacing(l -> 0);
        fillMods();
        panelRecess = Panel.createPanel(this, p -> p.appearance(LegacySprites.PANEL_RECESS, panel.width - 14, panel.height - 14), p -> p.pos(panel.x + 7, panel.y + 7));
    }

    public static Style urlClickStyle(String url){
        return Style.EMPTY.withClickEvent(/*? if <1.21.5 {*/new ClickEvent(ClickEvent.Action.OPEN_URL, url)/*?} else {*//*new ClickEvent.OpenUrl(URI.create(url))*//*?}*/);
    }

    public void fillMods() {
        Collection<ModInfo> mods = FactoryAPIPlatform.getMods();
        if (sorting.get() != 0) mods = mods.stream().sorted(Comparator.comparing(ModInfo::getName)).toList();
        mods.forEach(mod -> {
            if (mod.isHidden()) return;
            renderableVList.addRenderable(new ListButton(renderableVList, 0, 0, 260, 30, Component.literal(mod.getName())) {
                @Override
                public void onPress() {
                    if (isFocused()) {
                        Screen config = FactoryAPIClient.getConfigScreen(mod, ModsScreen.this);
                        if (config != null) minecraft.setScreen(config);
                    }
                }

                @Override
                protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
                    super.renderWidget(guiGraphics, i, j, f);
                    if (isFocused()) focusedMod = mod;
                    FactoryScreenUtil.enableBlend();
                    SizedLocation logo = modLogosCache.apply(mod);
                    if (logo != null) {
                        int iconHeight = accessor.getInteger(getRenderableVList().name + ".buttonIcon.size", 20);
                        int iconPos = (height - iconHeight) / 2;
                        FactoryGuiGraphics.of(guiGraphics).blit(logo.location, getX() + iconPos, getY() + iconPos, 0, 0, logo.getScaledWidth(iconHeight), iconHeight, logo.getScaledWidth(iconHeight), iconHeight);
                    }

                    FactoryScreenUtil.disableBlend();
                }

                @Override
                protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
                    LegacyFontUtil.applySDFont(sd -> {
                        SizedLocation logo = modLogosCache.apply(mod);
                        int iconHeight = accessor.getInteger(getRenderableVList().name + ".buttonIcon.size", 20);
                        int iconPos = (height - iconHeight) / 2;
                        int x = this.getX() + iconPos + accessor.getInteger(getRenderableVList().name + ".buttonMessage.xOffset", 10) + (logo == null ? iconHeight : logo.getScaledWidth(iconHeight));
                        LegacyRenderUtil.renderScrollingString(guiGraphics, font, this.getMessage(), x, this.getY(), getX() + this.getWidth() - i, this.getY() + this.getHeight(), j, true);
                    });
                }

            });
        });
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        LegacyRenderUtil.renderDefaultBackground(accessor, guiGraphics, false);
        tooltipBox.render(guiGraphics, i, j, f);
        if (focusedMod != null) {
            AdvancedTextWidget label = modLabelsCache.getUnchecked(focusedMod).withPos(panel.x + panel.width + 5, panel.y + 41);
            scrollableRenderer.scrolled.max = Math.max(0, Mth.ceil((label.getHeight() - (tooltipBox.getHeight() - 50)) / 12f));
            SizedLocation logo = modLogosCache.apply(focusedMod);
            int x = panel.x + panel.width + (logo == null ? 5 : logo.getScaledWidth(28) + 10);
            if (logo != null)
                FactoryGuiGraphics.of(guiGraphics).blit(logo.location, panel.x + panel.width + 5, panel.y + 10, 0.0f, 0.0f, logo.getScaledWidth(28), 28, logo.getScaledWidth(28), 28);
            if (logo == null || logo.getScaledWidth(28) < 120) {
                LegacyRenderUtil.renderScrollingString(guiGraphics, font, Component.translatable("legacy.menu.mods.id", focusedMod.getId()), x, panel.y + 12, panel.x + panel.width + 185, panel.y + 24, 0xFFFFFFFF, true);
                LegacyRenderUtil.renderScrollingString(guiGraphics, font, Component.translatable("legacy.menu.mods.version", focusedMod.getVersion()), x, panel.y + 24, panel.x + panel.width + 185, panel.y + 36, 0xFFFFFFFF, true);
            }
            scrollableRenderer.render(guiGraphics, panel.x + panel.width + 5, panel.y + 38, tooltipBox.getWidth() - 16, tooltipBox.getHeight() - 50, () -> label.render(guiGraphics, i, j + Math.round(scrollableRenderer.getYOffset()), f));
        }
    }

    @Override
    public boolean mouseClicked(double d, double e, int i) {
        if (focusedMod != null && LegacyRenderUtil.isMouseOver(d, e, panel.x + panel.width + 5, panel.y + 38, tooltipBox.getWidth() - 16, tooltipBox.getHeight() - 50)) {
            AdvancedTextWidget label = modLabelsCache.getUnchecked(focusedMod);
            if (label.mouseClicked(d, e + scrollableRenderer.getYOffset(), i)) return true;
        }
        return super.mouseClicked(d, e, i);
    }

    @Override
    public boolean mouseScrolled(double d, double e/*? if >1.20.1 {*/, double f/*?}*/, double g) {
        if ((tooltipBox.isHovered(d, e) || !ControlType.getActiveType().isKbm()) && scrollableRenderer.mouseScrolled(g))
            return true;
        return super.mouseScrolled(d, e/*? if >1.20.1 {*/, f/*?}*/, g);
    }

    @Override
    protected void panelInit() {
        super.panelInit();
        tooltipBox.init();
        panelRecess.init("panelRecess");
        addRenderableOnly(panelRecess);
    }

    @Override
    public void renderableVListInit() {
        int buttonsHeight = accessor.getInteger("buttonsHeight", 30);
        for (Renderable renderable : getRenderableVList().renderables) {
            if (renderable instanceof AbstractWidget widget) {
                //? if <=1.20.1 {
                /*((WidgetAccessor) widget).setHeight(accessor.getInteger("buttonsHeight", 30));
                *///?} else {
                widget.setHeight(accessor.getInteger("buttonsHeight", 30));
                //?}
            }
        }
        getRenderableVList().init(panel.x + 11, panel.y + 11, panel.width - 22, panel.height - 21);
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (i == InputConstants.KEY_X && sorting.add(1,true) != 0){
            renderableVList.renderables.clear();
            fillMods();
            repositionElements();
            return true;
        }
        return super.keyPressed(i, j, k);
    }

    @Override
    public void addControlTooltips(ControlTooltip.Renderer renderer) {
        super.addControlTooltips(renderer);
        renderer.add(() -> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_X) : ControllerBinding.LEFT_BUTTON.getIcon(), () -> Component.translatable("legacy.menu.sorting", this.sorting.get() == 0 ? LegacyComponents.NONE : LegacyComponents.ALPHABETICAL));
    }

    public record SizedLocation(ResourceLocation location, int width, int height) {
        public int getScaledWidth(int height) {
            return (int) (height * ((float) width() / height()));
        }

        public int getScaledHeight(int width) {
            return (int) (width * ((float) height() / width()));
        }
    }
}
