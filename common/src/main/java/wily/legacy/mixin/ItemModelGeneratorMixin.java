package wily.legacy.mixin;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4JClient;
import wily.legacy.Legacy4JPlatform;
import wily.legacy.client.LegacyOption;

import java.util.List;
import java.util.Map;

@Mixin(ItemModelGenerator.class)
public class ItemModelGeneratorMixin {
    @Inject(method = "processFrames", at = @At("HEAD"), cancellable = true)
    private void processFrames(int i, String string, SpriteContents spriteContents, CallbackInfoReturnable<List<BlockElement>> cir) {
        if (!LegacyOption.enhancedItemModel.get() || Legacy4JPlatform.isModLoaded("sodium")) return;
        int width = spriteContents.width();
        int height = spriteContents.height();
        float xFactor = 16.0F / width;
        float yFactor = 16.0F / height;
        List<BlockElement> elements = Lists.newArrayList();
        List<Legacy4JClient.ItemPart> parts = Lists.newArrayList();
        for (int x = 0; x < width; x++) {
            l : for (int y = 0; y < height; y++) {
                if (Legacy4JClient.ItemPart.isTransparent(spriteContents, x, y, width, height)) continue;
                for (Legacy4JClient.ItemPart part : parts) {
                    if (part.contains(x,y)) continue l;
                    else if (part.contains(x,y-1)){
                        part.height++;
                        continue l;
                    }
                }
                Legacy4JClient.ItemPart part = new Legacy4JClient.ItemPart(x,y,1,1);


                parts.add(part);
            }
        }

        for (int i1 = 0; i1 < parts.size() - 1; i1++) {
            Legacy4JClient.ItemPart p = parts.get(i1);
            for (int i2 = i1 + 1; i2 < parts.size(); i2++) {
                Legacy4JClient.ItemPart p1 = parts.get(i2);
                if (p.height == p1.height && p.y == p1.y && p.x + p.width == p1.x) {
                    p.width++;
                    parts.remove(i2);
                    break;
                }
            }
        }

        parts.forEach(p->{
            parts.forEach(p::collideWith);
            Map<Direction, BlockElementFace> map = Maps.newHashMap();
            float relX = p.x*xFactor;
            float relY = p.y*yFactor;
            float relWidth = p.width*xFactor;
            float relHeight = p.height*yFactor;
            p.forEach(d->map.put(d,switch (d){
                case UP, WEST, EAST, SOUTH -> new BlockElementFace(null, i, string, new BlockFaceUV(new float[]{relX, relY, relX + (d.getAxis() == Direction.Axis.Z || d.getAxis() == Direction.Axis.Y ? relWidth : xFactor), relY + (d.getAxis() == Direction.Axis.Z || d.getAxis() == Direction.Axis.X ? relHeight : yFactor)}, 0));
                case DOWN -> new BlockElementFace(null, i, string, new BlockFaceUV(new float[]{relX, relY + relHeight - yFactor, relX + relWidth , relY + relHeight}, 0));
                case NORTH -> new BlockElementFace(null, i, string, new BlockFaceUV(new float[]{relX + relWidth, relY, relX, relY + relHeight}, 0));
            }));
            elements.add(new BlockElement(new Vector3f(relX, 16 - (relY + relHeight), 7.5F), new Vector3f(relX + relWidth, 16 - relY, 8.5F), map, null, true));
        });
        cir.setReturnValue(elements);
    }

}
