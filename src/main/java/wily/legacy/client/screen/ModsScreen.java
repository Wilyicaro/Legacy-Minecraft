package wily.legacy.client.screen;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.FactoryAPIPlatform;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.util.ModInfo;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.ControlType;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ModsScreen extends PanelVListScreen {
    protected final Map<ModInfo, SizedLocation> modLogosCache = new ConcurrentHashMap<>();
    protected ScrollableRenderer scrollableRenderer =  new ScrollableRenderer(new LegacyScrollRenderer());

    public record SizedLocation(ResourceLocation location, int width, int height){
        public int getScaledWidth(int height){
            return (int) (height * ((float) width / height()));
        }
    }
    protected final Panel tooltipBox = Panel.tooltipBoxOf(panel,192);
    protected final Stocker.Sizeable sorting = new Stocker.Sizeable(0,1);
    public static final Component ALPHABETICAL = Component.translatable("legacy.menu.sorting.alphabetical");

    protected ModInfo focusedMod;
    protected final LoadingCache<ModInfo, List<FormattedCharSequence>> modLabelsCache = CacheBuilder.newBuilder().build(new CacheLoader<>() {
        @Override
        public List<FormattedCharSequence> load(ModInfo key) {
            List<Component> components = new ArrayList<>();
            SizedLocation logo = modLogosCache.get(key);
            if (logo != null && logo.getScaledWidth(28) >= 120){
                components.add(Component.literal(focusedMod.getName()));
                components.add(Component.translatable("legacy.menu.mods.id", focusedMod.getId()));
            }
            if (!key.getAuthors().isEmpty())
                components.add(Component.translatable("legacy.menu.mods.authors", String.join(", ", key.getAuthors())));
            if (!key.getCredits().isEmpty())
                components.add(Component.translatable("legacy.menu.mods.credits", String.join(", ", key.getCredits())));
            key.getHomepage().ifPresent(s-> components.add(Component.translatable("legacy.menu.mods.homepage",s).withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, s)))));
            key.getIssues().ifPresent(s-> components.add(Component.translatable("legacy.menu.mods.issues",s).withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, s)))));
            key.getSources().ifPresent(s-> components.add(Component.translatable("legacy.menu.mods.sources",s).withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, s)))));
            if (key.getLicense() != null && !key.getLicense().isEmpty()) components.add(Component.translatable("legacy.menu.mods.license", String.join(", ", key.getLicense())));
            components.add(Component.literal(key.getDescription()));
            MultilineTooltip tooltip = new MultilineTooltip(components,tooltipBox.getWidth() - 16);
            return tooltip.toCharSequence(minecraft);
        }
    });
    public ModsScreen(Screen parent) {
        super(parent,282,243, Component.empty());
        renderableVList.layoutSpacing(l->0);
        fillMods();
    }
    public void fillMods(){
        Collection<ModInfo> mods = FactoryAPIPlatform.getMods();
        if (sorting.get() != 0) mods = mods.stream().sorted(Comparator.comparing(ModInfo::getName)).toList();
        mods.forEach(mod->{
            if (mod.isHidden()) return;
            renderableVList.addRenderable(new AbstractButton(0,0,260,30, Component.literal(mod.getName())) {
                @Override
                public void onPress() {
                    if (isFocused()){
                        net.minecraft.client.gui.screens.Screen config = Legacy4JClient.getConfigScreen(mod,ModsScreen.this);
                        if (config != null) minecraft.setScreen(config);
                    }
                }

                @Override
                protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
                    super.renderWidget(guiGraphics, i, j, f);
                    if (isFocused()) focusedMod = mod;
                    RenderSystem.enableBlend();
                    SizedLocation logo = modLogosCache.computeIfAbsent(mod, m-> {
                        Optional<String> opt = m.getLogoFile(100);
                        if (opt.isPresent() && mod.findResource(opt.get()).isPresent())
                            try {
                                NativeImage image = NativeImage.read(Files.newInputStream(mod.findResource(opt.get()).get()));
                                ResourceLocation location = FactoryAPI.createLocation(mod.getId(),opt.get().toLowerCase(Locale.ENGLISH));
                                minecraft.getTextureManager().register(location, new DynamicTexture(image));
                                if (location != null) return new SizedLocation(location,image.getWidth(),image.getHeight());
                            } catch (IOException e) {
                            }
                        ResourceLocation defaultLogo = Assort.Selector.DEFAULT_ICON;
                        if (mod.getId().equals("minecraft")) defaultLogo = Assort.Selector.getPackIcon(minecraft.getResourcePackRepository().getPack("vanilla"));
                        return new SizedLocation(defaultLogo,1,1);
                    });
                    if (logo != null) FactoryGuiGraphics.of(guiGraphics).blit(logo.location,getX() + 5, getY() + 5, 0,0, logo.getScaledWidth(20),20,logo.getScaledWidth(20),20);
                    
                    RenderSystem.disableBlend();
                }
                @Override
                protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
                    SizedLocation logo = modLogosCache.get(mod);
                    ScreenUtil.renderScrollingString(guiGraphics, font, this.getMessage(),this.getX() + 10 + (logo == null ? 20 : logo.getScaledWidth(20)), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), j,true);
                }
                @Override
                protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                    defaultButtonNarrationText(narrationElementOutput);
                }
            });
        });
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        ScreenUtil.renderDefaultBackground(accessor, guiGraphics, false);
        if (ScreenUtil.hasTooltipBoxes(accessor)) {
            tooltipBox.render(guiGraphics,i,j,f);
            if (focusedMod != null) {
                List<FormattedCharSequence> label = modLabelsCache.getUnchecked(focusedMod);
                scrollableRenderer.scrolled.max = Math.max(0,label.size() - (tooltipBox.getHeight() - 50) / 12);
                SizedLocation logo = modLogosCache.get(focusedMod);
                int x = panel.x + panel.width + (logo == null ? 5 : logo.getScaledWidth(28) + 10);
                if (logo != null)
                    FactoryGuiGraphics.of(guiGraphics).blit(logo.location, panel.x + panel.width + 5, panel.y + 10, 0.0f, 0.0f, logo.getScaledWidth(28), 28, logo.getScaledWidth(28), 28);
                if (logo == null || logo.getScaledWidth(28) < 120) {
                    ScreenUtil.renderScrollingString(guiGraphics, font, Component.translatable("legacy.menu.mods.id", focusedMod.getId()), x, panel.y + 12, panel.x + panel.width + 185, panel.y + 24, 0xFFFFFF, true);
                    ScreenUtil.renderScrollingString(guiGraphics, font, Component.translatable("legacy.menu.mods.version",focusedMod.getVersion()), x, panel.y + 24, panel.x + panel.width + 185, panel.y + 36, 0xFFFFFF, true);
                }
                scrollableRenderer.render(guiGraphics, panel.x + panel.width + 5, panel.y + 38, tooltipBox.getWidth() - 16, tooltipBox.getHeight() - 50, () -> label.forEach(c->guiGraphics.drawString(font, c,panel.x + panel.width + 5, panel.y + 41 + label.indexOf(c) * 12, 0xFFFFFF)));
            }
        }
    }

    @Override
    public boolean mouseScrolled(double d, double e/*? if >1.20.1 {*/, double f/*?}*/, double g) {
        if ((tooltipBox.isHovered(d,e) || !ControlType.getActiveType().isKbm()) && scrollableRenderer.mouseScrolled(g)) return true;
        return super.mouseScrolled(d, e/*? if >1.20.1 {*/, f/*?}*/, g);
    }

    @Override
    public void renderableVListInit() {
        addRenderableOnly(((guiGraphics, i, j, f) -> FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.PANEL_RECESS, panel.x + 7, panel.y + 7, panel.width - 14, panel.height - 14)));
        tooltipBox.init();
        getRenderableVList().init(panel.x + 11,panel.y + 11,260, panel.height - 21);
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
        renderer.add(()->ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_X) : ControllerBinding.LEFT_BUTTON.bindingState.getIcon(), ()-> Component.translatable("legacy.menu.sorting", this.sorting.get() == 0 ? LegacyKeyBindsScreen.NONE : ALPHABETICAL));
    }
}
