package wily.legacy.mixin;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.client.screen.WidgetPanel;

@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin extends Screen {
    private ControlTooltip.Renderer controlTooltipRenderer = ControlTooltip.defaultScreen(this);
    private WidgetPanel panel = new WidgetPanel(this,100,100);

    @Shadow protected abstract void renderSign(GuiGraphics arg);

    @Shadow @Final private SignBlockEntity sign;

    @Shadow private int line;

    @Shadow private TextFieldHelper signField;

    protected AbstractSignEditScreenMixin(Component component) {
        super(component);
    }
    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/Button$Builder;bounds(IIII)Lnet/minecraft/client/gui/components/Button$Builder;"), index = 1)
    private int init(int i) {
        return height/ 2 + 80;
    }
    @Inject(method = "init", at = @At("HEAD"))
    private void init(CallbackInfo ci) {
        addWidget(panel);
        setFocused(panel);
    }
    @ModifyArg(method = "offsetSign", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"), index = 1)
    private float offsetSign(float f){
        return height/2f - 26.5f;
    }
    @Redirect(method = "renderSignText", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    private void renderSignText(PoseStack instance, float x, float y, float z){
        instance.translate(x,this.sign.getBlockState().getBlock() instanceof StandingSignBlock || this.sign.getBlockState().getBlock() instanceof WallSignBlock ? y - 14.5 : y + 10,10);
    }
    @Redirect(method = "renderSignText", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/SignBlockEntity;getTextLineHeight()I"))
    private int renderSignText(SignBlockEntity instance){
        return instance.getTextLineHeight() + 5;
    }

    @Redirect(method = "renderSignText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I", ordinal = 1))
    private int renderSignText(GuiGraphics instance, Font arg, String string, int i, int j, int k, boolean bl){
        return getFocused() == panel ? instance.drawString(arg,string,i,j,k,bl) : -1;
    }
    @Inject(method = "render",at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        super.render(guiGraphics, i, j, f);
        Lighting.setupForFlatItems();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate((width - font.width(title)*1.5f)/ 2f, height / 2f - 96, 0);
        guiGraphics.pose().scale(1.5f,1.5f,1.5f);
        guiGraphics.drawString(this.font, this.title, 0, 0, 16777215);
        guiGraphics.pose().popPose();
        this.renderSign(guiGraphics);
        Lighting.setupFor3DItems();
        controlTooltipRenderer.render(guiGraphics, i, j, f);
    }
    @Inject(method = "keyPressed",at = @At("HEAD"), cancellable = true)
    public void keyPressed(int i, int j, int k, CallbackInfoReturnable<Boolean> cir) {
        if (i == 265) {
            setFocused(panel);
            this.line = Math.max(0,this.line - 1);
            this.signField.setCursorToEnd();
            cir.setReturnValue(true);
            return;
        } else if (i != 264 && i != 257 && i != 335) {
            cir.setReturnValue(this.signField.keyPressed(i) || super.keyPressed(i, j, k));
            return;
        } else if (line < 3) {
            setFocused(panel);
            this.line = this.line + 1;
            this.signField.setCursorToEnd();
            cir.setReturnValue(true);
            return;
        }cir.setReturnValue(super.keyPressed(i,j,k));
    }
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {

    }

}
