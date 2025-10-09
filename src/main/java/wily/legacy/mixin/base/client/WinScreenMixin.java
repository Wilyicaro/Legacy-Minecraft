package wily.legacy.mixin.base.client;

//? if <1.21.5 {
/*import com.mojang.blaze3d.platform.GlStateManager;
*///?}
import com.mojang.blaze3d.platform.InputConstants;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.objectweb.asm.Opcodes;
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
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.Legacy4J;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ControlType;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;

import java.io.Reader;
import java.util.List;

@Mixin(WinScreen.class)
public abstract class WinScreenMixin extends Screen implements ControlTooltip.Event {

    @Unique
    ResourceLocation POEM_BACKGROUND = Legacy4J.createModLocation("textures/gui/end_poem_background.png");
    @Unique
    ResourceLocation CREDITS_BACKGROUND = Legacy4J.createModLocation(/*? if <1.21 {*//*"textures/gui/credits_background_120.png"*//*?} else {*/"textures/gui/credits_background.png"/*?}*/);
    @Unique
    ResourceLocation CREDITS_BACKGROUND_FADE = Legacy4J.createModLocation(/*? if <1.21 {*//*"textures/gui/credits_background_fade_120.png"*//*?} else {*/"textures/gui/credits_background_fade.png"/*?}*/);
    @Shadow @Final private boolean poem;
    @Unique
    private IntSet titleLines;
    @Unique
    private IntSet nameLines;

    protected WinScreenMixin(Component component) {
        super(component);
    }

