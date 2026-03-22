package wily.legacy.mixin.base;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.MapPostProcessing;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(MapId.class)
public abstract class MapIdMixin {
    @Inject(method = "addToTooltip", at = @At("HEAD"), cancellable = true)
    private void addToTooltip(Item.TooltipContext tooltipContext, Consumer<Component> consumer, TooltipFlag tooltipFlag, DataComponentGetter dataComponentGetter, CallbackInfo ci) {
        MapItemSavedData mapItemSavedData = tooltipContext.mapData((MapId) (Object) this);
        if (mapItemSavedData == null) {
            consumer.accept(Component.translatable("filled_map.unknown").withStyle(ChatFormatting.GRAY));
            ci.cancel();
            return;
        }
        MapPostProcessing mapPostProcessing = dataComponentGetter.get(DataComponents.MAP_POST_PROCESSING);
        if (mapItemSavedData.locked || mapPostProcessing == MapPostProcessing.LOCK) {
            consumer.accept(Component.translatable("filled_map.locked").withStyle(ChatFormatting.GRAY));
        }
        consumer.accept(Component.translatable("filled_map.level", Math.min(mapItemSavedData.scale + (mapPostProcessing == MapPostProcessing.SCALE ? 1 : 0), 4), 4).withStyle(ChatFormatting.GRAY));
        ci.cancel();
    }
}
