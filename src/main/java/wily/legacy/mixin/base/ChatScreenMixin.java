package wily.legacy.mixin.base;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import wily.legacy.client.ControlType;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.Controller;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.ScreenUtil;


@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen implements Controller.Event, ControlTooltip.Event {
    @Shadow public abstract /*? if >1.20.5 {*/void/*?} else {*//*boolean*//*?}*/ handleChatInput(String par1, boolean par2);

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
        ControlTooltip.Renderer.of(this).replace(1,i-> i,c-> ControlType.getActiveType().isKbm() ? input.getValue().isBlank() ? null : input.getValue().startsWith("/") ? LegacyComponents.SEND_COMMAND : LegacyComponents.SEND_MESSAGE : c).add(()-> !ControlType.getActiveType().isKbm() ? ControllerBinding.START.bindingState.getIcon() : null,()-> /*? if >1.20.1 {*/commandSuggestions.isVisible()/*?} else {*//*commandSuggestions.suggestions != null*//*?}*/ ? LegacyComponents.USE_SUGGESTION : input.getValue().isBlank() ? null : input.getValue().startsWith("/") ? LegacyComponents.SEND_COMMAND : LegacyComponents.SEND_MESSAGE);
    }

    @Redirect(method = "init",at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/ChatScreen;input:Lnet/minecraft/client/gui/components/EditBox;", opcode = Opcodes.PUTFIELD))
    private void init(ChatScreen instance, EditBox value){
        //? if >1.20.1 {
        this.input = value;
        value.setHeight(20);
        value.setPosition(4 + Math.round(ScreenUtil.getChatSafeZone()),height - value.getHeight() + (int)(ScreenUtil.getHUDDistance() - 56));
        value.setWidth(width - (8 + Math.round(ScreenUtil.getChatSafeZone()) * 2));
        //?} else {
        /*this.input = new EditBox(minecraft.font, 4 + Math.round(ScreenUtil.getChatSafeZone()),height - value.getHeight() + (int)(ScreenUtil.getHUDDistance() - 56), width - (8 + Math.round(ScreenUtil.getChatSafeZone()) * 2), 20,value.getMessage());
        *///?}
    }
    @Redirect(method = "init",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;setBordered(Z)V"))
    private void setBordered(EditBox instance, boolean bl){
    }
    //? if <1.20.5 {
    /*@Redirect(method = "render",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/CommandSuggestions;render(Lnet/minecraft/client/gui/GuiGraphics;II)V"))
    private void render(CommandSuggestions instance, GuiGraphics guiGraphics, int i, int j){
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0,(int)(ScreenUtil.getHUDDistance() - 56),200);
        instance.render(guiGraphics,i,j);
        guiGraphics.pose().popPose();
    }
    *///?} else {
    @ModifyArg(method = "render",at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"), index = 1)
    private float render(float value){
        return (int)(ScreenUtil.getHUDDistance() - 56);
    }
    //?}
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
            if (/*? if >1.20.1 {*/commandSuggestions.isVisible()/*?} else {*//*commandSuggestions.suggestions != null*//*?}*/) {
                commandSuggestions.suggestions.useSuggestion();
                commandSuggestions.hide();
            }else {
                this.handleChatInput(this.input.getValue(), true);
                onClose();
            }
        }
    }
}
