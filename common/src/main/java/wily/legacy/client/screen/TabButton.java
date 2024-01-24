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
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;
import org.joml.Vector3f;
import wily.legacy.LegacyMinecraft;
import wily.legacy.util.ScreenUtil;

import java.util.function.Consumer;
import java.util.function.Function;

public class TabButton extends AbstractButton {
    public static final ResourceLocation[][] SPRITES = new ResourceLocation[][]{new ResourceLocation[]{new ResourceLocation(LegacyMinecraft.MOD_ID, "tiles/high_tab_left"),new ResourceLocation(LegacyMinecraft.MOD_ID, "tiles/low_tab_left")}, new ResourceLocation[]{new ResourceLocation(LegacyMinecraft.MOD_ID, "tiles/high_tab_middle"),new ResourceLocation(LegacyMinecraft.MOD_ID, "tiles/low_tab_middle")}, new ResourceLocation[]{new ResourceLocation(LegacyMinecraft.MOD_ID, "tiles/high_tab_right"),new ResourceLocation(LegacyMinecraft.MOD_ID, "tiles/low_tab_right")}};
    public final ResourceLocation icon;
    public ItemStack itemIcon;
    private final Consumer<TabButton> onPress;
    public boolean selected;
    protected int type;
    public Function<TabButton, Vec3> translocation = (t)-> {
     if (!t.selected) return new Vec3(0,4,0);
     return Vec3.ZERO;
    };

    public TabButton(int i, int j, int width, int height, int type, ResourceLocation iconSprite, CompoundTag itemIconTag, Component text, Tooltip tooltip, Consumer<TabButton> onPress) {
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
        Vec3 translate = translocation.apply(this);
        if (!translate.equals(Vec3.ZERO)) {
            guiGraphics.pose().translate(translate.x,translate.y,translate.z);
            isHovered = isMouseOver(i,j);
        }
        if (selected) guiGraphics.pose().translate(0F,0f,1F);

        ScreenUtil.renderTiles(SPRITES[type][selected ? 0 : 1],guiGraphics,this.getX(), this.getY(), this.getWidth(), this.getHeight(),2);
        int k = this.active ? 0x404040 : 0xA0A0A0;
        if (icon == null) this.renderString(guiGraphics, minecraft.font, k | Mth.ceil(this.alpha * 255.0f) << 24);
        else {
            if (itemIcon == null)
                guiGraphics.blitSprite(icon,getX() + width / 2 - 12,getY() + height / 2 - 12,24,24);
            else {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(getX() + width / 2f - 12, getY() + height / 2f - 12, 0);
                guiGraphics.pose().scale(1.5f, 1.5f, 1.5f);
                guiGraphics.renderItem(itemIcon, 0, 0);
                guiGraphics.pose().popPose();
            }
        }
        guiGraphics.pose().popPose();
    }

    public boolean isMouseOver(double d, double e) {
        Vec3 translate = translocation.apply(this);
        double x =  getX() + (translate.equals(Vec3.ZERO) ? 0 : translate.x);
        double y =  getY() + (translate.equals(Vec3.ZERO) ? 0 : translate.y);
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
