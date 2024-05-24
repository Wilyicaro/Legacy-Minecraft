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
import wily.legacy.Legacy4JClient;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.Controller;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.controller.LegacyKeyMapping;
import wily.legacy.util.ScreenUtil;

import java.util.Arrays;
import java.util.Objects;

import static wily.legacy.client.screen.LegacyKeyBindsScreen.NONE;
import static wily.legacy.client.screen.LegacyKeyBindsScreen.SELECTION;

public class ControllerMappingScreen extends PanelVListScreen implements Controller.Event {
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
                ((LegacyKeyMapping)keyMapping).setBinding(((LegacyKeyMapping)keyMapping).getDefaultBinding());
            minecraft.setScreen(this);
        }))).size(240,20).build());
        renderableVList.addOptions(ScreenUtil.getLegacyOptions().selectedController(),ScreenUtil.getLegacyOptions().selectedControllerHandler(),ScreenUtil.getLegacyOptions().invertControllerButtons(),ScreenUtil.getLegacyOptions().leftStickDeadZone(),ScreenUtil.getLegacyOptions().rightStickDeadZone(),ScreenUtil.getLegacyOptions().leftTriggerDeadZone(),ScreenUtil.getLegacyOptions().rightTriggerDeadZone());
        for (KeyMapping keyMapping : keyMappings) {
            String category = keyMapping.getCategory();
            if (!Objects.equals(lastCategory, category)) {
                renderableVList.addRenderables(SimpleLayoutRenderable.create(240, 13, (l -> ((graphics, i, j, f) -> {}))), SimpleLayoutRenderable.create(240, 13, (l -> ((graphics, i, j, f) -> graphics.drawString(font, Component.translatable(category), l.x + 1, l.y + 4, 0x383838, false)))));
                if (category.equals("key.categories.movement"))
                    renderableVList.addOptions(ScreenUtil.getLegacyOptions().invertYController(),ScreenUtil.getLegacyOptions().smoothMovement());
            }
            lastCategory = keyMapping.getCategory();
            LegacyKeyMapping mapping = (LegacyKeyMapping) keyMapping;
            renderableVList.addRenderable(new AbstractButton(0,0,240,20,mapping.getDisplayName()) {
                @Override
                protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
                    if (!isFocused() && isPressed()) selectedKey = null;
                    super.renderWidget(guiGraphics, i, j, f);
                    Component c = isPressed() ? SELECTION : mapping.getBinding() == null ? null : mapping.getBinding().bindingState.getIcon(false);
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
                    ControllerBinding.DOWN_BUTTON.bindingState.block();
                    if (Screen.hasShiftDown() || ControllerBinding.LEFT_STICK_BUTTON.bindingState.pressed){
                        mapping.setBinding(mapping.getDefaultBinding());
                        options.save();
                    } else if (!ControlTooltip.getActiveType().isKeyboard()) selectedKey = mapping;
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
        if (i == InputConstants.KEY_ESCAPE && selectedKey != null) {
            selectedKey.setBinding(null);
            minecraft.options.save();
            selectedKey = null;
            return true;
        }
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
    public void bindingStateTick(BindingState state) {
        if (selectedKey != null) {
            if (!state.canClick() || !state.component.isBindable) return;
            selectedKey.setBinding(!state.is(ControllerBinding.BACK) || selectedKey.self() == Legacy4JClient.keyHostOptions ? state.component : null);
            minecraft.options.save();
            selectedKey = null;
            state.block();
        }
    }
}
