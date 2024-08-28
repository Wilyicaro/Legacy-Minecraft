package wily.legacy.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import wily.legacy.Legacy4J;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyGuiGraphics;
import wily.legacy.util.Offset;

import java.util.function.Consumer;
import java.util.function.Function;

public class LegacyTabButton extends AbstractButton {
    public static final ResourceLocation[][] SPRITES = new ResourceLocation[][]{new ResourceLocation[]{new ResourceLocation(Legacy4J.MOD_ID, "tiles/high_tab_left"),new ResourceLocation(Legacy4J.MOD_ID, "tiles/low_tab_left")}, new ResourceLocation[]{new ResourceLocation(Legacy4J.MOD_ID, "tiles/high_tab_middle"),new ResourceLocation(Legacy4J.MOD_ID, "tiles/low_tab_middle")}, new ResourceLocation[]{new ResourceLocation(Legacy4J.MOD_ID, "tiles/high_tab_right"),new ResourceLocation(Legacy4J.MOD_ID, "tiles/low_tab_right")},
                                                                                new ResourceLocation[]{new ResourceLocation(Legacy4J.MOD_ID, "tiles/high_vert_tab_up"),new ResourceLocation(Legacy4J.MOD_ID, "tiles/low_vert_tab_up")}, new ResourceLocation[]{new ResourceLocation(Legacy4J.MOD_ID, "tiles/high_vert_tab_middle"),new ResourceLocation(Legacy4J.MOD_ID, "tiles/low_vert_tab_middle")}, new ResourceLocation[]{new ResourceLocation(Legacy4J.MOD_ID, "tiles/high_vert_tab_down"),new ResourceLocation(Legacy4J.MOD_ID, "tiles/low_vert_tab_down")}};
    public static final Offset DEFAULT_DESACTIVE_OFFSET = new Offset(0,22,0);
    public static final Offset DEFAULT_UNSELECTED_OFFSET = new Offset(0,4,0);
    public final Function<LegacyTabButton,Renderable> icon;
    private final Consumer<LegacyTabButton> onPress;
    public boolean selected;
    protected int type;
    public Function<LegacyTabButton, Offset> offset = (t)-> {
        if (!isActive()) return DEFAULT_DESACTIVE_OFFSET;
        if (!t.selected) return DEFAULT_UNSELECTED_OFFSET;
        return Offset.ZERO;
    };

    public LegacyTabButton(int i, int j, int width, int height, int type, Function<LegacyTabButton,Renderable> icon, Component text, Tooltip tooltip, Consumer<LegacyTabButton> onPress) {
        super(i, j, width, height, text);
        setTooltip(tooltip);
        this.onPress = onPress;
        this.type = type;
        this.icon = icon;
    }

    @Override
    public void onPress() {
        selected = !selected;
        onPress.accept(this);
    }
    public static Function<LegacyTabButton,Renderable> iconOf(Item item){
        return t-> (poseStack, i, j, f) -> t.renderItemIcon(item.getDefaultInstance(),poseStack);
    }
    public static Function<LegacyTabButton,Renderable> iconOf(ItemStack stack){
        return t-> (poseStack, i, j, f) -> t.renderItemIcon(stack,poseStack);
    }
    public static Function<LegacyTabButton,Renderable> iconOf(ResourceLocation sprite){
        return t-> (poseStack, i, j, f) -> t.renderIconSprite(sprite,poseStack);
    }
    public  void renderString(PoseStack poseStack, Font font, int i, boolean shadow){
        poseStack.drawString(font,getMessage(),getX() + (width - font.width(getMessage())) / 2,getY() + (height - 7) / 2,i,shadow);
    }
    public  void renderIconSprite(ResourceLocation icon, PoseStack poseStack){
        LegacyGuiGraphics.of(poseStack).blitSprite(icon, getX() + width / 2 - 12, getY() + height / 2 - 12, 24, 24);
    }
    public  void renderItemIcon(ItemStack itemIcon, PoseStack poseStack){
        poseStack.pose().pushPose();
        poseStack.pose().translate(getX() + width / 2f - 12, getY() + height / 2f - 12, 0);
        poseStack.pose().scale(1.5f, 1.5f, 1.5f);
        poseStack.renderItem(itemIcon, 0, 0);
        poseStack.pose().popPose();
    }

    @Override
    protected void renderWidget(PoseStack poseStack, int i, int j, float f) {
        Minecraft minecraft = Minecraft.getInstance();
        poseStack.setColor(1.0f, 1.0f, 1.0f, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        poseStack.pose().pushPose();
        Offset translate = offset.apply(this);
        if (!translate.equals(Offset.ZERO)) {
            translate.apply(poseStack.pose());
            isHovered = isMouseOver(i,j);
        }
        if (selected) poseStack.pose().translate(0F,0f,1F);
        LegacyGuiGraphics.of(poseStack).blitSprite(SPRITES[type][selected ? 0 : 1], getX(), getY(), getWidth(), this.getHeight());
        if (!selected) poseStack.pose().translate(0,-1,0);
        if (active) {
            if (icon == null) this.renderString(poseStack, minecraft.font, CommonColor.INVENTORY_GRAY_TEXT.get() | Mth.ceil(this.alpha * 255.0f) << 24);
            else icon.apply(this).render(poseStack,i,j,f);
        }
        poseStack.pose().popPose();
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
    public void renderString(PoseStack poseStack, Font font, int i) {
        renderString(poseStack,font,i,false);
    }
}
