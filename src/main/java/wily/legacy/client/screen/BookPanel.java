package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.LecternScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIDefinition;
import wily.legacy.util.LegacyComponents;

public class BookPanel extends WidgetPanel {

    public BookPanel(Screen screen) {
        super(screen);
    }

    @Override
    public void init(String name) {
        super.init(name);
        size(201,248);
        pos(centeredLeftPos(accessor.getScreen()),centeredTopPos(accessor.getScreen()) - 30);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(getX(),getY(),0);
        guiGraphics.pose().scale(getWidth() / 146f,getHeight() / 180f,1f);
        FactoryGuiGraphics.of(guiGraphics).blit(BookViewScreen.BOOK_LOCATION,0,0,20,1,146,180);
        guiGraphics.pose().popPose();
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (canTakeBook(i)){
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(Minecraft.getInstance().player.containerMenu.containerId,3);
            return true;
        }
        return super.keyPressed(i, j, k);
    }

    public boolean canTakeBook(int key){
        return key == InputConstants.KEY_RETURN && accessor.getScreen() instanceof LecternScreen && Minecraft.getInstance().player.mayBuild();
    }


    public PageButton createLegacyPageButton(int i, int j, boolean bl, Button.OnPress onPress, boolean bl2){
        return new PageButton(i,j,bl,onPress,bl2){
            private long lastPressTime;
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
                isHovered = Util.getMillis() - lastPressTime <= 300 || /*? if <1.21.4 {*//*clicked*//*?} else {*/isMouseOver/*?}*/(i,j);
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(getX(),getY(),1.5f);
                guiGraphics.pose().scale(1.5f,1.5f,1.5f);
                guiGraphics.pose().translate(-getX(),-getY(),1.5f);
                super.renderWidget(guiGraphics, i, j, f);
                guiGraphics.pose().popPose();
            }

            @Override
            public void onPress() {
                super.onPress();
                lastPressTime = Util.getMillis();
            }

            @Nullable
            @Override
            public ComponentPath nextFocusPath(FocusNavigationEvent focusNavigationEvent) {
                return null;
            }

            @Override
            public boolean /*? if <1.21.4 {*//*clicked*//*?} else {*/isMouseOver/*?}*/(double d, double e) {
                return this.active && this.visible && d >= (double)this.getX() && e >= (double)this.getY() && d < (double)(this.getX() + this.getWidth() * 3/2) && e < (double)(this.getY() + this.getHeight() * 3/2);
            }
        };
    }

    @Override
    public @Nullable Component getAction(Context context) {
        return isFocused() ? context.actionOfContext(KeyContext.class, c-> canTakeBook(c.key()) ? LegacyComponents.TAKE_BOOK : null) : super.getAction(context);
    }
}
