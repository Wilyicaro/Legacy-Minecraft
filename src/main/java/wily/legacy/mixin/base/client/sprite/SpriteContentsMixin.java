package wily.legacy.mixin.base.client.sprite;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.BedrockSnowyLeaves;

import java.lang.foreign.MemorySegment;

import static wily.legacy.util.alpha_bleeding.alpha_bleeding;
import static wily.legacy.util.alpha_bleeding.alpha_remove;


@Mixin(SpriteContents.class)
public class SpriteContentsMixin {
    @Inject(method = "<init>(Lnet/minecraft/resources/Identifier;Lnet/minecraft/client/resources/metadata/animation/FrameSize;Lcom/mojang/blaze3d/platform/NativeImage;Ljava/util/Optional;Ljava/util/List;Ljava/util/Optional;)V", at = @At("HEAD"))
    private static void init(CallbackInfo ci, @Local(name = "name", argsOnly = true) Identifier name, @Local(name = "image", argsOnly = true) NativeImage image) {
        if (!("minecraft".equals(name.getNamespace()) && name.getPath().startsWith("entity/chest/")) || image.format().components() != 4) return;
        MemorySegment imagePointer = MemorySegment.ofAddress(image.getPointer());
        alpha_bleeding(imagePointer, image.getWidth(), image.getHeight());
        alpha_remove(imagePointer, image.getWidth(), image.getHeight());
    }

    @Inject(method = "<init>(Lnet/minecraft/resources/Identifier;Lnet/minecraft/client/resources/metadata/animation/FrameSize;Lcom/mojang/blaze3d/platform/NativeImage;Ljava/util/Optional;Ljava/util/List;Ljava/util/Optional;)V", at = @At("HEAD"))
    private static void legacy$brightenSnowyLeaves(CallbackInfo ci, @Local(name = "name", argsOnly = true) Identifier name, @Local(name = "image", argsOnly = true) NativeImage image) {
        BedrockSnowyLeaves.brighten(name, image);
    }
}
