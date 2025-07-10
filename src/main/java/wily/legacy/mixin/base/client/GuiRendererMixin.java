package wily.legacy.mixin.base.client;

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
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
    private Long2ObjectMap<LegacyGuiItemRenderer> guiItemRenderers = new Long2ObjectArrayMap<>();

    @ModifyArg(method = "prepareItemElements", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/state/GuiRenderState;forEachItem(Ljava/util/function/Consumer;)V"))
    private Consumer<GuiItemRenderState> prepareItemElements(Consumer<GuiItemRenderState> consumer) {
        return renderState -> {
            LegacyGuiItemRenderState legacyRenderState = LegacyGuiItemRenderState.of(renderState);
            if (legacyRenderState.size() == 16 && legacyRenderState.opacity() == 1.0) consumer.accept(renderState);
            else guiItemRenderers.computeIfAbsent(((long) legacyRenderState.size() << 32) | (Float.floatToIntBits(legacyRenderState.opacity()) & 4294967295L), LegacyGuiItemRenderer::new);
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
