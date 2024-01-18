package wily.legacy.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import wily.legacy.client.screen.LegacyIconHolder;
import wily.legacy.client.screen.SimpleLayoutRenderable;
import wily.legacy.util.ScreenUtil;

import java.util.List;
import java.util.function.Supplier;

public class LegacyTip extends SimpleLayoutRenderable implements  Toast{

    private final List<FormattedCharSequence> tipLines;

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
        this.tipLines = minecraft.font.split(tip,width - 26);
        disappearTime = tip.getString().toCharArray().length * 80L;
    }
    public LegacyTip(Component title, Component tip){
        this(tip, 250,0);
        height = 26 + tipLines.size() * 12;
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
    public LegacyTip disappearTime(long disappearTime){
        this.disappearTime = disappearTime;
        return this;
    }
    public LegacyTip itemStack(ItemStack itemStack){
        if (itemStack != null) height += 32;
        if (holder == null){
            holder = new LegacyIconHolder(32,32);
            holder.setX((width - 32 )/ 2);
            holder.setY(13 + (tipLines.size() + (title.getString().isEmpty() ? 0 : 1)) * 12);
            holder.allowItemDecorations = false;
        }
        holder.itemIcon = itemStack;
        return this;
    }

    public int width() {
        return getWidth() + 30;
    }

    public int height() {
        return getHeight() + y;
    }

    @Override
    public Visibility render(GuiGraphics guiGraphics, ToastComponent toastComponent, long l) {
        if (l >= disappearTime) visibility = Visibility.HIDE;
        renderTip(guiGraphics,0,0,0);
        return visibility;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        if (Util.getMillis() - createdTime >= disappearTime) visibility = Visibility.HIDE;
        renderTip(guiGraphics, i, j, f);
    }
    public void renderTip(GuiGraphics guiGraphics, int i, int j, float f) {
        if (canRemove.get()) visibility = Visibility.HIDE;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(getX(),getY(),0);
        ScreenUtil.renderPointerPanel(guiGraphics,0,0,getWidth(),getHeight());
        if (!title.getString().isEmpty()) {
            guiGraphics.drawString(minecraft.font,title,13,13,0xFFFFFF);
            for (FormattedCharSequence tipLine : tipLines)
                guiGraphics.drawString(minecraft.font,tipLine,13,13 + (1 + tipLines.indexOf(tipLine)) * 12,0xFFFFFF,false);
        }else{
            for (FormattedCharSequence tipLine : tipLines)
                guiGraphics.drawString(minecraft.font,tipLine,(width - minecraft.font.width(tipLine)) / 2,13 + tipLines.indexOf(tipLine) * 12,0xFFFFFF);
        }
        if (holder != null) holder.render(guiGraphics,i,j,f);
        guiGraphics.pose().popPose();
    }
}
