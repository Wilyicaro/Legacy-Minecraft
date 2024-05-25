
package wily.legacy.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.screen.ExitConfirmationScreen;
import wily.legacy.util.ScreenUtil;

import java.util.List;

@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin extends Screen {

    @Shadow @Final public boolean hardcore;

    @Shadow @Final public Component causeOfDeath;

    @Shadow @Nullable private Button exitToTitleButton;

    @Shadow @Final private static ResourceLocation DRAFT_REPORT_SPRITE;

    @Shadow @Nullable protected abstract Style getClickedComponentStyleAt(int i);

    @Shadow @Final private List<Button> exitButtons;


    @Shadow protected abstract void handleExitToTitleScreen();

    @Shadow protected abstract void setButtonsActive(boolean bl);

    protected DeathScreenMixin(Component component) {
        super(component);
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    protected void init(CallbackInfo ci) {
        ci.cancel();
        this.exitButtons.clear();
        Component component = this.hardcore ? Component.translatable("deathScreen.spectate") : Component.translatable("deathScreen.respawn");
        this.exitButtons.add(this.addRenderableWidget(Button.builder(component, (button) -> {
            this.minecraft.player.respawn();
            button.active = false;
        }).bounds(this.width / 2 - 100, this.height / 4 + 72, 200, 20).build()));
        this.exitToTitleButton = this.addRenderableWidget(Button.builder(Component.translatable("menu.quit"), (button) -> {
            this.minecraft.getReportingContext().draftReportHandled(this.minecraft, this, this::handleExitToTitleScreen, true);
        }).bounds(this.width / 2 - 100, this.height / 4 + 96, 200, 20).build());
        this.exitButtons.add(this.exitToTitleButton);
        setButtonsActive(false);
    }
    @Inject(method = "handleExitToTitleScreen", at = @At("HEAD"), cancellable = true)
    private void handleExitToTitleScreen(CallbackInfo ci) {
        ci.cancel();
        if (this.hardcore) {
            ExitConfirmationScreen.exit(minecraft,true);
        } else {
            this.minecraft.setScreen(new ExitConfirmationScreen(this));
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        renderBackground(guiGraphics,i,j,f);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(2.0F, 2.0F, 2.0F);
        ScreenUtil.drawOutlinedString(guiGraphics,this.font, this.title, (this.width - font.width(title) * 2) / 4, 40, 16777215,0,1f);
        guiGraphics.pose().popPose();
        if (this.causeOfDeath != null) {
            guiGraphics.drawCenteredString(this.font, this.causeOfDeath, this.width / 2, 110, 16777215);
            if (j > 110 && j < 119) guiGraphics.renderComponentHoverEffect(this.font, this.getClickedComponentStyleAt(i), i, j);
        }

        if (this.exitToTitleButton != null && this.minecraft.getReportingContext().hasDraftReport()) {
            guiGraphics.blitSprite(DRAFT_REPORT_SPRITE, this.exitToTitleButton.getX() + this.exitToTitleButton.getWidth() - 17, this.exitToTitleButton.getY() + 3, 15, 15);
        }
        for (Renderable renderable : this.renderables)
            renderable.render(guiGraphics, i, j, f);
    }

}
