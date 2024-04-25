package wily.legacy.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import wily.legacy.LegacyMinecraft;
import wily.legacy.client.Offset;
import wily.legacy.util.ScreenUtil;

import java.util.function.Consumer;
import java.util.function.Function;

public class LegacyTabButton extends AbstractButton {
    public static final ResourceLocation[][] SPRITES = new ResourceLocation[][]{new ResourceLocation[]{new ResourceLocation(LegacyMinecraft.MOD_ID, "tiles/high_tab_left"),new ResourceLocation(LegacyMinecraft.MOD_ID, "tiles/low_tab_left")}, new ResourceLocation[]{new ResourceLocation(LegacyMinecraft.MOD_ID, "tiles/high_tab_middle"),new ResourceLocation(LegacyMinecraft.MOD_ID, "tiles/low_tab_middle")}, new ResourceLocation[]{new ResourceLocation(LegacyMinecraft.MOD_ID, "tiles/high_tab_right"),new ResourceLocation(LegacyMinecraft.MOD_ID, "tiles/low_tab_right")},
                                                                                new ResourceLocation[]{new ResourceLocation(LegacyMinecraft.MOD_ID, "tiles/high_vert_tab_up"),new ResourceLocation(LegacyMinecraft.MOD_ID, "tiles/low_vert_tab_up")}, new ResourceLocation[]{new ResourceLocation(LegacyMinecraft.MOD_ID, "tiles/high_vert_tab_middle"),new ResourceLocation(LegacyMinecraft.MOD_ID, "tiles/low_vert_tab_middle")}, new ResourceLocation[]{new ResourceLocation(LegacyMinecraft.MOD_ID, "tiles/high_vert_tab_down"),new ResourceLocation(LegacyMinecraft.MOD_ID, "tiles/low_vert_tab_down")}};
    public static final Offset DEFAULT_OFFSET = new Offset(0,4,0);
    public static final Offset DEFAULT_DESACTIVE_OFFSET = new Offset(0,22,0);
    public final ResourceLocation icon;
    public ItemStack itemIcon;
    private final Consumer<LegacyTabButton> onPress;
    public boolean selected;
    protected int type;
    public Function<LegacyTabButton, Offset> offset = (t)-> {
        if (!isActive()) return DEFAULT_DESACTIVE_OFFSET;
        if (!t.selected) return DEFAULT_OFFSET;
        return Offset.ZERO;
    };

    public LegacyTabButton(int i, int j, int width, int height, int type, ResourceLocation iconSprite, CompoundTag itemIconTag, Component text, Tooltip tooltip, Consumer<LegacyTabButton> onPress) {
        super(i, j, width, height, text);
        setTooltip(tooltip);
        this.onPress = onPress;
        this.type = type;
        icon = iconSprite;
        if (itemIcon == null && icon != null)
            if (BuiltInRegistries.ITEM.containsKey(icon))
                (itemIcon = BuiltInRegistries.ITEM.get(icon).getDefaultInstance()).setTag(itemIconTag);
    }

    @Override
    public void onPress() {
        selected = !selected;
        onPress.accept(this);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        Minecraft minecraft = Minecraft.getInstance();
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        guiGraphics.pose().pushPose();
        Offset translate = offset.apply(this);
        if (!translate.equals(Offset.ZERO)) {
            translate.apply(guiGraphics.pose());
            isHovered = isMouseOver(i,j);
        }
        if (selected) guiGraphics.pose().translate(0F,0f,1F);
        ScreenUtil.renderTiles(SPRITES[type][selected ? 0 : 1],guiGraphics, getX(), getY(), getWidth(), this.getHeight(),2);
        if (active) {
            if (icon == null)
                this.renderString(guiGraphics, minecraft.font, 0x383838 | Mth.ceil(this.alpha * 255.0f) << 24);
            else {
                if (itemIcon == null)
                    guiGraphics.blitSprite(icon, getX() + width / 2 - 12, getY() + height / 2 - 12, 24, 24);
                else {
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(getX() + width / 2f - 12, getY() + height / 2f - 12, 0);
                    guiGraphics.pose().scale(1.5f, 1.5f, 1.5f);
                    guiGraphics.renderItem(itemIcon, 0, 0);
                    guiGraphics.pose().popPose();
                }
            }
        }
        guiGraphics.pose().popPose();
    }

    public boolean isMouseOver(double d, double e) {
        Offset translate = offset.apply(this);
        double x =  getX() + (translate.equals(Offset.ZERO) ? 0 : translate.x());
        double y =  getY() + (translate.equals(Offset.ZERO) ? 0 : translate.y());
        return this.active && this.visible && d >= x && e >= y && d < (x + this.width) && e < (y + this.height);
    }
    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.tab", this.getMessage().getString()));
    }

    @Override
    protected boolean clicked(double d, double e) {
        return isMouseOver(d,e);
    }

    @Override
    public void renderString(GuiGraphics guiGraphics, Font font, int i) {
        guiGraphics.drawString(font,getMessage(),getX() + (width - font.width(getMessage())) / 2,getY() + (height - 7) / 2,i,false);
    }
}
