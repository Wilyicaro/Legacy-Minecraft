package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.LecternScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.util.LegacyComponents;

public class BookPanel extends WidgetPanel {

    public BookPanel(Screen screen) {
        super(screen);
    }

    @Override
    public void init(String name) {
        super.init(name);
        appearance(201, 248);
        pos(value("x", centeredLeftPos(accessor.getScreen())), value("y", centeredTopPos(accessor.getScreen()) - 30));
    }

    public int textX() {
        return x("text.x", 20);
    }

    public int textY() {
        return y("text.y", 37);
    }

    public int editWidth() {
        return value("edit.width", getWidth() - 40);
    }

    public int editHeight() {
        return value("edit.height", getHeight() - 74);
    }

    public int splitWidth() {
        return value("text.width", value("edit.width", 159));
    }

    public int pageNumberX() {
        return x("pageNumber.x", getWidth() - 24);
    }

    public int pageNumberY() {
        return y("pageNumber.y", 22);
    }

    public int pageButtonY() {
        return y("pageButton.y", getHeight() - 34);
    }

    public int previousPageButtonX() {
        return x("previousPageButton.x", 26);
    }

    public int nextPageButtonX() {
        return x("nextPageButton.x", getWidth() - 62);
    }

    public int screenButtonY() {
        return y("screenButton.y", getHeight() + 5);
    }

    public int titleEditY() {
        return y("titleEdit.y", 50);
    }

    public int ownerY() {
        return y("owner.y", 61);
    }

    public int finalizeTextY() {
        return y("finalizeText.y", 85);
    }

    public int editLineLimit(int lineHeight) {
        return Math.max(1, value("edit.lineLimit", editHeight() / lineHeight));
    }

    public int maxPageLines(int lineHeight) {
        return Math.max(1, value("text.maxLines", 176 / lineHeight));
    }

    public float pageButtonScale() {
        return accessor.getFloat(name + ".pageButton.scale", 1.5f);
    }

    private int value(String key, int fallback) {
        return accessor.getInteger(name + "." + key, fallback);
    }

    private int x(String key, int fallback) {
        return getX() + value(key, fallback);
    }

    private int y(String key, int fallback) {
        return getY() + value(key, fallback);
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
                isHovered = Util.getMillis() - lastPressTime <= 300 || /*? if <1.21.4 {*/clicked/*?} else {*//*isMouseOver*//*?}*/(i,j);
                float scale = pageButtonScale();
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(getX(), getY(), scale);
                guiGraphics.pose().scale(scale, scale, scale);
                guiGraphics.pose().translate(-getX(), -getY(), scale);
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
            public boolean /*? if <1.21.4 {*/clicked/*?} else {*//*isMouseOver*//*?}*/(double d, double e) {
                float scale = pageButtonScale();
                return this.active && this.visible && d >= (double)this.getX() && e >= (double)this.getY() && d < (double)(this.getX() + this.getWidth() * scale) && e < (double)(this.getY() + this.getHeight() * scale);
            }
        };
    }

    @Override
    public @Nullable Component getAction(Context context) {
        return isFocused() ? context.actionOfContext(KeyContext.class, c-> canTakeBook(c.key()) ? LegacyComponents.TAKE_BOOK : null) : super.getAction(context);
    }
}
