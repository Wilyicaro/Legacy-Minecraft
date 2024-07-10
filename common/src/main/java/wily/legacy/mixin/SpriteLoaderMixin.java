package wily.legacy.mixin;

import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.GuiMetadataSection;
import wily.legacy.client.LegacySpriteContents;

import java.io.IOException;
import java.util.Optional;

@Mixin(SpriteLoader.class)
public class SpriteLoaderMixin {
    @Shadow @Final private static Logger LOGGER;

    @Inject(method = "loadSprite", at = @At("RETURN"))
    private static void loadSprite(ResourceLocation resourceLocation, Resource resource, CallbackInfoReturnable<SpriteContents> cir) {
        if (cir.getReturnValue() != null) {
            try {
                GuiMetadataSection section = resource.metadata().getSection(GuiMetadataSection.TYPE).orElse(null);
                ((LegacySpriteContents) cir.getReturnValue()).setMetadata(new ResourceMetadata() {
                    @Override
                    public <T> Optional<T> getSection(MetadataSectionSerializer<T> metadataSectionSerializer) {
                        return metadataSectionSerializer == GuiMetadataSection.TYPE ? (Optional<T>) Optional.ofNullable(section) : Optional.empty();
                    }
                });
            } catch (IOException e) {
            }
        }
    }
}
