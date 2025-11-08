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
    private final BookPanel panel = new BookPanel(this);

    protected BookSignScreenMixin(Component component) {
        super(component);
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void init(CallbackInfo ci) {
        panel.init();
        addRenderableOnly(panel);
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;<init>(Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/network/chat/Component;)V"), index = 1)
    public int titleEditX(int x) {
        return panel.x + 20;
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;<init>(Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/network/chat/Component;)V"), index = 2)
    public int titleEditY(int x) {
        return panel.y + 50;
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/Button$Builder;bounds(IIII)Lnet/minecraft/client/gui/components/Button$Builder;", ordinal = 0), index = 0)
    public int finalizeButtonX(int x) {
        return this.width / 2 - 108;
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/Button$Builder;bounds(IIII)Lnet/minecraft/client/gui/components/Button$Builder;", ordinal = 1), index = 0)
    public int cancelButtonX(int x) {
        return this.width / 2 + 8;
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/Button$Builder;bounds(IIII)Lnet/minecraft/client/gui/components/Button$Builder;"), index = 1)
    public int buttonsY(int x) {
        return panel.y + panel.height + 5;
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
    public int wrapX(int x) {
        return panel.x + 20;
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V", ordinal = 0), index = 3)
    public int bookTitleY(int x) {
        return panel.y + 37;
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V", ordinal = 1), index = 3)
    public int bookOwnerY(int x) {
        return panel.y + 61;
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawWordWrap(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/FormattedText;IIIIZ)V"), index = 3)
    public int finalizeY(int x) {
        return panel.y + 85;
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawWordWrap(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/FormattedText;IIIIZ)V"), index = 4)
    public int wrapWidth(int x) {
        return 159;
    }

}
