package wily.legacy.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.gui.ComponentPath;
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
    public void render(PoseStack poseStack, int i, int j, float f) {
        poseStack.pose().pushPose();
        poseStack.pose().translate(getX(),getY(),0);
        poseStack.pose().scale(getWidth() / 146f,getHeight() / 180f,1f);
        poseStack.blit(BookViewScreen.BOOK_LOCATION,0,0,20,1,146,180);
        poseStack.pose().popPose();
    }
    public PageButton createLegacyPageButton(int i, int j, boolean bl, Button.OnPress onPress, boolean bl2){
        return new PageButton(i,j,bl,onPress,bl2){
            private long lastPressTime;
            @Override
            public void renderWidget(PoseStack poseStack, int i, int j, float f) {
                isHovered = Util.getMillis() - lastPressTime <= 300 || clicked(i,j);
                poseStack.pose().pushPose();
                poseStack.pose().translate(getX(),getY(),1.5f);
                poseStack.pose().scale(1.5f,1.5f,1.5f);
                poseStack.pose().translate(-getX(),-getY(),1.5f);
                super.renderWidget(poseStack, i, j, f);
                poseStack.pose().popPose();
            }

            @Override
            public void onPress() {
                super.onPress();
                lastPressTime = Util.getMillis();
            }

            @Override
            public void setFocused(boolean bl) {
                if (bl) screen.setFocused(BookPanel.this);
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
