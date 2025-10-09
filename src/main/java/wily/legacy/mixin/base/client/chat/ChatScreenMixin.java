package wily.legacy.mixin.base.client.chat;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.ControlType;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.Controller;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.client.LegacyRenderUtil;


@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen implements Controller.Event, ControlTooltip.Event {
    @Shadow public abstract /*? if >1.20.5 {*/void/*?} else {*//*boolean*//*?}*/ handleChatInput(String par1, boolean par2);

    @Shadow protected EditBox input;

    @Shadow
    CommandSuggestions commandSuggestions;

    @Shadow private String initial;

    protected ChatScreenMixin(Component component) {
        super(component);
    }

    @Override
    public void added() {
        super.added();
        ControlTooltip.Renderer.of(this).replace(1,i-> i,c-> ControlType.getActiveType().isKbm() ? input.getValue().isBlank() ? null : input.getValue().startsWith("/") ? LegacyComponents.SEND_COMMAND : LegacyComponents.SEND_MESSAGE : c).add(()-> !ControlType.getActiveType().isKbm() ? ControllerBinding.START.getIcon() : null,()-> /*? if >1.20.1 {*/commandSuggestions.isVisible()/*?} else {*//*commandSuggestions.suggestions != null*//*?}*/ ? LegacyComponents.USE_SUGGESTION : input.getValue().isBlank() ? null : input.getValue().startsWith("/") ? LegacyComponents.SEND_COMMAND : LegacyComponents.SEND_MESSAGE);
    }

    @Inject(method = "init",at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/ChatScreen;input:Lnet/minecraft/client/gui/components/EditBox;", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void init(CallbackInfo ci){
        this.input.setHeight(20);
        this.input.setPosition(4 + Math.round(LegacyRenderUtil.getChatSafeZone()),height - input.getHeight() + (int)(LegacyRenderUtil.getHUDDistance() - 56));
        this.input.setWidth(width - (8 + Math.round(LegacyRenderUtil.getChatSafeZone()) * 2));
    }

    @Override
    public void tick() {
        super.tick();
        if (getFocused() != input) setFocused(input);
    }

    @Redirect(method = "init",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;setBordered(Z)V"))
    private void setBordered(EditBox instance, boolean bl){
    }

    @WrapOperation(method = "render",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/CommandSuggestions;render(Lnet/minecraft/client/gui/GuiGraphics;II)V"))
    private void render(CommandSuggestions instance, GuiGraphics guiGraphics, int i, int j, Operation<Void> original){
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(0, (int)(LegacyRenderUtil.getHUDDistance() - 56));
        original.call(instance, guiGraphics, i, j);
        guiGraphics.pose().popMatrix();
    }

    @ModifyArg(method = "render",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/CommandSuggestions;render(Lnet/minecraft/client/gui/GuiGraphics;II)V"),index = 2)
    private int render(int i){
        return i - (int)(LegacyRenderUtil.getHUDDistance() - 56);
    }

    @ModifyArg(method = "mouseClicked",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/CommandSuggestions;mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;)Z"))
    private MouseButtonEvent mouseClicked(MouseButtonEvent par1){
        return new MouseButtonEvent(par1.x() - (int)(LegacyRenderUtil.getHUDDistance() - 56), par1.y(), par1.buttonInfo());
    }

    @ModifyArg(method = "mouseScrolled",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;scrollChat(I)V"))
    private int mouseScrolled(int i) {
        return i - (int)(LegacyRenderUtil.getHUDDistance() - 56);
    }

    @WrapWithCondition(method = "render",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
    private boolean render(GuiGraphics instance, int i, int j, int k, int l, int m) {
        return false;
    }

    @Override
    public void repositionElements() {
        initial = this.input.getValue();
        super.repositionElements();
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
