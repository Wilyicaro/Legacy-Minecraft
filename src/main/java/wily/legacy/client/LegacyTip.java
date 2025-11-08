package wily.legacy.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
//? if <1.21.2 {
/*import net.minecraft.client.gui.components.toasts.ToastComponent;
 *///?} else {
import net.minecraft.client.gui.components.toasts.ToastManager;
//?}
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import wily.factoryapi.base.client.AdvancedTextWidget;
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.client.controller.Controller;
import wily.legacy.client.screen.LegacyIconHolder;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.function.Supplier;

public class LegacyTip extends SimpleLayoutRenderable implements Toast, Controller.Event {

    protected final AdvancedTextWidget tipLabel = new AdvancedTextWidget(FactoryScreenUtil.getGuiAccessor());
    public Visibility visibility = Visibility.SHOW;

    public Component title = null;
    public long disappearTime = -1;
    public long createdTime = Util.getMillis();
    public Supplier<Boolean> canRemove = () -> false;
    public LegacyIconHolder holder = null;
    protected boolean centered = false;
    protected boolean compactMode;
    protected Minecraft minecraft = Minecraft.getInstance();
    protected Screen initScreen = minecraft.screen;


    public LegacyTip(Component tip, int width, int height, boolean compactMode) {
        super(width, height);
        this.compactMode = compactMode;
        tip(tip);
    }

    public LegacyTip(Component tip, int width, int height) {
        this(tip, width, height, LegacyOptions.getUIMode().isSD());
    }

    public LegacyTip(Component title, Component tip) {
        this(tip, LegacyOptions.getUIMode().isSD() ? 136 : 250, 0);
        title(title);
        height = (title == null ? LegacyOptions.getUIMode().isSD() ? 14 : 26 : LegacyOptions.getUIMode().isSD() ? 22 : 38) + tipLabel.getHeight();
        setY(25);
        canRemove(() -> initScreen != minecraft.screen);
    }

    public LegacyTip(Component tip) {
        this(null, tip);
    }

    public LegacyTip canRemove(Supplier<Boolean> canRemove) {
        this.canRemove = canRemove;
        return this;
    }

    public LegacyTip title(Component title) {
        this.title = CommonComponents.EMPTY.equals(title) ? null : title;
        return this;
    }

    public LegacyTip centered() {
        centered = true;
        return this;
    }

    public LegacyTip tip(Component tip) {
        LegacyFontUtil.applyFontOverrideIf(compactMode, LegacyFontUtil.MOJANGLES_11_FONT, b -> tipLabel.lineSpacing(b ? 8 : 12).withLines(tip, width - (b ? 10 : 26)));
        return disappearTime(tip.getString().length() * 80L);
    }

    public LegacyTip disappearTime(long disappearTime) {
        if (disappearTime >= 0) {
            createdTime = Util.getMillis();
            this.disappearTime = disappearTime;
        }
        return this;
    }

    public LegacyTip itemStack(ItemStack itemStack) {
        if (!itemStack.isEmpty()) {
            height += 32;
            if (holder == null) {
                holder = new LegacyIconHolder(32, 32);
                holder.allowItemDecorations = false;
            }
            holder.setX((width - 32) / 2);
            holder.setY((LegacyOptions.getUIMode().isSD() ? 5 : 13) + tipLabel.getHeight() + (title == null ? 0 : LegacyOptions.getUIMode().isSD() ? 8 : 12));
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
        renderTip(guiGraphics, 0, 0, 0, l);
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
        return Math.min(5, Toast.super./*? if <1.21.2 {*//*slotCount*//*?} else {*/occcupiedSlotCount/*?}*/());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        renderTip(guiGraphics, i, j, f, Util.getMillis() - createdTime);
    }

    public void renderTip(GuiGraphics guiGraphics, int i, int j, float f, float l) {
        if (canRemove.get() || l >= disappearTime) visibility = Visibility.HIDE;
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(getX(), getY());
        LegacyRenderUtil.renderPointerPanel(guiGraphics, 0, 0, getWidth(), getHeight());
        if (title != null)
            LegacyFontUtil.applyFontOverrideIf(compactMode, LegacyFontUtil.MOJANGLES_11_FONT, b -> guiGraphics.drawString(minecraft.font, title, (centered ? (width() - minecraft.font.width(title)) / 2 : b ? 5 : 13), b ? 5 : 13, CommonColor.TIP_TITLE_TEXT.get()));
        LegacyFontUtil.applyFontOverrideIf(compactMode, LegacyFontUtil.MOJANGLES_11_FONT, b -> tipLabel.centered(centered).withColor(CommonColor.TIP_TEXT.get()).withShadow(centered).withPos(b ? 5 : 13, title == null ? b ? 5 : 13 : b ? 13 : 25).render(guiGraphics, i, j, f));
        if (holder != null) holder.render(guiGraphics, i, j, f);
        guiGraphics.pose().popMatrix();
    }
}
