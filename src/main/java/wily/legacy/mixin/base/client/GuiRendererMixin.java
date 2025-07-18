package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.GuiEntityRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.GuiItemRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.pip.GuiEntityRenderState;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4J;
import wily.legacy.client.LegacyGuiEntityRenderer;
import wily.legacy.client.LegacyGuiItemRenderState;
import wily.legacy.client.LegacyGuiItemRenderer;
import wily.legacy.client.LegacyOptions;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Mixin(GuiRenderer.class)
public class GuiRendererMixin {
    @Shadow @Final private MultiBufferSource.BufferSource bufferSource;
    @Shadow @Final
    GuiRenderState renderState;
    @Shadow private int frameNumber;
    @Unique
    private Long2ObjectMap<LegacyGuiItemRenderer> guiItemRenderers = new Long2ObjectArrayMap<>();
    @Unique
    private List<GuiEntityRenderer> guiEntityRenderers;

    @Inject(method = "<init>", at = @At("TAIL"))
    void initTail(GuiRenderState guiRenderState, MultiBufferSource.BufferSource bufferSource, List list, CallbackInfo ci) {
        guiEntityRenderers = List.of(
                new GuiEntityRenderer(bufferSource, Minecraft.getInstance().getEntityRenderDispatcher()),
                new GuiEntityRenderer(bufferSource, Minecraft.getInstance().getEntityRenderDispatcher()),
                new GuiEntityRenderer(bufferSource, Minecraft.getInstance().getEntityRenderDispatcher()),
                new GuiEntityRenderer(bufferSource, Minecraft.getInstance().getEntityRenderDispatcher()),
                new GuiEntityRenderer(bufferSource, Minecraft.getInstance().getEntityRenderDispatcher()),
                new GuiEntityRenderer(bufferSource, Minecraft.getInstance().getEntityRenderDispatcher()),
                new GuiEntityRenderer(bufferSource, Minecraft.getInstance().getEntityRenderDispatcher()),
                new GuiEntityRenderer(bufferSource, Minecraft.getInstance().getEntityRenderDispatcher()),
                new GuiEntityRenderer(bufferSource, Minecraft.getInstance().getEntityRenderDispatcher()),
                new GuiEntityRenderer(bufferSource, Minecraft.getInstance().getEntityRenderDispatcher()),
                new GuiEntityRenderer(bufferSource, Minecraft.getInstance().getEntityRenderDispatcher()),
                new GuiEntityRenderer(bufferSource, Minecraft.getInstance().getEntityRenderDispatcher()),
                new GuiEntityRenderer(bufferSource, Minecraft.getInstance().getEntityRenderDispatcher()),
                new GuiEntityRenderer(bufferSource, Minecraft.getInstance().getEntityRenderDispatcher()),
                new GuiEntityRenderer(bufferSource, Minecraft.getInstance().getEntityRenderDispatcher()),
                new GuiEntityRenderer(bufferSource, Minecraft.getInstance().getEntityRenderDispatcher()),
                new GuiEntityRenderer(bufferSource, Minecraft.getInstance().getEntityRenderDispatcher()),
                new GuiEntityRenderer(bufferSource, Minecraft.getInstance().getEntityRenderDispatcher()),
                new GuiEntityRenderer(bufferSource, Minecraft.getInstance().getEntityRenderDispatcher()),
                new GuiEntityRenderer(bufferSource, Minecraft.getInstance().getEntityRenderDispatcher())
        );
    }

    @Inject(method = "prepareItemElements", at = @At("HEAD"))
    private void prepareItemElementsHead(CallbackInfo ci) {
        if (guiItemRenderers == null) {
            Legacy4J.LOGGER.error("that can't be!");
            guiItemRenderers = new Long2ObjectArrayMap<>();
        }
        guiItemRenderers.forEach((i, renderer)-> renderer.markInvalid());
    }

    @ModifyArg(method = "prepareItemElements", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/state/GuiRenderState;forEachItem(Ljava/util/function/Consumer;)V"))
    private Consumer<GuiItemRenderState> prepareItemElements(Consumer<GuiItemRenderState> consumer) {
        return renderState -> {
            LegacyGuiItemRenderState legacyRenderState = LegacyGuiItemRenderState.of(renderState);
            if (legacyRenderState.size() == 16 && legacyRenderState.opacity() == 1.0) consumer.accept(renderState);
            else guiItemRenderers.computeIfAbsent(((long) legacyRenderState.size() << 32) | (Float.floatToIntBits(LegacyOptions.enhancedItemTranslucency.get() ? 1.0f : legacyRenderState.opacity()) & 4294967295L), LegacyGuiItemRenderer::new).markValid();
        };
    }

    @Inject(method = "prepareItemElements", at = @At("RETURN"))
    private void prepareItemElements(CallbackInfo ci) {
        for (ObjectIterator<LegacyGuiItemRenderer> iter = guiItemRenderers.values().iterator(); iter.hasNext(); ) {
            var renderer = iter.next();
            if (renderer.isValid()) renderer.prepareItemElements(bufferSource, renderState, frameNumber);
            else {
                renderer.close();
                iter.remove();
            }
        }
    }

    @Inject(method = "close", at = @At("RETURN"), remap = false)
    private void close(CallbackInfo ci) {
        guiItemRenderers.forEach((i, renderer)-> renderer.close());
        guiEntityRenderers.forEach(PictureInPictureRenderer::close);
    }

    @Inject(method = "preparePictureInPicture", at = @At("HEAD"))
    void preparePictureInPicture(CallbackInfo ci) {
        for (GuiEntityRenderer guiEntityRenderer : guiEntityRenderers) {
            LegacyGuiEntityRenderer.of(guiEntityRenderer).available();
        }
    }

    @Inject(method = "preparePictureInPictureState", at = @At("HEAD"), cancellable = true/*? if neoforge {*//*, remap = false*//*?}*/)
    void preparePictureInPictureState(PictureInPictureRenderState arg, int i, /*? if neoforge {*//*boolean firstPass, CallbackInfoReturnable<Boolean> cir*//*?} else {*/CallbackInfo ci/*?}*/) {
        if (arg.getClass() == GuiEntityRenderState.class) {
            GuiEntityRenderer guiEntityRenderer = guiEntityRenderers.stream().map(LegacyGuiEntityRenderer::of).filter(LegacyGuiEntityRenderer::isAvailable).findFirst().map(a -> ((GuiEntityRenderer) a)).orElse(guiEntityRenderers.get(0));
            LegacyGuiEntityRenderer.of(guiEntityRenderer).use();
            guiEntityRenderer.prepare((GuiEntityRenderState) arg, this.renderState, i);
            //? if neoforge {
            /*cir.setReturnValue(true);
            *///?} else {
            ci.cancel();
            //?}
        }
    }
}
