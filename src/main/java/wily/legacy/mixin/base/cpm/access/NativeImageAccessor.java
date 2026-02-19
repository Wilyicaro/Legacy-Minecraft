package wily.legacy.mixin.base.cpm.access;

// Accessor for NativeImage bits used by CPM texture code.

import com.mojang.blaze3d.platform.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

@Mixin(NativeImage.class)
public interface NativeImageAccessor {
    @Invoker
    boolean callWriteToChannel(final WritableByteChannel writableByteChannel) throws IOException;
}
