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
import wily.legacy.LegacyMinecraftClient;
import wily.legacy.client.controller.ComponentState;
import wily.legacy.client.controller.ControllerComponent;
import wily.legacy.client.controller.ControllerEvent;
import wily.legacy.client.controller.LegacyKeyMapping;
import wily.legacy.util.ScreenUtil;

import java.util.Arrays;
import java.util.Objects;

import static wily.legacy.client.screen.LegacyKeyBindsScreen.NONE;
import static wily.legacy.client.screen.LegacyKeyBindsScreen.SELECTION;

public class ControllerMappingScreen extends PanelVListScreen implements ControllerEvent {
    protected LegacyKeyMapping selectedKey = null;
    public ControllerMappingScreen(Screen parent, Options options) {
        super(parent, 255, 293, Component.translatable("legacy.controls.controller"));
        renderableVList.layoutSpacing(l->1);
        panel.dp = 3f;
        KeyMapping[] keyMappings = ArrayUtils.clone(options.keyMappings);
        Arrays.sort(keyMappings);
        String lastCategory = null;
        renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.reset_defaults"),button -> minecraft.setScreen(new ConfirmationScreen(this, Component.translatable("legacy.menu.reset_controls"),Component.translatable("legacy.menu.reset_controls_message"), b-> {
            for (KeyMapping keyMapping : keyMappings)
                ((LegacyKeyMapping)keyMapping).setComponent(((LegacyKeyMapping)keyMapping).getDefaultComponent());
            minecraft.setScreen(this);
        }))).size(240,20).build());
        for (KeyMapping keyMapping : keyMappings) {
            String category = keyMapping.getCategory();
            if (!Objects.equals(lastCategory, category)) {
                renderableVList.addRenderables(SimpleLayoutRenderable.create(240, 13, (l -> ((graphics, i, j, f) -> {}))), SimpleLayoutRenderable.create(240, 13, (l -> ((graphics, i, j, f) -> graphics.drawString(font, Component.translatable(category), l.x + 1, l.y + 4, 0x404040, false)))));
                if (category.equals("key.categories.movement")){
                    renderableVList.addRenderable(ScreenUtil.getLegacyOptions().invertYController().createButton(options,0,0,240));
                    renderableVList.addRenderable(ScreenUtil.getLegacyOptions().invertControllerButtons().createButton(options,0,0,240));
                }
            }
            lastCategory = keyMapping.getCategory();
            LegacyKeyMapping mapping = (LegacyKeyMapping) keyMapping;
            renderableVList.addRenderable(new AbstractButton(0,0,240,20,mapping.getDisplayName()) {
                @Override
                protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
                    if (!isFocused() && isPressed()) selectedKey = null;
                    super.renderWidget(guiGraphics, i, j, f);
                    Component c = isPressed() ? SELECTION : mapping.getComponent() == null ? null : mapping.getComponent().componentState.getIcon(false);
                    if (c == null){
                        guiGraphics.drawString(font,NONE, getX() + width - 20 - (font.width(NONE) / 2), getY() + (height -  font.lineHeight) / 2 + 1,0xFFFFFF);
                        return;
                    }
                    RenderSystem.enableBlend();
                    guiGraphics.drawString(font,c, getX() + width - 20 - (font.width(c) / 2), getY() + (height -  font.lineHeight) / 2 + 1,0xFFFFFF,false);
                    RenderSystem.disableBlend();
                }
                private boolean isPressed(){
                    return selectedKey != null && keyMapping == selectedKey.self();
                }
                @Override
                public void onPress() {
                    ControllerComponent.DOWN_BUTTON.componentState.block();
                    if (Screen.hasShiftDown() || ControllerComponent.LEFT_STICK_BUTTON.componentState.pressed) mapping.setComponent(mapping.getDefaultComponent());
                    else if (!ControlTooltip.getActiveType().isKeyboard()) selectedKey = mapping;
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
    public boolean keyPressed(int i, int j, int k) {
        if (i == InputConstants.KEY_ESCAPE && ControlTooltip.getActiveType().isKeyboard()) selectedKey = null;
        if (selectedKey != null) return false;
        return super.keyPressed(i, j, k);
    }

    @Override
    protected void init() {
        panel.height = Math.min(height,293);
        panel.init();
        addRenderableOnly(panel);
        getRenderableVList().init(this,panel.x + 7,panel.y + 6,panel.width - 14,panel.height);
    }

    @Override
    public void componentTick(ComponentState state) {
        if (selectedKey != null) {
            if (!state.canClick()) return;
            if (!state.is(ControllerComponent.BACK) || selectedKey.self() == LegacyMinecraftClient.keyHostOptions) selectedKey.setComponent(state.component);
            selectedKey = null;
            state.block();
        }
    }
}
