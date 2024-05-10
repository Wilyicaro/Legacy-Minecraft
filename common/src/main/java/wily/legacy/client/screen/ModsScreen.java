package wily.legacy.client.screen;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.architectury.platform.Mod;
import dev.architectury.platform.Platform;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import wily.legacy.Legacy4JPlatform;
import wily.legacy.util.ScreenUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ModsScreen extends PanelVListScreen{
    protected final Map<Mod, SizedLocation> modLogosCache = new ConcurrentHashMap<>();
    protected ScrollableRenderer scrollableRenderer =  new ScrollableRenderer(new LegacyScrollRenderer());

    public record SizedLocation(ResourceLocation location, int width, int height){
        public int getScaledWidth(int height){
            return (int) (height * ((float) width / height()));
        }
    }

    protected Mod focusedMod;
    protected final LoadingCache<Mod, MultiLineLabel> modLabelsCache = CacheBuilder.newBuilder().build(new CacheLoader<>() {
        @Override
        public MultiLineLabel load(Mod key) {
            List<Component> components = new ArrayList<>();
            SizedLocation logo = modLogosCache.get(key);
            if (logo != null && logo.getScaledWidth(28) >= 120){
                components.add(Component.literal(focusedMod.getName()));
                components.add(Component.translatable("legacy.menu.mods.id", focusedMod.getModId()));
            }
            if (!key.getAuthors().isEmpty())
                components.add(Component.translatable("legacy.menu.mods.authors", String.join(", ", key.getAuthors())));
            key.getHomepage().ifPresent(s-> components.add(Component.translatable("legacy.menu.mods.homepage",s)));
            key.getIssueTracker().ifPresent(s-> components.add(Component.translatable("legacy.menu.mods.issues",s)));
            key.getSources().ifPresent(s-> components.add(Component.translatable("legacy.menu.mods.sources",s)));
            if (key.getLicense() != null && !key.getLicense().isEmpty()) components.add(Component.translatable("legacy.menu.mods.license", String.join(", ", key.getLicense())));
            components.add(Component.literal(key.getDescription()));
            MultilineTooltip tooltip = new MultilineTooltip(components,176);
            return MultiLineLabel.createFixed(font, tooltip.toCharSequence(minecraft).stream().map(formattedCharSequence -> new MultiLineLabel.TextWithWidth(formattedCharSequence, font.width(formattedCharSequence))).toList());
        }
    });
    public ModsScreen(Screen parent) {
        super(s->new Panel(p -> (s.width - (p.width + (ScreenUtil.hasTooltipBoxes() ? 192 : 0))) / 2, p -> (s.height - p.height) / 2,282,240), Component.empty());
        this.parent = parent;
        renderableVList.layoutSpacing(l->0);
        Platform.getMods().forEach(mod->{
            if (Legacy4JPlatform.isHiddenMod(mod)) return;
            renderableVList.addRenderable(new AbstractButton(0,0,260,30, Component.literal(mod.getName())) {
                @Override
                public void onPress() {
                    if (isFocused()){
                        Screen config = Legacy4JPlatform.getConfigScreen(mod,ModsScreen.this);
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
                                return new SizedLocation(minecraft.getTextureManager().register(opt.get().toLowerCase(Locale.ENGLISH), new DynamicTexture(image)),image.getWidth(),image.getHeight());
                            } catch (IOException e) {
                            }
                        ResourceLocation defaultLogo = PackSelector.DEFAULT_ICON;
                        if (mod.getModId().equals("minecraft")) defaultLogo = PackSelector.loadPackIcon(minecraft.getTextureManager(),minecraft.getResourcePackRepository().getPack("vanilla"),"pack.png",defaultLogo);
                        return new SizedLocation(defaultLogo,1,1);
                        });
                    if (logo != null) guiGraphics.blit(logo.location,getX() + 5, getY() + 5, 0,0, logo.getScaledWidth(20),20,logo.getScaledWidth(20),20);

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
    public boolean isMouseOverTooltipBox(double d, double e){
        return ScreenUtil.isMouseOver(d,e,panel.x + panel.width - 2, panel.y + 5,192, panel.height - 10);
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        ScreenUtil.renderDefaultBackground(guiGraphics);
        if (ScreenUtil.hasTooltipBoxes()) {
            ScreenUtil.renderPointerPanel(guiGraphics,panel.x + panel.width - 2, panel.y + 5,192,panel.height - 10);
            if (focusedMod != null) {
                SizedLocation logo = modLogosCache.get(focusedMod);
                int x = panel.x + panel.width + (logo == null ? 5 : logo.getScaledWidth(28) + 10);
                if (logo != null)
                    guiGraphics.blit(logo.location, panel.x + panel.width + 5, panel.y + 10, 0.0f, 0.0f, logo.getScaledWidth(28), 28, logo.getScaledWidth(28), 28);
                if (logo == null || logo.getScaledWidth(28) < 120) {
                    ScreenUtil.renderScrollingString(guiGraphics, font, Component.translatable("legacy.menu.mods.id", focusedMod.getModId()), x, panel.y + 12, panel.x + panel.width + 185, panel.y + 24, 0xFFFFFF, true);
                    ScreenUtil.renderScrollingString(guiGraphics, font, Component.translatable("legacy.menu.mods.version",focusedMod.getVersion()), x, panel.y + 24, panel.x + panel.width + 185, panel.y + 36, 0xFFFFFF, true);
                }
                scrollableRenderer.render(guiGraphics, panel.x + panel.width + 5, panel.y + 38, 178, panel.height - 54, () -> modLabelsCache.getUnchecked(focusedMod).renderLeftAligned(guiGraphics, panel.x + panel.width + 5, panel.y + 41, 12, 0xFFFFFF));
            }
        }
    }

    @Override
    public boolean mouseScrolled(double d, double e, double f, double g) {
        if (isMouseOverTooltipBox(d,e) && scrollableRenderer.mouseScrolled(g)) return true;
        return super.mouseScrolled(d, e, f, g);
    }

    @Override
    protected void init() {
        panel.height = Math.min(height,248);
        addRenderableOnly(panel);
        panel.init();
        addRenderableOnly(((guiGraphics, i, j, f) -> ScreenUtil.renderPanelRecess(guiGraphics, panel.x + 7, panel.y + 7, panel.width - 14, panel.height - 14, 2)));
        getRenderableVList().init(this,panel.x + 11,panel.y + 11,260, panel.height - 5);
    }
}
