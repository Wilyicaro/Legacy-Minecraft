package wily.legacy.mixin.base.client.sign;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.client.screen.KeyboardScreen;
import wily.legacy.client.screen.WidgetPanel;
import wily.legacy.util.client.LegacySoundUtil;

@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin extends Screen implements ControlTooltip.Event {

    @Shadow
    @Final
    private SignBlockEntity sign;
    @Shadow
    private int line;
    @Shadow
    private TextFieldHelper signField;
    private final WidgetPanel panel = new WidgetPanel(this) {
        @Override
        public boolean charTyped(CharacterEvent characterEvent) {
            if (signField.charTyped(characterEvent)) return true;
            return super.charTyped(characterEvent);
        }

        @Override
        public void init(String name) {
            super.init(name);
            size(100, 100);
            pos(centeredLeftPos(AbstractSignEditScreenMixin.this), centeredTopPos(AbstractSignEditScreenMixin.this));
        }

        @Override
        public boolean keyPressed(KeyEvent keyEvent) {
            if (KeyboardScreen.isOpenKey(keyEvent.key())) {
                minecraft.setScreen(new KeyboardScreen(isSign() ? 60 : -100, () -> this, AbstractSignEditScreenMixin.this));
                return true;
            }
            if (keyEvent.isUp() && line > 0) {
                line = line - 1;
                signField.setCursorToEnd();
                return true;
            } else if (!keyEvent.isDown() && keyEvent.key() != 257) {
                return signField.keyPressed(keyEvent) || super.keyPressed(keyEvent);
            } else if (line < 3) {
                line = line + 1;
                signField.setCursorToEnd();
                return true;
            }
            return super.keyPressed(keyEvent);
        }

        @Override
        public @Nullable Component getAction(Context context) {
            return isFocused() ? context.actionOfContext(KeyContext.class, ControlTooltip::getKeyboardAction) : null;
        }
    };

    protected AbstractSignEditScreenMixin(Component component) {
        super(component);
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/Button$Builder;bounds(IIII)Lnet/minecraft/client/gui/components/Button$Builder;"), index = 1)
    private int init(int i) {
        return height / 2 + 80;
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void init(CallbackInfo ci) {
        addWidget(panel);
        setFocused(panel);
    }

    @Inject(method = "renderSignText", at = @At("HEAD"))
    private void renderSignText(GuiGraphics guiGraphics, CallbackInfo ci) {
        guiGraphics.pose().translate(0, isSign() ? -14.5f : 10);
    }

    @Unique
    private boolean isSign() {
        return this.sign.getBlockState().getBlock() instanceof StandingSignBlock || this.sign.getBlockState().getBlock() instanceof WallSignBlock;
    }

    @Redirect(method = "renderSignText", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/SignBlockEntity;getTextLineHeight()I"))
    private int renderSignText(SignBlockEntity instance) {
        return instance.getTextLineHeight() + 5;
    }

    @Redirect(method = "renderSignText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V", ordinal = 1))
    private void renderSignText(GuiGraphics instance, Font arg, String string, int i, int j, int k, boolean bl) {
        if (getFocused() == panel) instance.drawString(arg, string, i, j, k, bl);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawCenteredString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
    public void render(GuiGraphics guiGraphics, Font font, Component component, int i, int j, int k) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate((width - font.width(title) * 1.5f) / 2f, height / 2f - 96);
        guiGraphics.pose().scale(1.5f, 1.5f);
        guiGraphics.drawString(this.font, this.title, 0, 0, -1);
        guiGraphics.pose().popMatrix();
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    public void keyPressed(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(super.keyPressed(keyEvent));
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    public void charTyped(CharacterEvent characterEvent, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(super.charTyped(characterEvent));
    }

    @Inject(method = "onClose", at = @At("RETURN"))
    public void onClose(CallbackInfo info) {
        LegacySoundUtil.playBackSound();
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
    }
}
