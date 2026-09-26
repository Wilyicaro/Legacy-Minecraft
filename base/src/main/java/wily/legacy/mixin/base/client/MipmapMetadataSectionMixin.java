package wily.legacy.mixin.base.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.base.client.MipmapMetadataSection;
import wily.legacy.Legacy4J;
import wily.legacy.client.LegacyOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Mixin(value = MipmapMetadataSection.class, remap = false)
public class MipmapMetadataSectionMixin {
    @Unique
    private static final Set<String> LEGACY_LEAF_TEXTURES = Set.of(
            "block/oak_leaves", "block/spruce_leaves", "block/birch_leaves",
            "block/jungle_leaves", "block/acacia_leaves", "block/dark_oak_leaves");

    @Inject(method = "createFallback", at = @At("RETURN"), cancellable = true)
    private static void legacyLeafMipmaps(SpriteContents contents, int maxLevel, CallbackInfoReturnable<MipmapMetadataSection> cir) {
        Identifier sprite = contents.name();
        if (!LegacyOptions.legacyLeafMipmaps.get() || !cir.getReturnValue().levels().isEmpty()
                || !sprite.getNamespace().equals("minecraft") || !LEGACY_LEAF_TEXTURES.contains(sprite.getPath())) return;

        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        Optional<Resource> texture = resources.getResource(sprite.withPath(path -> "textures/" + path + ".png"));
        if (texture.isEmpty()) return;
        String pack = texture.get().sourcePackId();
        if (!pack.equals("vanilla") && !pack.equals("programmer_art")) return;

        String name = sprite.getPath().substring("block/".length());
        Map<Integer, MipmapMetadataSection.Level> levels = new HashMap<>();
        for (int level = 1; level <= maxLevel; level++) {
            Identifier mipmap = Legacy4J.createModLocation("textures/mipmap/" + name + "/" + level + ".png");
            if (resources.getResource(mipmap).isPresent()) levels.put(level, new MipmapMetadataSection.Level(mipmap));
        }
        if (!levels.isEmpty()) cir.setReturnValue(new MipmapMetadataSection(levels));
    }
}
