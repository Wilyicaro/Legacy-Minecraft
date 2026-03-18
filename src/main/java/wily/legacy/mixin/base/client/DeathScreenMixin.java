package wily.legacy.mixin.base.client;

import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
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

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawCenteredString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V", ordinal = 0))
    private void renderLegacyTitle(GuiGraphics guiGraphics, Font font, Component component, int i, int j, int k) {
        LegacyRenderUtil.drawOutlinedString(guiGraphics, font, component, i - font.width(component) / 2, this.height / 8 + 10, CommonColor.TITLE_TEXT.get(), CommonColor.TITLE_TEXT_OUTLINE.get(), 0.5f);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawCenteredString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V", ordinal = 1))
    private void renderLegacyCauseOfDeath(GuiGraphics guiGraphics, Font font, Component component, int i, int j, int k) {
        guiGraphics.drawCenteredString(font, component, i, this.height / 2 - 24, k);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawCenteredString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V", ordinal = 2))
    private void hideDeathScore(GuiGraphics guiGraphics, Font font, Component component, int i, int j, int k) {
    }

    @ModifyConstant(method = "mouseClicked", constant = @Constant(doubleValue = 85.0D))
    private double legacyCauseOfDeathClickY(double d) {
        return this.height / 2.0 - 24.0;
    }

    @ModifyConstant(method = {"render", "mouseClicked"}, constant = @Constant(intValue = 85))
    private int legacyCauseOfDeathY(int i) {
        return this.height / 2 - 24;
    }

}
