package wily.legacy.mixin.base.waterlogging;

import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(StateDefinition.Builder.class)
public interface StateDefinitionBuilderAccessor {
    @Accessor("properties")
    Map<String, Property<?>> legacy$getProperties();
}