    @Override
    public void added() {
        super.added();
        ControlTooltip.Renderer.of(this).clear().add(()-> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_ESCAPE) : ControllerBinding.RIGHT_BUTTON.getIcon(), ()-> poem ? CommonComponents.GUI_CONTINUE : CommonComponents.GUI_BACK);
    }
    //? if >=1.20.5 {
    @Shadow protected abstract void renderVignette(GuiGraphics arg);
    //?} else {
    /*@Unique
    private static final ResourceLocation VIGNETTE_LOCATION = new ResourceLocation("textures/misc/credits_vignette.png");
    *///?}
    @Shadow private IntSet centeredLines;

    @Shadow private float scroll;

    @Shadow private float scrollSpeed;

    @Shadow private List<FormattedCharSequence> lines;

    @Shadow private boolean speedupActive;


    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        this.scroll = Math.max(0.0F, this.scroll + f * this.scrollSpeed * (poem ? 1.0f : 4f));
        float g = -this.scroll;
        int m = height;
        if (poem) {
            FactoryGuiGraphics.of(guiGraphics).blit(POEM_BACKGROUND,0,0,0,Util.getMillis() / 280f, guiGraphics.guiWidth(), guiGraphics.guiHeight(),80,80);
            renderVignette(guiGraphics);
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(0.0F, g);
            LegacyFontUtil.defaultFontOverride = LegacyFontUtil.MOJANGLES_11_FONT;
            int k = this.width / 2 - 161;
            for(int n = 0; n < this.lines.size(); ++n) {
                if (n == this.lines.size() - 1) {
                    float h = (float)m + g - (float)(this.height / 2 - 6);
                    if (h < 0.0F) {
                        guiGraphics.pose().translate(0.0F, -h);
                    }
                }

                if ((float)m + g + 12.0F + 8.0F > 0.0F && (float)m + g < (float)this.height) {
                    FormattedCharSequence formattedCharSequence = this.lines.get(n);
                    boolean centered = this.centeredLines.contains(n);
                    guiGraphics.pose().pushMatrix();
                    guiGraphics.pose().translate(k,centered ? width / 2 : m);
                    guiGraphics.pose().scale(2,2);
                    if (centered) {
                        guiGraphics.drawCenteredString(this.font, formattedCharSequence, 0,0, 0xFFFFFFFF);
                    } else {
                        guiGraphics.drawString(this.font, formattedCharSequence, 0,0, 0xFFFFFFFF);
                    }
                    guiGraphics.pose().popMatrix();
                }

                m += 72;
            }
            guiGraphics.pose().popMatrix();
            LegacyFontUtil.defaultFontOverride = null;
        } else {
            int fixedWidth = Math.max(guiGraphics.guiHeight() * 16 / 9, guiGraphics.guiWidth());
            int fixedHeight = Math.max(guiGraphics.guiWidth() * 9 / 16, guiGraphics.guiHeight());
            float x = (fixedWidth - guiGraphics.guiWidth()) / 2f;
            float y = (fixedHeight - guiGraphics.guiHeight()) / 2f;
            FactoryGuiGraphics.of(guiGraphics).blit(CREDITS_BACKGROUND,0,0, x,y, guiGraphics.guiWidth(), guiGraphics.guiHeight(), fixedWidth, fixedHeight);

            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(0.0F, g);
            int k = this.width / 2;

            for(int n = 0; n < this.lines.size(); ++n) {
                if (n == this.lines.size() - 1) {
                    float h = (float)m + g - (float)(this.height / 2 - 6);
                    if (h < 0.0F) {
                        guiGraphics.pose().translate(0.0F, -h);
                    }
                }

                if ((float)m + g + 12.0F + 8.0F > 0.0F && (float)m + g < (float)this.height) {
                    FormattedCharSequence formattedCharSequence = this.lines.get(n);
                    boolean title = titleLines.contains(n);
                    guiGraphics.pose().pushMatrix();
                    guiGraphics.pose().translate(k - font.width(formattedCharSequence) * (title ? 1.5f : 1) / 2, m);
                    if (title) guiGraphics.pose().scale(1.5f,1.5f);
                    LegacyRenderUtil.drawOutlinedString(guiGraphics, font, formattedCharSequence, 0, 0, (nameLines.contains(n) ? CommonColor.YELLOW : CommonColor.WHITE).get(), 0xFF000000, 0.4f);
                    guiGraphics.pose().popMatrix();
                }

                m += 18;
            }
            guiGraphics.pose().popMatrix();
            FactoryScreenUtil.enableBlend();
            FactoryGuiGraphics.of(guiGraphics).disableDepthTest();
            FactoryGuiGraphics.of(guiGraphics).blit(CREDITS_BACKGROUND_FADE,0,0, x, y, guiGraphics.guiWidth(), guiGraphics.guiHeight(), fixedWidth, fixedHeight);
            FactoryGuiGraphics.of(guiGraphics).enableDepthTest();
            FactoryScreenUtil.disableBlend();
            LegacyRenderUtil.renderLogo(guiGraphics);
        }
        ci.cancel();
    }
    @Inject(method = "keyPressed", at = @At("HEAD"))
    public void keyPressed(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        if (keyEvent.isUp() || keyEvent.isDown()) {
            speedupActive = true;
        }
    }
    @Inject(method = "keyReleased", at = @At("HEAD"))
    public void keyReleased(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        if (keyEvent.isUp() || keyEvent.isDown()) {
            speedupActive = false;
        }
    }
    @Inject(method = "init", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/WinScreen;lines:Ljava/util/List;", opcode = Opcodes.PUTFIELD))
    private void init(CallbackInfo ci){
        this.titleLines = new IntOpenHashSet();
        this.nameLines = new IntOpenHashSet();
    }

    @Redirect(method = "addPoemFile", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/WinScreen;addEmptyLine()V", ordinal = 0))
    private void addPoemFile(WinScreen instance) {
    }
    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = /*? if <1.20.5 {*//*"Lnet/minecraft/client/gui/screens/WinScreen;wrapCreditsIO(Ljava/lang/String;Lnet/minecraft/client/gui/screens/WinScreen$CreditsReader;)V"*//*?} else {*/"Lnet/minecraft/client/gui/screens/WinScreen;wrapCreditsIO(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/client/gui/screens/WinScreen$CreditsReader;)V"/*?}*/, ordinal = 0))
    private /*? if <1.20.5 {*//*String*//*?} else {*/ResourceLocation/*?}*/ addPoemFile(/*? if <1.20.5 {*//*String*//*?} else {*/ResourceLocation/*?}*/ arg) {
        ResourceLocation langLocation = Legacy4J.createModLocation("end_poem/" + minecraft.getLanguageManager().getSelected()+ ".txt");
        return minecraft.getResourceManager().getResource(langLocation).isPresent() ? langLocation/*? if <1.20.5 {*//*.toString()*//*?}*/ : arg;
    }
    @Redirect(method = "addCreditsFile", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/WinScreen;addCreditsLine(Lnet/minecraft/network/chat/Component;ZZ)V",ordinal = 0))
    private void addCreditsFileHeading(WinScreen instance, Component arg, boolean bl, boolean bl2) {
    }
    @Redirect(method = "addCreditsFile", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/WinScreen;addCreditsLine(Lnet/minecraft/network/chat/Component;ZZ)V",ordinal = 2))
    private void addCreditsFileSecondHeading(WinScreen instance, Component arg, boolean bl, boolean bl2) {
    }
    @ModifyArg(method = "addCreditsFile", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;literal(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;", ordinal = 0))
    private String addCreditsFileSectionToUppercase(String string) {
        return string.toUpperCase();
    }
    @Redirect(method = "addCreditsFile", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/MutableComponent;withStyle(Lnet/minecraft/ChatFormatting;)Lnet/minecraft/network/chat/MutableComponent;"))
    private MutableComponent addCreditsFileSectionStyle(MutableComponent instance, ChatFormatting arg) {
        return instance;
    }
    @Inject(method = "addCreditsFile", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/WinScreen;addCreditsLine(Lnet/minecraft/network/chat/Component;ZZ)V", ordinal = 1))
    private void addCreditsFileSectionTitle(Reader reader, CallbackInfo ci) {
        titleLines.add(lines.size());
    }
    @Inject(method = "addCreditsFile", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/WinScreen;addCreditsLine(Lnet/minecraft/network/chat/Component;ZZ)V", ordinal = 3))
    private void addCreditsFileDisciplineTitle(Reader reader, CallbackInfo ci) {
        titleLines.add(lines.size());
    }

    @Inject(method = "addCreditsFile", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/WinScreen;addCreditsLine(Lnet/minecraft/network/chat/Component;ZZ)V", ordinal = 5))
    private void addCreditsFileNames(Reader reader, CallbackInfo ci) {
        nameLines.add(lines.size());
    }

    @Redirect(method = "addCreditsFile", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;literal(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;", ordinal = 3))
    private MutableComponent addCreditsFileRemoveNameSpacing(String string) {
        return Component.empty();
    }
    @ModifyArg(method = "addCreditsFile", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/MutableComponent;withStyle(Lnet/minecraft/ChatFormatting;)Lnet/minecraft/network/chat/MutableComponent;", ordinal = 3))
    private ChatFormatting addCreditsFile(ChatFormatting arg) {
        return ChatFormatting.YELLOW;
    }

}
