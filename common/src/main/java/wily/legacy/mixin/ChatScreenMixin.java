package wily.legacy.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
public abstract class ChatScreenMixin extends Screen implements Controller.Event,ControlTooltip.Event {
    @Shadow public abstract boolean handleChatInput(String string, boolean bl);

    @Shadow protected EditBox input;

    @Shadow protected abstract void setChatLine(String string);


    @Shadow CommandSuggestions commandSuggestions;

    protected ChatScreenMixin(Component component) {
        super(component);
    }

    @Override
    public void added() {
        super.added();
        ControlTooltip.Renderer.of(this).set(0,()-> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_RETURN) : ControllerBinding.DOWN_BUTTON.bindingState.getIcon(),()-> ControlType.getActiveType().isKbm() ? Util.isBlank(input.getValue()) ? null : ControlTooltip.getAction( input.getValue().startsWith("/") ? "legacy.action.send_command" : "legacy.action.send_message") : ControlTooltip.getSelectMessage(this)).add(()-> !ControlType.getActiveType().isKbm() ? ControllerBinding.START.bindingState.getIcon() : null,()-> commandSuggestions.suggestions != null ? ControlTooltip.getAction("legacy.action.use_suggestion") : Util.isBlank(input.getValue()) ? null : ControlTooltip.getAction(input.getValue().startsWith("/") ? "legacy.action.send_command" : "legacy.action.send_message"));
    }

    @Redirect(method = "init",at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/ChatScreen;input:Lnet/minecraft/client/gui/components/EditBox;", opcode = Opcodes.PUTFIELD))
    private void init(ChatScreen instance, EditBox value){
        this.input = new EditBox(this.minecraft.fontFilterFishy, 4 + Math.round(ScreenUtil.getChatSafeZone()),height - value.getHeight() + (int)(ScreenUtil.getHUDDistance() - 56), width - (8 + Math.round(ScreenUtil.getChatSafeZone()) * 2), 20, Component.translatable("chat.editBox")){

            @Override
            protected MutableComponent createNarrationMessage() {
                return super.createNarrationMessage().append(ChatScreenMixin.this.commandSuggestions.getNarrationMessage());
            }
        };
    }
    @Redirect(method = "init",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;setBordered(Z)V"))
    private void setBordered(EditBox instance, boolean bl){
    }
    @Redirect(method = "keyPressed",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"))
    private void keyPressed(Minecraft instance, Screen old){
    }
    @Redirect(method = "keyPressed",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/ChatScreen;handleChatInput(Ljava/lang/String;Z)Z"))
    private boolean keyPressed(ChatScreen instance, String string, boolean bl, int i, int j, int k){
        if (ControllerBinding.DOWN_BUTTON.bindingState.pressed) input.keyPressed(i, j, k);
        else{
            handleChatInput(string, bl);
            onClose();
        }
        return true;
    }
    @Redirect(method = "render",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/CommandSuggestions;render(Lnet/minecraft/client/gui/GuiGraphics;II)V"))
    private void render(CommandSuggestions instance, GuiGraphics guiGraphics, int i, int j){
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0,(int)(ScreenUtil.getHUDDistance() - 56),200);
        instance.render(guiGraphics,i,j);
        guiGraphics.pose().popPose();
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
            if (commandSuggestions.suggestions != null) {
                commandSuggestions.suggestions.useSuggestion();
                commandSuggestions.hide();
            }else {
                this.handleChatInput(this.input.getValue(), true);
                onClose();
            }
        }
    }
}
