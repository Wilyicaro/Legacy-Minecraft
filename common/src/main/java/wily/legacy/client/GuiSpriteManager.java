
package wily.legacy.client;

import java.util.Set;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.TextureAtlasHolder;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;


public class GuiSpriteManager extends TextureAtlasHolder {
    public static final Set<MetadataSectionSerializer<?>> GUI_METADATA_SECTIONS = Set.of(AnimationMetadataSection.SERIALIZER, GuiMetadataSection.TYPE);
    public static final Set<MetadataSectionSerializer<?>> DEFAULT_METADATA_SECTIONS = Set.of(AnimationMetadataSection.SERIALIZER);

    public GuiSpriteManager(TextureManager arg) {
        super(arg, new ResourceLocation("textures/atlas/gui.png"), new ResourceLocation("gui"));
    }

    @Override
    public TextureAtlasSprite getSprite(ResourceLocation resourceLocation) {
        return super.getSprite(resourceLocation);
    }

    public GuiSpriteScaling getSpriteScaling(TextureAtlasSprite arg) {
        return this.getMetadata(arg).scaling();
    }

    private GuiMetadataSection getMetadata(TextureAtlasSprite arg) {
        return ((LegacySpriteContents)arg.contents()).metadata().getSection(GuiMetadataSection.TYPE).orElse(GuiMetadataSection.DEFAULT);
    }

}
