package wily.legacy.mixin.base;

//? if <1.20.5 {
/*import com.mojang.blaze3d.vertex.BufferBuilder;
*///?} else {
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import java.util.SequencedMap;
//?}
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(MultiBufferSource.BufferSource.class)
public interface BufferSourceAccessor {
    @Accessor(/*? if <1.20.5 {*//*"builder"*//*?} else {*/"sharedBuffer"/*?}*/)
    /*? if <1.20.5 {*//*BufferBuilder*//*?} else {*/ByteBufferBuilder/*?}*/ buffer();
    @Accessor("fixedBuffers")
    /*? if <1.20.5 {*//*Map<RenderType, BufferBuilder>*//*?} else {*/SequencedMap<RenderType, ByteBufferBuilder>/*?}*/ fixedBuffers();
}
