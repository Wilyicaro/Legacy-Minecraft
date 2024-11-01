package wily.legacy.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import wily.legacy.client.ControlType;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.Controller;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.util.ScreenUtil;


@Mixin(ChatScreen.class)
public class ChatScreenMixin extends Screen implements Controller.Event, ControlTooltip.Event {
    @Shadow
    public void handleChatInput(String string, boolean bl) {

    }

    @Shadow protected EditBox input;

    @Shadow
    private void setChatLine(String string) {

    }


    @Shadow
    CommandSuggestions commandSuggestions;

    protected ChatScreenMixin(Component component) {
        super(component);
    }

    @Override
    public void added() {
        super.added();
        ControlTooltip.Renderer.of(this).set(0,()-> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_RETURN) : ControllerBinding.DOWN_BUTTON.bindingState.getIcon(),()-> ControlType.getActiveType().isKbm() ? StringUtil.isBlank(input.getValue()) ? null : ControlTooltip.getAction( input.getValue().startsWith("/") ? "legacy.action.send_command" : "legacy.action.send_message") : ControlTooltip.getSelectMessage(this)).add(()-> !ControlType.getActiveType().isKbm() ? ControllerBinding.START.bindingState.getIcon() : null,()-> commandSuggestions.isVisible() ? ControlTooltip.getAction("legacy.action.use_suggestion") : StringUtil.isBlank(input.getValue()) ? null : ControlTooltip.getAction(input.getValue().startsWith("/") ? "legacy.action.send_command" : "legacy.action.send_message"));
    }

    @Redirect(method = "init",at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/ChatScreen;input:Lnet/minecraft/client/gui/components/EditBox;", opcode = Opcodes.PUTFIELD))
    private void init(ChatScreen instance, EditBox value){
        this.input = value;
        value.setHeight(20);
        value.setPosition(4 + Math.round(ScreenUtil.getChatSafeZone()),height - value.getHeight() + (int)(ScreenUtil.getHUDDistance() - 56));
        value.setWidth(width - (8 + Math.round(ScreenUtil.getChatSafeZone()) * 2));
    }
    @Redirect(method = "init",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;setBordered(Z)V"))
    private void setBordered(EditBox instance, boolean bl){
    }

    @ModifyArg(method = "render",at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"), index = 1)
    private float render(float value){
        return (int)(ScreenUtil.getHUDDistance() - 56);
    }
    @ModifyArg(method = "render",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/CommandSuggestions;render(Lnet/minecraft/client/gui/GuiGraphics;II)V"),index = 2)
    private int render(int i){
        return i - (int)(ScreenUtil.getHUDDistance() - 56);
    }
    @ModifyArg(method = "mouseClicked",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/CommandSuggestions;mouseClicked(DDI)Z"),index = 1)
    private double mouseClicked(double d){
        return d - (int)(ScreenUtil.getHUDDistance() - 56);
    }
    @Redirect(method = "render",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
    private void render(GuiGraphics instance, int i, int j, int k, int l, int m){

    }

    @Override
    public void repositionElements() {
        String string = this.input.getValue();
        super.repositionElements();
        this.setChatLine(string);
        this.commandSuggestions.updateCommandInfo();
    }

    @Override
    public void bindingStateTick(BindingState state) {
        if (state.is(ControllerBinding.START) && state.justPressed) {
            if (commandSuggestions.isVisible()) {
                commandSuggestions.suggestions.useSuggestion();
                commandSuggestions.hide();
            }else {
                this.handleChatInput(this.input.getValue(), true);
                onClose();
            }
        }
    }
}
