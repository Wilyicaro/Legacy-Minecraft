package wily.legacy.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import wily.legacy.LegacyMinecraft;

import java.util.function.Supplier;

public class SlotButtonList <E extends SlotButtonList.SlotEntry<E>> extends ObjectSelectionList<E> {
    static final ResourceLocation SAVE_BUTTON_SPRITE = new ResourceLocation("widget/button");
    static final ResourceLocation HOVERED_SAVE_BUTTON_SPRITE =  new ResourceLocation("widget/button_highlighted");
    protected static ResourceLocation SCROLL_DOWN = new ResourceLocation(LegacyMinecraft.MOD_ID,"widget/scroll_down");
    protected static ResourceLocation SCROLL_UP = new ResourceLocation(LegacyMinecraft.MOD_ID,"widget/scroll_up");
    protected final Supplier<Boolean> active;


    public SlotButtonList(Supplier<Boolean> active, Minecraft minecraft, int i, int j, int k, int l, int m) {
        super(minecraft, i, j, k, l, m);
        this.active = active;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        if (!active.get()) return;
        this.hovered = this.isMouseOver(i, j) ? this.getEntryAtPosition(i, j) : null;
        this.enableScissor(guiGraphics);
        this.renderList(guiGraphics, i, j, f);
        guiGraphics.disableScissor();
        this.renderDecorations(guiGraphics, i, j);
        RenderSystem.disableBlend();

    }
    public int getRowLeft() {
        return this.x0 + (this.width - this.getRowWidth())/ 2;
    }
    @Override
    protected void renderSelection(GuiGraphics guiGraphics, int i, int j, int k, int l, int m) {

    }

    @Override
    public boolean mouseClicked(double d, double e, int i) {
        if (!active.get()) return false;
        return super.mouseClicked(d, e, i);
    }

    @Override
    public int getRowWidth() {
        return 270;
    }

    @Override
    public boolean mouseScrolled(double d, double e, double f, double g) {
        if (!active.get()) return false;
        this.setScrollAmount(this.getScrollAmount() - g * (double)this.itemHeight);
        return true;
    }

    @Override
    protected void renderItem(GuiGraphics guiGraphics, int i, int j, float f, int k, int l, int m, int n, int o) {
        if (getEntry(k).hasSlotBackground())
            guiGraphics.blitSprite(isSelectedItem(k)? HOVERED_SAVE_BUTTON_SPRITE : SAVE_BUTTON_SPRITE,getRowLeft(),m,getRowWidth(), itemHeight);
        super.renderItem(guiGraphics, i, j, f, k, l, m, n, o);
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {

    }
    public abstract static class SlotEntry<E extends SlotEntry<E>> extends Entry<E>{
        public boolean hasSlotBackground(){
            return true;
        }
    }
}
