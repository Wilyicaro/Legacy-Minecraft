package wily.legacy.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.toasts.Toast;
//? if <1.21.2 {
/*import net.minecraft.client.gui.components.toasts.ToastComponent;
*///?} else {
import net.minecraft.client.gui.components.toasts.ToastManager;
//?}
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

    protected boolean centered = false;

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
        this(Component.translatable(stack.getItem().getDescriptionId()), LegacyTipManager.getTipComponent(stack));
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
    public LegacyTip centered(){
        centered = true;
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
    public /*? if <1.21.2 {*//*Visibility*//*?} else {*/void/*?}*/ render(GuiGraphics guiGraphics, /*? if <1.21.2 {*//*ToastComponent toastComponent*//*?} else {*/Font font/*?}*/, long l) {
        renderTip(guiGraphics,0,0,0,l);
        //? if <1.21.2 {
        /*return visibility;
        *///?}
    }
    //? if >=1.21.2 {
    @Override
    public Visibility getWantedVisibility() {
        return visibility;
    }

    @Override
    public void update(ToastManager toastManager, long l) {
    }
    //?}

    @Override
    public int /*? if <1.21.2 {*//*slotCount*//*?} else {*/occcupiedSlotCount/*?}*/() {
        return Math.min(5,Toast.super./*? if <1.21.2 {*//*slotCount*//*?} else {*/occcupiedSlotCount/*?}*/());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        renderTip(guiGraphics, i, j, f,Util.getMillis() - createdTime);
    }
    public void renderTip(GuiGraphics guiGraphics, int i, int j, float f, float l) {
        if (canRemove.get() || l >= disappearTime) visibility = Visibility.HIDE;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(getX(),getY(),800);
        ScreenUtil.renderPointerPanel(guiGraphics,0,0,getWidth(),getHeight());
        if (!title.getString().isEmpty()) guiGraphics.drawString(minecraft.font,title,13,13,CommonColor.TIP_TITLE_TEXT.get());
        if (centered) tipLabel.renderCentered(guiGraphics,width / 2, title.getString().isEmpty() ? 13 : 25,12,CommonColor.TIP_TEXT.get());
        else tipLabel.renderLeftAlignedNoShadow(guiGraphics,13,title.getString().isEmpty() ? 13 : 25, 12,CommonColor.TIP_TEXT.get());
        if (holder != null) holder.render(guiGraphics,i,j,f);
        guiGraphics.pose().popPose();
    }
}
