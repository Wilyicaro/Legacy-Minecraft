package wily.legacy.mixin.base.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.GuiItemRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyGuiItemRenderState;
import wily.legacy.client.LegacyGuiItemRenderer;

import java.util.function.Consumer;

@Mixin(GuiRenderer.class)
public class GuiRendererMixin {
    @Shadow @Final private MultiBufferSource.BufferSource bufferSource;
    @Shadow @Final
    GuiRenderState renderState;
    @Shadow private int frameNumber;
    @Unique
    private Int2ObjectMap<LegacyGuiItemRenderer> guiItemRenderers = new Int2ObjectOpenHashMap<>();

    @ModifyArg(method = "prepareItemElements", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/state/GuiRenderState;forEachItem(Ljava/util/function/Consumer;)V"))
    private Consumer<GuiItemRenderState> prepareItemElements(Consumer<GuiItemRenderState> consumer) {
        return renderState -> {
            if (LegacyGuiItemRenderState.of(renderState).size() == 16) consumer.accept(renderState);
            else guiItemRenderers.computeIfAbsent(LegacyGuiItemRenderState.of(renderState).size(), LegacyGuiItemRenderer::new);
        };
    }

    @Inject(method = "prepareItemElements", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/state/GuiRenderState;forEachItem(Ljava/util/function/Consumer;)V", shift = At.Shift.AFTER))
    private void prepareItemElements(CallbackInfo ci) {
        guiItemRenderers.forEach((i, renderer)-> renderer.prepareItemElements(bufferSource, renderState, frameNumber));
    }

    @Inject(method = "close", at = @At("RETURN"), remap = false)
    private void close(CallbackInfo ci) {
        guiItemRenderers.forEach((i, renderer)-> renderer.close());
    }
}
