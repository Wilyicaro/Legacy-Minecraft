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
import wily.factoryapi.FactoryAPIPlatform;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyOption;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.Controller;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.controller.LegacyKeyMapping;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

import java.util.Arrays;
import java.util.Objects;

import static wily.legacy.client.screen.LegacyKeyBindsScreen.NONE;
import static wily.legacy.client.screen.LegacyKeyBindsScreen.SELECTION;

public class ControllerMappingScreen extends PanelVListScreen implements Controller.Event {
    protected LegacyKeyMapping selectedKey = null;
    public ControllerMappingScreen(Screen parent, Options options) {
        super(parent, s-> Panel.centered(s, LegacySprites.PANEL, 255, 293), Component.translatable("legacy.options.selectedController"));
        renderableVList.layoutSpacing(l->1);
        KeyMapping[] keyMappings = ArrayUtils.clone(options.keyMappings);
        Arrays.sort(keyMappings);
        String lastCategory = null;
        renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.reset_defaults"),button -> minecraft.setScreen(new ConfirmationScreen(this, Component.translatable("legacy.menu.reset_controls"),Component.translatable("legacy.menu.reset_controls_message"), b-> {
            for (KeyMapping keyMapping : keyMappings)
                LegacyKeyMapping.of(keyMapping).setBinding(LegacyKeyMapping.of(keyMapping).getDefaultBinding());
            options.save();
            minecraft.setScreen(this);
        }))).size(240,20).build());
        renderableVList.addOptions(LegacyOption.controllerToggleCrouch,LegacyOption.controllerToggleSprint,LegacyOption.selectedController,LegacyOption.selectedControllerHandler,LegacyOption.invertControllerButtons,LegacyOption.controllerSensitivity,LegacyOption.leftStickDeadZone,LegacyOption.rightStickDeadZone,LegacyOption.leftTriggerDeadZone,LegacyOption.rightTriggerDeadZone);
        for (KeyMapping keyMapping : keyMappings) {
            String category = keyMapping.getCategory();
            if (!Objects.equals(lastCategory, category)) {
                renderableVList.addRenderables(SimpleLayoutRenderable.create(240, 13, (l -> ((graphics, i, j, f) -> {}))), SimpleLayoutRenderable.create(240, 13, (l -> ((graphics, i, j, f) -> graphics.drawString(font, Component.translatable(category), l.x + 1, l.y + 4, CommonColor.INVENTORY_GRAY_TEXT.get(), false)))));
                if (category.equals("key.categories.movement"))
                    renderableVList.addOptions(LegacyOption.invertYController,LegacyOption.smoothMovement,LegacyOption.linearCameraMovement);
            }
            lastCategory = keyMapping.getCategory();
            LegacyKeyMapping mapping = (LegacyKeyMapping) keyMapping;
            renderableVList.addRenderable(new AbstractButton(0,0,240,20,mapping.getDisplayName()) {
                @Override
                protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
                    if (!isFocused() && isPressed()) selectedKey = null;
                    super.renderWidget(guiGraphics, i, j, f);
                    Component c = isPressed() ? SELECTION : ((LegacyKeyMapping) keyMapping).getBinding() == null ? NONE : null;
                    if (c != null){
                        guiGraphics.drawString(font,c, getX() + width - 20 - font.width(c) / 2, getY() + (height -  font.lineHeight) / 2 + 1,0xFFFFFF);
                        return;
                    }
                    ControlTooltip.Icon icon = ((LegacyKeyMapping) keyMapping).getBinding().bindingState.getIcon();
                    RenderSystem.enableBlend();
                    icon.render(guiGraphics, getX() + width - 20 - icon.render(guiGraphics,0,0,false,true) / 2, getY() + (height -  font.lineHeight) / 2 + 1,false,false);
                    RenderSystem.disableBlend();
                }
                private boolean isPressed(){
                    return selectedKey != null && keyMapping == selectedKey.self();
                }
                @Override
                public void onPress() {
                    ControllerBinding.DOWN_BUTTON.bindingState.block();
                    if (net.minecraft.client.gui.screens.Screen.hasShiftDown() || ControllerBinding.LEFT_STICK_BUTTON.bindingState.pressed){
                        mapping.setBinding(mapping.getDefaultBinding());
                        options.save();
                    } else if (!ControlType.getActiveType().isKbm()) selectedKey = mapping;
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
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        ScreenUtil.renderDefaultBackground(accessor, guiGraphics, false);
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
        super.init();
        addRenderableOnly(((guiGraphics, i, j, f) -> guiGraphics.drawString(font, FactoryAPIPlatform.getModInfo("minecraft").getVersion() + " " + Legacy4J.VERSION.get(),panel.getX() + panel.getWidth() + 81, panel.getY() + panel.getHeight() - 7,CommonColor.INVENTORY_GRAY_TEXT.get(),false)));
    }

    @Override
    public void renderableVListInit() {
        getRenderableVList().init(panel.x + 7,panel.y + 6,panel.width - 14,panel.height-20);
    }

    @Override
    public void bindingStateTick(BindingState state) {
        if (selectedKey != null) {
            if (!state.canClick() || !state.binding.isBindable) return;
            selectedKey.setBinding(!state.is(ControllerBinding.BACK) || selectedKey.self() == Legacy4JClient.keyHostOptions ? state.binding : null);
            minecraft.options.save();
            selectedKey = null;
            state.block();
        }
    }
}
