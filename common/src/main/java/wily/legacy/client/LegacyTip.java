package wily.legacy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.tutorial.OpenInventoryTutorialStep;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
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

    public int disappearTime = -1;
    public Supplier<Boolean> canRemove = ()->false;

    public LegacyIconHolder holder = null;
    public LegacyTip(Component tip){
        this(tip,400,55);
        disappearTime = tip.getString().toCharArray().length * 2;
    }
    public LegacyTip(Component tip, int width, int height){
        super(width, height);
        this.tipLines = minecraft.font.split(tip,width - 26);
    }
    public LegacyTip(ItemStack stack){
        this(ScreenUtil.getDescription(stack.getItem()), 250,0);
        height = 26 + tipLines.size() * 12;
        title(stack.getDisplayName());
        itemStack(stack);
        disappearTime(2000);
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
    public LegacyTip disappearTime(int disappearTime){
        this.disappearTime = disappearTime;
        return this;
    }
    public LegacyTip itemStack(ItemStack itemStack){
        if (itemStack != null) height += 32;
        if (holder == null){
            holder = new LegacyIconHolder(32,32);
            holder.setX((width - 32 )/ 2);
            holder.setY(13 + tipLines.size() * (12 + (title.getString().isEmpty() ? 0 : 1)));
        }
        holder.itemIcon = itemStack;
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
        render(guiGraphics,0,0,0);
        return visibility;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        if (disappearTime > 0) disappearTime--;
        if (disappearTime == 0 || canRemove.get()) visibility = Visibility.HIDE;
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
