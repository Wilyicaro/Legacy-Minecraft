package wily.legacy.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import wily.legacy.client.controller.Controller;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.util.ScreenUtil;

public class LegacyScreen extends Screen implements Controller.Event, ControlTooltip.Event {
    public Screen parent;

    protected LegacyScreen(Component component) {
        super(component);
    }
    public void renderDefaultBackground(PoseStack poseStack, int i, int j, float f){
        ScreenUtil.renderDefaultBackground(poseStack);
    }

    @Override
    public void render(PoseStack poseStack, int i, int j, float f) {
        renderDefaultBackground(poseStack, i, j, f);
        super.render(poseStack, i, j, f);
    }

    @Override
    public void renderBackground(PoseStack poseStack) {
        renderDefaultBackground(poseStack,0,0,0);
    }

    @Override
    public void onClose() {
        ScreenUtil.playSimpleUISound(LegacyRegistries.BACK.get(),1.0f);
        this.minecraft.setScreen(parent);
    }
}
