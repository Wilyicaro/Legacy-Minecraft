package wily.legacy.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.client.LegacyBiomeOverride;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

import java.util.function.Consumer;

public class LegacyBuffetWorldScreen extends PanelVListScreen {
    private final Consumer<Holder<Biome>> applySettings;
    protected Holder<Biome> selectedBiome;

    public LegacyBuffetWorldScreen(CreateWorldScreen screen, HolderLookup.RegistryLookup<Biome> biomeGetter, Consumer<Holder<Biome>> consumer) {
        super(screen,s->Panel.centered(s,()->282,()->Math.min(248,s.height)),Component.translatable("createWorld.customize.buffet.title"));
        parent = Minecraft.getInstance().screen instanceof WorldMoreOptionsScreen s ? s : screen;
        renderableVList.layoutSpacing(l->0);
        this.applySettings = consumer;
        biomeGetter.listElements().forEach(this::addBiome);
    }
    public void addBiome(Holder.Reference<Biome> biome){
        renderableVList.addRenderable(new AbstractButton(0,0,260,30, Component.translatable("biome."+biome.key().location().toLanguageKey())) {
            @Override
            public void onPress() {
                selectedBiome = biome;
            }

            @Override
            protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
                super.renderWidget(guiGraphics, i, j, f);
                ItemStack s = LegacyBiomeOverride.getOrDefault(biome.unwrapKey()).icon();
                if (!s.isEmpty()){
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(getX() + 26, getY() + 5,0);
                    guiGraphics.pose().scale(1.25f,1.25f,1.25f);
                    guiGraphics.renderItem(s,0, 0);
                    guiGraphics.pose().popPose();
                }
                RenderSystem.enableBlend();
                FactoryGuiGraphics.of(guiGraphics).blitSprite(TickBox.SPRITES[isHoveredOrFocused() ? 1 : 0], this.getX() + 6, this.getY() + (height - 12) / 2, 12, 12);
                if (selectedBiome == biome) FactoryGuiGraphics.of(guiGraphics).blitSprite(TickBox.TICK, this.getX() + 6, this.getY()  + (height - 12) / 2, 14, 12);
                RenderSystem.disableBlend();
            }
            @Override
            protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
                int k = this.getX() + 54;
                int l = this.getX() + this.getWidth();
                ScreenUtil.renderScrollingString(guiGraphics, font, this.getMessage(), k, this.getY(), l, this.getY() + this.getHeight(), j,true);
            }
            @Override
            protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                defaultButtonNarrationText(narrationElementOutput);
            }
        });
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        ScreenUtil.renderDefaultBackground(accessor, guiGraphics, false);
    }

    @Override
    protected void panelInit() {
        super.panelInit();
        addRenderableOnly(((guiGraphics, i, j, f) -> FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.PANEL_RECESS, panel.x + 7, panel.y + 7, panel.width - 14, panel.height - 14)));
    }

    @Override
    public void renderableVListInit() {
        getRenderableVList().init(panel.x + 11,panel.y + 11,260, panel.height - 25);
    }

    @Override
    public void onClose() {
        super.onClose();
        if (selectedBiome != null) applySettings.accept(selectedBiome);
    }
}

