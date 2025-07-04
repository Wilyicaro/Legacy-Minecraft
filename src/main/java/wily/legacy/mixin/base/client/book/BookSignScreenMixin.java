package wily.legacy.mixin.base.client.book;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookSignScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.screen.BookPanel;

@Mixin(BookSignScreen.class)
public class BookSignScreenMixin extends Screen {

    @Unique
    private BookPanel panel = new BookPanel(this);

    protected BookSignScreenMixin(Component component) {
        super(component);
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void init(CallbackInfo ci){
        panel.init();
        addRenderableOnly(panel);
    }

    @ModifyArg(method = "init", at = @At(value = "NEW", target = "(Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/network/chat/Component;)Lnet/minecraft/client/gui/components/EditBox;"), index = 0)
    public int titleEditX(int x) {
        return panel.x + 20;
    }

    @ModifyArg(method = "init", at = @At(value = "NEW", target = "(Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/network/chat/Component;)Lnet/minecraft/client/gui/components/EditBox;"), index = 1)
    public int titleEditY(int x) {
        return panel.y + 61;
    }

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    public void renderBackground(CallbackInfo ci) {
        ci.cancel();
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"), index = 2)
    public int bookStringX(int x) {
        return panel.x + 20;
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawWordWrap(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/FormattedText;IIIIZ)V"), index = 2)
    public int finalizeWrapX(int x) {
        return panel.x + 20;
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"), index = 3)
    public int bookTitleY(int x) {
        return panel.y + 37;
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"), index = 3)
    public int bookOwnerY(int x) {
        return panel.y + 61;
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawWordWrap(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/FormattedText;IIIIZ)V"), index = 3)
    public int finalizeY(int x) {
        return panel.y + 85;
    }

}
