package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.ArrayUtils;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.controller.LegacyKeyMapping;
import wily.legacy.util.ScreenUtil;

import java.util.Arrays;
import java.util.Objects;

public class LegacyKeyBindsScreen extends PanelVListScreen{
    protected KeyMapping selectedKey = null;
    public static final Component SELECTION = Component.literal("...");
    public static final Component NONE = Component.translatable("legacy.controls.none");
    public LegacyKeyBindsScreen(Screen parent, Options options) {
        super(parent, 255, 293, Component.translatable("controls.keybinds.title"));
        renderableVList.layoutSpacing(l->1);
        panel.dp = 3f;
        KeyMapping[] keyMappings = ArrayUtils.clone(options.keyMappings);
        Arrays.sort(keyMappings);
        String lastCategory = null;
        renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.reset_defaults"),button -> minecraft.setScreen(new ConfirmationScreen(this, Component.translatable("legacy.menu.reset_keyBinds"),Component.translatable("legacy.menu.reset_keyBinds_message"), b-> {
            for (KeyMapping keyMapping : keyMappings)
                minecraft.options.setKey(keyMapping, keyMapping.getDefaultKey());
            minecraft.setScreen(this);
        }))).size(240,20).build());
        for (KeyMapping keyMapping : keyMappings) {
            String category = keyMapping.getCategory();
            if (!Objects.equals(lastCategory, category))
                renderableVList.addRenderables(SimpleLayoutRenderable.create(240, 13, (l -> ((graphics, i, j, f) -> {}))), SimpleLayoutRenderable.create(240, 13, (l -> ((graphics, i, j, f) -> graphics.drawString(font, Component.translatable(category), l.x + 1, l.y + 4, 0x383838, false)))));
            lastCategory = keyMapping.getCategory();
            renderableVList.addRenderable(new AbstractButton(0,0,240,20,((LegacyKeyMapping)keyMapping).getDisplayName()) {
                @Override
                protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
                    if (!isFocused() && isPressed()) selectedKey = null;
                    super.renderWidget(guiGraphics, i, j, f);
                    Component c = isPressed() ? SELECTION : ControlTooltip.getKeyIcon(((LegacyKeyMapping) keyMapping).getKey().getValue(),false);
                    if (c == null){
                        guiGraphics.drawString(font,NONE, getX() + width - 16 - font.width(NONE), getY() + (height -  font.lineHeight) / 2 + 1,0xFFFFFF);
                        return;
                    }
                    RenderSystem.enableBlend();
                    guiGraphics.drawString(font,c, getX() + width - 16 - font.width(c), getY() + (height -  font.lineHeight) / 2 + 1,0xFFFFFF,false);
                    RenderSystem.disableBlend();
                }
                private boolean isPressed(){
                    return keyMapping == selectedKey;
                }
                @Override
                public void onPress() {
                    if (Screen.hasShiftDown() || ControllerBinding.LEFT_STICK_BUTTON.bindingState.pressed) setAndUpdateKey(keyMapping, keyMapping.getDefaultKey());
                    else selectedKey = keyMapping;
                }
                @Override
                protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
                    ScreenUtil.renderScrollingString(guiGraphics, font, this.getMessage(), this.getX() + 8, this.getY(), getX() + getWidth(), this.getY() + this.getHeight(), j,true);
                }
                @Override
                protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                    defaultButtonNarrationText(narrationElementOutput);
                }
            });
        }
    }

    @Override
    public boolean mouseClicked(double d, double e, int i) {
        if (selectedKey != null) {
            setAndUpdateKey(selectedKey, InputConstants.Type.MOUSE.getOrCreate(i));
            selectedKey = null;
            return true;
        }
        return super.mouseClicked(d, e, i);
    }
    public void setAndUpdateKey(KeyMapping key, InputConstants.Key input){
        minecraft.options.setKey(key,input);
        KeyMapping.resetMapping();
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (selectedKey != null){
            setAndUpdateKey(selectedKey,i == 256 ? InputConstants.UNKNOWN : InputConstants.getKey(i, j));
            selectedKey = null;
            return true;
        }
        return super.keyPressed(i, j, k);
    }

    @Override
    protected void init() {
        panel.height = Math.min(height,293);
        panel.init();
        addRenderableOnly(panel);
        getRenderableVList().init(this,panel.x + 7,panel.y + 6,panel.width - 14,panel.height);
    }
}
