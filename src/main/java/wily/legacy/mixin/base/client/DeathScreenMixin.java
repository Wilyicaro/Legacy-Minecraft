package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.util.Util;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.CommonColor;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.client.screen.ExitConfirmationScreen;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.List;

@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin extends Screen implements ControlTooltip.Event {

    @Shadow
    @Final
    public boolean hardcore;

    @Shadow
    private Button exitToTitleButton;
    @Shadow
    @Final
    private List<Button> exitButtons;
    @Shadow
    private int delayTicker;
    @Unique
    private final long screenInit = Util.getMillis();

    protected DeathScreenMixin(Component component) {
        super(component);
    }

    @Shadow
    protected abstract void handleExitToTitleScreen();

    @Shadow
    protected abstract void setButtonsActive(boolean bl);

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    protected void init(CallbackInfo ci) {
        ci.cancel();
        this.delayTicker = 0;
        this.exitButtons.clear();
        Component component = this.hardcore ? Component.translatable("deathScreen.spectate") : Component.translatable("deathScreen.respawn");
        this.exitButtons.add(this.addRenderableWidget(Button.builder(component, (button) -> {
            this.minecraft.player.respawn();
            button.active = false;
        }).bounds(this.width / 2 - 100, this.height / 2 + 20, 200, 20).build()));
        this.exitButtons.add(exitToTitleButton = this.addRenderableWidget(Button.builder(Component.translatable("menu.quit"), (button) -> {
            this.minecraft.getReportingContext().draftReportHandled(this.minecraft, this, this::handleExitToTitleScreen, true);
        }).bounds(this.width / 2 - 100, this.height / 2 + 45, 200, 20).build()));
        setButtonsActive(false);
    }

    @Inject(method = "handleExitToTitleScreen", at = @At("HEAD"), cancellable = true)
    private void handleExitToTitleScreen(CallbackInfo ci) {
        ci.cancel();
        if (this.hardcore) {
            ExitConfirmationScreen.exit(minecraft, true);
        } else {
            this.minecraft.setScreen(new ExitConfirmationScreen(this));
        }
    }

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void renderBackground(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        float alpha = Math.min((Util.getMillis() - screenInit) / 1200f, 1.0f);
        guiGraphics.fill(0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight(), 3672076 | Mth.ceil(alpha * 160.0F) << 24);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/DeathScreen;visitText(Lnet/minecraft/client/gui/ActiveTextCollector;)V", shift = At.Shift.AFTER))
    private void renderLegacyTitle(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        LegacyRenderUtil.drawOutlinedString(guiGraphics, this.font, this.title, this.width / 2 - this.font.width(this.title) / 2, this.height / 8 + 10, CommonColor.TITLE_TEXT.get(), CommonColor.TITLE_TEXT_OUTLINE.get(), 0.5f);
    }

    @WrapOperation(method = "visitText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/ActiveTextCollector;accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/network/chat/Component;)V", ordinal = 0))
    private void hideVanillaTitle(ActiveTextCollector collector, TextAlignment alignment, int x, int y, Component component, Operation<Void> original) {
    }

    @ModifyArg(method = "visitText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/ActiveTextCollector;accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/network/chat/Component;)V", ordinal = 1), index = 2)
    private int legacyCauseOfDeathY(int y) {
        return this.height / 2 - 24;
    }

    @WrapOperation(method = "visitText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/ActiveTextCollector;accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/network/chat/Component;)V", ordinal = 2))
    private void hideDeathScore(ActiveTextCollector collector, TextAlignment alignment, int x, int y, Component component, Operation<Void> original) {
    }

}
