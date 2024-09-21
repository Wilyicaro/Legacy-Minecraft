package wily.legacy.client.screen;

import net.minecraft.Util;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import org.jetbrains.annotations.Nullable;

public class BookPanel extends WidgetPanel implements GuiEventListener, NarratableEntry {
    private final Screen screen;
    public BookPanel(Screen screen) {
        super(l-> l.centeredLeftPos(screen),l-> l.centeredTopPos(screen) - 30,201,248);
        this.screen = screen;
    }
    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(getX(),getY(),0);
        guiGraphics.pose().scale(getWidth() / 146f,getHeight() / 180f,1f);
        guiGraphics.blit(BookViewScreen.BOOK_LOCATION,0,0,20,1,146,180);
        guiGraphics.pose().popPose();
    }
    public PageButton createLegacyPageButton(int i, int j, boolean bl, Button.OnPress onPress, boolean bl2){
        return new PageButton(i,j,bl,onPress,bl2){
            private long lastPressTime;
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
                isHovered = Util.getMillis() - lastPressTime <= 300 || clicked(i,j);
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
            protected boolean clicked(double d, double e) {
                return this.active && this.visible && d >= (double)this.getX() && e >= (double)this.getY() && d < (double)(this.getX() + this.getWidth() * 3/2) && e < (double)(this.getY() + this.getHeight() * 3/2);
            }
        };
    }
}
