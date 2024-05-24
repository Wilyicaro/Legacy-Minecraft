package wily.legacy.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import wily.legacy.client.controller.Controller;
import wily.legacy.client.screen.LegacyIconHolder;
import wily.legacy.client.screen.SimpleLayoutRenderable;
import wily.legacy.util.ScreenUtil;

import java.util.function.Supplier;

public class LegacyTip extends SimpleLayoutRenderable implements Toast, Controller.Event{

    private MultiLineLabel tipLabel;
    public Visibility visibility = Visibility.SHOW;

    public Component title = Component.empty();

    protected Minecraft minecraft = Minecraft.getInstance();
    protected Screen initScreen = minecraft.screen;

    public long disappearTime = -1;
    public long createdTime = Util.getMillis();
    public Supplier<Boolean> canRemove = ()->false;

    public LegacyIconHolder holder = null;
    public LegacyTip(Component tip){
        this(tip,400,55);
    }
    public LegacyTip(Component tip, int width, int height){
        super(width, height);
        tip(tip);
    }
    public LegacyTip(Component title, Component tip){
        this(tip, 250,0);
        autoHeight();
        title(title);
        setY(25);
        canRemove(()-> initScreen != minecraft.screen);
    }
    public LegacyTip(ItemStack stack){
        this(Component.translatable(stack.getDescriptionId()),ScreenUtil.getTip(stack));
        itemStack(stack);
    }
    public LegacyTip canRemove(Supplier<Boolean> canRemove){
        this.canRemove = canRemove;
        return this;
    }
    public LegacyTip title(Component title){
        if (!title.getString().isEmpty()) height += 12;
        this.title = title;
        return this;
    }
    public LegacyTip autoHeight(){
        height = 26 + tipLabel.getLineCount() * 12;
        return this;
    }
    public LegacyTip tip(Component tip){
        tipLabel = MultiLineLabel.create(minecraft.font,tip,width-26);
        return disappearTime(tip.getString().toCharArray().length * 80L);
    }
    public LegacyTip disappearTime(long disappearTime){
        if (disappearTime >= 0) {
            createdTime = Util.getMillis();
            this.disappearTime = disappearTime;
        }
        return this;
    }
    public LegacyTip itemStack(ItemStack itemStack){
        if (!itemStack.isEmpty()) {
            height += 32;
            if (holder == null) {
                holder = new LegacyIconHolder(32, 32);
                holder.allowItemDecorations = false;
            }
            holder.setX((width - 32) / 2);
            holder.setY(13 + (tipLabel.getLineCount() + (title.getString().isEmpty() ? 0 : 1)) * 12);
            holder.itemIcon = itemStack;
        }
        return this;
    }

    public int width() {
        return getWidth();
    }

    public int height() {
        return getHeight();
    }

    @Override
    public Visibility render(GuiGraphics guiGraphics, ToastComponent toastComponent, long l) {
        renderTip(guiGraphics,0,0,0);
        return visibility;
    }

    @Override
    public int slotCount() {
        return Math.min(5,Toast.super.slotCount());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        renderTip(guiGraphics, i, j, f);
    }
    public void renderTip(GuiGraphics guiGraphics, int i, int j, float f) {
        if (canRemove.get() || Util.getMillis() - createdTime >= disappearTime) visibility = Visibility.HIDE;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(getX(),getY(),800);
        ScreenUtil.renderPointerPanel(guiGraphics,0,0,getWidth(),getHeight());
        if (!title.getString().isEmpty()) {
            guiGraphics.drawString(minecraft.font,title,13,13,0xFFFFFF);
            tipLabel.renderLeftAlignedNoShadow(guiGraphics,13,25, 12,0xFFFFFF);
        }else
            tipLabel.renderCentered(guiGraphics,width / 2,13,12,0xFFFFFF);
        if (holder != null) holder.render(guiGraphics,i,j,f);
        guiGraphics.pose().popPose();
    }
}
